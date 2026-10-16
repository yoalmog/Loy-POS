package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DatabaseStats(
    val categoryCount: Int = 0,
    val productCount: Int = 0,
    val itemCount: Int = 0,
    val shiftCount: Int = 0,
    val receiptCount: Int = 0,
    val customerCount: Int = 0,
    val timecardCount: Int = 0,
    val loyaltyPointCount: Int = 0
) {
    val totalRecords: Int
        get() = categoryCount + productCount + itemCount + shiftCount + receiptCount + customerCount + timecardCount + loyaltyPointCount
}

enum class ImportMode {
    MERGE,
    OVERWRITE
}

data class ImportResult(
    val success: Boolean,
    val message: String,
    val stats: DatabaseStats = DatabaseStats()
)

object DatabaseBackupManager {

    suspend fun getDatabaseStats(database: AppDatabase): DatabaseStats = withContext(Dispatchers.IO) {
        try {
            val categories = database.categoryDao().getAllCategories().first()
            val products = database.productDao().getAllProducts().first()
            val items = database.itemDao().getAllItems().first()
            val shifts = database.shiftDao().getAllShifts().first()
            val receipts = database.receiptDao().getAllReceipts().first()
            val customers = database.customerDao().getAllCustomers().first()
            val timecards = database.timecardDao().getAllTimecards().first()

            DatabaseStats(
                categoryCount = categories.size,
                productCount = products.size,
                itemCount = items.size,
                shiftCount = shifts.size,
                receiptCount = receipts.size,
                customerCount = customers.size,
                timecardCount = timecards.size
            )
        } catch (e: Exception) {
            DatabaseStats()
        }
    }

    suspend fun exportToJsonString(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        // Metadata
        val meta = JSONObject()
        meta.put("version", 1)
        meta.put("appName", "AuraPOS")
        meta.put("storeName", "Bistro & Grill #1")
        meta.put("exportedAt", System.currentTimeMillis())
        meta.put("exportedAtFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("metadata", meta)

        // Categories
        val categories = database.categoryDao().getAllCategories().first()
        val categoriesArr = JSONArray()
        for (c in categories) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("colorHex", c.colorHex)
            categoriesArr.put(obj)
        }
        root.put("categories", categoriesArr)

        // Products
        val products = database.productDao().getAllProducts().first()
        val productsArr = JSONArray()
        for (p in products) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("price", p.price)
            obj.put("category", p.category)
            obj.put("stockLevel", p.stockLevel)
            obj.put("imageUrl", p.imageUrl)
            productsArr.put(obj)
        }
        root.put("products", productsArr)

        // Items
        val items = database.itemDao().getAllItems().first()
        val itemsArr = JSONArray()
        for (i in items) {
            val obj = JSONObject()
            obj.put("id", i.id)
            obj.put("name", i.name)
            obj.put("categoryId", i.categoryId)
            obj.put("categoryName", i.categoryName)
            obj.put("price", i.price)
            obj.put("cost", i.cost)
            obj.put("sku", i.sku)
            obj.put("barcode", i.barcode)
            obj.put("soldBy", i.soldBy.name)
            obj.put("inStock", i.inStock)
            obj.put("lowStockThreshold", i.lowStockThreshold)
            obj.put("trackStock", i.trackStock)
            obj.put("colorHex", i.colorHex)
            obj.put("modifierIds", i.modifierIds)
            itemsArr.put(obj)
        }
        root.put("items", itemsArr)

        // Modifier Groups
        val modifierGroups = database.modifierGroupDao().getAllModifierGroups().first()
        val mgArr = JSONArray()
        for (mg in modifierGroups) {
            val obj = JSONObject()
            obj.put("id", mg.id)
            obj.put("name", mg.name)
            obj.put("optionsJson", mg.optionsJson)
            mgArr.put(obj)
        }
        root.put("modifierGroups", mgArr)

        // Modifiers
        val modifiers = database.modifierDao().getAllModifiers().first()
        val modifiersArr = JSONArray()
        for (m in modifiers) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            obj.put("price", m.price)
            m.categoryId?.let { obj.put("categoryId", it) }
            modifiersArr.put(obj)
        }
        root.put("modifiers", modifiersArr)

        // Shifts
        val shifts = database.shiftDao().getAllShifts().first()
        val shiftsArr = JSONArray()
        for (s in shifts) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("openedAt", s.openedAt)
            s.closedAt?.let { obj.put("closedAt", it) }
            obj.put("cashierName", s.cashierName)
            obj.put("startingCash", s.startingCash)
            obj.put("cashPayments", s.cashPayments)
            obj.put("cashRefunds", s.cashRefunds)
            obj.put("cashPaidIn", s.cashPaidIn)
            obj.put("cashPaidOut", s.cashPaidOut)
            obj.put("cardPayments", s.cardPayments)
            s.actualCashCounted?.let { obj.put("actualCashCounted", it) }
            obj.put("isOpen", s.isOpen)
            shiftsArr.put(obj)
        }
        root.put("shifts", shiftsArr)

        // Customers
        val customers = database.customerDao().getAllCustomers().first()
        val customersArr = JSONArray()
        for (cust in customers) {
            val obj = JSONObject()
            obj.put("id", cust.id)
            obj.put("name", cust.name)
            obj.put("phone", cust.phone)
            obj.put("email", cust.email)
            obj.put("customerCode", cust.customerCode)
            obj.put("pointsBalance", cust.pointsBalance)
            obj.put("totalVisits", cust.totalVisits)
            obj.put("totalSpent", cust.totalSpent)
            obj.put("note", cust.note)
            customersArr.put(obj)
        }
        root.put("customers", customersArr)

        // Receipts
        val receipts = database.receiptDao().getAllReceipts().first()
        val receiptsArr = JSONArray()
        for (r in receipts) {
            val obj = JSONObject()
            obj.put("id", r.id)
            obj.put("receiptNumber", r.receiptNumber)
            obj.put("timestamp", r.timestamp)
            obj.put("shiftId", r.shiftId)
            obj.put("cashierName", r.cashierName)
            obj.put("ticketName", r.ticketName)
            obj.put("diningOption", r.diningOption.name)
            obj.put("subtotal", r.subtotal)
            obj.put("taxAmount", r.taxAmount)
            obj.put("discountAmount", r.discountAmount)
            obj.put("tipAmount", r.tipAmount)
            obj.put("totalAmount", r.totalAmount)
            obj.put("paymentType", r.paymentType.name)
            obj.put("paymentDetailsJson", r.paymentDetailsJson)
            r.customerId?.let { obj.put("customerId", it) }
            r.customerName?.let { obj.put("customerName", it) }
            obj.put("isRefunded", r.isRefunded)
            r.refundTimestamp?.let { obj.put("refundTimestamp", it) }
            obj.put("itemsSummaryJson", r.itemsSummaryJson)
            receiptsArr.put(obj)
        }
        root.put("receipts", receiptsArr)

        // Timecards
        val timecards = database.timecardDao().getAllTimecards().first()
        val timecardsArr = JSONArray()
        for (t in timecards) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("employeeName", t.employeeName)
            obj.put("clockInTime", t.clockInTime)
            t.clockOutTime?.let { obj.put("clockOutTime", it) }
            obj.put("totalHours", t.totalHours)
            obj.put("isClockedIn", t.isClockedIn)
            timecardsArr.put(obj)
        }
        root.put("timecards", timecardsArr)

        root.toString(2)
    }

    suspend fun exportToFile(context: Context, database: AppDatabase, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = exportToJsonString(database)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonString.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromFile(
        context: Context,
        database: AppDatabase,
        uri: Uri,
        mode: ImportMode
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        content.append(line)
                        line = reader.readLine()
                    }
                }
            }
            importFromJsonString(database, content.toString(), mode)
        } catch (e: Exception) {
            ImportResult(false, "Failed to read backup file: ${e.localizedMessage}")
        }
    }

    suspend fun importFromJsonString(
        database: AppDatabase,
        jsonString: String,
        mode: ImportMode
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            var catCount = 0
            var prodCount = 0
            var itemCount = 0
            var shiftCount = 0
            var receiptCount = 0
            var custCount = 0
            var tcCount = 0

            database.runInTransaction {
                // Parse Categories
                if (root.has("categories")) {
                    val arr = root.getJSONArray("categories")
                    catCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = CategoryEntity(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            name = obj.getString("name"),
                            colorHex = obj.optString("colorHex", "#2E7D32")
                        )
                        kotlinx.coroutines.runBlocking { database.categoryDao().insert(entity) }
                    }
                }

                // Parse Products
                if (root.has("products")) {
                    val arr = root.getJSONArray("products")
                    prodCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = Product(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            name = obj.getString("name"),
                            price = obj.getDouble("price"),
                            category = obj.getString("category"),
                            stockLevel = obj.optInt("stockLevel", 10),
                            imageUrl = obj.optString("imageUrl", "")
                        )
                        kotlinx.coroutines.runBlocking { database.productDao().insert(entity) }
                    }
                }

                // Parse Items
                if (root.has("items")) {
                    val arr = root.getJSONArray("items")
                    itemCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val soldByEnum = try {
                            SoldBy.valueOf(obj.optString("soldBy", "EACH"))
                        } catch (e: Exception) {
                            SoldBy.EACH
                        }
                        val entity = ItemEntity(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            name = obj.getString("name"),
                            categoryId = obj.optLong("categoryId", 1L),
                            categoryName = obj.optString("categoryName", "General"),
                            price = obj.getDouble("price"),
                            cost = obj.optDouble("cost", 0.0),
                            sku = obj.optString("sku", ""),
                            barcode = obj.optString("barcode", ""),
                            soldBy = soldByEnum,
                            inStock = obj.optDouble("inStock", 10.0),
                            lowStockThreshold = obj.optDouble("lowStockThreshold", 5.0),
                            trackStock = obj.optBoolean("trackStock", true),
                            colorHex = obj.optString("colorHex", "#4CAF50"),
                            modifierIds = obj.optString("modifierIds", "")
                        )
                        kotlinx.coroutines.runBlocking { database.itemDao().insert(entity) }
                    }
                }

                // Parse Modifiers
                if (root.has("modifiers")) {
                    val arr = root.getJSONArray("modifiers")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = Modifier(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            name = obj.getString("name"),
                            price = obj.optDouble("price", 0.0),
                            categoryId = if (obj.has("categoryId")) obj.getLong("categoryId") else null
                        )
                        kotlinx.coroutines.runBlocking { database.modifierDao().insert(entity) }
                    }
                }

                // Parse Shifts
                if (root.has("shifts")) {
                    val arr = root.getJSONArray("shifts")
                    shiftCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = ShiftEntity(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            openedAt = obj.optLong("openedAt", System.currentTimeMillis()),
                            closedAt = if (obj.has("closedAt")) obj.getLong("closedAt") else null,
                            cashierName = obj.optString("cashierName", "Manager"),
                            startingCash = obj.optDouble("startingCash", 100.0),
                            cashPayments = obj.optDouble("cashPayments", 0.0),
                            cashRefunds = obj.optDouble("cashRefunds", 0.0),
                            cashPaidIn = obj.optDouble("cashPaidIn", 0.0),
                            cashPaidOut = obj.optDouble("cashPaidOut", 0.0),
                            cardPayments = obj.optDouble("cardPayments", 0.0),
                            actualCashCounted = if (obj.has("actualCashCounted")) obj.getDouble("actualCashCounted") else null,
                            isOpen = obj.optBoolean("isOpen", false)
                        )
                        kotlinx.coroutines.runBlocking { database.shiftDao().insert(entity) }
                    }
                }

                // Parse Customers
                if (root.has("customers")) {
                    val arr = root.getJSONArray("customers")
                    custCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = CustomerEntity(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            customerCode = obj.optString("customerCode", ""),
                            pointsBalance = obj.optDouble("pointsBalance", 0.0),
                            totalVisits = obj.optInt("totalVisits", 0),
                            totalSpent = obj.optDouble("totalSpent", 0.0),
                            note = obj.optString("note", "")
                        )
                        kotlinx.coroutines.runBlocking { database.customerDao().insert(entity) }
                    }
                }

                // Parse Receipts
                if (root.has("receipts")) {
                    val arr = root.getJSONArray("receipts")
                    receiptCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val diningOpt = try {
                            DiningOption.valueOf(obj.optString("diningOption", "DINE_IN"))
                        } catch (e: Exception) {
                            DiningOption.DINE_IN
                        }
                        val paymentTp = try {
                            PaymentType.valueOf(obj.optString("paymentType", "CASH"))
                        } catch (e: Exception) {
                            PaymentType.CASH
                        }

                        val entity = ReceiptEntity(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            receiptNumber = obj.getString("receiptNumber"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            shiftId = obj.optLong("shiftId", 1L),
                            cashierName = obj.optString("cashierName", "Cashier"),
                            ticketName = obj.optString("ticketName", "Ticket"),
                            diningOption = diningOpt,
                            subtotal = obj.optDouble("subtotal", 0.0),
                            taxAmount = obj.optDouble("taxAmount", 0.0),
                            discountAmount = obj.optDouble("discountAmount", 0.0),
                            tipAmount = obj.optDouble("tipAmount", 0.0),
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            paymentType = paymentTp,
                            paymentDetailsJson = obj.optString("paymentDetailsJson", "{}"),
                            customerId = if (obj.has("customerId")) obj.getLong("customerId") else null,
                            customerName = if (obj.has("customerName")) obj.getString("customerName") else null,
                            isRefunded = obj.optBoolean("isRefunded", false),
                            refundTimestamp = if (obj.has("refundTimestamp")) obj.getLong("refundTimestamp") else null,
                            itemsSummaryJson = obj.optString("itemsSummaryJson", "")
                        )
                        kotlinx.coroutines.runBlocking { database.receiptDao().insert(entity) }
                    }
                }

                // Parse Timecards
                if (root.has("timecards")) {
                    val arr = root.getJSONArray("timecards")
                    tcCount = arr.length()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = Timecard(
                            id = if (mode == ImportMode.MERGE) obj.optLong("id", 0L) else 0L,
                            employeeName = obj.getString("employeeName"),
                            clockInTime = obj.optLong("clockInTime", System.currentTimeMillis()),
                            clockOutTime = if (obj.has("clockOutTime")) obj.getLong("clockOutTime") else null,
                            totalHours = obj.optDouble("totalHours", 0.0),
                            isClockedIn = obj.optBoolean("isClockedIn", false)
                        )
                        kotlinx.coroutines.runBlocking { database.timecardDao().insert(entity) }
                    }
                }
            }

            val importedStats = DatabaseStats(
                categoryCount = catCount,
                productCount = prodCount,
                itemCount = itemCount,
                shiftCount = shiftCount,
                receiptCount = receiptCount,
                customerCount = custCount,
                timecardCount = tcCount
            )

            ImportResult(
                success = true,
                message = "Successfully restored ${importedStats.totalRecords} records into Room database",
                stats = importedStats
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ImportResult(
                success = false,
                message = "Backup restore failed: ${e.localizedMessage ?: "Invalid JSON syntax"}"
            )
        }
    }
}
