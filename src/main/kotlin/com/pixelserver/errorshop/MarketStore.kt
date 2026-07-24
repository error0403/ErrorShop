package com.pixelserver.errorshop

import com.google.gson.Gson
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.Properties
import java.util.UUID

/** Permission based market slot group. */
data class MarketGroup(val id: String, val permission: String?, val slots: Int)

data class ClusterSettings(
    val enabled: Boolean,
    val group: String,
    val serverId: String,
    val redisEnabled: Boolean,
    val redisHost: String,
    val redisPort: Int,
    val redisPassword: String?,
    val redisDatabase: Int,
    val redisChannel: String,
    val marketBackend: String,
    val failPolicy: String,
    val mysql: MysqlSettings
)

data class MysqlSettings(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val tablePrefix: String,
    val useSsl: Boolean
) {
    val jdbcUrl: String get() = "jdbc:mysql://$host:$port/$database?useSSL=$useSsl&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8"
}

data class MarketEvent(
    val type: String,
    val eventId: String = UUID.randomUUID().toString(),
    val originServer: String,
    val group: String,
    val listingId: String? = null,
    val time: Long = System.currentTimeMillis()
)

data class MarketListing(val id: String, val seller: UUID, val sellerName: String, val item: ItemStack, val price: Double) {
    fun icon(viewer: UUID, formattedPrice: String, validPrice: Boolean): ItemStack {
        val copy = item.clone()
        val meta = copy.itemMeta
        if (meta != null) {
            val lore = (meta.lore ?: emptyList()).toMutableList()
            lore += colorText("&7卖家: &f$sellerName")
            lore += colorText(if (validPrice) "&e价格: &6$formattedPrice 金币" else "&c价格异常，禁止购买")
            lore += colorText(if (seller == viewer) "&c右键两次下架" else if (validPrice) "&a左键购买" else "&7请联系卖家下架")
            meta.lore = lore
            copy.itemMeta = meta
        }
        return copy
    }
}

data class PurchaseResult(val status: PurchaseStatus, val listing: MarketListing? = null)
enum class PurchaseStatus { SUCCESS, NOT_FOUND, OWN_ITEM, NO_MONEY_PROVIDER, NOT_ENOUGH_MONEY, INVENTORY_FULL, STORAGE_UNAVAILABLE }

interface MarketBackend {
    fun load(dataFolder: File, database: YamlConfiguration)
    fun listings(): List<MarketListing>
    fun countBySeller(seller: UUID): Int
    fun add(seller: UUID, sellerName: String, item: ItemStack, price: Double): String?
    fun cancelAndQueueDelivery(id: String, seller: UUID): MarketListing?
    fun reserve(id: String, buyer: UUID): MarketListing?
    fun completeSale(id: String, buyer: UUID, sellerEarning: Double): MarketListing?
    fun releaseReservation(id: String, buyer: UUID)
    fun addPendingEarning(seller: UUID, amount: Double)
    fun takePendingEarning(seller: UUID): Double
    fun restorePendingEarning(seller: UUID, amount: Double)
    fun queueDelivery(buyer: UUID, item: ItemStack)
    fun takeDeliveries(buyer: UUID): List<ItemStack>
}

class MarketStore : MarketBackend {
    private val listings = linkedMapOf<String, MarketListing>()
    private val pendingEarnings = linkedMapOf<UUID, Double>()
    private val deliveries = linkedMapOf<UUID, MutableList<ItemStack>>()
    private val reservations = mutableMapOf<String, UUID>()
    private var file: File? = null

    @Synchronized
    override fun load(dataFolder: File, database: YamlConfiguration) {
        file = resolveStorageFile(dataFolder, database)
        listings.clear(); pendingEarnings.clear(); deliveries.clear(); reservations.clear()
        val f = file ?: return
        if (!f.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(f)
        yaml.getConfigurationSection("listings")?.getKeys(false)?.forEach { id ->
            val path = "listings.$id"
            val seller = runCatching { UUID.fromString(yaml.getString("$path.seller")) }.getOrNull() ?: return@forEach
            val item = yaml.getItemStack("$path.item") ?: legacyItem(yaml, path) ?: return@forEach
            listings[id] = MarketListing(id, seller, yaml.getString("$path.seller-name") ?: "unknown", item, yaml.getDouble("$path.price"))
        }
        yaml.getConfigurationSection("pending-earnings")?.getKeys(false)?.forEach { raw ->
            val uuid = runCatching { UUID.fromString(raw) }.getOrNull() ?: return@forEach
            val amount = yaml.getDouble("pending-earnings.$raw", 0.0)
            if (amount.isFinite() && amount > 0) pendingEarnings[uuid] = amount
        }
        yaml.getConfigurationSection("deliveries")?.getKeys(false)?.forEach { raw ->
            val uuid = runCatching { UUID.fromString(raw) }.getOrNull() ?: return@forEach
            val list = mutableListOf<ItemStack>()
            yaml.getConfigurationSection("deliveries.$raw")?.getKeys(false)?.forEach { k ->
                yaml.getItemStack("deliveries.$raw.$k")?.let { list += it }
            }
            if (list.isNotEmpty()) deliveries[uuid] = list
        }
    }

    @Synchronized
    override fun listings(): List<MarketListing> = listings.values.filter { it.id !in reservations }

    @Synchronized
    override fun countBySeller(seller: UUID): Int = listings.values.count { it.seller == seller }

    @Synchronized
    override fun add(seller: UUID, sellerName: String, item: ItemStack, price: Double): String {
        val id = UUID.randomUUID().toString()
        listings[id] = MarketListing(id, seller, sellerName, item.clone(), price)
        return try {
            save()
            id
        } catch (error: Throwable) {
            listings.remove(id)
            throw error
        }
    }

    @Synchronized
    override fun cancelAndQueueDelivery(id: String, seller: UUID): MarketListing? {
        val listing = listings[id]?.takeIf { it.seller == seller && it.id !in reservations } ?: return null
        listings.remove(id)
        val queuedItems = deliveries.getOrPut(seller) { mutableListOf() }
        queuedItems.add(listing.item.clone())
        return try {
            save()
            listing
        } catch (t: Throwable) {
            queuedItems.removeAt(queuedItems.lastIndex)
            if (queuedItems.isEmpty()) deliveries.remove(seller)
            listings[id] = listing
            throw t
        }
    }

    @Synchronized
    override fun reserve(id: String, buyer: UUID): MarketListing? {
        val listing = listings[id] ?: return null
        if (reservations.putIfAbsent(id, buyer) != null) return null
        return listing
    }

    @Synchronized
    override fun completeSale(id: String, buyer: UUID, sellerEarning: Double): MarketListing? {
        val listing = listings[id]?.takeIf { reservations[id] == buyer } ?: return null
        val buyerDeliveries = deliveries.getOrPut(buyer) { mutableListOf() }
        val previousEarning = pendingEarnings[listing.seller]
        listings.remove(id)
        reservations.remove(id)
        buyerDeliveries.add(listing.item.clone())
        if (sellerEarning.isFinite() && sellerEarning > 0) pendingEarnings[listing.seller] = (previousEarning ?: 0.0) + sellerEarning
        return try {
            save()
            listing
        } catch (t: Throwable) {
            buyerDeliveries.removeAt(buyerDeliveries.lastIndex)
            if (buyerDeliveries.isEmpty()) deliveries.remove(buyer)
            if (previousEarning == null) pendingEarnings.remove(listing.seller) else pendingEarnings[listing.seller] = previousEarning
            listings[id] = listing
            reservations[id] = buyer
            throw t
        }
    }

    @Synchronized
    override fun releaseReservation(id: String, buyer: UUID) {
        if (reservations[id] == buyer) reservations.remove(id)
    }

    @Synchronized
    override fun addPendingEarning(seller: UUID, amount: Double) {
        if (!amount.isFinite() || amount <= 0) return
        val previous = pendingEarnings[seller]
        pendingEarnings[seller] = (previous ?: 0.0) + amount
        try {
            save()
        } catch (error: Throwable) {
            if (previous == null) pendingEarnings.remove(seller) else pendingEarnings[seller] = previous
            throw error
        }
    }

    @Synchronized
    override fun takePendingEarning(seller: UUID): Double {
        val amount = pendingEarnings.remove(seller) ?: return 0.0
        return try {
            save()
            amount
        } catch (t: Throwable) {
            pendingEarnings[seller] = amount
            throw t
        }
    }

    @Synchronized
    override fun restorePendingEarning(seller: UUID, amount: Double) = addPendingEarning(seller, amount)

    @Synchronized
    override fun queueDelivery(buyer: UUID, item: ItemStack) {
        val buyerDeliveries = deliveries.getOrPut(buyer) { mutableListOf() }
        buyerDeliveries.add(item.clone())
        try {
            save()
        } catch (t: Throwable) {
            buyerDeliveries.removeAt(buyerDeliveries.lastIndex)
            if (buyerDeliveries.isEmpty()) deliveries.remove(buyer)
            throw t
        }
    }

    @Synchronized
    override fun takeDeliveries(buyer: UUID): List<ItemStack> {
        val items = deliveries.remove(buyer) ?: return emptyList()
        return try {
            save()
            items.map(ItemStack::clone)
        } catch (t: Throwable) {
            deliveries[buyer] = items
            throw t
        }
    }

    private fun save() {
        val f = file ?: return
        f.parentFile?.mkdirs()
        val yaml = YamlConfiguration()
        listings.values.forEach { l ->
            val path = "listings.${l.id}"
            yaml.set("$path.seller", l.seller.toString()); yaml.set("$path.seller-name", l.sellerName); yaml.set("$path.item", l.item); yaml.set("$path.price", l.price)
        }
        pendingEarnings.forEach { (seller, amount) -> if (amount > 0) yaml.set("pending-earnings.$seller", amount) }
        deliveries.forEach { (buyer, items) -> items.forEachIndexed { i, item -> yaml.set("deliveries.$buyer.$i", item) } }
        val temporary = File(f.parentFile ?: f.absoluteFile.parentFile, "${f.name}.tmp")
        yaml.save(temporary)
        try {
            Files.move(
                temporary.toPath(),
                f.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun resolveStorageFile(dataFolder: File, database: YamlConfiguration): File {
        val configured = database.getString("storage.yaml.file") ?: database.getString("storage.file") ?: database.getString("storage.market-file") ?: "market.yml"
        val f = File(configured)
        return if (f.isAbsolute) f else File(dataFolder, configured)
    }

    private fun legacyItem(yaml: YamlConfiguration, path: String): ItemStack? {
        val material = Material.matchMaterial(yaml.getString("$path.material") ?: return null) ?: return null
        return ItemStack(material, yaml.getInt("$path.amount", 1).coerceAtLeast(1))
    }
}

class MysqlMarketBackend(
    private val settings: MysqlSettings,
    reservationTimeoutSeconds: Long = 60
) : MarketBackend {
    private val listingsTable = settings.tablePrefix + "market_listings"
    private val pendingTable = settings.tablePrefix + "market_pending_earnings"
    private val deliveriesTable = settings.tablePrefix + "market_deliveries"
    private val reservationTimeoutMillis = reservationTimeoutSeconds.coerceIn(10, 600) * 1_000

    override fun load(dataFolder: File, database: YamlConfiguration) { initTables() }

    private fun connection(): Connection {
        val props = Properties()
        props.setProperty("user", settings.username)
        props.setProperty("password", settings.password)
        return DriverManager.getConnection(settings.jdbcUrl, props)
    }

    private fun initTables() = connection().use { c ->
        c.createStatement().use { st ->
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS $listingsTable (
                    id VARCHAR(64) PRIMARY KEY,
                    seller_uuid VARCHAR(36) NOT NULL,
                    seller_name VARCHAR(32) NOT NULL,
                    item_blob MEDIUMTEXT NOT NULL,
                    price DOUBLE NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                    buyer_uuid VARCHAR(36) NULL,
                    locked_at BIGINT NULL,
                    created_at BIGINT NOT NULL,
                    updated_at BIGINT NOT NULL,
                    INDEX idx_status_created(status, created_at),
                    INDEX idx_seller_status(seller_uuid, status)
                )
            """.trimIndent())
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS $pendingTable (
                    seller_uuid VARCHAR(36) PRIMARY KEY,
                    amount DOUBLE NOT NULL DEFAULT 0
                )
            """.trimIndent())
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS $deliveriesTable (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    buyer_uuid VARCHAR(36) NOT NULL,
                    item_blob MEDIUMTEXT NOT NULL,
                    created_at BIGINT NOT NULL,
                    INDEX idx_buyer(buyer_uuid)
                )
            """.trimIndent())
        }
        releaseExpiredReservations(c)
    }

    override fun listings(): List<MarketListing> = connection().use { c ->
        releaseExpiredReservations(c)
        c.prepareStatement("SELECT id,seller_uuid,seller_name,item_blob,price FROM $listingsTable WHERE status='ACTIVE' ORDER BY created_at DESC").use { ps ->
            ps.executeQuery().use { rs -> buildList { while (rs.next()) toListing(rs)?.let { add(it) } } }
        }
    }

    override fun countBySeller(seller: UUID): Int = connection().use { c ->
        releaseExpiredReservations(c)
        c.prepareStatement("SELECT COUNT(*) FROM $listingsTable WHERE seller_uuid=? AND status IN ('ACTIVE','LOCKED')").use { ps -> ps.setString(1, seller.toString()); ps.executeQuery().use { rs -> if (rs.next()) rs.getInt(1) else 0 } }
    }

    override fun add(seller: UUID, sellerName: String, item: ItemStack, price: Double): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        connection().use { c ->
            c.prepareStatement("INSERT INTO $listingsTable(id,seller_uuid,seller_name,item_blob,price,status,created_at,updated_at) VALUES(?,?,?,?,?,'ACTIVE',?,?)").use { ps ->
                ps.setString(1, id); ps.setString(2, seller.toString()); ps.setString(3, sellerName); ps.setString(4, serializeItem(item)); ps.setDouble(5, price); ps.setLong(6, now); ps.setLong(7, now); ps.executeUpdate()
            }
        }
        return id
    }

    override fun cancelAndQueueDelivery(id: String, seller: UUID): MarketListing? = connection().use { c ->
        c.autoCommit = false
        try {
            val listing = c.prepareStatement("SELECT id,seller_uuid,seller_name,item_blob,price FROM $listingsTable WHERE id=? AND seller_uuid=? AND status='ACTIVE' FOR UPDATE").use { ps ->
                ps.setString(1, id)
                ps.setString(2, seller.toString())
                ps.executeQuery().use { rs -> if (rs.next()) toListing(rs) else null }
            }
            if (listing == null) { c.rollback(); return@use null }
            val updated = c.prepareStatement("UPDATE $listingsTable SET status='REMOVED',buyer_uuid=NULL,locked_at=NULL,updated_at=? WHERE id=? AND seller_uuid=? AND status='ACTIVE'").use { ps ->
                ps.setLong(1, System.currentTimeMillis())
                ps.setString(2, id)
                ps.setString(3, seller.toString())
                ps.executeUpdate()
            }
            if (updated != 1) { c.rollback(); return@use null }
            c.prepareStatement("INSERT INTO $deliveriesTable(buyer_uuid,item_blob,created_at) VALUES(?,?,?)").use { ps ->
                ps.setString(1, seller.toString())
                ps.setString(2, serializeItem(listing.item))
                ps.setLong(3, System.currentTimeMillis())
                ps.executeUpdate()
            }
            c.commit()
            listing
        } catch (t: Throwable) {
            c.rollback()
            throw t
        } finally {
            c.autoCommit = true
        }
    }

    override fun reserve(id: String, buyer: UUID): MarketListing? = connection().use { c ->
        c.autoCommit = FalseCompat.FALSE
        try {
            c.prepareStatement("UPDATE $listingsTable SET status='ACTIVE',buyer_uuid=NULL,locked_at=NULL,updated_at=? WHERE id=? AND status='LOCKED' AND locked_at<?").use { ps ->
                val now = System.currentTimeMillis()
                ps.setLong(1, now)
                ps.setString(2, id)
                ps.setLong(3, now - reservationTimeoutMillis)
                ps.executeUpdate()
            }
            val updated = c.prepareStatement("UPDATE $listingsTable SET status='LOCKED',buyer_uuid=?,locked_at=?,updated_at=? WHERE id=? AND status='ACTIVE'").use { ps ->
                ps.setString(1, buyer.toString()); ps.setLong(2, System.currentTimeMillis()); ps.setLong(3, System.currentTimeMillis()); ps.setString(4, id); ps.executeUpdate()
            }
            if (updated != 1) { c.rollback(); return@use null }
            val listing = c.prepareStatement("SELECT id,seller_uuid,seller_name,item_blob,price FROM $listingsTable WHERE id=? AND status='LOCKED'").use { ps -> ps.setString(1, id); ps.executeQuery().use { rs -> if (rs.next()) toListing(rs) else null } }
            c.commit(); listing
        } catch (t: Throwable) { c.rollback(); throw t } finally { c.autoCommit = true }
    }

    override fun completeSale(id: String, buyer: UUID, sellerEarning: Double): MarketListing? {
        try {
            return connection().use { c ->
                c.autoCommit = false
                try {
                    val listing = c.prepareStatement("SELECT id,seller_uuid,seller_name,item_blob,price FROM $listingsTable WHERE id=? AND buyer_uuid=? AND status='LOCKED' FOR UPDATE").use { ps ->
                        ps.setString(1, id)
                        ps.setString(2, buyer.toString())
                        ps.executeQuery().use { rs -> if (rs.next()) toListing(rs) else null }
                    }
                    if (listing == null) {
                        c.rollback()
                        return@use findCompletedSale(c, id, buyer)
                    }
                    val updated = c.prepareStatement("UPDATE $listingsTable SET status='SOLD',updated_at=? WHERE id=? AND buyer_uuid=? AND status='LOCKED'").use { ps ->
                        ps.setLong(1, System.currentTimeMillis())
                        ps.setString(2, id)
                        ps.setString(3, buyer.toString())
                        ps.executeUpdate()
                    }
                    if (updated != 1) {
                        c.rollback()
                        return@use findCompletedSale(c, id, buyer)
                    }
                    c.prepareStatement("INSERT INTO $deliveriesTable(buyer_uuid,item_blob,created_at) VALUES(?,?,?)").use { ps ->
                        ps.setString(1, buyer.toString())
                        ps.setString(2, serializeItem(listing.item))
                        ps.setLong(3, System.currentTimeMillis())
                        ps.executeUpdate()
                    }
                    if (sellerEarning.isFinite() && sellerEarning > 0) {
                        c.prepareStatement("INSERT INTO $pendingTable(seller_uuid,amount) VALUES(?,?) ON DUPLICATE KEY UPDATE amount=amount+?").use { ps ->
                            ps.setString(1, listing.seller.toString())
                            ps.setDouble(2, sellerEarning)
                            ps.setDouble(3, sellerEarning)
                            ps.executeUpdate()
                        }
                    }
                    c.commit()
                    listing
                } catch (error: Throwable) {
                    runCatching { c.rollback() }
                    throw error
                } finally {
                    runCatching { c.autoCommit = true }
                }
            }
        } catch (error: Throwable) {
            findCompletedSale(id, buyer)?.let { return it }
            throw error
        }
    }

    override fun releaseReservation(id: String, buyer: UUID) {
        connection().use { c ->
            c.prepareStatement("UPDATE $listingsTable SET status='ACTIVE',buyer_uuid=NULL,locked_at=NULL,updated_at=? WHERE id=? AND buyer_uuid=? AND status='LOCKED'").use { ps ->
                ps.setLong(1, System.currentTimeMillis())
                ps.setString(2, id)
                ps.setString(3, buyer.toString())
                ps.executeUpdate()
            }
        }
    }

    override fun addPendingEarning(seller: UUID, amount: Double) { if (!amount.isFinite() || amount <= 0) return; connection().use { c -> c.prepareStatement("INSERT INTO $pendingTable(seller_uuid,amount) VALUES(?,?) ON DUPLICATE KEY UPDATE amount=amount+?").use { ps -> ps.setString(1, seller.toString()); ps.setDouble(2, amount); ps.setDouble(3, amount); ps.executeUpdate() } } }
    override fun takePendingEarning(seller: UUID): Double = connection().use { c ->
        c.autoCommit = false
        try {
            val amount = c.prepareStatement("SELECT amount FROM $pendingTable WHERE seller_uuid=? FOR UPDATE").use { ps -> ps.setString(1, seller.toString()); ps.executeQuery().use { rs -> if (rs.next()) rs.getDouble(1) else 0.0 } }
            if (amount.isFinite() && amount > 0) {
                c.prepareStatement("DELETE FROM $pendingTable WHERE seller_uuid=?").use { ps -> ps.setString(1, seller.toString()); ps.executeUpdate() }
            }
            c.commit(); if (amount.isFinite() && amount > 0) amount else 0.0
        } catch (t: Throwable) { c.rollback(); throw t } finally { c.autoCommit = true }
    }
    override fun restorePendingEarning(seller: UUID, amount: Double) = addPendingEarning(seller, amount)
    override fun queueDelivery(buyer: UUID, item: ItemStack) { connection().use { c -> c.prepareStatement("INSERT INTO $deliveriesTable(buyer_uuid,item_blob,created_at) VALUES(?,?,?)").use { ps -> ps.setString(1, buyer.toString()); ps.setString(2, serializeItem(item)); ps.setLong(3, System.currentTimeMillis()); ps.executeUpdate() } } }
    override fun takeDeliveries(buyer: UUID): List<ItemStack> = connection().use { c ->
        c.autoCommit = false
        try {
            val ids = mutableListOf<Long>(); val items = mutableListOf<ItemStack>()
            c.prepareStatement("SELECT id,item_blob FROM $deliveriesTable WHERE buyer_uuid=? ORDER BY id ASC LIMIT 54 FOR UPDATE").use { ps -> ps.setString(1, buyer.toString()); ps.executeQuery().use { rs -> while (rs.next()) { deserializeItem(rs.getString(2))?.let { ids += rs.getLong(1); items += it } } } }
            if (ids.isNotEmpty()) c.prepareStatement("DELETE FROM $deliveriesTable WHERE id IN (${ids.joinToString(",")})").use { it.executeUpdate() }
            c.commit(); items
        } catch (t: Throwable) { c.rollback(); throw t } finally { c.autoCommit = true }
    }

    private fun toListing(rs: ResultSet): MarketListing? = deserializeItem(rs.getString("item_blob"))?.let { MarketListing(rs.getString("id"), UUID.fromString(rs.getString("seller_uuid")), rs.getString("seller_name"), it, rs.getDouble("price")) }
    private fun findCompletedSale(id: String, buyer: UUID): MarketListing? = connection().use { findCompletedSale(it, id, buyer) }
    private fun findCompletedSale(connection: Connection, id: String, buyer: UUID): MarketListing? =
        connection.prepareStatement("SELECT id,seller_uuid,seller_name,item_blob,price FROM $listingsTable WHERE id=? AND buyer_uuid=? AND status='SOLD'").use { ps ->
            ps.setString(1, id)
            ps.setString(2, buyer.toString())
            ps.executeQuery().use { rs -> if (rs.next()) toListing(rs) else null }
        }
    private fun releaseExpiredReservations(connection: Connection) {
        connection.prepareStatement("UPDATE $listingsTable SET status='ACTIVE',buyer_uuid=NULL,locked_at=NULL,updated_at=? WHERE status='LOCKED' AND locked_at<?").use { ps ->
            val now = System.currentTimeMillis()
            ps.setLong(1, now)
            ps.setLong(2, now - reservationTimeoutMillis)
            ps.executeUpdate()
        }
    }
    private fun serializeItem(item: ItemStack): String { val out=ByteArrayOutputStream(); org.bukkit.util.io.BukkitObjectOutputStream(out).use { it.writeObject(item) }; return Base64.getEncoder().encodeToString(out.toByteArray()) }
    private fun deserializeItem(raw: String): ItemStack? = runCatching { org.bukkit.util.io.BukkitObjectInputStream(ByteArrayInputStream(Base64.getDecoder().decode(raw))).use { it.readObject() as ItemStack } }.getOrNull()
}

private object FalseCompat { const val FALSE = false }
