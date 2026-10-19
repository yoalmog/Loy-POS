package com.example.util

import android.content.Context
import com.example.data.api.SupabaseApiClient
import com.example.data.api.SupabaseItemDto
import com.example.data.api.SupabaseReceiptDto
import com.example.data.model.ItemEntity
import com.example.data.model.Product
import com.example.data.repository.PosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncState(val label: String) {
    SYNCED("Synced"),
    SYNCING("Syncing..."),
    OFFLINE_LOCAL_MODE("Offline (Local Mode)"),
    ERROR("Sync Warning")
}

class SyncManager(
    context: Context,
    private val repository: PosRepository
) {
    val networkMonitor = NetworkMonitor(context)

    private val _syncState = MutableStateFlow(
        if (networkMonitor.isOnline.value) SyncState.SYNCED else SyncState.OFFLINE_LOCAL_MODE
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    performAutoSync()
                } else {
                    _syncState.value = SyncState.OFFLINE_LOCAL_MODE
                }
            }
        }
    }

    fun triggerSync() {
        scope.launch {
            if (!networkMonitor.isOnline.value) {
                _syncState.value = SyncState.OFFLINE_LOCAL_MODE
                return@launch
            }

            _syncState.value = SyncState.SYNCING
            try {
                performRealDatabaseSync()
                _pendingSyncCount.value = 0
                _lastSyncTimestamp.value = System.currentTimeMillis()
                _syncState.value = SyncState.SYNCED
            } catch (e: Exception) {
                _syncState.value = SyncState.ERROR
            }
        }
    }

    private suspend fun performAutoSync() {
        _syncState.value = SyncState.SYNCING
        try {
            performRealDatabaseSync()
            _pendingSyncCount.value = 0
            _lastSyncTimestamp.value = System.currentTimeMillis()
            _syncState.value = SyncState.SYNCED
        } catch (e: Exception) {
            _syncState.value = SyncState.ERROR
        }
    }

    private suspend fun performRealDatabaseSync() = withContext(Dispatchers.IO) {
        var syncedAny = false

        // 1. Primary Sync: Fetch live snapshot from Back Office API
        try {
            val syncResponse = com.example.data.api.BackOfficeApiClient.api.getSyncSnapshot()
            if (syncResponse.categories.isNotEmpty()) {
                val localCategories = syncResponse.categories.map { catDto ->
                    com.example.data.model.CategoryEntity(
                        id = catDto.id ?: 0L,
                        name = catDto.name,
                        colorHex = catDto.color_hex ?: "#2E7D32"
                    )
                }
                repository.database.categoryDao().insertAll(localCategories)
            }

            if (syncResponse.items.isNotEmpty()) {
                val localItems = syncResponse.items.map { dto ->
                    ItemEntity(
                        id = dto.id ?: 0L,
                        name = dto.name,
                        categoryId = dto.category_id ?: 1L,
                        categoryName = dto.category_name ?: "General",
                        price = dto.price,
                        cost = dto.cost ?: 0.0,
                        sku = dto.sku ?: "",
                        barcode = dto.barcode ?: "",
                        soldBy = if (dto.sold_by == "WEIGHT") com.example.data.model.SoldBy.WEIGHT else com.example.data.model.SoldBy.EACH,
                        inStock = dto.in_stock,
                        lowStockThreshold = dto.low_stock_threshold,
                        trackStock = dto.track_stock,
                        colorHex = dto.color_hex ?: "#0288D1",
                        modifierIds = dto.modifier_ids ?: "",
                        imageUrl = dto.image_url ?: ""
                    )
                }
                repository.insertItemsFromSync(localItems)

                // Keep Product inventory table in sync with ItemEntity
                val products = localItems.map { item ->
                    Product(
                        id = item.id,
                        name = item.name,
                        price = item.price,
                        category = item.categoryName,
                        stockLevel = item.inStock.toInt(),
                        imageUrl = item.imageUrl
                    )
                }
                repository.database.productDao().insertAll(products)
                syncedAny = true
            }
        } catch (e: Exception) {
            // Back Office network error - will attempt fallback
        }

        // 2. Secondary Sync: Direct Supabase client if configured
        if (SupabaseApiClient.isConfigured()) {
            try {
                val remoteItems = SupabaseApiClient.api.getItems()
                if (remoteItems.isNotEmpty()) {
                    val localItems = remoteItems.map { dto ->
                        ItemEntity(
                            id = dto.id ?: 0L,
                            name = dto.name,
                            categoryId = dto.category_id ?: 1L,
                            categoryName = dto.category_name ?: "General",
                            price = dto.price,
                            cost = dto.cost ?: 0.0,
                            sku = dto.sku ?: "",
                            barcode = dto.barcode ?: "",
                            soldBy = if (dto.sold_by == "WEIGHT") com.example.data.model.SoldBy.WEIGHT else com.example.data.model.SoldBy.EACH,
                            inStock = dto.in_stock,
                            lowStockThreshold = dto.low_stock_threshold,
                            trackStock = dto.track_stock,
                            colorHex = dto.color_hex ?: "#0288D1",
                            modifierIds = dto.modifier_ids ?: "",
                            imageUrl = dto.image_url ?: ""
                        )
                    }
                    repository.insertItemsFromSync(localItems)
                    syncedAny = true
                }
            } catch (e: Exception) {
                // Ignore secondary error
            }
        }

        if (!syncedAny && repository.database.itemDao().countItems() == 0) {
            // Ensure local DB is never empty
            com.example.data.db.populateInitialBistroData(repository.database)
        }
    }
}
