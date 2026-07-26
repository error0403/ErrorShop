package com.pixelserver.errorshop

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
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
import com.xbaimiao.easylib.skedule.SynchronizationContext
import com.xbaimiao.easylib.skedule.launchCoroutine
import com.google.gson.Gson
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisPubSub
import kotlin.concurrent.thread
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class ErrorShopPlugin : EasyPlugin(), Listener {
    private val shops = mutableMapOf<String, ShopConfig>()
    private val menus = mutableMapOf<String, MenuConfig>()
    private lateinit var market: MarketBackend
    private var marketGroups: List<MarketGroup> = emptyList()
    private lateinit var clusterSettings: ClusterSettings
    private lateinit var economy: EconomyBridge
    @Volatile
    private var jedisPool: JedisPool? = null
    @Volatile
    private var subscriber: JedisPubSub? = null
    private val gson = Gson()
    private val pendingMarketCancellations = mutableMapOf<UUID, PendingMarketCancellation>()
    private val marketOperations = ConcurrentHashMap.newKeySet<UUID>()
    private val reloadInProgress = AtomicBoolean(false)
    @Volatile
    private var marketReady = false
    @Volatile
    private var lang = YamlConfiguration()
    private var priceSettings = MarketPriceSettings.defaults()
    private val defaultLang: YamlConfiguration by lazy {
        getTextResource("lang.yml")?.use { YamlConfiguration.loadConfiguration(it) } ?: YamlConfiguration()
    }

    override fun enable() {
        saveDefaultConfig()
        saveResourceIfMissing("lang.yml")
        saveResourceIfMissing("database.yml")
        saveResourceIfMissing("help/guide_zh.yml")
        saveResourceIfMissing("help/guide_en.yml")
        saveResourceIfMissing("shops/default.yml")
        saveResourceIfMissing("menus/main.yml")
        economy = EconomyBridge(this)
        Bukkit.getPluginManager().registerEvents(this, this)
        reloadAll()
    }

    private fun reloadAll(notify: CommandSender? = null): Boolean {
        if (marketOperations.isNotEmpty()) {
            notify?.let { sendText(it, msg("reload-market-busy")) }
            return false
        }
        if (!reloadInProgress.compareAndSet(false, true)) {
            notify?.let { sendText(it, msg("reload-in-progress")) }
            return false
        }
        marketReady = false
        val candidate: MarketBackend
        val settings: ClusterSettings
        try {
            reloadConfig()
            economy.setup()
            loadLang()
            loadShops()
            loadMenus()
            loadMarketSettings()
            settings = clusterSettings
            candidate = createMarketBackend(settings)
        } catch (error: Throwable) {
            reloadInProgress.set(false)
            logger.severe("ErrorShop configuration reload failed: ${error.message}")
            notify?.let { sendText(it, msg("reload-failed")) }
            return false
        }
        notify?.let { sendText(it, msg("reload-started")) }
        launchCoroutine(SynchronizationContext.ASYNC) {
            val loaded = loadMarketBackend(candidate, settings)
            setupRedisBus(settings)
            switchContext(SynchronizationContext.SYNC)
            try {
                if (loaded != null) {
                    market = loaded
                    marketReady = true
                    logger.info("ErrorShop ready with ${shops.size} shops and ${menus.size} menus; market=${settings.marketBackend}")
                    notify?.let { sendText(it, msg("reloaded")) }
                    server.onlinePlayers.forEach { settlePlayer(it) }
                } else {
                    logger.severe("ErrorShop market is unavailable; player market operations remain disabled")
                    notify?.let { sendText(it, msg("reload-failed")) }
                }
            } finally {
                reloadInProgress.set(false)
            }
        }
        return true
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


    private fun loadMarketBackend(candidate: MarketBackend, settings: ClusterSettings): MarketBackend? {
        val database = YamlConfiguration.loadConfiguration(File(dataFolder, "database.yml"))
        return runCatching {
            candidate.load(dataFolder, database)
            candidate
        }.getOrElse { error ->
            logger.warning("Market backend load failed: ${error.message}")
            if (settings.enabled && settings.marketBackend.equals("mysql", true) && !settings.failPolicy.equals("disable-market", true)) {
                logger.warning("Falling back to local YAML market backend because fail-policy=${settings.failPolicy}")
                runCatching { MarketStore().also { it.load(dataFolder, database) } }
                    .onFailure { logger.severe("Local market fallback failed: ${it.message}") }
                    .getOrNull()
            } else {
                null
            }
        }
    }

    private fun loadMarketSettings() {
        clusterSettings = ClusterSettings(
            enabled = config.getBoolean("cluster.enabled", false),
            group = config.getString("cluster.group") ?: "default",
            serverId = config.getString("cluster.server-id") ?: server.name,
            redisEnabled = config.getBoolean("cluster.redis.enabled", false),
            redisHost = config.getString("cluster.redis.host") ?: "localhost",
            redisPort = config.getInt("cluster.redis.port", 6379),
            redisPassword = config.getString("cluster.redis.password")?.takeIf { it.isNotBlank() },
            redisDatabase = config.getInt("cluster.redis.database", 0),
            redisChannel = config.getString("cluster.redis.channel") ?: "errorshop:market",
            marketBackend = config.getString("market.backend") ?: "local",
            failPolicy = config.getString("market.fail-policy") ?: "disable-market",
            mysql = MysqlSettings(
                host = config.getString("database.mysql.host") ?: "localhost",
                port = config.getInt("database.mysql.port", 3306),
                database = config.getString("database.mysql.database") ?: "errorshop",
                username = config.getString("database.mysql.username") ?: "root",
                password = config.getString("database.mysql.password") ?: "",
                tablePrefix = config.getString("database.mysql.table-prefix") ?: "errorshop_",
                useSsl = config.getBoolean("database.mysql.use-ssl", false)
            )
        )
        val minimumPrice = configDecimal("market.min-price", "0.01").max(BigDecimal.ZERO)
        val maximumPrice = configDecimal("market.max-price", "1000000000").max(minimumPrice)
        priceSettings = MarketPriceSettings(
            minimum = minimumPrice,
            maximum = maximumPrice,
            decimals = config.getInt("market.price-decimals", 2).coerceIn(0, 8),
            taxRate = configDecimal("market.tax-rate", "0").coerceIn(BigDecimal.ZERO, BigDecimal.ONE)
        )
        val section = config.getConfigurationSection("market.groups")
        marketGroups = section?.getKeys(false)?.map { id ->
            val path = "market.groups.$id"
            MarketGroup(
                id = id,
                permission = config.getString("$path.permission"),
                slots = config.getInt("$path.slots", config.getInt("market.max-listings-per-player", 20))
            )
        }?.filter { it.slots > 0 } ?: listOf(MarketGroup("default", null, config.getInt("market.max-listings-per-player", 20)))
    }

    private fun marketLimit(player: Player): Int {
        if (marketGroups.isEmpty()) return config.getInt("market.max-listings-per-player", 20)
        return marketGroups.filter { it.permission.isNullOrBlank() || player.hasPermission(it.permission) }
            .maxOfOrNull { it.slots } ?: config.getInt("market.max-listings-per-player", 20)
    }

    private fun createMarketBackend(settings: ClusterSettings): MarketBackend {
        return if (settings.enabled && settings.marketBackend.equals("mysql", true)) {
            MysqlMarketBackend(settings.mysql, config.getLong("market.reservation-timeout-seconds", 60))
        } else {
            MarketStore()
        }
    }

    private fun setupRedisBus(settings: ClusterSettings) {
        val oldSubscriber = subscriber
        if (oldSubscriber != null) runCatching { oldSubscriber.unsubscribe() }
        subscriber = null
        runCatching { jedisPool?.close() }
        jedisPool = null
        if (!settings.enabled || !settings.redisEnabled) return
        runCatching {
            val pool = if (settings.redisPassword != null) {
                JedisPool(JedisPoolConfig(), settings.redisHost, settings.redisPort, 2000, settings.redisPassword, settings.redisDatabase)
            } else {
                JedisPool(JedisPoolConfig(), settings.redisHost, settings.redisPort, 2000, null, settings.redisDatabase)
            }
            pool.resource.use { it.ping() }
            jedisPool = pool
            val sub = object : JedisPubSub() {
                override fun onMessage(channel: String, message: String) {
                    runCatching {
                        val event = gson.fromJson(message, MarketEvent::class.java)
                        if (event.originServer == settings.serverId) return
                        if (event.group != settings.group) return
                        logger.info("[cluster:${event.group}] received ${event.type} listing=${event.listingId} from ${event.originServer}")
                    }.onFailure { logger.warning("Invalid market redis event: ${it.message}") }
                }
            }
            subscriber = sub
            thread(name = "ErrorShop-Redis-Market", isDaemon = true) {
                runCatching { pool.resource.use { it.subscribe(sub, settings.redisChannel) } }
                    .onFailure { logger.warning("Redis market subscriber stopped: ${it.message}") }
            }
            logger.info("ErrorShop market Redis bus connected to ${settings.redisHost}:${settings.redisPort}/${settings.redisDatabase} channel=${settings.redisChannel}")
        }.onFailure {
            logger.warning("Redis market bus unavailable: ${it.message}")
            if (settings.failPolicy.equals("disable-market", true)) logger.warning("Cluster market events disabled until reload.")
        }
    }

    private fun publishMarketEvent(type: String, listingId: String? = null) {
        if (!::clusterSettings.isInitialized || !clusterSettings.enabled || !clusterSettings.redisEnabled) return
        val pool = jedisPool ?: return
        val event = MarketEvent(type = type, originServer = clusterSettings.serverId, group = clusterSettings.group, listingId = listingId)
        runCatching { pool.resource.use { it.publish(clusterSettings.redisChannel, gson.toJson(event)) } }
            .onFailure { logger.warning("Failed to publish market event $type: ${it.message}") }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) { help(sender); return true }
        when (args[0].lowercase()) {
            "reload" -> { if (!sender.hasPermission("errorshop.reload")) return deny(sender); reloadAll(sender); return true }
            "shop" -> { val p = sender as? Player ?: return playerOnly(sender); openShop(p, args.getOrNull(1) ?: (config.getString("settings.default-shop") ?: "default")); return true }
            "market" -> {
                val p = sender as? Player ?: return playerOnly(sender)
                if (!p.hasPermission("errorshop.market.buy") && !p.hasPermission("errorshop.market.cancel")) return deny(p)
                openMarket(p, query = args.drop(1).joinToString(" ").trim())
                return true
            }
            "sell" -> { sellToMarket(sender, args); return true }
            "claim" -> {
                val p = sender as? Player ?: return playerOnly(sender)
                if (!p.hasPermission("errorshop.market.claim")) return deny(p)
                settlePlayer(p, manual = true)
                return true
            }
            "menu" -> { val p = sender as? Player ?: return playerOnly(sender); openMenu(p, args.getOrNull(1) ?: (config.getString("settings.default-menu") ?: "main")); return true }
            else -> sendText(sender, msg("unknown-command"))
        }
        return true
    }

    private fun help(sender: CommandSender) {
        sendText(sender, "&6/errorshop shop <id> &7打开官方商店")
        sendText(sender, "&6/errorshop market [关键词] &7打开或搜索全球市场")
        sendText(sender, "&6/errorshop sell <price> &7上架手持物品")
        sendText(sender, "&6/errorshop claim &7领取背包满时暂存的物品")
        sendText(sender, "&6/errorshop menu <id> &7打开自定义菜单")
        sendText(sender, "&6/errorshop reload &7重载配置")
    }

    private fun sellToMarket(sender: CommandSender, args: Array<out String>) {
        val player = sender as? Player ?: run { playerOnly(sender); return }
        if (!player.hasPermission("errorshop.market.sell")) { deny(player); return }
        val price = parseMarketPrice(args.getOrNull(1)) ?: run {
            sendText(player, msg("invalid-price", priceVariables()))
            return
        }
        val listedItem = player.inventory.itemInMainHand.clone()
        if (listedItem.type.isAir) { sendText(player, msg("empty-hand")); return }
        if (!beginMarketOperation(player)) return
        val backend = market
        val playerId = player.uniqueId
        val playerName = player.name
        val limit = marketLimit(player)
        launchCoroutine(SynchronizationContext.ASYNC, entity = player) {
            var removedFromHand = false
            var listingCreated = false
            try {
                val currentCount = backend.countBySeller(playerId)
                switchContext(SynchronizationContext.SYNC)
                if (!player.isOnline) return@launchCoroutine
                if (currentCount >= limit) {
                    sendText(player, msg("listing-limit", mapOf("limit" to limit.toString())))
                    return@launchCoroutine
                }
                val current = player.inventory.itemInMainHand
                if (current.type.isAir || !current.isSimilar(listedItem) || current.amount != listedItem.amount) {
                    sendText(player, msg("market-item-changed"))
                    return@launchCoroutine
                }
                player.inventory.setItemInMainHand(ItemStack(Material.AIR))
                removedFromHand = true
                switchContext(SynchronizationContext.ASYNC)
                val listingId = backend.add(playerId, playerName, listedItem, price)
                    ?: error("Market backend did not return a listing ID")
                listingCreated = true
                publishMarketEvent("LISTING_CREATED", listingId)
                val amounts = saleAmounts(price)
                switchContext(SynchronizationContext.SYNC)
                if (player.isOnline) {
                    sendText(player, msg("sell-success", saleVariables(listedItem, playerName, playerName, amounts) + ("id" to listingId)))
                }
            } catch (error: Throwable) {
                logger.warning("Market listing creation failed for $playerId: ${error.message}")
                switchContext(SynchronizationContext.SYNC)
                if (removedFromHand && !listingCreated) {
                    if (player.isOnline) {
                        giveItem(player, listedItem)
                    } else {
                        switchContext(SynchronizationContext.ASYNC)
                        runCatching { backend.queueDelivery(playerId, listedItem) }
                            .onFailure { logger.severe("Failed to queue returned listing item for $playerId: ${it.message}") }
                        switchContext(SynchronizationContext.SYNC)
                    }
                }
                if (player.isOnline) sendText(player, msg("market-cluster-unavailable"))
            } finally {
                marketOperations.remove(playerId)
            }
        }
    }

    private fun openShop(player: Player, id: String) {
        val shop = shops[id] ?: run { sendText(player, msg("shop-not-found")); return }
        if (!player.hasPermission(shop.permission ?: "errorshop.shop.$id")) { deny(player); return }
        val inv = Bukkit.createInventory(null, 54, color(shop.title))
        val itemKeys = linkedMapOf<Int, String>()
        shop.items.entries.take(45).forEachIndexed { slot, entry ->
            inv.setItem(slot, entry.value.toIcon())
            itemKeys[slot] = entry.key
        }
        player.openInventory(inv)
        openSessions[player.uniqueId] = OpenSession.Shop(id, itemKeys, shop.title)
    }

    private fun openMarket(player: Player, page: Int = 0, query: String = "") {
        if (!player.hasPermission("errorshop.market.buy") && !player.hasPermission("errorshop.market.cancel")) { deny(player); return }
        if (!beginMarketOperation(player)) return
        val backend = market
        val playerId = player.uniqueId
        val safeQuery = query.trim().take(64)
        launchCoroutine(SynchronizationContext.ASYNC, entity = player) {
            try {
                val filtered = backend.listings().filter { listingMatches(it, safeQuery) }
                val totalPages = max(1, (filtered.size + MARKET_PAGE_SIZE - 1) / MARKET_PAGE_SIZE)
                val safePage = page.coerceIn(0, totalPages - 1)
                val pageListings = filtered.drop(safePage * MARKET_PAGE_SIZE).take(MARKET_PAGE_SIZE)
                switchContext(SynchronizationContext.SYNC)
                if (!player.isOnline) return@launchCoroutine
                val title = marketTitle(safePage, totalPages)
                val inventory = Bukkit.createInventory(null, 54, color(title))
                val listingIds = linkedMapOf<Int, String>()
                val snapshots = linkedMapOf<String, MarketListing>()
                pageListings.forEachIndexed { slot, listing ->
                    inventory.setItem(slot, listing.icon(playerId, formatMoney(listing.price), isValidMarketPrice(listing.price)))
                    listingIds[slot] = listing.id
                    snapshots[listing.id] = listing
                }
                if (safePage > 0) inventory.setItem(PREVIOUS_PAGE_SLOT, menuIcon(Material.ARROW, "&e上一页", listOf("&7点击前往第 $safePage 页")))
                inventory.setItem(PAGE_INFO_SLOT, menuIcon(Material.PAPER, "&b市场分页", listOf(
                    "&7页码: &f${safePage + 1}&7/&f$totalPages",
                    "&7结果: &f${filtered.size}",
                    if (safeQuery.isBlank()) "&7搜索: &f无" else "&7搜索: &f$safeQuery"
                )))
                if (safePage + 1 < totalPages) inventory.setItem(NEXT_PAGE_SLOT, menuIcon(Material.ARROW, "&e下一页", listOf("&7点击前往第 ${safePage + 2} 页")))
                player.openInventory(inventory)
                openSessions[playerId] = OpenSession.Market(safePage, safeQuery, totalPages, listingIds, snapshots, title)
                if (filtered.isEmpty()) sendText(player, msg(if (safeQuery.isBlank()) "market-empty" else "market-no-results", mapOf("query" to safeQuery)))
            } catch (error: Throwable) {
                logger.warning("Market listing load failed for $playerId: ${error.message}")
                switchContext(SynchronizationContext.SYNC)
                if (player.isOnline) sendText(player, msg("market-cluster-unavailable"))
            } finally {
                marketOperations.remove(playerId)
            }
        }
    }

    private fun openMenu(player: Player, id: String) {
        val menu = menus[id] ?: run { sendText(player, msg("menu-not-found")); return }
        if (!player.hasPermission("errorshop.menu.$id")) { deny(player); return }
        val rows = max(1, menu.layout.size).coerceAtMost(6)
        val inv = Bukkit.createInventory(null, rows * 9, color(menu.title))
        menu.layout.forEachIndexed { row, line -> line.take(9).forEachIndexed { col, key -> menu.items[key]?.let { inv.setItem(row * 9 + col, it.toIcon()) } } }
        player.openInventory(inv); openSessions[player.uniqueId] = OpenSession.Menu(id, menu.title)
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
            is OpenSession.Shop -> handleShopClick(player, session, event.slot)
            is OpenSession.Market -> handleMarketClick(player, session, event.slot, event.click.isRightClick)
            is OpenSession.Menu -> handleMenuClick(player, session.id, event.slot, event.click.isRightClick)
        }
    }

    @EventHandler fun onClose(event: InventoryCloseEvent) {
        openSessions.remove(event.player.uniqueId)
        pendingMarketCancellations.remove(event.player.uniqueId)
    }

    private fun loadLang() {
        lang = YamlConfiguration.loadConfiguration(File(dataFolder, "lang.yml"))
    }

    @EventHandler fun onJoin(event: PlayerJoinEvent) {
        settlePlayer(event.player)
    }

    private fun handleShopClick(player: Player, session: OpenSession.Shop, slot: Int) {
        val shop = shops[session.id] ?: return
        val itemKey = session.itemKeys[slot] ?: return
        val match = shop.items[itemKey] ?: return
        val item = ItemStack(match.material, match.amount)
        val meta = item.itemMeta
        if (meta != null) { meta.setDisplayName(color(match.name ?: match.material.name)); item.itemMeta = meta }
        if (!hasSpaceFor(player, item)) { sendText(player, msg("inventory-full")); return }
        val charge = chargeShopItem(player, match)
        if (charge != null) { sendText(player, charge); return }
        player.inventory.addItem(item)
        sendText(player, msg("buy-success", mapOf("item" to (match.name?.let(::colorless) ?: itemName(item)), "amount" to match.amount.toString(), "price" to formatMoney(match.buy), "points" to match.points.toString())))
        if (config.getBoolean("shop.close-after-purchase", false)) player.closeInventory()
    }

    private fun handleMarketClick(player: Player, session: OpenSession.Market, slot: Int, rightClick: Boolean) {
        if (slot == PREVIOUS_PAGE_SLOT && session.page > 0) {
            openMarket(player, session.page - 1, session.query)
            return
        }
        if (slot == NEXT_PAGE_SLOT && session.page + 1 < session.totalPages) {
            openMarket(player, session.page + 1, session.query)
            return
        }
        val listingId = session.listingIds[slot] ?: return
        val selected = session.snapshots[listingId] ?: return
        if (selected.seller == player.uniqueId) {
            if (!player.hasPermission("errorshop.market.cancel")) { deny(player); return }
            if (!rightClick) { sendText(player, msg("market-own-item")); return }
            confirmMarketCancellation(player, selected)
            return
        }
        pendingMarketCancellations.remove(player.uniqueId)
        if (rightClick) return
        if (!player.hasPermission("errorshop.market.buy")) { deny(player); return }
        if (!economy.available()) { sendText(player, msg("vault-missing")); return }
        if (!isValidMarketPrice(selected.price)) {
            sendText(player, msg("market-invalid-listing-price", priceVariables()))
            return
        }
        purchaseMarketListing(player, listingId)
    }

    private fun purchaseMarketListing(player: Player, listingId: String) {
        if (!beginMarketOperation(player)) return
        val backend = market
        val buyerId = player.uniqueId
        val buyerName = player.name
        player.closeInventory()
        sendText(player, msg("market-purchase-processing"))
        launchCoroutine(SynchronizationContext.ASYNC, entity = player) {
            var reserved: MarketListing? = null
            var charged = false
            var committed = false
            try {
                val listing = backend.reserve(listingId, buyerId) ?: run {
                    switchContext(SynchronizationContext.SYNC)
                    if (player.isOnline) sendText(player, msg("market-item-sold"))
                    return@launchCoroutine
                }
                reserved = listing
                if (listing.seller == buyerId || !isValidMarketPrice(listing.price)) {
                    backend.releaseReservation(listing.id, buyerId)
                    switchContext(SynchronizationContext.SYNC)
                    if (player.isOnline) sendText(player, msg(if (listing.seller == buyerId) "market-own-item" else "market-invalid-listing-price", priceVariables()))
                    return@launchCoroutine
                }
                val amounts = saleAmounts(listing.price)
                switchContext(SynchronizationContext.SYNC)
                if (!player.isOnline) {
                    switchContext(SynchronizationContext.ASYNC)
                    backend.releaseReservation(listing.id, buyerId)
                    return@launchCoroutine
                }
                if (!economy.available()) {
                    switchContext(SynchronizationContext.ASYNC)
                    backend.releaseReservation(listing.id, buyerId)
                    switchContext(SynchronizationContext.SYNC)
                    sendText(player, msg("vault-missing"))
                    return@launchCoroutine
                }
                if (!economy.withdraw(player, listing.price)) {
                    switchContext(SynchronizationContext.ASYNC)
                    backend.releaseReservation(listing.id, buyerId)
                    switchContext(SynchronizationContext.SYNC)
                    sendText(player, msg("not-enough-money"))
                    return@launchCoroutine
                }
                charged = true
                switchContext(SynchronizationContext.ASYNC)
                val completed = backend.completeSale(listing.id, buyerId, amounts.earning)
                if (completed == null) {
                    backend.releaseReservation(listing.id, buyerId)
                    switchContext(SynchronizationContext.SYNC)
                    val refunded = economy.deposit(player, listing.price)
                    switchContext(SynchronizationContext.ASYNC)
                    if (!refunded) backend.addPendingEarning(buyerId, listing.price)
                    charged = false
                    switchContext(SynchronizationContext.SYNC)
                    sendText(player, msg(if (refunded) "market-purchase-refunded" else "market-refund-pending", saleVariables(listing.item, listing.sellerName, buyerName, amounts)))
                    return@launchCoroutine
                }
                committed = true
                publishMarketEvent("LISTING_BOUGHT", completed.id)

                val deliveryResult = runCatching { backend.takeDeliveries(buyerId) }
                val earningResult = runCatching { backend.takePendingEarning(completed.seller) }
                switchContext(SynchronizationContext.SYNC)
                val deliveryOutcome = if (deliveryResult.isSuccess && player.isOnline) {
                    deliverItems(player, deliveryResult.getOrThrow())
                } else {
                    DeliveryOutcome(deliveryResult.getOrDefault(emptyList()), 0)
                }
                val seller = Bukkit.getOfflinePlayer(completed.seller)
                val pendingEarning = earningResult.getOrDefault(0.0)
                val payoutSucceeded = pendingEarning > 0 && economy.deposit(seller, pendingEarning, completed.sellerName)
                switchContext(SynchronizationContext.ASYNC)
                var deliveryRestoreFailed = false
                deliveryOutcome.remaining.forEach { item ->
                    runCatching { backend.queueDelivery(buyerId, item) }
                        .onFailure { deliveryRestoreFailed = true; logger.severe("Failed to restore queued delivery for $buyerId: ${it.message}") }
                }
                var earningRestoreFailed = false
                if (pendingEarning > 0 && !payoutSucceeded) {
                    runCatching { backend.restorePendingEarning(completed.seller, pendingEarning) }
                        .onFailure { earningRestoreFailed = true; logger.severe("Failed to restore pending earnings for ${completed.seller}: ${it.message}") }
                }
                switchContext(SynchronizationContext.SYNC)
                val variables = saleVariables(completed.item, completed.sellerName, buyerName, amounts)
                seller.player?.let { onlineSeller ->
                    sendText(onlineSeller, msg(if (payoutSucceeded || amounts.earning <= 0) "market-sold" else "market-earning-pending", variables))
                }
                if (player.isOnline) {
                    sendText(player, msg("market-bought", variables))
                    if (deliveryResult.isFailure || deliveryRestoreFailed) {
                        sendText(player, msg("market-delivery-pending", variables))
                    } else if (deliveryOutcome.remaining.isNotEmpty()) {
                        sendText(player, msg("market-delivery-queued", variables + ("count" to deliveryOutcome.remaining.sumOf { it.amount }.toString())))
                    }
                }
                if (earningRestoreFailed) logger.severe("Seller ${completed.seller} requires manual payout review for listing ${completed.id}")
            } catch (error: Throwable) {
                logger.warning("Market purchase failed for $buyerId/$listingId: ${error.message}")
                if (!committed) {
                    switchContext(SynchronizationContext.ASYNC)
                    reserved?.let { runCatching { backend.releaseReservation(it.id, buyerId) } }
                    switchContext(SynchronizationContext.SYNC)
                    val wasCharged = charged
                    val refunded = !wasCharged || economy.deposit(player, reserved?.price ?: 0.0)
                    if (refunded) charged = false
                    var refundQueued = false
                    if (wasCharged && !refunded) {
                        switchContext(SynchronizationContext.ASYNC)
                        refundQueued = runCatching { backend.addPendingEarning(buyerId, reserved?.price ?: 0.0) }
                            .onFailure { logger.severe("Failed to queue purchase refund for $buyerId: ${it.message}") }
                            .isSuccess
                        if (refundQueued) charged = false
                        switchContext(SynchronizationContext.SYNC)
                    }
                    if (player.isOnline) {
                        val message = when {
                            !wasCharged -> "market-cluster-unavailable"
                            refunded -> "market-purchase-refunded"
                            refundQueued -> "market-refund-pending"
                            else -> "market-refund-failed"
                        }
                        sendText(player, msg(message))
                    }
                } else {
                    switchContext(SynchronizationContext.SYNC)
                    if (player.isOnline) sendText(player, msg("market-completed-pending"))
                }
            } finally {
                marketOperations.remove(buyerId)
            }
        }
    }

    private fun confirmMarketCancellation(player: Player, listing: MarketListing) {
        val now = System.currentTimeMillis()
        val pending = pendingMarketCancellations[player.uniqueId]
        if (pending == null || pending.listingId != listing.id || pending.expiresAt < now) {
            pendingMarketCancellations[player.uniqueId] = PendingMarketCancellation(listing.id, now + 5_000)
            sendText(player, msg("market-cancel-confirm", mapOf(
                "item" to itemName(listing.item),
                "amount" to listing.item.amount.toString(),
                "price" to formatMoney(listing.price),
                "seconds" to "5"
            )))
            return
        }
        pendingMarketCancellations.remove(player.uniqueId)
        cancelMarketListing(player, listing.id)
    }

    private fun cancelMarketListing(player: Player, listingId: String) {
        if (!beginMarketOperation(player)) return
        val backend = market
        val playerId = player.uniqueId
        player.closeInventory()
        sendText(player, msg("market-cancel-processing"))
        launchCoroutine(SynchronizationContext.ASYNC, entity = player) {
            try {
                val listing = backend.cancelAndQueueDelivery(listingId, playerId) ?: run {
                    switchContext(SynchronizationContext.SYNC)
                    if (player.isOnline) sendText(player, msg("market-item-sold"))
                    return@launchCoroutine
                }
                publishMarketEvent("LISTING_REMOVED", listing.id)
                val deliveryResult = runCatching { backend.takeDeliveries(playerId) }
                switchContext(SynchronizationContext.SYNC)
                val outcome = if (deliveryResult.isSuccess && player.isOnline) {
                    deliverItems(player, deliveryResult.getOrThrow())
                } else {
                    DeliveryOutcome(deliveryResult.getOrDefault(emptyList()), 0)
                }
                switchContext(SynchronizationContext.ASYNC)
                var restoreFailed = false
                outcome.remaining.forEach { item ->
                    runCatching { backend.queueDelivery(playerId, item) }
                        .onFailure { restoreFailed = true; logger.severe("Failed to restore cancelled listing delivery ${listing.id}: ${it.message}") }
                }
                switchContext(SynchronizationContext.SYNC)
                if (player.isOnline) {
                    sendText(player, msg("market-cancelled", mapOf(
                        "item" to itemName(listing.item),
                        "amount" to listing.item.amount.toString(),
                        "price" to formatMoney(listing.price)
                    )))
                    if (deliveryResult.isFailure || restoreFailed) sendText(player, msg("market-delivery-pending"))
                }
            } catch (error: Throwable) {
                logger.warning("Market listing cancellation failed for $listingId: ${error.message}")
                switchContext(SynchronizationContext.SYNC)
                if (player.isOnline) sendText(player, msg("market-cluster-unavailable"))
            } finally {
                marketOperations.remove(playerId)
            }
        }
    }

    private fun chargeShopItem(player: Player, item: ShopItem): String? {
        val money = item.buy
        val points = item.points
        if (money <= 0 && points <= 0) return null
        if (item.currencyMode.equals("or", true) && money > 0 && points > 0) {
            if (economy.available() && economy.charge(player, money, 0) == ChargeResult.SUCCESS) return null
            if (economy.pointsAvailable() && economy.charge(player, 0.0, points) == ChargeResult.SUCCESS) return null
            return msg("not-enough-any", mapOf("price" to money.toString(), "points" to points.toString()))
        }
        return when (economy.charge(player, money, points)) {
            ChargeResult.SUCCESS -> null
            ChargeResult.MISSING_MONEY_PROVIDER -> msg("vault-missing")
            ChargeResult.MISSING_POINTS_PROVIDER -> msg("points-missing")
            ChargeResult.NOT_ENOUGH_MONEY -> msg("not-enough-money")
            ChargeResult.NOT_ENOUGH_POINTS -> msg("not-enough-points")
        }
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

    private fun itemName(item: ItemStack): String = item.type.name.lowercase().replace('_', ' ')

    private fun settlePlayer(player: Player, manual: Boolean = false) {
        val playerId = player.uniqueId
        if (manual) {
            if (!beginMarketOperation(player)) return
            sendText(player, msg("market-claim-processing"))
        } else if (!marketReady || !marketOperations.add(playerId)) {
            return
        }
        val backend = market
        val claimEarnings = economy.available()
        launchCoroutine(SynchronizationContext.ASYNC, entity = player) {
            try {
                val deliveryResult = runCatching { backend.takeDeliveries(playerId) }
                val earningResult = if (claimEarnings) runCatching { backend.takePendingEarning(playerId) } else Result.success(0.0)
                switchContext(SynchronizationContext.SYNC)
                val outcome = if (deliveryResult.isSuccess && player.isOnline) {
                    deliverItems(player, deliveryResult.getOrThrow())
                } else {
                    DeliveryOutcome(deliveryResult.getOrDefault(emptyList()), 0)
                }
                val pendingEarning = earningResult.getOrDefault(0.0)
                val payoutSucceeded = pendingEarning > 0 && economy.deposit(player, pendingEarning, player.name)
                switchContext(SynchronizationContext.ASYNC)
                var restoreFailed = false
                outcome.remaining.forEach { item ->
                    runCatching { backend.queueDelivery(playerId, item) }
                        .onFailure { restoreFailed = true; logger.severe("Failed to restore queued delivery for $playerId: ${it.message}") }
                }
                if (pendingEarning > 0 && !payoutSucceeded) {
                    runCatching { backend.restorePendingEarning(playerId, pendingEarning) }
                        .onFailure { restoreFailed = true; logger.severe("Failed to restore pending earnings for $playerId: ${it.message}") }
                }
                switchContext(SynchronizationContext.SYNC)
                if (player.isOnline) {
                    if (outcome.deliveredAmount > 0) {
                        val message = if (manual) "market-delivery-claimed-manual" else "market-delivery-claimed"
                        sendText(player, msg(message, mapOf("count" to outcome.deliveredAmount.toString())))
                    }
                    if (outcome.remaining.isNotEmpty()) sendText(player, msg("market-delivery-still-queued", mapOf("count" to outcome.remaining.sumOf { it.amount }.toString())))
                    if (manual && deliveryResult.isSuccess && outcome.deliveredAmount == 0 && outcome.remaining.isEmpty()) {
                        sendText(player, msg("market-delivery-empty"))
                    }
                    if (pendingEarning > 0 && payoutSucceeded) sendText(player, msg("pending-earnings-paid", mapOf("amount" to formatMoney(pendingEarning))))
                    if (deliveryResult.isFailure || earningResult.isFailure || restoreFailed) sendText(player, msg("market-delivery-pending"))
                }
            } catch (error: Throwable) {
                logger.warning("Pending market settlement failed for $playerId: ${error.message}")
            } finally {
                marketOperations.remove(playerId)
            }
        }
    }

    private fun beginMarketOperation(player: Player): Boolean {
        if (!marketReady) {
            sendText(player, msg(if (reloadInProgress.get()) "market-loading" else "market-cluster-unavailable"))
            return false
        }
        if (!marketOperations.add(player.uniqueId)) {
            sendText(player, msg("market-operation-in-progress"))
            return false
        }
        return true
    }

    private fun deliverItems(player: Player, items: List<ItemStack>): DeliveryOutcome {
        val remaining = mutableListOf<ItemStack>()
        var deliveredAmount = 0
        items.forEach { original ->
            val leftovers = player.inventory.addItem(original.clone()).values.toList()
            val remainingAmount = leftovers.sumOf { it.amount }
            deliveredAmount += (original.amount - remainingAmount).coerceAtLeast(0)
            remaining += leftovers.map(ItemStack::clone)
        }
        return DeliveryOutcome(remaining, deliveredAmount)
    }

    private fun giveItem(player: Player, item: ItemStack) {
        if (!player.isOnline) return
        player.inventory.addItem(item.clone()).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }
    }

    private fun listingMatches(listing: MarketListing, query: String): Boolean {
        if (query.isBlank()) return true
        val displayName = listing.item.itemMeta?.displayName?.let { ChatColor.stripColor(it) }.orEmpty()
        val searchable = listOf(listing.sellerName, listing.item.type.name, itemName(listing.item), displayName)
            .joinToString(" ").lowercase(Locale.ROOT).replace('_', ' ')
        return query.lowercase(Locale.ROOT).replace('_', ' ').split(Regex("\\s+")).all(searchable::contains)
    }

    private fun menuIcon(material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        if (meta != null) {
            meta.setDisplayName(color(name))
            meta.lore = lore.map(::color)
            item.itemMeta = meta
        }
        return item
    }

    private fun marketTitle(page: Int, totalPages: Int): String = "&b全球市场 &8- &7${page + 1}/$totalPages"

    private fun configDecimal(path: String, fallback: String): BigDecimal =
        runCatching { BigDecimal(config.get(path)?.toString() ?: fallback) }.getOrElse { BigDecimal(fallback) }

    private fun parseMarketPrice(raw: String?): Double? {
        val value = runCatching { BigDecimal(raw?.trim() ?: return null) }.getOrNull() ?: return null
        val normalizedScale = value.stripTrailingZeros().scale().coerceAtLeast(0)
        if (value < priceSettings.minimum || value > priceSettings.maximum || normalizedScale > priceSettings.decimals) return null
        return value.toDouble().takeIf(Double::isFinite)
    }

    private fun isValidMarketPrice(price: Double): Boolean {
        if (!price.isFinite()) return false
        val value = BigDecimal.valueOf(price)
        val normalizedScale = value.stripTrailingZeros().scale().coerceAtLeast(0)
        return value >= priceSettings.minimum && value <= priceSettings.maximum && normalizedScale <= priceSettings.decimals
    }

    private fun saleAmounts(price: Double): SaleAmounts {
        val priceValue = BigDecimal.valueOf(price)
        val earning = priceValue.multiply(BigDecimal.ONE.subtract(priceSettings.taxRate))
            .setScale(priceSettings.decimals, RoundingMode.HALF_UP)
        val tax = priceValue.subtract(earning).max(BigDecimal.ZERO)
        return SaleAmounts(price, earning.toDouble(), tax.toDouble())
    }

    private fun saleVariables(item: ItemStack, seller: String, buyer: String, amounts: SaleAmounts): Map<String, String> = mapOf(
        "item" to itemName(item),
        "amount" to item.amount.toString(),
        "seller" to seller,
        "buyer" to buyer,
        "price" to formatMoney(amounts.price),
        "earning" to formatMoney(amounts.earning),
        "tax" to formatMoney(amounts.tax)
    )

    private fun priceVariables(): Map<String, String> = mapOf(
        "min" to formatMoney(priceSettings.minimum.toDouble()),
        "max" to formatMoney(priceSettings.maximum.toDouble()),
        "decimals" to priceSettings.decimals.toString()
    )

    private fun formatMoney(amount: Double): String {
        if (!amount.isFinite()) return "无效"
        val scaled = BigDecimal.valueOf(amount).setScale(priceSettings.decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (scaled.signum() == 0) "0" else scaled.toPlainString()
    }

    private fun colorless(raw: String): String = ChatColor.stripColor(color(raw)) ?: raw

    private fun isErrorShopInventory(player: Player, session: OpenSession, inventory: org.bukkit.inventory.Inventory): Boolean {
        if (player.openInventory.topInventory != inventory) return false
        val expectedTitle = when (session) {
            is OpenSession.Shop -> session.title
            is OpenSession.Market -> session.title
            is OpenSession.Menu -> session.title
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
            action.startsWith("[console]", true) -> {
                if (config.getBoolean("menu.allow-console-actions", true)) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), action.removePrefixIgnoreCase("[console]").trim().replace("%player%", player.name))
                } else {
                    sendText(player, msg("console-action-disabled"))
                }
            }
            action.startsWith("[player]", true) -> player.performCommand(action.removePrefixIgnoreCase("[player]").trim())
            action.startsWith("[tell]", true) -> sendText(player, action.removePrefixIgnoreCase("[tell]").trim())
            action.startsWith("[shop]", true) -> openShop(player, action.removePrefixIgnoreCase("[shop]").trim())
            action.startsWith("[market]", true) -> openMarket(player)
            action.startsWith("[menu]", true) -> openMenu(player, action.removePrefixIgnoreCase("[menu]").trim())
        }
    }

    private fun String.removePrefixIgnoreCase(prefix: String): String = if (startsWith(prefix, true)) substring(prefix.length) else this
    private fun playerOnly(sender: CommandSender): Boolean { sendText(sender, msg("player-only")); return true }
    private fun deny(sender: CommandSender): Boolean { sendText(sender, msg("no-permission")); return true }
    private fun msg(key: String, vars: Map<String, String> = emptyMap()): String {
        val currentLang = lang
        var text = currentLang.getString(key) ?: defaultLang.getString(key) ?: key
        val prefix = currentLang.getString("prefix") ?: defaultLang.getString("prefix") ?: ""
        vars.forEach { (k, v) -> text = text.replace("{$k}", v).replace("%$k%", v) }
        return prefix + text
    }
    private fun color(s: String): String = LegacyComponentSerializer.legacySection().serialize(formatText(s))
    private fun sendText(sender: CommandSender, raw: String) { sender.sendMessage(formatText(raw)) }
    private fun formatText(raw: String): Component {
        val legacy = ChatColor.translateAlternateColorCodes('&', raw)
        return runCatching { MiniMessage.miniMessage().deserialize(legacy) }
            .getOrElse { LegacyComponentSerializer.legacySection().deserialize(legacy) }
    }
    override fun disable() {
        marketReady = false
        runCatching { subscriber?.unsubscribe() }
        runCatching { jedisPool?.close() }
    }

    companion object {
        private const val MARKET_PAGE_SIZE = 45
        private const val PREVIOUS_PAGE_SLOT = 45
        private const val PAGE_INFO_SLOT = 49
        private const val NEXT_PAGE_SLOT = 53
        val openSessions = mutableMapOf<UUID, OpenSession>()
    }
}

data class PendingMarketCancellation(val listingId: String, val expiresAt: Long)
data class DeliveryOutcome(val remaining: List<ItemStack>, val deliveredAmount: Int)
data class SaleAmounts(val price: Double, val earning: Double, val tax: Double)
data class MarketPriceSettings(val minimum: BigDecimal, val maximum: BigDecimal, val decimals: Int, val taxRate: BigDecimal) {
    companion object {
        fun defaults() = MarketPriceSettings(BigDecimal("0.01"), BigDecimal("1000000000"), 2, BigDecimal.ZERO)
    }
}

sealed class OpenSession {
    data class Shop(val id: String, val itemKeys: Map<Int, String>, val title: String): OpenSession()
    data class Market(
        val page: Int,
        val query: String,
        val totalPages: Int,
        val listingIds: Map<Int, String>,
        val snapshots: Map<String, MarketListing>,
        val title: String
    ): OpenSession()
    data class Menu(val id: String, val title: String): OpenSession()
}
