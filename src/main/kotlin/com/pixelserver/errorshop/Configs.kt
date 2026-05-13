package com.pixelserver.errorshop

import org.bukkit.ChatColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack

fun colorText(s: String): String {
    val legacy = ChatColor.translateAlternateColorCodes('&', s)
    return runCatching {
        LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(legacy))
    }.getOrElse { legacy }
}

data class ShopConfig(val id: String, val title: String, val permission: String?, val items: Map<String, ShopItem>) {
    companion object {
        fun from(id: String, yaml: YamlConfiguration): ShopConfig {
            val section = yaml.getConfigurationSection("items")
            val items = linkedMapOf<String, ShopItem>()
            section?.getKeys(false)?.forEach { key ->
                val path = "items.$key"
                val material = Material.matchMaterial(yaml.getString("$path.material") ?: key) ?: Material.STONE
                items[key] = ShopItem(material, yaml.getString("$path.name"), yaml.getStringList("$path.lore"), yaml.getDouble("$path.buy", 0.0), yaml.getInt("$path.points", 0).coerceAtLeast(0), yaml.getString("$path.currency-mode", "and") ?: "and", yaml.getDouble("$path.sell", 0.0), yaml.getInt("$path.amount", 1).coerceAtLeast(1))
            }
            return ShopConfig(id, yaml.getString("title") ?: "&6Shop", yaml.getString("permission"), items)
        }
    }
}

data class ShopItem(val material: Material, val name: String?, val lore: List<String>, val buy: Double, val points: Int, val currencyMode: String, val sell: Double, val amount: Int) {
    fun toIcon(): ItemStack {
        val item = ItemStack(material, amount)
        val meta = item.itemMeta
        if (meta != null) {
            meta.setDisplayName(colorText(name ?: "&f${material.name}"))
            val out = lore.toMutableList()
            if (buy > 0 && points > 0) out += if (currencyMode.equals("or", true)) "&a购买: &e$buy &7或 &d$points 点券" else "&a购买: &e$buy &7+ &d$points 点券"
            else if (buy > 0) out += "&a购买: &e$buy"
            else if (points > 0) out += "&a购买: &d$points 点券"
            if (sell > 0) out += "&b出售: &e$sell"
            meta.lore = out.map(::colorText)
            item.itemMeta = meta
        }
        return item
    }
}

data class MenuConfig(val id: String, val title: String, val layout: List<String>, val items: Map<Char, MenuItem>) {
    companion object {
        fun from(id: String, yaml: YamlConfiguration): MenuConfig {
            val map = linkedMapOf<Char, MenuItem>()
            yaml.getConfigurationSection("items")?.getKeys(false)?.forEach { key ->
                val c = key.firstOrNull() ?: return@forEach
                val path = "items.$key"
                val material = Material.matchMaterial(yaml.getString("$path.material") ?: "STONE") ?: Material.STONE
                map[c] = MenuItem(material, yaml.getString("$path.name") ?: " ", yaml.getStringList("$path.lore"), yaml.getStringList("$path.left"), yaml.getStringList("$path.right"), yaml.getStringList("$path.all"))
            }
            return MenuConfig(id, yaml.getString("title") ?: "&6Menu", yaml.getStringList("layout"), map)
        }
    }
}

data class MenuItem(val material: Material, val name: String, val lore: List<String>, val left: List<String>, val right: List<String>, val all: List<String>) {
    fun toIcon(): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        if (meta != null) { meta.setDisplayName(colorText(name)); meta.lore = lore.map(::colorText); item.itemMeta = meta }
        return item
    }
}
