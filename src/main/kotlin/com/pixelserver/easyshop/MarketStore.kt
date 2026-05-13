package com.pixelserver.easyshop

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.UUID

data class MarketListing(val id: String, val seller: UUID, val sellerName: String, val item: ItemStack, val price: Double) {
    fun icon(): ItemStack {
        val copy = item.clone()
        val meta = copy.itemMeta
        if (meta != null) {
            val lore = (meta.lore ?: emptyList()).toMutableList()
            lore += colorText("&7卖家: &f$sellerName")
            lore += colorText("&e价格: $price")
            lore += colorText("&a点击购买")
            meta.lore = lore
            copy.itemMeta = meta
        }
        return copy
    }
}

class MarketStore {
    private val listings = linkedMapOf<String, MarketListing>()
    private val pendingEarnings = linkedMapOf<UUID, Double>()
    private var file: File? = null

    fun load(dataFolder: File, database: YamlConfiguration) {
        file = resolveStorageFile(dataFolder, database)
        listings.clear(); pendingEarnings.clear()
        val f = file ?: return
        if (!f.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(f)
        yaml.getConfigurationSection("listings")?.getKeys(false)?.forEach { id ->
            val path = "listings.$id"
            val seller = runCatching { UUID.fromString(yaml.getString("$path.seller")) }.getOrNull() ?: return@forEach
            val item = yaml.getItemStack("$path.item")
                ?: legacyItem(yaml, path)
                ?: return@forEach
            listings[id] = MarketListing(id, seller, yaml.getString("$path.seller-name") ?: "unknown", item, yaml.getDouble("$path.price"))
        }
        yaml.getConfigurationSection("pending-earnings")?.getKeys(false)?.forEach { raw ->
            val uuid = runCatching { UUID.fromString(raw) }.getOrNull() ?: return@forEach
            val amount = yaml.getDouble("pending-earnings.$raw", 0.0)
            if (amount > 0) pendingEarnings[uuid] = amount
        }
    }

    fun listings(): List<MarketListing> = listings.values.toList()
    fun countBySeller(seller: UUID): Int = listings.values.count { it.seller == seller }
    fun add(seller: UUID, sellerName: String, item: ItemStack, price: Double) { val id = System.currentTimeMillis().toString(36) + "-" + seller.toString().take(8); listings[id] = MarketListing(id, seller, sellerName, item, price); save() }
    fun remove(id: String) { listings.remove(id); save() }

    fun addPendingEarning(seller: UUID, amount: Double) {
        if (amount <= 0) return
        pendingEarnings[seller] = (pendingEarnings[seller] ?: 0.0) + amount
        save()
    }

    fun takePendingEarning(seller: UUID): Double {
        val amount = pendingEarnings.remove(seller) ?: return 0.0
        save()
        return amount
    }

    fun restorePendingEarning(seller: UUID, amount: Double) = addPendingEarning(seller, amount)

    private fun save() {
        val f = file ?: return
        f.parentFile?.mkdirs()
        val yaml = YamlConfiguration()
        listings.values.forEach { l ->
            val path = "listings.${l.id}"
            yaml.set("$path.seller", l.seller.toString()); yaml.set("$path.seller-name", l.sellerName); yaml.set("$path.item", l.item); yaml.set("$path.price", l.price)
        }
        pendingEarnings.forEach { (seller, amount) ->
            if (amount > 0) yaml.set("pending-earnings.$seller", amount)
        }
        yaml.save(f)
    }

    private fun resolveStorageFile(dataFolder: File, database: YamlConfiguration): File {
        val configured = database.getString("storage.yaml.file")
            ?: database.getString("storage.file")
            ?: database.getString("storage.market-file")
            ?: "market.yml"
        val f = File(configured)
        return if (f.isAbsolute) f else File(dataFolder, configured)
    }

    private fun legacyItem(yaml: YamlConfiguration, path: String): ItemStack? {
        val material = org.bukkit.Material.matchMaterial(yaml.getString("$path.material") ?: return null) ?: return null
        return ItemStack(material, yaml.getInt("$path.amount", 1).coerceAtLeast(1))
    }
}
