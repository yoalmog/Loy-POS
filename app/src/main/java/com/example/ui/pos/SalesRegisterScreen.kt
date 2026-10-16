package com.example.ui.pos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.pos.components.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesRegisterScreen(
    viewModel: PosViewModel,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val modifierGroups by viewModel.modifierGroups.collectAsState()
    val activeTickets by viewModel.activeTickets.collectAsState()
    val currentShift by viewModel.currentShift.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val receipts by viewModel.receipts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val syncManager = remember { com.example.util.SyncManager(context, viewModel.repository) }
    val database = remember { com.example.data.db.AppDatabase.getDatabase(context) }

    val isOnline by syncManager.networkMonitor.isOnline.collectAsState()
    val connectionType by syncManager.networkMonitor.connectionType.collectAsState()
    val syncState by syncManager.syncState.collectAsState()
    val lastSyncTime by syncManager.lastSyncTimestamp.collectAsState()
    val pendingSyncCount by syncManager.pendingSyncCount.collectAsState()

    var showMobileCartSheet by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }

    // Filter items by category & search query
    val displayedItems = remember(allItems, uiState.selectedCategoryId, uiState.searchQuery) {
        allItems.filter { item ->
            val matchCategory = uiState.selectedCategoryId == null || item.categoryId == uiState.selectedCategoryId
            val matchSearch = uiState.searchQuery.isBlank() ||
                    item.name.contains(uiState.searchQuery, ignoreCase = true) ||
                    item.sku.contains(uiState.searchQuery, ignoreCase = true) ||
                    item.barcode.contains(uiState.searchQuery)
            matchCategory && matchSearch
        }
    }

    // Snackbar for feedback
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 760.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.storeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Shift status badge
                                Surface(
                                    color = if (currentShift?.isOpen == true) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.clickable { viewModel.setShiftDialog(true) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (currentShift?.isOpen == true) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.error
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (currentShift?.isOpen == true) "Shift Open" else "Shift Closed",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Cashier: ${uiState.cashierName} • Ticket: ${uiState.currentTicketName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("drawer_menu_button")) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Barcode Scan Button
                        IconButton(onClick = { viewModel.setBarcodeDialog(true) }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }

                        // Customer Loyalty Button
                        IconButton(onClick = { viewModel.setCustomerSelectDialog(true) }) {
                            BadgedBox(
                                badge = {
                                    if (uiState.selectedCustomer != null) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("★") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Customer")
                            }
                        }

                        // Thermal Printer Settings Button
                        IconButton(onClick = { viewModel.setPrinterSettingsDialog(true) }) {
                            Icon(Icons.Default.Print, contentDescription = "Thermal Printer")
                        }

                        // Low Stock Warning Alert Button
                        IconButton(onClick = { viewModel.setLowStockDialog(true) }) {
                            BadgedBox(
                                badge = {
                                    if (lowStockProducts.isNotEmpty()) {
                                        Badge(containerColor = Color(0xFFE65100)) {
                                            Text("${lowStockProducts.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Low Stock Warning",
                                    tint = if (lowStockProducts.isNotEmpty()) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Sync & Connectivity Status Button
                        IconButton(
                            onClick = { viewModel.setSyncStatusDialog(true) },
                            modifier = Modifier.testTag("sync_status_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = CircleShape,
                                        color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    ) {}
                                }
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                    contentDescription = "Sync Status",
                                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFE65100)
                                )
                            }
                        }

                        // Grid / List toggle
                        IconButton(onClick = { viewModel.toggleLayoutMode() }) {
                            Icon(
                                if (uiState.isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // On compact screens, bottom sticky bar for cart & charge
                if (!isWideScreen) {
                    CompactCartBottomBar(
                        uiState = uiState,
                        onViewCart = { showMobileCartSheet = true },
                        onCharge = {
                            if (uiState.cartItems.isNotEmpty()) {
                                viewModel.setCheckoutSheet(true)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Item Catalog Pane
                Column(
                    modifier = Modifier
                        .weight(if (isWideScreen) 0.62f else 1f)
                        .fillMaxHeight()
                ) {
                    // Category Chips Bar
                    CategoryFilterRow(
                        categories = categories,
                        selectedCategoryId = uiState.selectedCategoryId,
                        onCategorySelect = { viewModel.selectCategory(it) }
                    )

                    // Low Stock Alert Warning Banner
                    AnimatedVisibility(
                        visible = lowStockProducts.isNotEmpty() && !uiState.lowStockBannerDismissed,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Low Stock Warning: ${lowStockProducts.size} item(s) running low",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                        val firstTwo = lowStockProducts.take(2).joinToString(", ") { "${it.name} (${it.stockLevel} left)" }
                                        Text(
                                            text = "$firstTwo${if (lowStockProducts.size > 2) "..." else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TextButton(
                                        onClick = { viewModel.setLowStockDialog(true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Review", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissLowStockBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss banner",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search and Dining option selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search menu, barcode or SKU...", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        // Dining Option Dropdown
                        var diningMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { diningMenuExpanded = true },
                                label = { Text(uiState.diningOption.label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(
                                        when (uiState.diningOption) {
                                            DiningOption.DINE_IN -> Icons.Default.Restaurant
                                            DiningOption.TAKEOUT -> Icons.Default.TakeoutDining
                                            DiningOption.DELIVERY -> Icons.Default.DeliveryDining
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                            )

                            DropdownMenu(
                                expanded = diningMenuExpanded,
                                onDismissRequest = { diningMenuExpanded = false }
                            ) {
                                DiningOption.values().forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt.label) },
                                        onClick = {
                                            viewModel.setDiningOption(opt)
                                            diningMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Items Grid or List
                    if (displayedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (uiState.isGridLayout) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 135.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(displayedItems) { item ->
                                ItemGridCard(
                                    item = item,
                                    onClick = { viewModel.onItemClick(item) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayedItems) { item ->
                                ItemListRow(
                                    item = item,
                                    onClick = { viewModel.onItemClick(item) }
                                )
                            }
                        }
                    }
                }

                // Wide Screen Ticket Pane (1/3 of screen)
                if (isWideScreen) {
                    Box(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        TicketCartPanel(
                            uiState = uiState,
                            openTicketsCount = activeTickets.size,
                            onUpdateQuantity = { idx, qty -> viewModel.updateCartItemQuantity(idx, qty) },
                            onRemoveItem = { idx -> viewModel.removeCartItem(idx) },
                            onClearCart = { viewModel.clearCart() },
                            onOpenTicketsClick = { viewModel.setOpenTicketsDialog(true) },
                            onSaveTicketClick = { viewModel.setSaveTicketDialog(true) },
                            onDiscountClick = { showDiscountDialog = true },
                            onChargeClick = { viewModel.setCheckoutSheet(true) }
                        )
                    }
                }
            }
        }
    }

    // Mobile Bottom Sheet for Ticket Cart
    if (showMobileCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMobileCartSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Box(modifier = Modifier.fillMaxHeight(0.85f)) {
                TicketCartPanel(
                    uiState = uiState,
                    openTicketsCount = activeTickets.size,
                    onUpdateQuantity = { idx, qty -> viewModel.updateCartItemQuantity(idx, qty) },
                    onRemoveItem = { idx -> viewModel.removeCartItem(idx) },
                    onClearCart = { viewModel.clearCart() },
                    onOpenTicketsClick = {
                        showMobileCartSheet = false
                        viewModel.setOpenTicketsDialog(true)
                    },
                    onSaveTicketClick = {
                        showMobileCartSheet = false
                        viewModel.setSaveTicketDialog(true)
                    },
                    onDiscountClick = { showDiscountDialog = true },
                    onChargeClick = {
                        showMobileCartSheet = false
                        viewModel.setCheckoutSheet(true)
                    }
                )
            }
        }
    }

    // Item Customization Dialog
    if (uiState.isItemCustomizationOpen && uiState.customizingItem != null) {
        ItemCustomizationDialog(
            item = uiState.customizingItem!!,
            availableModifierGroups = modifierGroups,
            onDismiss = { viewModel.setCustomizationDialog(false) },
            onConfirm = { qty, modText, modCost, comment ->
                viewModel.addCustomizedItem(uiState.customizingItem!!, qty, modText, modCost, comment)
            }
        )
    }

    // Open Tickets Dialog
    if (uiState.showOpenTicketsDialog) {
        OpenTicketsDialog(
            openTickets = activeTickets,
            onDismiss = { viewModel.setOpenTicketsDialog(false) },
            onSelectTicket = { ticket -> viewModel.loadOpenTicket(ticket) },
            onMergeTickets = { targetId, tickets -> viewModel.mergeTickets(targetId, tickets) },
            onVoidTicket = { ticketId -> viewModel.closeOrVoidTicket(ticketId) }
        )
    }

    // Save Ticket Dialog
    if (uiState.showSaveTicketDialog) {
        SaveTicketDialog(
            initialName = uiState.currentTicketName,
            onDismiss = { viewModel.setSaveTicketDialog(false) },
            onConfirm = { name, table -> viewModel.saveTicket(name, table) }
        )
    }

    // Checkout Sheet Dialog
    if (uiState.showCheckoutSheet) {
        CheckoutDialog(
            grandTotal = uiState.grandTotal,
            subtotal = uiState.subtotal,
            taxAmount = uiState.taxAmount,
            discountAmount = uiState.discountAmount,
            diningOption = uiState.diningOption,
            selectedCustomer = uiState.selectedCustomer,
            onDismiss = { viewModel.setCheckoutSheet(false) },
            onRedeemPoints = { amt -> viewModel.redeemCustomerPoints(amt) },
            onPaymentConfirmed = { type, details, tip ->
                viewModel.completePayment(type, details, tip)
            }
        )
    }

    // Receipt Confirmation Dialog
    uiState.completedReceipt?.let { receipt ->
        ReceiptDialog(
            receipt = receipt,
            onDismiss = { viewModel.startNewSale() },
            onNewSale = { viewModel.startNewSale() }
        )
    }

    // Shift Management Dialog
    if (uiState.showShiftDialog) {
        ShiftManagementDialog(
            currentShift = currentShift,
            onDismiss = { viewModel.setShiftDialog(false) },
            onOpenShift = { float -> viewModel.openShift(float) },
            onCashAction = { isPayIn, amt, comment -> viewModel.recordCashAction(isPayIn, amt, comment) },
            onCloseShift = { actual -> viewModel.closeShift(actual) }
        )
    }

    // Barcode Scanner Dialog
    if (uiState.showBarcodeDialog) {
        BarcodeScannerDialog(
            onDismiss = { viewModel.setBarcodeDialog(false) },
            onBarcodeScanned = { code -> viewModel.onBarcodeScanned(code) }
        )
    }

    // Customer Selection Dialog
    if (uiState.showCustomerSelectDialog) {
        CustomerSelectionDialog(
            customers = customers,
            selectedCustomer = uiState.selectedCustomer,
            onDismiss = { viewModel.setCustomerSelectDialog(false) },
            onSelectCustomer = { customer ->
                viewModel.selectCustomer(customer)
                viewModel.setCustomerSelectDialog(false)
            }
        )
    }

    // Receipts Archive Dialog
    if (uiState.showReceiptsDialog) {
        ReceiptsHistoryDialog(
            receipts = receipts,
            onDismiss = { viewModel.setReceiptsDialog(false) },
            onRefundReceipt = { receipt -> viewModel.refundReceipt(receipt) }
        )
    }

    // Thermal Printer Manager Dialog
    if (uiState.showPrinterSettingsDialog) {
        ThermalPrinterSettingsDialog(
            onDismiss = { viewModel.setPrinterSettingsDialog(false) }
        )
    }

    // Low Stock Alert Dialog
    if (uiState.showLowStockDialog) {
        LowStockAlertDialog(
            lowStockProducts = lowStockProducts,
            currentThreshold = uiState.lowStockThreshold,
            onThresholdChange = { threshold -> viewModel.setLowStockThreshold(threshold) },
            onRestockProduct = { product, addedQty -> viewModel.restockProduct(product, addedQty) },
            onRunInventoryCheck = { viewModel.checkAndNotifyLowStock(uiState.lowStockThreshold) },
            onDismiss = { viewModel.setLowStockDialog(false) }
        )
    }

    // Sync & Connectivity Status Dialog
    if (uiState.showSyncStatusDialog) {
        com.example.ui.components.SyncStatusDialog(
            isOnline = isOnline,
            connectionType = connectionType,
            syncState = syncState,
            lastSyncTime = lastSyncTime,
            pendingSyncCount = pendingSyncCount,
            onTriggerSync = { syncManager.triggerSync() },
            onOpenBackupDialog = {
                viewModel.setSyncStatusDialog(false)
                viewModel.setDatabaseBackupDialog(true)
            },
            onDismiss = { viewModel.setSyncStatusDialog(false) }
        )
    }

    // Database Backup & Restore Dialog
    if (uiState.showDatabaseBackupDialog) {
        com.example.ui.components.DatabaseBackupDialog(
            database = database,
            onDismiss = { viewModel.setDatabaseBackupDialog(false) }
        )
    }

    // Ticket-level Discount Dialog
    if (showDiscountDialog) {
        var discountInput by remember { mutableStateOf(uiState.ticketDiscountPercent.toString()) }
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Apply Ticket Discount", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5.0, 10.0, 15.0, 20.0).forEach { pct ->
                            OutlinedButton(
                                onClick = { discountInput = pct.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("${pct.toInt()}%", fontSize = 12.sp)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text("Discount Percentage (%)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pct = discountInput.toDoubleOrNull() ?: 0.0
                    viewModel.applyTicketDiscount(pct)
                    showDiscountDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CategoryFilterRow(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategorySelect: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Items", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }
        items(categories) { category ->
            val isSelected = selectedCategoryId == category.id
            val chipColor = try {
                Color(android.graphics.Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                MaterialTheme.colorScheme.primary
            }

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category.id) },
                label = { Text(category.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(chipColor)
                    )
                }
            )
        }
    }
}

@Composable
fun ItemGridCard(
    item: ItemEntity,
    onClick: () -> Unit
) {
    val cardAccentColor = remember(item.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.colorHex))
        } catch (e: Exception) {
            Color(0xFF2E7D32)
        }
    }

    val isLowStock = item.trackStock && item.inStock <= item.lowStockThreshold && item.inStock > 0.0
    val isOutOfStock = item.trackStock && item.inStock <= 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Top Accent Color stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(cardAccentColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Name & Stock badge
                Column {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Price and stock warning status
                Column {
                    if (isOutOfStock) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OUT OF STOCK",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isLowStock) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Low: ${item.inStock.toInt()} left",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", item.price)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.soldBy == SoldBy.WEIGHT) {
                            Text("/ kg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemListRow(
    item: ItemEntity,
    onClick: () -> Unit
) {
    val cardAccentColor = remember(item.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.colorHex))
        } catch (e: Exception) {
            Color(0xFF2E7D32)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardAccentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = cardAccentColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${item.categoryName} • ${if (item.trackStock) "Stock: ${item.inStock.toInt()}" else "Stock not tracked"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "$${String.format(Locale.US, "%.2f", item.price)}${if (item.soldBy == SoldBy.WEIGHT) "/kg" else ""}",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CompactCartBottomBar(
    uiState: PosUiState,
    onViewCart: () -> Unit,
    onCharge: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clickable(onClick = onViewCart)
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BadgedBox(
                    badge = {
                        if (uiState.itemCount > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text(uiState.itemCount.toString())
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Ticket (${uiState.diningOption.label})", style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = onCharge,
                enabled = uiState.cartItems.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("charge_button")
            ) {
                Text(
                    "CHARGE $${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TicketCartPanel(
    uiState: PosUiState,
    openTicketsCount: Int,
    onUpdateQuantity: (index: Int, newQuantity: Double) -> Unit,
    onRemoveItem: (index: Int) -> Unit,
    onClearCart: () -> Unit,
    onOpenTicketsClick: () -> Unit,
    onSaveTicketClick: () -> Unit,
    onDiscountClick: () -> Unit,
    onChargeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Ticket Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = uiState.currentTicketName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.diningOption.label} • ${if (uiState.selectedCustomer != null) uiState.selectedCustomer.name else "Walk-in Guest"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Open tickets button with badge
                    FilledTonalButton(
                        onClick = onOpenTicketsClick,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open ($openTicketsCount)", fontSize = 12.sp)
                    }

                    if (uiState.cartItems.isNotEmpty()) {
                        IconButton(onClick = onClearCart) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Ticket", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Ticket Items List
        if (uiState.cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCartCheckout,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ticket is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Select items from the catalog",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.cartItems) { index, item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.itemName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.selectedModifiersJson.isNotBlank()) {
                                        Text(
                                            text = item.selectedModifiersJson,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (item.comment.isNotBlank()) {
                                        Text(
                                            text = "Note: ${item.comment}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quantity controls & delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilledIconButton(
                                        onClick = { onUpdateQuantity(index, item.quantity - 1.0) },
                                        modifier = Modifier.size(28.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.2f", item.quantity),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    FilledIconButton(
                                        onClick = { onUpdateQuantity(index, item.quantity + 1.0) },
                                        modifier = Modifier.size(28.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveItem(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Calculations & Action Buttons
        Surface(
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodySmall)
                    Text("$${String.format(Locale.US, "%.2f", uiState.subtotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                if (uiState.discountAmount > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Discounts ${if (uiState.ticketDiscountPercent > 0) "(${uiState.ticketDiscountPercent.toInt()}%)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "-$${String.format(Locale.US, "%.2f", uiState.discountAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sales Tax (8.25%)", style = MaterialTheme.typography.bodySmall)
                    Text("$${String.format(Locale.US, "%.2f", uiState.taxAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons: Save Ticket, Discount, Charge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveTicketClick,
                        modifier = Modifier.weight(1f),
                        enabled = uiState.cartItems.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDiscountClick,
                        modifier = Modifier.weight(1f),
                        enabled = uiState.cartItems.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Discount", fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onChargeClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = uiState.cartItems.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "CHARGE $${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
