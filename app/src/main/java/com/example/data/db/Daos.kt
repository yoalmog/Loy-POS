package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT * FROM products WHERE stockLevel <= :threshold ORDER BY stockLevel ASC")
    fun getLowStockProducts(threshold: Int = 5): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE stockLevel <= :threshold ORDER BY stockLevel ASC")
    suspend fun checkLowStockProducts(threshold: Int = 5): List<Product>

    @Query("SELECT COUNT(*) FROM products WHERE stockLevel <= :threshold")
    fun getLowStockCount(threshold: Int = 5): Flow<Int>

    @Query("UPDATE products SET stockLevel = MAX(0, stockLevel - :quantity) WHERE id = :productId")
    suspend fun decrementProductStock(productId: Long, quantity: Int)

    @Query("UPDATE products SET stockLevel = stockLevel + :addedStock WHERE id = :productId")
    suspend fun restockProduct(productId: Long, addedStock: Int)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}

@Dao
interface ItemDao {
    @Query("SELECT COUNT(*) FROM items")
    suspend fun countItems(): Int

    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getItemsByCategory(categoryId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE barcode = :barcode LIMIT 1")
    suspend fun getItemByBarcode(barcode: String): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Query("UPDATE items SET inStock = inStock - :quantity WHERE id = :itemId")
    suspend fun decrementStock(itemId: Long, quantity: Double)

    @Query("UPDATE items SET inStock = inStock + :quantity WHERE id = :itemId")
    suspend fun incrementStock(itemId: Long, quantity: Double)

    @Delete
    suspend fun delete(item: ItemEntity)
}

@Dao
interface ModifierDao {
    @Query("SELECT * FROM modifiers ORDER BY name ASC")
    fun getAllModifiers(): Flow<List<Modifier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(modifier: Modifier): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modifiers: List<Modifier>)

    @Delete
    suspend fun delete(modifier: Modifier)
}

@Dao
interface ShiftRecordDao {
    @Query("SELECT * FROM shift_records WHERE isOpen = 1 ORDER BY startTime DESC LIMIT 1")
    fun getCurrentShift(): Flow<Shift?>

    @Query("SELECT * FROM shift_records ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shift: Shift): Long

    @Update
    suspend fun update(shift: Shift)
}

@Dao
interface TimecardDao {
    @Query("SELECT * FROM timecards ORDER BY clockInTime DESC")
    fun getAllTimecards(): Flow<List<Timecard>>

    @Query("SELECT * FROM timecards WHERE employeeName = :name AND isClockedIn = 1 LIMIT 1")
    suspend fun getActiveSession(name: String): Timecard?

    @Query("SELECT * FROM timecards WHERE isClockedIn = 1")
    fun getActiveClockedInSessions(): Flow<List<Timecard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timecard: Timecard): Long

    @Update
    suspend fun update(timecard: Timecard)
}

@Dao
interface ModifierGroupDao {
    @Query("SELECT * FROM modifier_groups")
    fun getAllModifierGroups(): Flow<List<ModifierGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(modifierGroup: ModifierGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(modifierGroups: List<ModifierGroupEntity>)
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM open_tickets WHERE isClosed = 0 ORDER BY createdAt DESC")
    fun getActiveTickets(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM open_tickets WHERE id = :id")
    suspend fun getTicketById(id: Long): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity): Long

    @Update
    suspend fun updateTicket(ticket: TicketEntity)

    @Query("UPDATE open_tickets SET isClosed = 1 WHERE id = :id")
    suspend fun closeTicket(id: Long)

    @Delete
    suspend fun deleteTicket(ticket: TicketEntity)

    // Ticket items
    @Query("SELECT * FROM ticket_items WHERE ticketId = :ticketId")
    fun getTicketItems(ticketId: Long): Flow<List<TicketItemEntity>>

    @Query("SELECT * FROM ticket_items WHERE ticketId = :ticketId")
    suspend fun getTicketItemsSync(ticketId: Long): List<TicketItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicketItems(items: List<TicketItemEntity>)

    @Query("DELETE FROM ticket_items WHERE ticketId = :ticketId")
    suspend fun deleteTicketItems(ticketId: Long)
}

@Dao
interface TicketOrderDao {
    @Query("SELECT * FROM tickets WHERE isClosed = 0 ORDER BY createdAt DESC")
    fun getAllOpenTickets(): Flow<List<Ticket>>

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getTicketById(id: Long): Ticket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ticket: Ticket): Long

    @Update
    suspend fun update(ticket: Ticket)

    @Query("UPDATE tickets SET isClosed = 1 WHERE id = :id")
    suspend fun closeTicket(id: Long)

    @Delete
    suspend fun delete(ticket: Ticket)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    fun getCurrentShift(): Flow<ShiftEntity?>

    @Query("SELECT * FROM shifts WHERE isOpen = 1 ORDER BY openedAt DESC LIMIT 1")
    suspend fun getCurrentShiftSync(): ShiftEntity?

    @Query("SELECT * FROM shifts ORDER BY openedAt DESC")
    fun getAllShifts(): Flow<List<ShiftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shift: ShiftEntity): Long

    @Update
    suspend fun update(shift: ShiftEntity)
}

@Dao
interface CashActionDao {
    @Query("SELECT * FROM cash_actions WHERE shiftId = :shiftId ORDER BY timestamp DESC")
    fun getCashActions(shiftId: Long): Flow<List<CashActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: CashActionEntity): Long
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE phone LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR customerCode = :query")
    fun searchCustomers(query: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE customers SET pointsBalance = pointsBalance + :pointsAdded, totalSpent = totalSpent + :spentAdded, totalVisits = totalVisits + 1 WHERE id = :customerId")
    suspend fun recordPurchase(customerId: Long, pointsAdded: Double, spentAdded: Double)

    @Query("UPDATE customers SET pointsBalance = pointsBalance - :pointsRedeemed WHERE id = :customerId")
    suspend fun redeemPoints(customerId: Long, pointsRedeemed: Double)
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY timestamp DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getReceiptsByCustomer(customerId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getReceiptsInRange(startTime: Long, endTime: Long): Flow<List<ReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity): Long

    @Query("UPDATE receipts SET isRefunded = 1, refundTimestamp = :timestamp WHERE id = :receiptId")
    suspend fun markRefunded(receiptId: Long, timestamp: Long)
}

@Dao
interface LoyaltyPointDao {
    @Query("SELECT * FROM loyalty_points WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getPointsForCustomer(customerId: Long): Flow<List<LoyaltyPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LoyaltyPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<LoyaltyPoint>)

    @Query("SELECT SUM(points) FROM loyalty_points WHERE customerId = :customerId")
    suspend fun getTotalPoints(customerId: Long): Double?
}

