package com.example.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PosUiState(
    val searchQuery: String = "",
    val selectedCategoryId: Long? = null,
    val isGridLayout: Boolean = true,
    val diningOption: DiningOption = DiningOption.DINE_IN,
    val currentTicketName: String = "Ticket #1",
    val activeTicketId: Long? = null,
    val selectedCustomer: CustomerEntity? = null,
    val cartItems: List<TicketItemEntity> = emptyList(),
    val ticketDiscountPercent: Double = 0.0,
    val redeemedPointsAmount: Double = 0.0,
    val isItemCustomizationOpen: Boolean = false,
    val customizingItem: ItemEntity? = null,
    val showOpenTicketsDialog: Boolean = false,
    val showSaveTicketDialog: Boolean = false,
    val showCheckoutSheet: Boolean = false,
    val showCustomerSelectDialog: Boolean = false,
    val showShiftDialog: Boolean = false,
    val showBarcodeDialog: Boolean = false,
    val showReceiptsDialog: Boolean = false,
    val showPrinterSettingsDialog: Boolean = false,
    val showLowStockDialog: Boolean = false,
    val showSyncStatusDialog: Boolean = false,
    val showDatabaseBackupDialog: Boolean = false,
    val lowStockThreshold: Int = 5,
    val lowStockBannerDismissed: Boolean = false,
    val isKeypadSelected: Boolean = false,
    val isFavoritesSelected: Boolean = false,
    val isSearchActive: Boolean = false,
    val keypadInputAmount: String = "",
    val completedReceipt: ReceiptEntity? = null,
    val cashierName: String = "Alex (Cashier)",
    val storeName: String = "Bistro & Grill #1",
    val statusMessage: String? = null
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.lineTotal }

    val discountAmount: Double
        get() {
            val percentDiscount = subtotal * (ticketDiscountPercent / 100.0)
            return (percentDiscount + redeemedPointsAmount).coerceAtMost(subtotal)
        }

    val taxableSubtotal: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0)

    // Sales tax 8.25%
    val taxRate: Double = 0.0825

    val taxAmount: Double
        get() = taxableSubtotal * taxRate

    val grandTotal: Double
        get() = (taxableSubtotal + taxAmount).coerceAtLeast(0.0)

    val itemCount: Int
        get() = cartItems.sumOf { it.quantity }.toInt()
}

class PosViewModel(val repository: PosRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<ItemEntity>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modifierGroups: StateFlow<List<ModifierGroupEntity>> = repository.allModifierGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTickets: StateFlow<List<TicketEntity>> = repository.activeTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentShift: StateFlow<ShiftEntity?> = repository.currentShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receipts: StateFlow<List<ReceiptEntity>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lowStockThreshold = MutableStateFlow(5)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val lowStockProducts: StateFlow<List<Product>> = _lowStockThreshold
        .flatMapLatest { threshold -> repository.getLowStockProducts(threshold) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLowStockThreshold(threshold: Int) {
        val validThreshold = threshold.coerceIn(1, 100)
        _lowStockThreshold.value = validThreshold
        _uiState.update { it.copy(lowStockThreshold = validThreshold) }
    }

    fun setLowStockDialog(open: Boolean) = _uiState.update { it.copy(showLowStockDialog = open) }

    fun dismissLowStockBanner() = _uiState.update { it.copy(lowStockBannerDismissed = true) }

    fun restockProduct(product: Product, addedStock: Int) {
        viewModelScope.launch {
            repository.restockProduct(product.id, addedStock)
            _uiState.update { it.copy(statusMessage = "Restocked ${product.name} (+ $addedStock units)") }
        }
    }

    fun checkAndNotifyLowStock(threshold: Int = 5) {
        viewModelScope.launch {
            val lowStockItems = repository.checkLowStockProducts(threshold)
            if (lowStockItems.isNotEmpty()) {
                val names = lowStockItems.take(2).joinToString(", ") { "${it.name} (${it.stockLevel} left)" }
                _uiState.update { 
                    it.copy(
                        showLowStockDialog = true,
                        statusMessage = "⚠️ Warning: ${lowStockItems.size} item(s) low on stock: $names"
                    ) 
                }
            } else {
                _uiState.update { it.copy(statusMessage = "All product inventory levels are healthy") }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active, searchQuery = if (!active) "" else it.searchQuery) }
    }

    fun selectCategory(categoryId: Long?) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                isKeypadSelected = false,
                isFavoritesSelected = false
            )
        }
    }

    fun selectFavoritesTab() {
        _uiState.update {
            it.copy(
                isFavoritesSelected = true,
                isKeypadSelected = false,
                selectedCategoryId = null
            )
        }
    }

    fun selectKeypadTab() {
        _uiState.update {
            it.copy(
                isKeypadSelected = true,
                isFavoritesSelected = false,
                selectedCategoryId = null
            )
        }
    }

    fun onKeypadDigit(digit: String) {
        _uiState.update { state ->
            val current = state.keypadInputAmount
            if (current.length < 9) {
                state.copy(keypadInputAmount = current + digit)
            } else state
        }
    }

    fun onKeypadBackspace() {
        _uiState.update { state ->
            if (state.keypadInputAmount.isNotEmpty()) {
                state.copy(keypadInputAmount = state.keypadInputAmount.dropLast(1))
            } else state
        }
    }

    fun onKeypadClear() {
        _uiState.update { it.copy(keypadInputAmount = "") }
    }

    fun addKeypadAmountToTicket() {
        val raw = _uiState.value.keypadInputAmount
        val amount = (raw.toDoubleOrNull() ?: 0.0) / 100.0
        if (amount > 0.0) {
            addCustomAmount(amount, "Custom Amount")
            _uiState.update { it.copy(keypadInputAmount = "") }
        }
    }

    fun addCustomAmount(amount: Double, itemName: String = "Custom Amount") {
        if (amount <= 0.0) return
        _uiState.update { state ->
            val updatedItems = state.cartItems.toMutableList()
            updatedItems.add(
                TicketItemEntity(
                    ticketId = state.activeTicketId ?: 0L,
                    itemId = 999999L,
                    itemName = itemName,
                    unitPrice = amount,
                    quantity = 1.0,
                    selectedModifiersJson = "",
                    modifiersCost = 0.0,
                    comment = ""
                )
            )
            state.copy(cartItems = updatedItems)
        }
    }

    fun toggleLayoutMode() {
        _uiState.update { it.copy(isGridLayout = !it.isGridLayout) }
    }

    fun setDiningOption(option: DiningOption) {
        _uiState.update { it.copy(diningOption = option) }
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer, redeemedPointsAmount = 0.0) }
    }

    fun applyTicketDiscount(percent: Double) {
        _uiState.update { it.copy(ticketDiscountPercent = percent) }
    }

    fun redeemCustomerPoints(amount: Double) {
        _uiState.update { it.copy(redeemedPointsAmount = amount) }
    }

    fun onItemClick(item: ItemEntity) {
        // If sold by weight or has modifiers, open customization dialog
        if (item.soldBy == SoldBy.WEIGHT || item.modifierIds.isNotBlank()) {
            _uiState.update { it.copy(customizingItem = item, isItemCustomizationOpen = true) }
        } else {
            // Direct addition
            addOrIncrementItem(item, quantity = 1.0, modifiersText = "", modifiersCost = 0.0, comment = "")
        }
    }

    fun addCustomizedItem(
        item: ItemEntity,
        quantity: Double,
        modifiersText: String,
        modifiersCost: Double,
        comment: String
    ) {
        addOrIncrementItem(item, quantity, modifiersText, modifiersCost, comment)
        _uiState.update { it.copy(isItemCustomizationOpen = false, customizingItem = null) }
    }

    private fun addOrIncrementItem(
        item: ItemEntity,
        quantity: Double,
        modifiersText: String,
        modifiersCost: Double,
        comment: String
    ) {
        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst {
                it.itemId == item.id &&
                        it.selectedModifiersJson == modifiersText &&
                        it.comment == comment
            }

            val updatedItems = state.cartItems.toMutableList()
            if (existingIndex >= 0 && item.soldBy == SoldBy.EACH) {
                val current = updatedItems[existingIndex]
                updatedItems[existingIndex] = current.copy(quantity = current.quantity + quantity)
            } else {
                updatedItems.add(
                    TicketItemEntity(
                        ticketId = state.activeTicketId ?: 0L,
                        itemId = item.id,
                        itemName = item.name,
                        unitPrice = item.price,
                        quantity = quantity,
                        selectedModifiersJson = modifiersText,
                        modifiersCost = modifiersCost,
                        comment = comment
                    )
                )
            }
            state.copy(cartItems = updatedItems)
        }
    }

    fun updateCartItemQuantity(index: Int, newQuantity: Double) {
        _uiState.update { state ->
            if (index in state.cartItems.indices) {
                val updated = state.cartItems.toMutableList()
                if (newQuantity <= 0.0) {
                    updated.removeAt(index)
                } else {
                    updated[index] = updated[index].copy(quantity = newQuantity)
                }
                state.copy(cartItems = updated)
            } else state
        }
    }

    fun removeCartItem(index: Int) {
        _uiState.update { state ->
            if (index in state.cartItems.indices) {
                val updated = state.cartItems.toMutableList()
                updated.removeAt(index)
                state.copy(cartItems = updated)
            } else state
        }
    }

    fun clearCart() {
        _uiState.update {
            it.copy(
                cartItems = emptyList(),
                activeTicketId = null,
                currentTicketName = "Ticket #${(System.currentTimeMillis() % 1000)}",
                ticketDiscountPercent = 0.0,
                redeemedPointsAmount = 0.0,
                selectedCustomer = null
            )
        }
    }

    // Open Tickets operations
    fun saveTicket(name: String, tableName: String) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return

        viewModelScope.launch {
            val ticket = TicketEntity(
                id = state.activeTicketId ?: 0L,
                ticketName = name.ifBlank { "Ticket #${System.currentTimeMillis() % 1000}" },
                tableName = tableName,
                diningOption = state.diningOption,
                serverName = state.cashierName,
                customerId = state.selectedCustomer?.id,
                customerName = state.selectedCustomer?.name
            )
            repository.saveTicketWithItems(ticket, state.cartItems)
            clearCart()
            _uiState.update { it.copy(showSaveTicketDialog = false, statusMessage = "Ticket '$name' saved") }
        }
    }

    fun loadOpenTicket(ticket: TicketEntity) {
        viewModelScope.launch {
            val items = repository.getTicketItems(ticket.id)
            val customer = ticket.customerId?.let { id ->
                customers.value.firstOrNull { it.id == id }
            }
            _uiState.update {
                it.copy(
                    activeTicketId = ticket.id,
                    currentTicketName = ticket.ticketName,
                    diningOption = ticket.diningOption,
                    selectedCustomer = customer,
                    cartItems = items,
                    showOpenTicketsDialog = false
                )
            }
        }
    }

    fun mergeTickets(targetTicketId: Long, tickets: List<TicketEntity>) {
        viewModelScope.launch {
            repository.mergeTickets(targetTicketId, tickets)
            _uiState.update { it.copy(statusMessage = "Tickets merged successfully") }
        }
    }

    fun splitTicket(sourceTicketId: Long, newName: String, itemsToMove: List<TicketItemEntity>) {
        viewModelScope.launch {
            repository.splitTicket(sourceTicketId, newName, itemsToMove)
            _uiState.update { it.copy(statusMessage = "Ticket split created") }
        }
    }

    fun closeOrVoidTicket(ticketId: Long) {
        viewModelScope.launch {
            repository.closeOrVoidTicket(ticketId)
            if (_uiState.value.activeTicketId == ticketId) {
                clearCart()
            }
            _uiState.update { it.copy(statusMessage = "Ticket voided") }
        }
    }

    // Checkout
    fun completePayment(paymentType: PaymentType, details: String, tipAmount: Double = 0.0) {
        val state = _uiState.value
        if (state.cartItems.isEmpty()) return

        viewModelScope.launch {
            val receipt = repository.completeSale(
                ticketId = state.activeTicketId,
                ticketName = state.currentTicketName,
                diningOption = state.diningOption,
                items = state.cartItems,
                subtotal = state.subtotal,
                taxAmount = state.taxAmount,
                discountAmount = state.discountAmount,
                tipAmount = tipAmount,
                totalAmount = state.grandTotal + tipAmount,
                paymentType = paymentType,
                paymentDetails = details,
                customerId = state.selectedCustomer?.id,
                customerName = state.selectedCustomer?.name,
                cashierName = state.cashierName
            )

            // If points were redeemed, deduct from customer
            if (state.selectedCustomer != null && state.redeemedPointsAmount > 0) {
                repository.redeemCustomerPoints(state.selectedCustomer.id, state.redeemedPointsAmount)
            }

            _uiState.update {
                it.copy(
                    showCheckoutSheet = false,
                    completedReceipt = receipt
                )
            }
        }
    }

    fun startNewSale() {
        _uiState.update {
            it.copy(
                completedReceipt = null,
                cartItems = emptyList(),
                activeTicketId = null,
                currentTicketName = "Ticket #${(System.currentTimeMillis() % 1000)}",
                ticketDiscountPercent = 0.0,
                redeemedPointsAmount = 0.0,
                selectedCustomer = null
            )
        }
    }

    fun refundReceipt(receipt: ReceiptEntity) {
        viewModelScope.launch {
            repository.refundReceipt(receipt)
            _uiState.update { it.copy(statusMessage = "Receipt ${receipt.receiptNumber} refunded") }
        }
    }

    // Barcode scanning
    fun onBarcodeScanned(code: String) {
        viewModelScope.launch {
            // Check weight embedded barcode (Loyverse pattern: 02CCCCCWWWWWX or 20CCCCCWWWWWX)
            if ((code.startsWith("02") || code.startsWith("20")) && code.length >= 12) {
                val sku = code.substring(2, 7)
                val weightGrams = code.substring(7, 12).toDoubleOrNull() ?: 0.0
                val weightKg = weightGrams / 1000.0

                val item = allItems.value.firstOrNull { it.sku == sku || it.sku == sku.trimStart('0') }
                if (item != null && item.soldBy == SoldBy.WEIGHT) {
                    addOrIncrementItem(item, weightKg, "", 0.0, "")
                    _uiState.update { it.copy(showBarcodeDialog = false, statusMessage = "Added ${item.name} ($weightKg kg)") }
                    return@launch
                }
            }

            // Normal item barcode lookup
            val item = repository.getItemByBarcode(code)
            if (item != null) {
                onItemClick(item)
                _uiState.update { it.copy(showBarcodeDialog = false, statusMessage = "Added ${item.name}") }
                return@launch
            }

            // Customer loyalty card barcode lookup
            val customer = customers.value.firstOrNull { it.customerCode.equals(code, ignoreCase = true) }
            if (customer != null) {
                selectCustomer(customer)
                _uiState.update { it.copy(showBarcodeDialog = false, statusMessage = "Customer ${customer.name} attached") }
                return@launch
            }

            _uiState.update { it.copy(statusMessage = "Barcode '$code' not recognized") }
        }
    }

    // Shift Operations
    fun openShift(startingCash: Double) {
        viewModelScope.launch {
            repository.openShift(startingCash, _uiState.value.cashierName)
            _uiState.update { it.copy(statusMessage = "Shift opened with $${String.format("%.2f", startingCash)}") }
        }
    }

    fun recordCashAction(isPayIn: Boolean, amount: Double, comment: String) {
        viewModelScope.launch {
            repository.recordCashAction(isPayIn, amount, comment)
            val actionLabel = if (isPayIn) "Pay In" else "Pay Out"
            _uiState.update { it.copy(statusMessage = "$actionLabel of $${String.format("%.2f", amount)} recorded") }
        }
    }

    fun closeShift(actualCash: Double) {
        viewModelScope.launch {
            repository.closeShift(actualCash)
            _uiState.update { it.copy(statusMessage = "Shift closed successfully") }
        }
    }

    // Dialog state toggles
    fun setOpenTicketsDialog(open: Boolean) = _uiState.update { it.copy(showOpenTicketsDialog = open) }
    fun setSaveTicketDialog(open: Boolean) = _uiState.update { it.copy(showSaveTicketDialog = open) }
    fun setCheckoutSheet(open: Boolean) = _uiState.update { it.copy(showCheckoutSheet = open) }
    fun setCustomerSelectDialog(open: Boolean) = _uiState.update { it.copy(showCustomerSelectDialog = open) }
    fun setShiftDialog(open: Boolean) = _uiState.update { it.copy(showShiftDialog = open) }
    fun setBarcodeDialog(open: Boolean) = _uiState.update { it.copy(showBarcodeDialog = open) }
    fun setReceiptsDialog(open: Boolean) = _uiState.update { it.copy(showReceiptsDialog = open) }
    fun setPrinterSettingsDialog(open: Boolean) = _uiState.update { it.copy(showPrinterSettingsDialog = open) }
    fun setSyncStatusDialog(open: Boolean) = _uiState.update { it.copy(showSyncStatusDialog = open) }
    fun setDatabaseBackupDialog(open: Boolean) = _uiState.update { it.copy(showDatabaseBackupDialog = open) }
    fun setCustomizationDialog(open: Boolean) = _uiState.update { it.copy(isItemCustomizationOpen = open) }
    fun dismissStatusMessage() = _uiState.update { it.copy(statusMessage = null) }
}

class PosViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PosViewModel(repository) as T
    }
}
