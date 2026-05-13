package com.pixelserver.errorshop

import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** Soft Vault economy bridge using reflection, so Vault is optional at compile time. */
class EconomyBridge(private val plugin: ErrorShopPlugin) {
    private var economy: Any? = null
    private var pointsApi: Any? = null
    val enabled: Boolean get() = plugin.config.getString("currency.provider", "none").equals("vault", true)
    val pointsEnabled: Boolean get() = plugin.config.getBoolean("points.enabled", false)

    fun setup() {
        economy = null
        pointsApi = null
        if (enabled) {
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
                runCatching {
                    val economyClass = Class.forName("net.milkbowl.vault.economy.Economy")
                    val rsp = Bukkit.getServicesManager().getRegistration(economyClass) ?: return@runCatching
                    economy = rsp.provider
                }.onFailure { plugin.logger.warning("Vault economy provider not available: ${it.message}") }
            }
        }
        if (pointsEnabled) {
            if (Bukkit.getPluginManager().getPlugin("PlayerPoints") != null) {
                runCatching {
                    val apiClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints")
                    val pluginInstance = Bukkit.getPluginManager().getPlugin("PlayerPoints") ?: return@runCatching
                    pointsApi = apiClass.methods.first { it.name == "getAPI" && it.parameterTypes.isEmpty() }.invoke(pluginInstance)
                }.onFailure { plugin.logger.warning("PlayerPoints API not available: ${it.message}") }
            }
        }
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

    fun hasPoints(player: Player, amount: Int): Boolean {
        if (amount <= 0) return true
        val api = pointsApi ?: return !pointsEnabled
        return runCatching { api.javaClass.methods.first { it.name == "look" && it.parameterTypes.size == 1 }.invoke(api, player.uniqueId) as? Int ?: 0 }.getOrDefault(0) >= amount
    }

    fun takePoints(player: Player, amount: Int): Boolean {
        if (amount <= 0) return true
        val api = pointsApi ?: return !pointsEnabled
        if (!hasPoints(player, amount)) return false
        return runCatching { api.javaClass.methods.first { it.name == "take" && it.parameterTypes.size == 2 }.invoke(api, player.uniqueId, amount) as? Boolean ?: false }.getOrDefault(false)
    }

    fun givePoints(player: Player, amount: Int): Boolean {
        if (amount <= 0) return true
        val api = pointsApi ?: return !pointsEnabled
        return runCatching { api.javaClass.methods.first { it.name == "give" && it.parameterTypes.size == 2 }.invoke(api, player.uniqueId, amount) as? Boolean ?: false }.getOrDefault(false)
    }

    fun charge(player: Player, money: Double, points: Int): ChargeResult {
        if (!available()) return ChargeResult.MISSING_MONEY_PROVIDER
        if (!pointsAvailable()) return ChargeResult.MISSING_POINTS_PROVIDER
        if (money > 0 && !hasMoney(player, money)) return ChargeResult.NOT_ENOUGH_MONEY
        if (points > 0 && !hasPoints(player, points)) return ChargeResult.NOT_ENOUGH_POINTS
        var moneyTaken = false
        var pointsTaken = false
        if (money > 0) {
            if (!withdraw(player, money)) return ChargeResult.NOT_ENOUGH_MONEY
            moneyTaken = true
        }
        if (points > 0) {
            if (!takePoints(player, points)) {
                if (moneyTaken) deposit(player, money)
                return ChargeResult.NOT_ENOUGH_POINTS
            }
            pointsTaken = true
        }
        return ChargeResult.SUCCESS
    }

    fun refund(player: Player, money: Double, points: Int) {
        if (money > 0) deposit(player, money)
        if (points > 0) givePoints(player, points)
    }

    fun available(): Boolean = !enabled || economy != null
    fun pointsAvailable(): Boolean = !pointsEnabled || pointsApi != null

    private fun hasMoney(player: Player, amount: Double): Boolean {
        val eco = economy ?: return !enabled
        return callBoolean(eco, "has", player, amount)
    }

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


enum class ChargeResult { SUCCESS, MISSING_MONEY_PROVIDER, MISSING_POINTS_PROVIDER, NOT_ENOUGH_MONEY, NOT_ENOUGH_POINTS }
