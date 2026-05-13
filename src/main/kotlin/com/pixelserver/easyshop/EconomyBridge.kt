package com.pixelserver.easyshop

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** Soft Vault economy bridge using reflection, so Vault is optional at compile time. */
class EconomyBridge(private val plugin: EasyShopPlugin) {
    private var economy: Any? = null
    val enabled: Boolean get() = plugin.config.getString("currency.provider", "none").equals("vault", true)

    fun setup() {
        economy = null
        if (!enabled) return
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return
        runCatching {
            val economyClass = Class.forName("net.milkbowl.vault.economy.Economy")
            val rsp = Bukkit.getServicesManager().getRegistration(economyClass) ?: return
            economy = rsp.provider
        }.onFailure { plugin.logger.warning("Vault economy provider not available: ${it.message}") }
    }

    fun withdraw(player: Player, amount: Double): Boolean {
        val eco = economy ?: return !enabled
        if (!callBoolean(eco, "has", player, amount)) return false
        return transactionSuccess(callAny(eco, "withdrawPlayer", player, amount))
    }

    fun deposit(player: Player, amount: Double): Boolean {
        val eco = economy ?: return !enabled
        return transactionSuccess(callAny(eco, "depositPlayer", player, amount))
    }

    fun available(): Boolean = !enabled || economy != null

    private fun callBoolean(target: Any, name: String, player: Player, amount: Double): Boolean = runCatching {
        target.javaClass.methods.first { it.name == name && it.parameterTypes.size == 2 }.invoke(target, player, amount) as? Boolean ?: false
    }.getOrDefault(false)

    private fun callAny(target: Any, name: String, player: Player, amount: Double): Any? = runCatching {
        target.javaClass.methods.first { it.name == name && it.parameterTypes.size == 2 }.invoke(target, player, amount)
    }.getOrNull()

    private fun transactionSuccess(response: Any?): Boolean = runCatching {
        response?.javaClass?.methods?.firstOrNull { it.name == "transactionSuccess" && it.parameterTypes.isEmpty() }?.invoke(response) as? Boolean ?: false
    }.getOrDefault(false)
}
