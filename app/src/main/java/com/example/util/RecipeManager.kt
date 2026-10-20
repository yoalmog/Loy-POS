package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object RecipeManager {
    private const val TAG = "RecipeManager"
    private const val PREFS_NAME = "aura_pos_recipes"
    private const val KEY_RECIPES = "recipes_json"
    private const val KEY_TICKETS = "kitchen_tickets_json"

    private val _recipes = MutableStateFlow<Map<Long, DishRecipe>>(emptyMap())
    val recipes: StateFlow<Map<Long, DishRecipe>> = _recipes.asStateFlow()

    private val _kitchenTickets = MutableStateFlow<List<KitchenOrderTicket>>(emptyList())
    val kitchenTickets: StateFlow<List<KitchenOrderTicket>> = _kitchenTickets.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJson = prefs.getString(KEY_RECIPES, null)

        val loadedMap = mutableMapOf<Long, DishRecipe>()
        // 1. Seed defaults first
        loadedMap.putAll(DefaultRecipesSeed.getDefaultSeedRecipes())

        // 2. Overlay user custom recipes
        if (!savedJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val recipe = parseDishRecipeFromJson(obj)
                    loadedMap[recipe.itemId] = recipe
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing saved recipes", e)
            }
        }
        _recipes.value = loadedMap

        // 3. Load or seed initial kitchen tickets for KDS
        val savedTickets = prefs.getString(KEY_TICKETS, null)
        if (!savedTickets.isNullOrBlank()) {
            try {
                val array = JSONArray(savedTickets)
                val ticketList = mutableListOf<KitchenOrderTicket>()
                for (i in 0 until array.length()) {
                    ticketList.add(parseTicketFromJson(array.getJSONObject(i)))
                }
                _kitchenTickets.value = ticketList
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing kitchen tickets", e)
            }
        } else {
            // Seed 2 sample active kitchen tickets so the screen looks vibrant on first launch
            val sampleTickets = listOf(
                KitchenOrderTicket(
                    id = "kds_seed_1",
                    orderNumber = 1042L,
                    tableName = "שולחן 4",
                    serverName = "דניאל",
                    timestamp = System.currentTimeMillis() - 4 * 60 * 1000L,
                    status = KitchenTicketStatus.PREPARING,
                    items = listOf(
                        KitchenOrderItem(
                            id = 101L,
                            name = "Lomo Saltado Criollo",
                            quantity = 2,
                            notes = "בלי כוסברה, מידת עשייה מדיום",
                            recipe = loadedMap[101L]
                        ),
                        KitchenOrderItem(
                            id = 105L,
                            name = "Tequeños de Queso",
                            quantity = 1,
                            notes = "אקסטרה גוואקמולי בצד",
                            recipe = loadedMap[105L]
                        )
                    )
                ),
                KitchenOrderTicket(
                    id = "kds_seed_2",
                    orderNumber = 1043L,
                    tableName = "שולחן 7 (מרפסת)",
                    serverName = "מיכל",
                    timestamp = System.currentTimeMillis() - 11 * 60 * 1000L,
                    status = KitchenTicketStatus.NEW,
                    items = listOf(
                        KitchenOrderItem(
                            id = 103L,
                            name = "Hamburguesa Artesanal Doble",
                            quantity = 1,
                            notes = "בלי בצל, צ'דר מותך כפול",
                            recipe = loadedMap[103L]
                        ),
                        KitchenOrderItem(
                            id = 104L,
                            name = "Pizza Margherita Rústica",
                            quantity = 1,
                            notes = "אפייה קריספית בטאבון",
                            recipe = loadedMap[104L]
                        ),
                        KitchenOrderItem(
                            id = 106L,
                            name = "Chicha Morada Tradicional",
                            quantity = 2,
                            notes = "הרבה קרח",
                            recipe = loadedMap[106L]
                        )
                    )
                )
            )
            _kitchenTickets.value = sampleTickets
        }
    }

    fun getRecipeForItem(itemId: Long, itemName: String, categoryName: String = "כללי"): DishRecipe {
        return _recipes.value[itemId] ?: DishRecipe(
            itemId = itemId,
            itemName = itemName,
            categoryName = categoryName,
            ingredients = listOf(
                RecipeIngredient(name = "חומר גלם ראשי", quantity = 150.0, unit = "גרם", costPerUnit = 0.04),
                RecipeIngredient(name = "רוטב ותבלינים", quantity = 30.0, unit = "גרם", costPerUnit = 0.01)
            ),
            prepGuide = KitchenPrepGuide(
                prepTimeMinutes = 10,
                station = KitchenStation.GENERAL,
                steps = listOf(
                    "הכנת חומרי הגלם ושקילת המרכיבים לפי המשקל המדויק.",
                    "בישול או צלייה בטמפרטורת עבודה מבוקרת.",
                    "צלחות נקי ואסתטי והגשה מידית לחלון המסירות."
                ),
                allergens = emptyList(),
                chefNotes = "הקפדה על טריות המוצרים והגשה חמה.",
                platingGuide = "צלחת חרס מלבנית."
            )
        )
    }

    fun saveRecipe(context: Context, recipe: DishRecipe) {
        _recipes.update { current ->
            current + (recipe.itemId to recipe)
        }
        persistRecipes(context)
    }

    fun addTicketFromOrder(
        context: Context,
        orderNumber: Long,
        tableName: String,
        serverName: String,
        items: List<Pair<ItemEntity, Int>>
    ) {
        val kitchenItems = items.map { (item, qty) ->
            KitchenOrderItem(
                id = item.id,
                name = item.name,
                quantity = qty,
                notes = "",
                recipe = getRecipeForItem(item.id, item.name, item.categoryName)
            )
        }

        val newTicket = KitchenOrderTicket(
            orderNumber = orderNumber,
            tableName = tableName.ifBlank { "דלפק ראשי" },
            serverName = serverName.ifBlank { "קופאי" },
            timestamp = System.currentTimeMillis(),
            status = KitchenTicketStatus.NEW,
            items = kitchenItems
        )

        _kitchenTickets.update { listOf(newTicket) + it }
        persistTickets(context)
    }

    fun addTicketFromTicketItems(
        orderNumber: Long,
        tableName: String,
        serverName: String,
        items: List<TicketItemEntity>
    ) {
        val kitchenItems = items.map { item ->
            val note = if (item.comment.isNotBlank()) item.comment else item.selectedModifiersJson
            KitchenOrderItem(
                id = item.itemId,
                name = item.itemName,
                quantity = item.quantity.toInt().coerceAtLeast(1),
                notes = note,
                recipe = getRecipeForItem(item.itemId, item.itemName, "")
            )
        }

        val newTicket = KitchenOrderTicket(
            orderNumber = orderNumber,
            tableName = tableName.ifBlank { "דלפק ראשי" },
            serverName = serverName.ifBlank { "קופאי" },
            timestamp = System.currentTimeMillis(),
            status = KitchenTicketStatus.NEW,
            items = kitchenItems
        )

        _kitchenTickets.update { listOf(newTicket) + it }
    }

    fun simulateNewIncomingOrder(context: Context) {
        val currentRecipes = _recipes.value.values.toList()
        val randomOrderNumber = 1050L + (1..400).random()
        val randomTables = listOf("שולחן 2 (חלון)", "שולחן 5", "שולחן 9 (VIP)", "בר ראשי", "Takeout #12", "שולחן 14")
        val randomWaiters = listOf("דניאל", "נועה", "מיכל", "איתי", "שירה")

        val chosenRecipes = if (currentRecipes.isNotEmpty()) {
            currentRecipes.shuffled().take((1..3).random())
        } else emptyList()

        val items = chosenRecipes.map { recipe ->
            KitchenOrderItem(
                id = recipe.itemId,
                name = recipe.itemName,
                quantity = (1..2).random(),
                notes = if (listOf(true, false).random()) "דגש לטבח: הגשה לוהטת" else "",
                recipe = recipe
            )
        }

        val newTicket = KitchenOrderTicket(
            orderNumber = randomOrderNumber,
            tableName = randomTables.random(),
            serverName = randomWaiters.random(),
            timestamp = System.currentTimeMillis(),
            status = KitchenTicketStatus.NEW,
            items = items
        )

        _kitchenTickets.update { listOf(newTicket) + it }
        persistTickets(context)
    }

    fun updateTicketStatus(context: Context, ticketId: String, newStatus: KitchenTicketStatus) {
        _kitchenTickets.update { list ->
            list.map {
                if (it.id == ticketId) it.copy(status = newStatus) else it
            }
        }
        persistTickets(context)
    }

    fun toggleItemCompleted(context: Context, ticketId: String, itemId: Long) {
        _kitchenTickets.update { list ->
            list.map { ticket ->
                if (ticket.id == ticketId) {
                    val updatedItems = ticket.items.map { item ->
                        if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
                    }
                    val allDone = updatedItems.all { it.isCompleted }
                    ticket.copy(
                        items = updatedItems,
                        status = if (allDone) KitchenTicketStatus.READY else ticket.status
                    )
                } else ticket
            }
        }
        persistTickets(context)
    }

    /**
     * Automatic Raw Material Inventory Deduction
     * Triggered on successful POS payment/checkout
     */
    suspend fun deductRecipeStockFromDb(database: AppDatabase, itemId: Long, quantitySold: Double) {
        try {
            val recipe = _recipes.value[itemId] ?: return
            // Deduct each active ingredient from database stock
            for (ingredient in recipe.ingredients) {
                if (!ingredient.deductStock) continue
                val totalDeduction = ingredient.quantity * quantitySold

                // Find matching product in database inventory by name or category
                val products = database.productDao().getAllProductsSync()
                val matchedProduct = products.find {
                    it.name.contains(ingredient.name, ignoreCase = true) ||
                            ingredient.name.contains(it.name, ignoreCase = true)
                }

                if (matchedProduct != null) {
                    val newStock = (matchedProduct.stockLevel - totalDeduction.toInt()).coerceAtLeast(0)
                    database.productDao().update(matchedProduct.copy(stockLevel = newStock))
                    Log.d(TAG, "Deducted ${totalDeduction}${ingredient.unit} of ${ingredient.name}. New stock: $newStock")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deducting recipe stock", e)
        }
    }

    suspend fun deductRecipeStockOnSale(context: Context, itemId: Long, quantitySold: Double) {
        deductRecipeStockFromDb(AppDatabase.getDatabase(context), itemId, quantitySold)
    }

    private fun persistRecipes(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val array = JSONArray()
                _recipes.value.values.forEach { recipe ->
                    array.put(recipeToJson(recipe))
                }
                prefs.edit().putString(KEY_RECIPES, array.toString()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting recipes", e)
            }
        }
    }

    private fun persistTickets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val array = JSONArray()
                _kitchenTickets.value.take(20).forEach { ticket ->
                    array.put(ticketToJson(ticket))
                }
                prefs.edit().putString(KEY_TICKETS, array.toString()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting tickets", e)
            }
        }
    }

    // JSON serialization helpers
    private fun recipeToJson(r: DishRecipe): JSONObject {
        val obj = JSONObject()
        obj.put("itemId", r.itemId)
        obj.put("itemName", r.itemName)
        obj.put("categoryName", r.categoryName)

        val ingArr = JSONArray()
        r.ingredients.forEach { ing ->
            val iObj = JSONObject()
            iObj.put("id", ing.id)
            iObj.put("name", ing.name)
            iObj.put("quantity", ing.quantity)
            iObj.put("unit", ing.unit)
            iObj.put("costPerUnit", ing.costPerUnit)
            iObj.put("deductStock", ing.deductStock)
            ingArr.put(iObj)
        }
        obj.put("ingredients", ingArr)

        val prepObj = JSONObject()
        prepObj.put("prepTimeMinutes", r.prepGuide.prepTimeMinutes)
        prepObj.put("station", r.prepGuide.station.name)
        prepObj.put("chefNotes", r.prepGuide.chefNotes)
        prepObj.put("platingGuide", r.prepGuide.platingGuide)

        val stepsArr = JSONArray()
        r.prepGuide.steps.forEach { stepsArr.put(it) }
        prepObj.put("steps", stepsArr)

        val allerArr = JSONArray()
        r.prepGuide.allergens.forEach { allerArr.put(it) }
        prepObj.put("allergens", allerArr)

        obj.put("prepGuide", prepObj)
        return obj
    }

    private fun parseDishRecipeFromJson(obj: JSONObject): DishRecipe {
        val itemId = obj.optLong("itemId", 0L)
        val itemName = obj.optString("itemName", "")
        val categoryName = obj.optString("categoryName", "כללי")

        val ingredients = mutableListOf<RecipeIngredient>()
        val ingArr = obj.optJSONArray("ingredients")
        if (ingArr != null) {
            for (i in 0 until ingArr.length()) {
                val iObj = ingArr.getJSONObject(i)
                ingredients.add(
                    RecipeIngredient(
                        id = iObj.optString("id", UUID.randomUUID().toString()),
                        name = iObj.optString("name", ""),
                        quantity = iObj.optDouble("quantity", 0.0),
                        unit = iObj.optString("unit", "גרם"),
                        costPerUnit = iObj.optDouble("costPerUnit", 0.0),
                        deductStock = iObj.optBoolean("deductStock", true)
                    )
                )
            }
        }

        val prepObj = obj.optJSONObject("prepGuide")
        val prepGuide = if (prepObj != null) {
            val stationStr = prepObj.optString("station", KitchenStation.GENERAL.name)
            val station = try { KitchenStation.valueOf(stationStr) } catch (_: Exception) { KitchenStation.GENERAL }
            val steps = mutableListOf<String>()
            val stepsArr = prepObj.optJSONArray("steps")
            if (stepsArr != null) {
                for (s in 0 until stepsArr.length()) steps.add(stepsArr.getString(s))
            }
            val allergens = mutableListOf<String>()
            val allerArr = prepObj.optJSONArray("allergens")
            if (allerArr != null) {
                for (a in 0 until allerArr.length()) allergens.add(allerArr.getString(a))
            }

            KitchenPrepGuide(
                prepTimeMinutes = prepObj.optInt("prepTimeMinutes", 10),
                station = station,
                steps = steps,
                allergens = allergens,
                chefNotes = prepObj.optString("chefNotes", ""),
                platingGuide = prepObj.optString("platingGuide", "")
            )
        } else KitchenPrepGuide()

        return DishRecipe(itemId, itemName, categoryName, ingredients, prepGuide)
    }

    private fun ticketToJson(t: KitchenOrderTicket): JSONObject {
        val obj = JSONObject()
        obj.put("id", t.id)
        obj.put("orderNumber", t.orderNumber)
        obj.put("tableName", t.tableName)
        obj.put("serverName", t.serverName)
        obj.put("timestamp", t.timestamp)
        obj.put("status", t.status.name)

        val itemsArr = JSONArray()
        t.items.forEach { item ->
            val iObj = JSONObject()
            iObj.put("id", item.id)
            iObj.put("name", item.name)
            iObj.put("quantity", item.quantity)
            iObj.put("notes", item.notes)
            iObj.put("isCompleted", item.isCompleted)
            item.recipe?.let { iObj.put("recipe", recipeToJson(it)) }
            itemsArr.put(iObj)
        }
        obj.put("items", itemsArr)
        return obj
    }

    private fun parseTicketFromJson(obj: JSONObject): KitchenOrderTicket {
        val id = obj.optString("id", UUID.randomUUID().toString())
        val orderNumber = obj.optLong("orderNumber", 1000L)
        val tableName = obj.optString("tableName", "שולחן")
        val serverName = obj.optString("serverName", "קופאי")
        val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        val status = try {
            KitchenTicketStatus.valueOf(obj.optString("status", KitchenTicketStatus.NEW.name))
        } catch (_: Exception) { KitchenTicketStatus.NEW }

        val items = mutableListOf<KitchenOrderItem>()
        val itemsArr = obj.optJSONArray("items")
        if (itemsArr != null) {
            for (i in 0 until itemsArr.length()) {
                val iObj = itemsArr.getJSONObject(i)
                val recipe = if (iObj.has("recipe")) parseDishRecipeFromJson(iObj.getJSONObject("recipe")) else null
                items.add(
                    KitchenOrderItem(
                        id = iObj.optLong("id", 0L),
                        name = iObj.optString("name", ""),
                        quantity = iObj.optInt("quantity", 1),
                        notes = iObj.optString("notes", ""),
                        recipe = recipe,
                        isCompleted = iObj.optBoolean("isCompleted", false)
                    )
                )
            }
        }

        return KitchenOrderTicket(id, orderNumber, tableName, serverName, timestamp, items, status)
    }
}
