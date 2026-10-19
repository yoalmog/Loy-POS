package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        ModifierGroupEntity::class,
        TicketEntity::class,
        TicketItemEntity::class,
        ShiftEntity::class,
        CashActionEntity::class,
        CustomerEntity::class,
        ReceiptEntity::class,
        Product::class,
        Ticket::class,
        Modifier::class,
        Shift::class,
        Timecard::class,
        LoyaltyPoint::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun productDao(): ProductDao
    abstract fun ticketOrderDao(): TicketOrderDao
    abstract fun modifierDao(): ModifierDao
    abstract fun shiftRecordDao(): ShiftRecordDao
    abstract fun timecardDao(): TimecardDao
    abstract fun modifierGroupDao(): ModifierGroupDao
    abstract fun ticketDao(): TicketDao
    abstract fun shiftDao(): ShiftDao
    abstract fun cashActionDao(): CashActionDao
    abstract fun customerDao(): CustomerDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun loyaltyPointDao(): LoyaltyPointDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loyverse_pos_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val CANONICAL_INITIAL_PRODUCTS = listOf(
            Product(id = 101, name = "Hamburguesa Clásica Angus", price = 58.00, category = "Hamburguesas y Carnes", stockLevel = 45, imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&auto=format&fit=crop&q=80"),
            Product(id = 102, name = "Smash Burger Doble", price = 64.00, category = "Hamburguesas y Carnes", stockLevel = 32, imageUrl = "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=600&auto=format&fit=crop&q=80"),
            Product(id = 103, name = "Pizza Margherita Napolitana", price = 52.00, category = "Pizzas y Hornadas", stockLevel = 60, imageUrl = "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=600&auto=format&fit=crop&q=80"),
            Product(id = 104, name = "Ensalada César Crujiente", price = 46.00, category = "Ensaladas y Guarniciones", stockLevel = 28, imageUrl = "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=600&auto=format&fit=crop&q=80"),
            Product(id = 105, name = "Papas Fritas Rústicas", price = 24.00, category = "Ensaladas y Guarniciones", stockLevel = 80, imageUrl = "https://images.unsplash.com/photo-1576107232684-1279f3908594?w=600&auto=format&fit=crop&q=80"),
            Product(id = 106, name = "Coca-Cola Zero (Botella)", price = 14.00, category = "Bebidas Frías", stockLevel = 120, imageUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=600&auto=format&fit=crop&q=80"),
            Product(id = 107, name = "Capuchino Artesanal", price = 15.00, category = "Café y Bebidas Calientes", stockLevel = 200, imageUrl = "https://images.unsplash.com/photo-1534778101976-62847782c213?w=600&auto=format&fit=crop&q=80"),
            Product(id = 108, name = "Cerveza de Barril Artesanal", price = 29.00, category = "Cervezas y Licores", stockLevel = 50, imageUrl = "https://images.unsplash.com/photo-1535958636474-b021ee887b13?w=600&auto=format&fit=crop&q=80"),
            Product(id = 109, name = "Volcán de Chocolate Fundido", price = 38.00, category = "Postres y Helados", stockLevel = 18, imageUrl = "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=600&auto=format&fit=crop&q=80")
        )
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (!com.example.data.api.SupabaseApiClient.isConfigured()) {
                        populateInitialBistroData(database)
                    }
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    if (!com.example.data.api.SupabaseApiClient.isConfigured()) {
                        populateInitialBistroData(database)
                    }
                }
            }
        }
    }
}

suspend fun populateInitialBistroData(db: AppDatabase) {
    // 1. Categories matching Back Office
    val categories = listOf(
        CategoryEntity(id = 1, name = "Hamburguesas y Carnes", colorHex = "#EF4444"),
        CategoryEntity(id = 2, name = "Pizzas y Hornadas", colorHex = "#F59E0B"),
        CategoryEntity(id = 3, name = "Ensaladas y Guarniciones", colorHex = "#10B981"),
        CategoryEntity(id = 4, name = "Bebidas Frías", colorHex = "#06B6D4"),
        CategoryEntity(id = 5, name = "Café y Bebidas Calientes", colorHex = "#8B5CF6"),
        CategoryEntity(id = 6, name = "Cervezas y Licores", colorHex = "#F97316"),
        CategoryEntity(id = 7, name = "Postres y Helados", colorHex = "#EC4899")
    )
    db.categoryDao().insertAll(categories)

    // 2. Modifier Groups
    val modBurgerToppings = ModifierGroupEntity(
        id = 1,
        name = "Ingredientes Adicionales",
        optionsJson = "Queso Cheddar Fundido:8.00,Cebolla Caramelizada:6.00,Huevo Frito:7.00,Tiras de Asado Ahumado:16.00"
    )
    val modDrinkSizes = ModifierGroupEntity(
        id = 2,
        name = "Tamaño",
        optionsJson = "Personal:0.00,Familiar:10.00"
    )
    val modDressing = ModifierGroupEntity(
        id = 3,
        name = "Salsas",
        optionsJson = "Mayonesa Trufada:4.00,Salsa BBQ Ahumada:3.50,Chimichurri Casero:4.00"
    )
    db.modifierGroupDao().insertAll(listOf(modBurgerToppings, modDrinkSizes, modDressing))

    // 3. Fast Food & Bistro Items with Real Image URLs
    val items = listOf(
        ItemEntity(
            id = 101,
            name = "Hamburguesa Clásica Angus",
            categoryId = 1,
            categoryName = "Hamburguesas y Carnes",
            price = 58.00,
            cost = 18.00,
            sku = "BGR-ANG-01",
            barcode = "729000100101",
            soldBy = SoldBy.EACH,
            inStock = 45.0,
            lowStockThreshold = 10.0,
            trackStock = true,
            colorHex = "#EF4444",
            modifierIds = "1",
            imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 102,
            name = "Smash Burger Doble",
            categoryId = 1,
            categoryName = "Hamburguesas y Carnes",
            price = 64.00,
            cost = 21.00,
            sku = "BGR-SMASH-02",
            barcode = "729000100102",
            soldBy = SoldBy.EACH,
            inStock = 32.0,
            lowStockThreshold = 8.0,
            trackStock = true,
            colorHex = "#DC2626",
            modifierIds = "1",
            imageUrl = "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 103,
            name = "Pizza Margherita Napolitana",
            categoryId = 2,
            categoryName = "Pizzas y Hornadas",
            price = 52.00,
            cost = 12.00,
            sku = "PIZ-MARG-01",
            barcode = "729000100103",
            soldBy = SoldBy.EACH,
            inStock = 60.0,
            lowStockThreshold = 12.0,
            trackStock = true,
            colorHex = "#F59E0B",
            modifierIds = "",
            imageUrl = "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 104,
            name = "Ensalada César Crujiente",
            categoryId = 3,
            categoryName = "Ensaladas y Guarniciones",
            price = 46.00,
            cost = 11.00,
            sku = "SLD-CES-01",
            barcode = "729000100104",
            soldBy = SoldBy.EACH,
            inStock = 28.0,
            lowStockThreshold = 6.0,
            trackStock = true,
            colorHex = "#10B981",
            modifierIds = "3",
            imageUrl = "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 105,
            name = "Papas Fritas Rústicas",
            categoryId = 3,
            categoryName = "Ensaladas y Guarniciones",
            price = 24.00,
            cost = 5.00,
            sku = "SID-FRY-01",
            barcode = "729000100105",
            soldBy = SoldBy.EACH,
            inStock = 80.0,
            lowStockThreshold = 20.0,
            trackStock = true,
            colorHex = "#059669",
            modifierIds = "3",
            imageUrl = "https://images.unsplash.com/photo-1576107232684-1279f3908594?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 106,
            name = "Coca-Cola Zero (Botella)",
            categoryId = 4,
            categoryName = "Bebidas Frías",
            price = 14.00,
            cost = 4.00,
            sku = "DRK-COK-01",
            barcode = "729000100106",
            soldBy = SoldBy.EACH,
            inStock = 120.0,
            lowStockThreshold = 24.0,
            trackStock = true,
            colorHex = "#06B6D4",
            modifierIds = "",
            imageUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 107,
            name = "Capuchino Artesanal",
            categoryId = 5,
            categoryName = "Café y Bebidas Calientes",
            price = 15.00,
            cost = 3.00,
            sku = "HOT-CAP-01",
            barcode = "729000100107",
            soldBy = SoldBy.EACH,
            inStock = 200.0,
            lowStockThreshold = 30.0,
            trackStock = false,
            colorHex = "#8B5CF6",
            modifierIds = "2",
            imageUrl = "https://images.unsplash.com/photo-1534778101976-62847782c213?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 108,
            name = "Cerveza de Barril Artesanal",
            categoryId = 6,
            categoryName = "Cervezas y Licores",
            price = 29.00,
            cost = 8.00,
            sku = "ALC-BER-01",
            barcode = "729000100108",
            soldBy = SoldBy.EACH,
            inStock = 50.0,
            lowStockThreshold = 15.0,
            trackStock = true,
            colorHex = "#F97316",
            modifierIds = "",
            imageUrl = "https://images.unsplash.com/photo-1535958636474-b021ee887b13?w=600&auto=format&fit=crop&q=80"
        ),
        ItemEntity(
            id = 109,
            name = "Volcán de Chocolate Fundido",
            categoryId = 7,
            categoryName = "Postres y Helados",
            price = 38.00,
            cost = 9.50,
            sku = "DES-CHOC-01",
            barcode = "729000100109",
            soldBy = SoldBy.EACH,
            inStock = 18.0,
            lowStockThreshold = 5.0,
            trackStock = true,
            colorHex = "#EC4899",
            modifierIds = "",
            imageUrl = "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=600&auto=format&fit=crop&q=80"
        )
    )
    db.itemDao().insertAll(items)

    // 4. Initial Customers
    val customers = listOf(
        CustomerEntity(
            id = 1,
            name = "Sarah Connor",
            phone = "+1 555-0142",
            email = "sarah.c@example.com",
            customerCode = "LOY8801",
            pointsBalance = 14.50,
            totalVisits = 12,
            totalSpent = 182.40,
            note = "Prefers gluten-free bun"
        ),
        CustomerEntity(
            id = 2,
            name = "Alex Mercer",
            phone = "+1 555-0199",
            email = "alex.m@example.com",
            customerCode = "LOY8802",
            pointsBalance = 8.20,
            totalVisits = 6,
            totalSpent = 94.50,
            note = "Loves spicy sauce"
        ),
        CustomerEntity(
            id = 3,
            name = "David Miller",
            phone = "+1 555-0177",
            email = "david.miller@example.com",
            customerCode = "LOY8803",
            pointsBalance = 25.00,
            totalVisits = 20,
            totalSpent = 310.00,
            note = "VIP Bistro Member"
        )
    )
    db.customerDao().insertAll(customers)

    // 5. Initial Open Shift
    val initialShift = ShiftEntity(
        id = 1,
        openedAt = System.currentTimeMillis() - 1000 * 60 * 120, // 2 hours ago
        cashierName = "Alex (Cashier)",
        startingCash = 150.00,
        cashPayments = 48.50,
        cashRefunds = 0.0,
        cashPaidIn = 20.00,
        cashPaidOut = 10.00,
        cardPayments = 76.25,
        actualCashCounted = null,
        isOpen = true
    )
    db.shiftDao().insert(initialShift)

    // 6. Pre-existing Receipts for reporting
    val r1 = ReceiptEntity(
        receiptNumber = "#1001",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 90,
        shiftId = 1,
        cashierName = "Alex (Cashier)",
        ticketName = "Table 1",
        diningOption = DiningOption.DINE_IN,
        subtotal = 25.50,
        taxAmount = 2.10,
        discountAmount = 0.0,
        tipAmount = 3.00,
        totalAmount = 30.60,
        paymentType = PaymentType.CARD,
        paymentDetailsJson = "Visa ending in 4012 (Approved)",
        customerName = "Sarah Connor",
        itemsSummaryJson = "Classic Bistro Burger x1, Rustic French Fries x1, Craft Lemonade x1"
    )
    val r2 = ReceiptEntity(
        receiptNumber = "#1002",
        timestamp = System.currentTimeMillis() - 1000 * 60 * 45,
        shiftId = 1,
        cashierName = "Alex (Cashier)",
        ticketName = "Takeout Order",
        diningOption = DiningOption.TAKEOUT,
        subtotal = 44.50,
        taxAmount = 3.67,
        discountAmount = 4.45,
        tipAmount = 0.0,
        totalAmount = 43.72,
        paymentType = PaymentType.CASH,
        paymentDetailsJson = "Cash Tendered: $50.00, Change: $6.28",
        customerName = "Alex Mercer",
        itemsSummaryJson = "Double Bacon Cheeseburger x2, Onion Rings x2, Iced Cold Brew x2"
    )
    db.receiptDao().insert(r1)
    db.receiptDao().insert(r2)

    // 7. Seed an active Open Table Ticket
    val openTicket = TicketEntity(
        id = 1,
        ticketName = "Table 4 - Patio",
        tableName = "Table 4",
        diningOption = DiningOption.DINE_IN,
        serverName = "Alex",
        comment = "Waiting for drinks first",
        createdAt = System.currentTimeMillis() - 1000 * 60 * 25,
        customerId = 1,
        customerName = "Sarah Connor",
        isClosed = false
    )
    db.ticketDao().insertTicket(openTicket)

    val openTicketItems = listOf(
        TicketItemEntity(
            ticketId = 1,
            itemId = 1,
            itemName = "Classic Bistro Burger",
            unitPrice = 9.50,
            quantity = 2.0,
            selectedModifiersJson = "Extra Cheddar (+$1.00)",
            modifiersCost = 1.00,
            comment = "Medium well",
            discountPercent = 0.0
        ),
        TicketItemEntity(
            ticketId = 1,
            itemId = 6,
            itemName = "Rustic French Fries",
            unitPrice = 4.25,
            quantity = 1.0,
            selectedModifiersJson = "Truffle Mayo (+$0.50)",
            modifiersCost = 0.50,
            comment = "",
            discountPercent = 0.0
        )
    )
    db.ticketDao().insertTicketItems(openTicketItems)

    // 8. Seed Products for Product Inventory
    val initialProducts = listOf(
        Product(id = 101, name = "Hamburguesa Clásica Angus", price = 58.00, category = "Hamburguesas y Carnes", stockLevel = 45, imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&auto=format&fit=crop&q=80"),
        Product(id = 102, name = "Smash Burger Doble", price = 64.00, category = "Hamburguesas y Carnes", stockLevel = 32, imageUrl = "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?w=600&auto=format&fit=crop&q=80"),
        Product(id = 103, name = "Pizza Margherita Napolitana", price = 52.00, category = "Pizzas y Hornadas", stockLevel = 60, imageUrl = "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=600&auto=format&fit=crop&q=80"),
        Product(id = 104, name = "Ensalada César Crujiente", price = 46.00, category = "Ensaladas y Guarniciones", stockLevel = 28, imageUrl = "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?w=600&auto=format&fit=crop&q=80"),
        Product(id = 105, name = "Papas Fritas Rústicas", price = 24.00, category = "Ensaladas y Guarniciones", stockLevel = 80, imageUrl = "https://images.unsplash.com/photo-1576107232684-1279f3908594?w=600&auto=format&fit=crop&q=80"),
        Product(id = 106, name = "Coca-Cola Zero (Botella)", price = 14.00, category = "Bebidas Frías", stockLevel = 120, imageUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=600&auto=format&fit=crop&q=80"),
        Product(id = 107, name = "Capuchino Artesanal", price = 15.00, category = "Café y Bebidas Calientes", stockLevel = 200, imageUrl = "https://images.unsplash.com/photo-1534778101976-62847782c213?w=600&auto=format&fit=crop&q=80"),
        Product(id = 108, name = "Cerveza de Barril Artesanal", price = 29.00, category = "Cervezas y Licores", stockLevel = 50, imageUrl = "https://images.unsplash.com/photo-1535958636474-b021ee887b13?w=600&auto=format&fit=crop&q=80"),
        Product(id = 109, name = "Volcán de Chocolate Fundido", price = 38.00, category = "Postres y Helados", stockLevel = 18, imageUrl = "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=600&auto=format&fit=crop&q=80")
    )
    db.productDao().insertAll(initialProducts)

    // 9. Seed open Tickets for multi-order management
    val initialTickets = listOf(
        Ticket(id = 1, name = "Table 1", tableName = "Table 1", diningOption = "Dine in", serverName = "Alex", note = "VIP guest"),
        Ticket(id = 2, name = "Table 4 - Patio", tableName = "Table 4", diningOption = "Dine in", serverName = "Alex", note = "Waiting for drinks first"),
        Ticket(id = 3, name = "Takeout #104", tableName = "Counter", diningOption = "Takeout", serverName = "Sarah", note = "Customer will pick up at 12:30")
    )
    for (t in initialTickets) {
        db.ticketOrderDao().insert(t)
    }

    // 10. Seed custom Modifiers
    val initialModifiers = listOf(
        Modifier(id = 1, name = "Extra Cheese", price = 1.00),
        Modifier(id = 2, name = "No Onions", price = 0.00),
        Modifier(id = 3, name = "Crispy Bacon", price = 1.75),
        Modifier(id = 4, name = "Pickled Jalapeños", price = 0.75),
        Modifier(id = 5, name = "Extra Truffle Mayo", price = 0.50),
        Modifier(id = 6, name = "Gluten-Free Bun", price = 1.25)
    )
    db.modifierDao().insertAll(initialModifiers)

    // 11. Seed initial Shift Record
    val shiftRecord = Shift(
        id = 1,
        cashierName = "Alex (Cashier)",
        startTime = System.currentTimeMillis() - 1000 * 60 * 120,
        endTime = null,
        openingCash = 150.00,
        closingCash = null,
        totalSales = 74.32,
        isOpen = true
    )
    db.shiftRecordDao().insert(shiftRecord)

    // 12. Seed initial Timecard session
    val timecard = Timecard(
        id = 1,
        employeeName = "Alex (Cashier)",
        clockInTime = System.currentTimeMillis() - 1000 * 60 * 120,
        clockOutTime = null,
        totalHours = 0.0,
        isClockedIn = true
    )
    db.timecardDao().insert(timecard)

    // 13. Seed initial Loyalty Points history
    val initialPoints = listOf(
        LoyaltyPoint(id = 1, customerId = 1, points = 14.50, description = "Welcome bonus & purchase reward"),
        LoyaltyPoint(id = 2, customerId = 2, points = 8.20, description = "Earned from receipt #1002"),
        LoyaltyPoint(id = 3, customerId = 3, points = 25.00, description = "VIP Loyalty Tier Bonus")
    )
    db.loyaltyPointDao().insertAll(initialPoints)
}

