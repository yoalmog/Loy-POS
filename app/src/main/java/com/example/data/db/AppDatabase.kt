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
    version = 6,
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
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialBistroData(database)
                }
            }
        }

        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
            super.onDestructiveMigration(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialBistroData(database)
                }
            }
        }
    }
}

suspend fun populateInitialBistroData(db: AppDatabase) {
    // 1. Categories
    val catBurgers = CategoryEntity(id = 1, name = "Burgers", colorHex = "#D32F2F")
    val catCombos = CategoryEntity(id = 2, name = "Combos & Meals", colorHex = "#E65100")
    val catSides = CategoryEntity(id = 3, name = "Sides & Fries", colorHex = "#F57F17")
    val catDrinks = CategoryEntity(id = 4, name = "Cold Drinks", colorHex = "#0288D1")
    val catHot = CategoryEntity(id = 5, name = "Hot Drinks", colorHex = "#5D4037")
    val catDesserts = CategoryEntity(id = 6, name = "Desserts", colorHex = "#7B1FA2")

    db.categoryDao().insertAll(listOf(catBurgers, catCombos, catSides, catDrinks, catHot, catDesserts))

    // 2. Modifier Groups
    val modBurgerToppings = ModifierGroupEntity(
        id = 1,
        name = "Add-ons",
        optionsJson = "Extra Cheddar:1.00,Crispy Bacon:1.75,Pickled Jalapeños:0.75,Grilled Mushrooms:1.25,Fried Egg:1.50"
    )
    val modDrinkSizes = ModifierGroupEntity(
        id = 2,
        name = "Size",
        optionsJson = "Regular:0.00,Large:0.80,Monster Size:1.50"
    )
    val modDressing = ModifierGroupEntity(
        id = 3,
        name = "Sauces",
        optionsJson = "Truffle Mayo:0.50,Smoky BBQ:0.50,Garlic Herb:0.50,Spicy Sriracha:0.50"
    )
    db.modifierGroupDao().insertAll(listOf(modBurgerToppings, modDrinkSizes, modDressing))

    // 3. Fast Food & Bistro Items
    val items = listOf(
        ItemEntity(
            id = 1,
            name = "Classic Bistro Burger",
            categoryId = 1,
            categoryName = "Burgers",
            price = 9.50,
            cost = 3.80,
            sku = "1001",
            barcode = "200001001000",
            soldBy = SoldBy.EACH,
            inStock = 45.0,
            lowStockThreshold = 10.0,
            trackStock = true,
            colorHex = "#D32F2F",
            modifierIds = "1"
        ),
        ItemEntity(
            id = 2,
            name = "Double Bacon Cheeseburger",
            categoryId = 1,
            categoryName = "Burgers",
            price = 12.75,
            cost = 5.10,
            sku = "1002",
            barcode = "200001002000",
            soldBy = SoldBy.EACH,
            inStock = 30.0,
            lowStockThreshold = 8.0,
            trackStock = true,
            colorHex = "#C62828",
            modifierIds = "1"
        ),
        ItemEntity(
            id = 3,
            name = "Crispy Truffle Chicken Burger",
            categoryId = 1,
            categoryName = "Burgers",
            price = 11.00,
            cost = 4.20,
            sku = "1003",
            barcode = "200001003000",
            soldBy = SoldBy.EACH,
            inStock = 22.0,
            lowStockThreshold = 5.0,
            trackStock = true,
            colorHex = "#AD1457",
            modifierIds = "1,3"
        ),
        ItemEntity(
            id = 4,
            name = "Bistro Deluxe Combo",
            categoryId = 2,
            categoryName = "Combos & Meals",
            price = 15.50,
            cost = 5.90,
            sku = "1004",
            barcode = "200001004000",
            soldBy = SoldBy.EACH,
            inStock = 25.0,
            lowStockThreshold = 5.0,
            trackStock = true,
            colorHex = "#EF6C00",
            modifierIds = "2,3"
        ),
        ItemEntity(
            id = 5,
            name = "Family Feast Basket",
            categoryId = 2,
            categoryName = "Combos & Meals",
            price = 32.00,
            cost = 13.00,
            sku = "1005",
            barcode = "200001005000",
            soldBy = SoldBy.EACH,
            inStock = 12.0,
            lowStockThreshold = 4.0,
            trackStock = true,
            colorHex = "#E65100",
            modifierIds = ""
        ),
        ItemEntity(
            id = 6,
            name = "Rustic French Fries",
            categoryId = 3,
            categoryName = "Sides & Fries",
            price = 4.25,
            cost = 1.10,
            sku = "1006",
            barcode = "200001006000",
            soldBy = SoldBy.EACH,
            inStock = 60.0,
            lowStockThreshold = 15.0,
            trackStock = true,
            colorHex = "#F57F17",
            modifierIds = "3"
        ),
        ItemEntity(
            id = 7,
            name = "Crispy Onion Rings",
            categoryId = 3,
            categoryName = "Sides & Fries",
            price = 5.00,
            cost = 1.40,
            sku = "1007",
            barcode = "200001007000",
            soldBy = SoldBy.EACH,
            inStock = 40.0,
            lowStockThreshold = 10.0,
            trackStock = true,
            colorHex = "#F9A825",
            modifierIds = "3"
        ),
        ItemEntity(
            id = 8,
            name = "Fresh Deli Roast Beef",
            categoryId = 3,
            categoryName = "Sides & Fries",
            price = 18.00, // $18 per kg
            cost = 9.00,
            sku = "1008",
            barcode = "021008000000", // Weight-embedded barcode prefix
            soldBy = SoldBy.WEIGHT,
            inStock = 15.5, // 15.5 kg in stock
            lowStockThreshold = 3.0,
            trackStock = true,
            colorHex = "#8D6E63",
            modifierIds = ""
        ),
        ItemEntity(
            id = 9,
            name = "Craft Lemonade & Mint",
            categoryId = 4,
            categoryName = "Cold Drinks",
            price = 3.75,
            cost = 0.90,
            sku = "1009",
            barcode = "200001009000",
            soldBy = SoldBy.EACH,
            inStock = 50.0,
            lowStockThreshold = 10.0,
            trackStock = true,
            colorHex = "#00ACC1",
            modifierIds = "2"
        ),
        ItemEntity(
            id = 10,
            name = "Iced Vanilla Cold Brew",
            categoryId = 4,
            categoryName = "Cold Drinks",
            price = 4.95,
            cost = 1.20,
            sku = "1010",
            barcode = "200001010000",
            soldBy = SoldBy.EACH,
            inStock = 35.0,
            lowStockThreshold = 8.0,
            trackStock = true,
            colorHex = "#0288D1",
            modifierIds = "2"
        ),
        ItemEntity(
            id = 11,
            name = "Hot Caramel Cappuccino",
            categoryId = 5,
            categoryName = "Hot Drinks",
            price = 4.50,
            cost = 1.05,
            sku = "1011",
            barcode = "200001011000",
            soldBy = SoldBy.EACH,
            inStock = 40.0,
            lowStockThreshold = 8.0,
            trackStock = true,
            colorHex = "#6D4C41",
            modifierIds = "2"
        ),
        ItemEntity(
            id = 12,
            name = "Warm Chocolate Lava Cake",
            categoryId = 6,
            categoryName = "Desserts",
            price = 6.50,
            cost = 2.00,
            sku = "1012",
            barcode = "200001012000",
            soldBy = SoldBy.EACH,
            inStock = 18.0,
            lowStockThreshold = 5.0,
            trackStock = true,
            colorHex = "#6A1B9A",
            modifierIds = ""
        ),
        ItemEntity(
            id = 13,
            name = "Berry Cheesecake Slice",
            categoryId = 6,
            categoryName = "Desserts",
            price = 5.75,
            cost = 1.80,
            sku = "1013",
            barcode = "200001013000",
            soldBy = SoldBy.EACH,
            inStock = 4.0, // Low stock demo!
            lowStockThreshold = 5.0,
            trackStock = true,
            colorHex = "#8E24AA",
            modifierIds = ""
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
        Product(id = 1, name = "Classic Bistro Burger", price = 9.50, category = "Burgers", stockLevel = 45, imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd"),
        Product(id = 2, name = "Double Bacon Cheeseburger", price = 12.75, category = "Burgers", stockLevel = 30, imageUrl = "https://images.unsplash.com/photo-1586190848861-99aa4a171e90"),
        Product(id = 3, name = "Crispy Chicken Sandwich", price = 11.00, category = "Burgers", stockLevel = 22, imageUrl = "https://images.unsplash.com/photo-1625813506062-0aeb1d7a094b"),
        Product(id = 4, name = "Bistro Deluxe Combo", price = 15.50, category = "Combos", stockLevel = 25, imageUrl = "https://images.unsplash.com/photo-1594212699903-ec8a3eca50f5"),
        Product(id = 5, name = "Rustic French Fries", price = 4.25, category = "Sides", stockLevel = 60, imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877"),
        Product(id = 6, name = "Crispy Onion Rings", price = 5.00, category = "Sides", stockLevel = 40, imageUrl = "https://images.unsplash.com/photo-1639024471287-035216709848"),
        Product(id = 7, name = "Craft Lemonade & Mint", price = 3.75, category = "Drinks", stockLevel = 50, imageUrl = "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd"),
        Product(id = 8, name = "Iced Vanilla Cold Brew", price = 4.95, category = "Drinks", stockLevel = 35, imageUrl = "https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5"),
        Product(id = 9, name = "Chocolate Lava Cake", price = 6.50, category = "Desserts", stockLevel = 18, imageUrl = "https://images.unsplash.com/photo-1606313564200-e75d5e30476c"),
        Product(id = 10, name = "Berry Cheesecake Slice", price = 5.75, category = "Desserts", stockLevel = 4, imageUrl = "https://images.unsplash.com/photo-1533134242443-d4fd215305ad")
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

