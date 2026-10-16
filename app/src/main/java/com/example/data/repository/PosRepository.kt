package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(private val database: AppDatabase) {

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                if (database.itemDao().countItems() == 0) {
                    com.example.data.db.populateInitialBistroData(database)
                }
            } catch (e: Exception) {
                // Ignore if DB is currently migrating
            }
        }
    }

    val allCategories: Flow<List<CategoryEntity>> = database.categoryDao().getAllCategories()
    val allItems: Flow<List<ItemEntity>> = database.itemDao().getAllItems()
    val allModifierGroups: Flow<List<ModifierGroupEntity>> = database.modifierGroupDao().getAllModifierGroups()
    val activeTickets: Flow<List<TicketEntity>> = database.ticketDao().getActiveTickets()
    val currentShift: Flow<ShiftEntity?> = database.shiftDao().getCurrentShift()
    val allShifts: Flow<List<ShiftEntity>> = database.shiftDao().getAllShifts()
    val allCustomers: Flow<List<CustomerEntity>> = database.customerDao().getAllCustomers()
    val allReceipts: Flow<List<ReceiptEntity>> = database.receiptDao().getAllReceipts()
    val allProducts: Flow<List<Product>> = database.productDao().getAllProducts()
    val allOpenTicketOrders: Flow<List<Ticket>> = database.ticketOrderDao().getAllOpenTickets()
    val allModifiers: Flow<List<Modifier>> = database.modifierDao().getAllModifiers()
    val allShiftRecords: Flow<List<Shift>> = database.shiftRecordDao().getAllShifts()
    val currentShiftRecord: Flow<Shift?> = database.shiftRecordDao().getCurrentShift()
    val allTimecards: Flow<List<Timecard>> = database.timecardDao().getAllTimecards()

    fun getLowStockProducts(threshold: Int = 5): Flow<List<Product>> = database.productDao().getLowStockProducts(threshold)

    suspend fun checkLowStockProducts(threshold: Int = 5): List<Product> = database.productDao().checkLowStockProducts(threshold)

    suspend fun restockProduct(productId: Long, addedStock: Int) {
        database.productDao().restockProduct(productId, addedStock)
        database.itemDao().getItemById(productId)?.let { item ->
            val updated = item.copy(inStock = item.inStock + addedStock)
            database.itemDao().update(updated)
        }
    }

    suspend fun saveModifier(modifier: Modifier): Long {
        return if (modifier.id == 0L) database.modifierDao().insert(modifier)
        else {
            database.modifierDao().insert(modifier)
            modifier.id
        }
    }

    suspend fun deleteModifier(modifier: Modifier) {
        database.modifierDao().delete(modifier)
    }

    suspend fun clockIn(employeeName: String): Timecard {
        val existing = database.timecardDao().getActiveSession(employeeName)
        if (existing != null) return existing

        val timecard = Timecard(
            employeeName = employeeName,
            clockInTime = System.currentTimeMillis(),
            isClockedIn = true
        )
        val id = database.timecardDao().insert(timecard)
        return timecard.copy(id = id)
    }

    suspend fun clockOut(employeeName: String): Timecard? {
        val active = database.timecardDao().getActiveSession(employeeName) ?: return null
        val now = System.currentTimeMillis()
        val durationMillis = now - active.clockInTime
        val hours = (durationMillis / (1000.0 * 60.0 * 60.0))
        val updated = active.copy(
            clockOutTime = now,
            totalHours = Math.round(hours * 100.0) / 100.0,
            isClockedIn = false
        )
        database.timecardDao().update(updated)
        return updated
    }

    suspend fun getTicketOrderById(id: Long): Ticket? = database.ticketOrderDao().getTicketById(id)

    suspend fun saveTicketOrder(ticket: Ticket): Long {
        return if (ticket.id == 0L) {
            database.ticketOrderDao().insert(ticket)
        } else {
            database.ticketOrderDao().update(ticket)
            ticket.id
        }
    }

    suspend fun closeTicketOrder(id: Long) {
        database.ticketOrderDao().closeTicket(id)
    }

    suspend fun deleteTicketOrder(ticket: Ticket) {
        database.ticketOrderDao().delete(ticket)
    }

    fun getProductsByCategory(category: String): Flow<List<Product>> = database.productDao().getProductsByCategory(category)

    suspend fun saveProduct(product: Product): Long {
        return if (product.id == 0L) {
            database.productDao().insert(product)
        } else {
            database.productDao().update(product)
            product.id
        }
    }

    suspend fun deleteProduct(product: Product) {
        database.productDao().delete(product)
    }

    suspend fun getItemByBarcode(barcode: String): ItemEntity? {
        return database.itemDao().getItemByBarcode(barcode)
    }

    suspend fun saveItem(item: ItemEntity): Long {
        return if (item.id == 0L) {
            database.itemDao().insert(item)
        } else {
            database.itemDao().update(item)
            item.id
        }
    }

    suspend fun deleteItem(item: ItemEntity) {
        database.itemDao().delete(item)
    }

    suspend fun adjustStock(itemId: Long, quantityDiff: Double) {
        if (quantityDiff > 0) {
            database.itemDao().incrementStock(itemId, quantityDiff)
        } else if (quantityDiff < 0) {
            database.itemDao().decrementStock(itemId, -quantityDiff)
        }
    }

    suspend fun getTicketItems(ticketId: Long): List<TicketItemEntity> {
        return database.ticketDao().getTicketItemsSync(ticketId)
    }

    suspend fun saveTicketWithItems(
        ticket: TicketEntity,
        items: List<TicketItemEntity>
    ): Long {
        val ticketId = if (ticket.id == 0L) {
            database.ticketDao().insertTicket(ticket)
        } else {
            database.ticketDao().updateTicket(ticket)
            ticket.id
        }

        database.ticketDao().deleteTicketItems(ticketId)
        val itemsWithId = items.map { it.copy(ticketId = ticketId) }
        database.ticketDao().insertTicketItems(itemsWithId)
        return ticketId
    }

    suspend fun mergeTickets(targetTicketId: Long, ticketsToMerge: List<TicketEntity>): Long {
        val allMergedItems = mutableListOf<TicketItemEntity>()
        val targetItems = database.ticketDao().getTicketItemsSync(targetTicketId)
        allMergedItems.addAll(targetItems)

        for (ticket in ticketsToMerge) {
            if (ticket.id != targetTicketId) {
                val items = database.ticketDao().getTicketItemsSync(ticket.id)
                allMergedItems.addAll(items.map { it.copy(id = 0, ticketId = targetTicketId) })
                database.ticketDao().closeTicket(ticket.id)
            }
        }

        database.ticketDao().deleteTicketItems(targetTicketId)
        database.ticketDao().insertTicketItems(allMergedItems)
        return targetTicketId
    }

    suspend fun splitTicket(
        sourceTicketId: Long,
        newTicketName: String,
        itemsToMove: List<TicketItemEntity>
    ): Long {
        val sourceTicket = database.ticketDao().getTicketById(sourceTicketId) ?: return 0L
        val newTicket = TicketEntity(
            ticketName = newTicketName,
            tableName = sourceTicket.tableName,
            diningOption = sourceTicket.diningOption,
            serverName = sourceTicket.serverName
        )
        val newTicketId = database.ticketDao().insertTicket(newTicket)

        // Add moved items to new ticket
        val movedItems = itemsToMove.map { it.copy(id = 0, ticketId = newTicketId) }
        database.ticketDao().insertTicketItems(movedItems)

        // Remove from source ticket
        val remainingSourceItems = database.ticketDao().getTicketItemsSync(sourceTicketId)
            .filterNot { sourceItem -> itemsToMove.any { it.id == sourceItem.id } }

        database.ticketDao().deleteTicketItems(sourceTicketId)
        database.ticketDao().insertTicketItems(remainingSourceItems)

        return newTicketId
    }

    suspend fun closeOrVoidTicket(ticketId: Long) {
        database.ticketDao().closeTicket(ticketId)
    }

    suspend fun completeSale(
        ticketId: Long?,
        ticketName: String,
        diningOption: DiningOption,
        items: List<TicketItemEntity>,
        subtotal: Double,
        taxAmount: Double,
        discountAmount: Double,
        tipAmount: Double,
        totalAmount: Double,
        paymentType: PaymentType,
        paymentDetails: String,
        customerId: Long?,
        customerName: String?,
        cashierName: String
    ): ReceiptEntity {
        val currentShift = database.shiftDao().getCurrentShiftSync()
        val shiftId = currentShift?.id ?: 1L

        // 1. Decrement stock for tracked items
        for (item in items) {
            database.itemDao().decrementStock(item.itemId, item.quantity)
            database.productDao().decrementProductStock(item.itemId, item.quantity.toInt().coerceAtLeast(1))
        }

        // 2. Generate Receipt
        val receiptNumber = "#${System.currentTimeMillis() % 100000}"
        val itemsSummary = items.joinToString(", ") { "${it.itemName} x${it.quantity.toInt()}" }

        val receipt = ReceiptEntity(
            receiptNumber = receiptNumber,
            timestamp = System.currentTimeMillis(),
            shiftId = shiftId,
            cashierName = cashierName,
            ticketName = ticketName,
            diningOption = diningOption,
            subtotal = subtotal,
            taxAmount = taxAmount,
            discountAmount = discountAmount,
            tipAmount = tipAmount,
            totalAmount = totalAmount,
            paymentType = paymentType,
            paymentDetailsJson = paymentDetails,
            customerId = customerId,
            customerName = customerName,
            itemsSummaryJson = itemsSummary
        )
        val receiptId = database.receiptDao().insert(receipt)

        // 3. Update customer loyalty points (1% earned on total)
        if (customerId != null) {
            val pointsEarned = totalAmount * 0.01
            database.customerDao().recordPurchase(customerId, pointsEarned, totalAmount)
            database.loyaltyPointDao().insert(
                LoyaltyPoint(
                    customerId = customerId,
                    points = pointsEarned,
                    description = "Earned from sale $receiptNumber"
                )
            )
        }

        // 4. Update Shift Cash/Card totals
        if (currentShift != null) {
            val updatedShift = when (paymentType) {
                PaymentType.CASH -> currentShift.copy(
                    cashPayments = currentShift.cashPayments + totalAmount
                )
                PaymentType.CARD -> currentShift.copy(
                    cardPayments = currentShift.cardPayments + totalAmount
                )
                PaymentType.SPLIT -> {
                    // Extract cash from details if available or split 50/50
                    currentShift.copy(
                        cashPayments = currentShift.cashPayments + (totalAmount / 2),
                        cardPayments = currentShift.cardPayments + (totalAmount / 2)
                    )
                }
            }
            database.shiftDao().update(updatedShift)
        }

        // 5. Close open ticket if associated
        if (ticketId != null && ticketId > 0L) {
            database.ticketDao().closeTicket(ticketId)
        }

        return receipt.copy(id = receiptId)
    }

    suspend fun refundReceipt(receipt: ReceiptEntity) {
        database.receiptDao().markRefunded(receipt.id, System.currentTimeMillis())
        val currentShift = database.shiftDao().getCurrentShiftSync()
        if (currentShift != null && receipt.paymentType == PaymentType.CASH) {
            database.shiftDao().update(
                currentShift.copy(cashRefunds = currentShift.cashRefunds + receipt.totalAmount)
            )
        }
    }

    // Shift Management
    suspend fun openShift(startingCash: Double, cashierName: String): Long {
        val shift = ShiftEntity(
            openedAt = System.currentTimeMillis(),
            cashierName = cashierName,
            startingCash = startingCash,
            isOpen = true
        )
        return database.shiftDao().insert(shift)
    }

    suspend fun recordCashAction(isPayIn: Boolean, amount: Double, comment: String) {
        val currentShift = database.shiftDao().getCurrentShiftSync() ?: return
        val action = CashActionEntity(
            shiftId = currentShift.id,
            isPayIn = isPayIn,
            amount = amount,
            comment = comment
        )
        database.cashActionDao().insert(action)

        val updatedShift = if (isPayIn) {
            currentShift.copy(cashPaidIn = currentShift.cashPaidIn + amount)
        } else {
            currentShift.copy(cashPaidOut = currentShift.cashPaidOut + amount)
        }
        database.shiftDao().update(updatedShift)
    }

    suspend fun closeShift(actualCashCounted: Double) {
        val currentShift = database.shiftDao().getCurrentShiftSync() ?: return
        val updatedShift = currentShift.copy(
            closedAt = System.currentTimeMillis(),
            actualCashCounted = actualCashCounted,
            isOpen = false
        )
        database.shiftDao().update(updatedShift)
    }

    // Customers & Loyalty
    fun getLoyaltyPointsForCustomer(customerId: Long): Flow<List<LoyaltyPoint>> {
        return database.loyaltyPointDao().getPointsForCustomer(customerId)
    }

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            database.customerDao().insert(customer)
        } else {
            database.customerDao().update(customer)
            customer.id
        }
    }

    suspend fun redeemCustomerPoints(customerId: Long, points: Double) {
        database.customerDao().redeemPoints(customerId, points)
        database.loyaltyPointDao().insert(
            LoyaltyPoint(
                customerId = customerId,
                points = -points,
                description = "Redeemed $${String.format(Locale.US, "%.2f", points)} discount at checkout"
            )
        )
    }
}

