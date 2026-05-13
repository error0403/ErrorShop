package com.pixelserver.errorshop

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import com.xbaimiao.easylib.EasyPlugin
import java.io.File
import java.util.UUID
import kotlin.math.max

class ErrorShopPlugin : EasyPlugin(), Listener {
    private val shops = mutableMapOf<String, ShopConfig>()
    private val menus = mutableMapOf<String, MenuConfig>()
    private val market = MarketStore()
    private lateinit var economy: EconomyBridge

    override fun enable() {
        saveDefaultConfig()
        saveResourceIfMissing("lang.yml")
        saveResourceIfMissing("database.yml")
        saveResourceIfMissing("help/guide_zh.yml")
        saveResourceIfMissing("help/guide_en.yml")
        saveResourceIfMissing("shops/default.yml")
        saveResourceIfMissing("menus/main.yml")
        economy = EconomyBridge(this)
        reloadAll()
        Bukkit.getPluginManager().registerEvents(this, this)
        logger.info("ErrorShop enabled with ${shops.size} shops and ${menus.size} menus")
    }

    fun reloadAll() {
        reloadConfig(); economy.setup(); loadShops(); loadMenus(); market.load(dataFolder, YamlConfiguration.loadConfiguration(File(dataFolder, "database.yml")))
    }

    private fun saveResourceIfMissing(path: String) {
        val file = File(dataFolder, path)
        if (!file.exists()) saveResource(path, false)
    }

    private fun loadShops() {
        shops.clear(); val dir = File(dataFolder, "shops"); dir.mkdirs()
        dir.listFiles { f -> f.isFile && f.extension.equals("yml", true) }?.forEach { file ->
            val id = file.nameWithoutExtension
            shops[id] = ShopConfig.from(id, YamlConfiguration.loadConfiguration(file))
        }
    }

    private fun loadMenus() {
        menus.clear(); val dir = File(dataFolder, "menus"); dir.mkdirs()
        dir.listFiles { f -> f.isFile && f.extension.equals("yml", true) }?.forEach { file ->
            val id = file.nameWithoutExtension
            menus[id] = MenuConfig.from(id, YamlConfiguration.loadConfiguration(file))
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) { help(sender); return true }
        when (args[0].lowercase()) {
            "reload" -> { if (!sender.hasPermission("errorshop.reload")) return deny(sender); reloadAll(); sender.sendMessage(msg("reloaded")); return true }
            "shop" -> { val p = sender as? Player ?: return playerOnly(sender); openShop(p, args.getOrNull(1) ?: (config.getString("settings.default-shop") ?: "default")); return true }
            "market" -> { val p = sender as? Player ?: return playerOnly(sender); if (!p.hasPermission("errorshop.market.buy")) return deny(p); openMarket(p); return true }
            "sell" -> { sellToMarket(sender, args); return true }
            "menu" -> { val p = sender as? Player ?: return playerOnly(sender); openMenu(p, args.getOrNull(1) ?: (config.getString("settings.default-menu") ?: "main")); return true }
            else -> sender.sendMessage(msg("unknown-command"))
        }
        return true
    }

    private fun help(sender: CommandSender) {
        sender.sendMessage(color("&6/errorshop shop <id> &7打开官方商店"))
        sender.sendMessage(color("&6/errorshop market &7打开全球市场"))
        sender.sendMessage(color("&6/errorshop sell <price> &7上架手持物品"))
        sender.sendMessage(color("&6/errorshop menu <id> &7打开自定义菜单"))
        sender.sendMessage(color("&6/errorshop reload &7重载配置"))
    }

    private fun sellToMarket(sender: CommandSender, args: Array<out String>) {
        val p = sender as? Player ?: run { playerOnly(sender); return }
        if (!p.hasPermission("errorshop.market.sell")) { deny(p); return }
        val price = args.getOrNull(1)?.toDoubleOrNull()?.takeIf { it > 0 } ?: run { p.sendMessage(msg("invalid-price")); return }
        val item = p.inventory.itemInMainHand
        if (item.type.isAir) { p.sendMessage(msg("empty-hand")); return }
        val limit = config.getInt("market.max-listings-per-player", 20)
        if (market.countBySeller(p.uniqueId) >= limit) { p.sendMessage(msg("listing-limit")); return }
        market.add(p.uniqueId, p.name, item.clone(), price)
        item.amount = 0
        p.sendMessage(msg("sell-success", mapOf("price" to price.toString())))
    }

    private fun openShop(player: Player, id: String) {
        val shop = shops[id] ?: run { player.sendMessage(msg("shop-not-found")); return }
        if (!player.hasPermission(shop.permission ?: "errorshop.shop.$id")) { deny(player); return }
        val inv = Bukkit.createInventory(null, 54, color(shop.title))
        shop.items.values.take(45).forEachIndexed { idx, item -> inv.setItem(idx, item.toIcon()) }
        player.openInventory(inv); openSessions[player.uniqueId] = OpenSession.Shop(id)
    }

    private fun openMarket(player: Player) {
        val listings = market.listings()
        val inv = Bukkit.createInventory(null, 54, color("&b全球市场"))
        if (listings.isEmpty()) player.sendMessage(msg("market-empty"))
        listings.take(45).forEachIndexed { idx, listing -> inv.setItem(idx, listing.icon()) }
        player.openInventory(inv); openSessions[player.uniqueId] = OpenSession.Market
    }

    private fun openMenu(player: Player, id: String) {
        val menu = menus[id] ?: run { player.sendMessage(msg("menu-not-found")); return }
        if (!player.hasPermission("errorshop.menu.$id")) { deny(player); return }
        val rows = max(1, menu.layout.size).coerceAtMost(6)
        val inv = Bukkit.createInventory(null, rows * 9, color(menu.title))
        menu.layout.forEachIndexed { row, line -> line.take(9).forEachIndexed { col, key -> menu.items[key]?.let { inv.setItem(row * 9 + col, it.toIcon()) } } }
        player.openInventory(inv); openSessions[player.uniqueId] = OpenSession.Menu(id)
    }

    @EventHandler fun onClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val session = openSessions[player.uniqueId] ?: return
        if (!isErrorShopInventory(player, session, event.view.topInventory)) {
            openSessions.remove(player.uniqueId)
            return
        }
        event.isCancelled = true
        if (event.view.topInventory != event.clickedInventory) return
        when (session) {
            is OpenSession.Shop -> handleShopClick(player, session.id, event.currentItem ?: return)
            OpenSession.Market -> handleMarketClick(player, event.slot)
            is OpenSession.Menu -> handleMenuClick(player, session.id, event.slot, event.click.isRightClick)
        }
    }

    @EventHandler fun onClose(event: InventoryCloseEvent) {
        openSessions.remove(event.player.uniqueId)
    }

    @EventHandler fun onJoin(event: PlayerJoinEvent) {
        payPendingEarnings(event.player)
    }

    private fun handleShopClick(player: Player, shopId: String, clicked: ItemStack) {
        val shop = shops[shopId] ?: return
        val match = shop.items.values.firstOrNull { it.material == clicked.type } ?: return
        val item = ItemStack(match.material, match.amount)
        val meta = item.itemMeta
        if (meta != null) { meta.setDisplayName(color(match.name ?: match.material.name)); item.itemMeta = meta }
        if (!economy.available()) { player.sendMessage(msg("vault-missing")); return }
        if (match.buy > 0 && !economy.withdraw(player, match.buy)) { player.sendMessage(msg("not-enough-money")); return }
        if (!hasSpaceFor(player, item)) {
            if (match.buy > 0) economy.deposit(player, match.buy)
            player.sendMessage(msg("inventory-full"))
            return
        }
        player.inventory.addItem(item)
        player.sendMessage(msg("buy-success", mapOf("item" to match.material.name, "amount" to match.amount.toString(), "price" to match.buy.toString())))
    }

    private fun handleMarketClick(player: Player, slot: Int) {
        val listing = market.listings().getOrNull(slot) ?: return
        if (listing.seller == player.uniqueId) { player.sendMessage(msg("market-own-item")); return }
        if (!economy.available()) { player.sendMessage(msg("vault-missing")); return }
        if (!hasSpaceFor(player, listing.item)) { player.sendMessage(msg("inventory-full")); return }
        if (listing.price > 0 && !economy.withdraw(player, listing.price)) { player.sendMessage(msg("not-enough-money")); return }
        market.remove(listing.id)
        val seller = Bukkit.getPlayer(listing.seller)
        if (seller != null) {
            if (!economy.deposit(seller, listing.price)) market.addPendingEarning(listing.seller, listing.price)
        } else {
            market.addPendingEarning(listing.seller, listing.price)
        }
        player.inventory.addItem(listing.item.clone())
        player.sendMessage(msg("market-bought", mapOf("price" to listing.price.toString())))
    }

    private fun hasSpaceFor(player: Player, item: ItemStack): Boolean {
        var remaining = item.amount
        player.inventory.storageContents.forEach { slot ->
            if (remaining <= 0) return true
            if (slot == null || slot.type.isAir) {
                remaining -= item.maxStackSize
            } else if (slot.isSimilar(item)) {
                remaining -= (slot.maxStackSize - slot.amount).coerceAtLeast(0)
            }
        }
        return remaining <= 0
    }

    private fun payPendingEarnings(player: Player) {
        if (!economy.available()) return
        val amount = market.takePendingEarning(player.uniqueId)
        if (amount <= 0) return
        if (economy.deposit(player, amount)) {
            player.sendMessage(msg("pending-earnings-paid", mapOf("amount" to amount.toString())))
        } else {
            market.restorePendingEarning(player.uniqueId, amount)
        }
    }

    private fun isErrorShopInventory(player: Player, session: OpenSession, inventory: org.bukkit.inventory.Inventory): Boolean {
        if (player.openInventory.topInventory != inventory) return false
        val expectedTitle = when (session) {
            is OpenSession.Shop -> shops[session.id]?.title ?: return false
            OpenSession.Market -> "&b全球市场"
            is OpenSession.Menu -> menus[session.id]?.title ?: return false
        }
        return player.openInventory.title == color(expectedTitle)
    }

    private fun handleMenuClick(player: Player, menuId: String, slot: Int, right: Boolean) {
        val menu = menus[menuId] ?: return
        val key = menu.layout.getOrNull(slot / 9)?.getOrNull(slot % 9) ?: return
        val item = menu.items[key] ?: return
        val actions = if (right && item.right.isNotEmpty()) item.right else if (item.left.isNotEmpty()) item.left else item.all
        actions.forEach { runAction(player, it) }
    }

    private fun runAction(player: Player, raw: String) {
        val action = raw.trim()
        when {
            action.startsWith("[console]", true) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.removePrefixIgnoreCase("[console]").trim().replace("%player%", player.name))
            action.startsWith("[player]", true) -> player.performCommand(action.removePrefixIgnoreCase("[player]").trim())
            action.startsWith("[tell]", true) -> player.sendMessage(color(action.removePrefixIgnoreCase("[tell]").trim()))
            action.startsWith("[shop]", true) -> openShop(player, action.removePrefixIgnoreCase("[shop]").trim())
            action.startsWith("[market]", true) -> openMarket(player)
            action.startsWith("[menu]", true) -> openMenu(player, action.removePrefixIgnoreCase("[menu]").trim())
        }
    }

    private fun String.removePrefixIgnoreCase(prefix: String): String = if (startsWith(prefix, true)) substring(prefix.length) else this
    private fun playerOnly(sender: CommandSender): Boolean { sender.sendMessage(msg("player-only")); return true }
    private fun deny(sender: CommandSender): Boolean { sender.sendMessage(msg("no-permission")); return true }
    private fun msg(key: String, vars: Map<String, String> = emptyMap()): String {
        var text = YamlConfiguration.loadConfiguration(File(dataFolder, "lang.yml")).getString(key) ?: key
        val prefix = YamlConfiguration.loadConfiguration(File(dataFolder, "lang.yml")).getString("prefix") ?: ""
        vars.forEach { (k, v) -> text = text.replace("{$k}", v) }
        return color(prefix + text)
    }
    private fun color(s: String): String = ChatColor.translateAlternateColorCodes('&', s)
    companion object { val openSessions = mutableMapOf<UUID, OpenSession>() }
}

sealed class OpenSession { data class Shop(val id: String): OpenSession(); object Market: OpenSession(); data class Menu(val id: String): OpenSession() }
