package com.example.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CurrentOrderState(
    val activeTicket: Ticket? = null,
    val ticketName: String = "Order #1",
    val diningOption: DiningOption = DiningOption.DINE_IN,
    val selectedItems: List<TicketItemEntity> = emptyList(),
    val discountPercent: Double = 0.0
) {
    val subtotal: Double
        get() = selectedItems.sumOf { it.lineTotal }

    val discountAmount: Double
        get() = subtotal * (discountPercent / 100.0)

    val taxRate: Double = 0.0825

    val tax: Double
        get() = (subtotal - discountAmount).coerceAtLeast(0.0) * taxRate

    val total: Double
        get() = (subtotal - discountAmount + tax).coerceAtLeast(0.0)

    val itemCount: Int
        get() = selectedItems.sumOf { it.quantity }.toInt()
}

/**
 * CurrentOrderViewModel manages the active order/cart items,
 * calculates the subtotal, tax, and total, and enables saving,
 * naming, and switching between multiple open tickets from Room database.
 */
class CurrentOrderViewModel(private val repository: PosRepository) : ViewModel() {

    private val _orderState = MutableStateFlow(CurrentOrderState())
    val orderState: StateFlow<CurrentOrderState> = _orderState.asStateFlow()

    // Observable list of open orders from Room
    val openTickets: StateFlow<List<Ticket>> = repository.allOpenTicketOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Subtotal stream
    val subtotal: StateFlow<Double> = _orderState
        .map { it.subtotal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Total stream
    val total: StateFlow<Double> = _orderState
        .map { it.total }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /**
     * Add an item to the current order cart
     */
    fun addItem(
        item: ItemEntity,
        quantity: Double = 1.0,
        modifiersText: String = "",
        modifiersCost: Double = 0.0,
        comment: String = ""
    ) {
        _orderState.update { state ->
            val existingIndex = state.selectedItems.indexOfFirst {
                it.itemId == item.id &&
                        it.selectedModifiersJson == modifiersText &&
                        it.comment == comment
            }

            val updatedItems = state.selectedItems.toMutableList()
            if (existingIndex >= 0 && item.soldBy == SoldBy.EACH) {
                val current = updatedItems[existingIndex]
                updatedItems[existingIndex] = current.copy(quantity = current.quantity + quantity)
            } else {
                updatedItems.add(
                    TicketItemEntity(
                        ticketId = state.activeTicket?.id ?: 0L,
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
            state.copy(selectedItems = updatedItems)
        }
    }

    /**
     * Overload to add a Product entity directly to the current order cart
     */
    fun addItem(product: Product, quantity: Double = 1.0) {
        _orderState.update { state ->
            val existingIndex = state.selectedItems.indexOfFirst {
                it.itemId == product.id && it.selectedModifiersJson.isEmpty()
            }

            val updatedItems = state.selectedItems.toMutableList()
            if (existingIndex >= 0) {
                val current = updatedItems[existingIndex]
                updatedItems[existingIndex] = current.copy(quantity = current.quantity + quantity)
            } else {
                updatedItems.add(
                    TicketItemEntity(
                        ticketId = state.activeTicket?.id ?: 0L,
                        itemId = product.id,
                        itemName = product.name,
                        unitPrice = product.price,
                        quantity = quantity
                    )
                )
            }
            state.copy(selectedItems = updatedItems)
        }
    }

    /**
     * Remove an item from the current order cart by index
     */
    fun removeItem(index: Int) {
        _orderState.update { state ->
            if (index in state.selectedItems.indices) {
                val updated = state.selectedItems.toMutableList()
                updated.removeAt(index)
                state.copy(selectedItems = updated)
            } else state
        }
    }

    /**
     * Update quantity of an item at the given index
     */
    fun updateQuantity(index: Int, newQuantity: Double) {
        _orderState.update { state ->
            if (index in state.selectedItems.indices) {
                val updated = state.selectedItems.toMutableList()
                if (newQuantity <= 0.0) {
                    updated.removeAt(index)
                } else {
                    updated[index] = updated[index].copy(quantity = newQuantity)
                }
                state.copy(selectedItems = updated)
            } else state
        }
    }

    /**
     * Clear all items in the current cart
     */
    fun clearOrder() {
        _orderState.update {
            it.copy(
                selectedItems = emptyList(),
                activeTicket = null,
                ticketName = "Order #${System.currentTimeMillis() % 1000}",
                discountPercent = 0.0
            )
        }
    }

    /**
     * Set/rename the ticket name
     */
    fun setTicketName(name: String) {
        _orderState.update { it.copy(ticketName = name) }
    }

    fun setDiningOption(option: DiningOption) {
        _orderState.update { it.copy(diningOption = option) }
    }

    fun setDiscountPercent(percent: Double) {
        _orderState.update { it.copy(discountPercent = percent) }
    }

    /**
     * Save the current cart as a named open Ticket in Room
     */
    fun saveCurrentOrder(name: String, tableName: String = "") {
        viewModelScope.launch {
            val state = _orderState.value
            val ticket = Ticket(
                id = state.activeTicket?.id ?: 0L,
                name = name.ifBlank { state.ticketName },
                tableName = tableName,
                diningOption = state.diningOption.label,
                serverName = "Server"
            )
            val savedId = repository.saveTicketOrder(ticket)
            _orderState.update {
                it.copy(
                    activeTicket = ticket.copy(id = savedId),
                    ticketName = ticket.name
                )
            }
        }
    }

    /**
     * Switch between open orders/tickets
     */
    fun switchToTicket(ticket: Ticket) {
        viewModelScope.launch {
            _orderState.update {
                it.copy(
                    activeTicket = ticket,
                    ticketName = ticket.name,
                    diningOption = when (ticket.diningOption.lowercase()) {
                        "takeout" -> DiningOption.TAKEOUT
                        "delivery" -> DiningOption.DELIVERY
                        else -> DiningOption.DINE_IN
                    }
                )
            }
        }
    }

    /**
     * Close/complete the currently active ticket
     */
    fun closeCurrentOrder() {
        viewModelScope.launch {
            _orderState.value.activeTicket?.let { ticket ->
                repository.closeTicketOrder(ticket.id)
            }
            clearOrder()
        }
    }

    // --- Modifiers & Custom Modifiers ---
    val availableModifiers: StateFlow<List<Modifier>> = repository.allModifiers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Add a custom modifier (e.g. 'Extra Cheese', 'No Onions') to an item in the existing ticket
     */
    fun addCustomModifierToItem(itemIndex: Int, modifierName: String, modifierPrice: Double = 0.0) {
        _orderState.update { state ->
            if (itemIndex in state.selectedItems.indices) {
                val updated = state.selectedItems.toMutableList()
                val current = updated[itemIndex]

                val formattedMod = if (modifierPrice > 0) "$modifierName (+$${String.format(java.util.Locale.US, "%.2f", modifierPrice)})" else modifierName
                val newModifiersJson = if (current.selectedModifiersJson.isBlank()) formattedMod
                else "${current.selectedModifiersJson}, $formattedMod"

                val newModifiersCost = current.modifiersCost + modifierPrice

                updated[itemIndex] = current.copy(
                    selectedModifiersJson = newModifiersJson,
                    modifiersCost = newModifiersCost
                )
                state.copy(selectedItems = updated)
            } else state
        }
    }

    /**
     * Add a Modifier entity to an item in the existing ticket
     */
    fun addModifierToItem(itemIndex: Int, modifier: Modifier) {
        addCustomModifierToItem(itemIndex, modifier.name, modifier.price)
    }

    // --- Time Clock Session Management ---
    val allTimecards: StateFlow<List<Timecard>> = repository.allTimecards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clockIn(employeeName: String, onResult: (Timecard) -> Unit = {}) {
        viewModelScope.launch {
            val tc = repository.clockIn(employeeName)
            onResult(tc)
        }
    }

    fun clockOut(employeeName: String, onResult: (Timecard?) -> Unit = {}) {
        viewModelScope.launch {
            val tc = repository.clockOut(employeeName)
            onResult(tc)
        }
    }
}

class CurrentOrderViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CurrentOrderViewModel(repository) as T
    }
}
