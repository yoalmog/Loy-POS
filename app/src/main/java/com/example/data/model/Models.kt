package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DiningOption(val label: String) {
    DINE_IN("Dine in"),
    TAKEOUT("Takeout"),
    DELIVERY("Delivery")
}

enum class SoldBy(val label: String) {
    EACH("Each"),
    WEIGHT("Weight")
}

enum class PaymentType(val label: String) {
    CASH("Cash"),
    CARD("Card"),
    SPLIT("Split Payment")
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#2E7D32"
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val category: String,
    val stockLevel: Int,
    val imageUrl: String = ""
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long,
    val categoryName: String,
    val price: Double,
    val cost: Double = 0.0,
    val sku: String = "",
    val barcode: String = "",
    val soldBy: SoldBy = SoldBy.EACH,
    val inStock: Double = 0.0,
    val lowStockThreshold: Double = 5.0,
    val trackStock: Boolean = true,
    val colorHex: String = "#4CAF50",
    val modifierIds: String = "" // Comma-separated ModifierGroup ids
)

@Entity(tableName = "modifiers")
data class Modifier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double = 0.0,
    val categoryId: Long? = null
)

@Entity(tableName = "shift_records")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashierName: String = "Cashier",
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val openingCash: Double = 100.0,
    val closingCash: Double? = null,
    val totalSales: Double = 0.0,
    val isOpen: Boolean = true
)

@Entity(tableName = "timecards")
data class Timecard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeName: String,
    val clockInTime: Long = System.currentTimeMillis(),
    val clockOutTime: Long? = null,
    val totalHours: Double = 0.0,
    val isClockedIn: Boolean = true
)

@Entity(tableName = "modifier_groups")
data class ModifierGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. "Toppings", "Sauces", "Sizes"
    val optionsJson: String // Serialized list of name:price e.g. "Cheddar Cheese:1.0,Bacon:1.5,Jalapenos:0.75"
)

data class ModifierOption(
    val name: String,
    val price: Double
)

@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val tableName: String = "",
    val diningOption: String = "Dine in",
    val serverName: String = "Server",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false
)

@Entity(tableName = "open_tickets")
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketName: String,
    val tableName: String = "",
    val diningOption: DiningOption = DiningOption.DINE_IN,
    val serverName: String = "Server",
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val customerId: Long? = null,
    val customerName: String? = null,
    val isClosed: Boolean = false
)

@Entity(tableName = "ticket_items")
data class TicketItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketId: Long,
    val itemId: Long,
    val itemName: String,
    val unitPrice: Double,
    val quantity: Double = 1.0,
    val selectedModifiersJson: String = "", // e.g. "Bacon (+$1.50), Cheddar (+$1.00)"
    val modifiersCost: Double = 0.0,
    val comment: String = "",
    val discountPercent: Double = 0.0
) {
    val lineTotal: Double
        get() {
            val base = (unitPrice + modifiersCost) * quantity
            val discount = base * (discountPercent / 100.0)
            return (base - discount).coerceAtLeast(0.0)
        }
}

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val cashierName: String = "Manager",
    val startingCash: Double = 100.0,
    val cashPayments: Double = 0.0,
    val cashRefunds: Double = 0.0,
    val cashPaidIn: Double = 0.0,
    val cashPaidOut: Double = 0.0,
    val cardPayments: Double = 0.0,
    val actualCashCounted: Double? = null,
    val isOpen: Boolean = true
) {
    val expectedCash: Double
        get() = startingCash + cashPayments - cashRefunds + cashPaidIn - cashPaidOut

    val cashDifference: Double
        get() = (actualCashCounted ?: expectedCash) - expectedCash
}

@Entity(tableName = "cash_actions")
data class CashActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long,
    val isPayIn: Boolean, // true = Pay In, false = Pay Out
    val amount: Double,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val customerCode: String = "",
    val pointsBalance: Double = 0.0,
    val totalVisits: Int = 0,
    val totalSpent: Double = 0.0,
    val note: String = ""
)

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val shiftId: Long,
    val cashierName: String,
    val ticketName: String,
    val diningOption: DiningOption,
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val tipAmount: Double = 0.0,
    val totalAmount: Double,
    val paymentType: PaymentType,
    val paymentDetailsJson: String, // Breakdown if split or card auth
    val customerId: Long? = null,
    val customerName: String? = null,
    val isRefunded: Boolean = false,
    val refundTimestamp: Long? = null,
    val itemsSummaryJson: String = "" // List of item names & quantities
)

@Entity(tableName = "loyalty_points")
data class LoyaltyPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val points: Double,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

