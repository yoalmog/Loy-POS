package com.example.ui.backoffice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BackOfficeTab(val label: String) {
    SALES_SUMMARY("Sales Summary"),
    ITEMS_INVENTORY("Items & Stock"),
    CUSTOMERS("Customer Loyalty"),
    SHIFTS("Shift History"),
    SETTINGS("Settings")
}

enum class DateFilter(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class BackOfficeUiState(
    val selectedTab: BackOfficeTab = BackOfficeTab.SALES_SUMMARY,
    val dateFilter: DateFilter = DateFilter.ALL_TIME,
    val itemFilterQuery: String = "",
    val editingItem: ItemEntity? = null,
    val showItemEditorDialog: Boolean = false,
    val adjustingItem: ItemEntity? = null,
    val showStockAdjustDialog: Boolean = false,
    val showAddCustomerDialog: Boolean = false,
    val showSyncStatusDialog: Boolean = false,
    val showDatabaseBackupDialog: Boolean = false,
    val viewingCustomerReceipts: CustomerEntity? = null,
    val currencySymbol: String = "$",
    val storeName: String = "Bistro & Grill #1",
    val statusMessage: String? = null
)

class BackOfficeViewModel(val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BackOfficeUiState())
    val uiState: StateFlow<BackOfficeUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemEntity>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shifts: StateFlow<List<ShiftEntity>> = repository.allShifts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receipts: StateFlow<List<ReceiptEntity>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun quickAdjustProductStock(product: Product, delta: Int) {
        viewModelScope.launch {
            val newStock = (product.stockLevel + delta).coerceAtLeast(0)
            repository.saveProduct(product.copy(stockLevel = newStock))
            _uiState.update { it.copy(statusMessage = "${product.name} stock: $newStock") }
        }
    }

    fun selectTab(tab: BackOfficeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setDateFilter(filter: DateFilter) {
        _uiState.update { it.copy(dateFilter = filter) }
    }

    fun setItemFilterQuery(query: String) {
        _uiState.update { it.copy(itemFilterQuery = query) }
    }

    // Item management
    fun openItemEditor(item: ItemEntity?) {
        _uiState.update { it.copy(editingItem = item, showItemEditorDialog = true) }
    }

    fun closeItemEditor() {
        _uiState.update { it.copy(editingItem = null, showItemEditorDialog = false) }
    }

    fun saveItem(
        id: Long,
        name: String,
        categoryId: Long,
        categoryName: String,
        price: Double,
        cost: Double,
        sku: String,
        barcode: String,
        soldBy: SoldBy,
        inStock: Double,
        lowStockThreshold: Double,
        trackStock: Boolean,
        colorHex: String
    ) {
        viewModelScope.launch {
            val item = ItemEntity(
                id = id,
                name = name,
                categoryId = categoryId,
                categoryName = categoryName,
                price = price,
                cost = cost,
                sku = sku,
                barcode = barcode,
                soldBy = soldBy,
                inStock = inStock,
                lowStockThreshold = lowStockThreshold,
                trackStock = trackStock,
                colorHex = colorHex
            )
            repository.saveItem(item)
            _uiState.update {
                it.copy(
                    showItemEditorDialog = false,
                    editingItem = null,
                    statusMessage = "Item '$name' saved"
                )
            }
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _uiState.update {
                it.copy(
                    showItemEditorDialog = false,
                    editingItem = null,
                    statusMessage = "Item '${item.name}' deleted"
                )
            }
        }
    }

    fun openStockAdjustment(item: ItemEntity) {
        _uiState.update { it.copy(adjustingItem = item, showStockAdjustDialog = true) }
    }

    fun closeStockAdjustment() {
        _uiState.update { it.copy(adjustingItem = null, showStockAdjustDialog = false) }
    }

    fun applyStockAdjustment(itemId: Long, diff: Double, reason: String) {
        viewModelScope.launch {
            repository.adjustStock(itemId, diff)
            _uiState.update {
                it.copy(
                    showStockAdjustDialog = false,
                    adjustingItem = null,
                    statusMessage = "Stock adjusted: ${if (diff > 0) "+$diff" else "$diff"} ($reason)"
                )
            }
        }
    }

    // Customer management
    fun openAddCustomer() = _uiState.update { it.copy(showAddCustomerDialog = true) }
    fun closeAddCustomer() = _uiState.update { it.copy(showAddCustomerDialog = false) }

    fun saveCustomer(name: String, phone: String, email: String, note: String) {
        viewModelScope.launch {
            val code = "LOY" + (1000 + (Math.random() * 9000).toInt())
            val customer = CustomerEntity(
                name = name,
                phone = phone,
                email = email,
                customerCode = code,
                note = note
            )
            repository.saveCustomer(customer)
            _uiState.update {
                it.copy(showAddCustomerDialog = false, statusMessage = "Customer '$name' added")
            }
        }
    }

    fun viewCustomerReceipts(customer: CustomerEntity?) {
        _uiState.update { it.copy(viewingCustomerReceipts = customer) }
    }

    fun setSyncStatusDialog(open: Boolean) = _uiState.update { it.copy(showSyncStatusDialog = open) }
    fun setDatabaseBackupDialog(open: Boolean) = _uiState.update { it.copy(showDatabaseBackupDialog = open) }

    fun dismissStatusMessage() = _uiState.update { it.copy(statusMessage = null) }
}

class BackOfficeViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BackOfficeViewModel(repository) as T
    }
}
