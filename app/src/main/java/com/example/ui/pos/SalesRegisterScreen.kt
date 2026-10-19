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
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
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

    // Filter items by category, favorites & search query
    val displayedItems = remember(allItems, uiState.selectedCategoryId, uiState.isFavoritesSelected, uiState.searchQuery) {
        allItems.filter { item ->
            val matchCategory = when {
                uiState.isFavoritesSelected -> item.id in listOf(101L, 102L, 103L, 105L, 106L) || item.inStock >= 40.0
                uiState.selectedCategoryId != null -> item.categoryId == uiState.selectedCategoryId
                else -> true
            }
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
                var showMoreMenu by remember { mutableStateOf(false) }

                TopAppBar(
                    title = {
                        if (uiState.isSearchActive) {
                            TextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        "Search menu, barcode or SKU...",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 14.sp
                                    )
                                },
                                singleLine = true,
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                                        }
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White,
                                    focusedIndicatorColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("appbar_search_input")
                            )
                        } else {
                            Column(modifier = Modifier.clickable { viewModel.setOpenTicketsDialog(true) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = uiState.currentTicketName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Switch Ticket",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "${uiState.cashierName} • ${if (currentShift?.isOpen == true) "Turno Abierto" else "Turno Cerrado"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.testTag("drawer_menu_button")) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        // Search Toggle Button
                        IconButton(onClick = { viewModel.setSearchActive(!uiState.isSearchActive) }) {
                            Icon(
                                if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }

                        // Barcode Scan Button
                        IconButton(onClick = { viewModel.setBarcodeDialog(true) }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = Color.White)
                        }

                        // Customer Loyalty Button
                        IconButton(onClick = { viewModel.setCustomerSelectDialog(true) }) {
                            BadgedBox(
                                badge = {
                                    if (uiState.selectedCustomer != null) {
                                        Badge(containerColor = Color(0xFFFFD54F), contentColor = Color.Black) { Text("★") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Customer", tint = Color.White)
                            }
                        }

                        // Sync Status & Trigger Button
                        IconButton(
                            onClick = {
                                syncManager.triggerSync()
                                viewModel.setSyncStatusDialog(true)
                            },
                            modifier = Modifier.testTag("sync_status_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = CircleShape,
                                        color = if (isOnline) Color(0xFF81C784) else Color(0xFFFFB74D)
                                    ) {}
                                }
                            ) {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Sync Status",
                                    tint = Color.White
                                )
                            }
                        }

                        // Grid / List View Toggle
                        IconButton(onClick = { viewModel.toggleLayoutMode() }) {
                            Icon(
                                if (uiState.isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View",
                                tint = Color.White
                            )
                        }

                        // More Options Dropdown Menu
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Shift Management") },
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.setShiftDialog(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Thermal Printer Settings") },
                                    leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.setPrinterSettingsDialog(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Review Low Stock (${lowStockProducts.size})") },
                                    leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100)) },
                                    onClick = {
                                        showMoreMenu = false
                                        viewModel.setLowStockDialog(true)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Open Online Back Office") },
                                    leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color(0xFF2E7D32)) },
                                    onClick = {
                                        showMoreMenu = false
                                        try {
                                            val intent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse(com.example.util.AppConfig.BACK_OFFICE_URL)
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2E7D32),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
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
                        isKeypadSelected = uiState.isKeypadSelected,
                        isFavoritesSelected = uiState.isFavoritesSelected,
                        onCategorySelect = { viewModel.selectCategory(it) },
                        onFavoritesSelect = { viewModel.selectFavoritesTab() },
                        onKeypadSelect = { viewModel.selectKeypadTab() }
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

                    // Items Grid, List or Loyverse Keypad
                    if (uiState.isKeypadSelected) {
                        LoyverseKeypadView(
                            inputAmountCents = uiState.keypadInputAmount,
                            onDigit = { viewModel.onKeypadDigit(it) },
                            onBackspace = { viewModel.onKeypadBackspace() },
                            onClear = { viewModel.onKeypadClear() },
                            onAddToTicket = { viewModel.addKeypadAmountToTicket() }
                        )
                    } else if (displayedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (syncState == com.example.util.SyncState.SYNCING && allItems.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                                    Text(
                                        text = "Connecting to Back Office & loading catalog...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (syncState == com.example.util.SyncState.ERROR && allItems.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Unable to connect to Back Office",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Check network connection or tap retry to re-sync.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { syncManager.triggerSync() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Sync")
                                    }
                                }
                            } else if (uiState.searchQuery.isNotEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No items match '${uiState.searchQuery}'",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (uiState.isFavoritesSelected) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No favorite items yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No items in this category",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
    isKeypadSelected: Boolean,
    isFavoritesSelected: Boolean,
    onCategorySelect: (Long?) -> Unit,
    onFavoritesSelect: () -> Unit,
    onKeypadSelect: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // All Items tab
        item {
            FilterChip(
                selected = !isKeypadSelected && !isFavoritesSelected && selectedCategoryId == null,
                onClick = { onCategorySelect(null) },
                label = { Text("All Items", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                    selectedLabelColor = Color(0xFF2E7D32),
                    selectedLeadingIconColor = Color(0xFF2E7D32)
                )
            )
        }

        // Favorites tab
        item {
            FilterChip(
                selected = !isKeypadSelected && isFavoritesSelected,
                onClick = { onFavoritesSelect() },
                label = { Text("Favorites", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFB300)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFB300).copy(alpha = 0.15f),
                    selectedLabelColor = Color(0xFFF57F17),
                    selectedLeadingIconColor = Color(0xFFF57F17)
                )
            )
        }

        // Dynamic categories from Back Office / Room
        items(categories) { category ->
            val isSelected = !isKeypadSelected && !isFavoritesSelected && selectedCategoryId == category.id
            val chipColor = try {
                Color(android.graphics.Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                Color(0xFF2E7D32)
            }

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(category.id) },
                label = { Text(category.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(chipColor)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.15f),
                    selectedLabelColor = chipColor
                )
            )
        }

        // Keypad tab
        item {
            FilterChip(
                selected = isKeypadSelected,
                onClick = { onKeypadSelect() },
                label = { Text("Keypad", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF2E7D32).copy(alpha = 0.15f),
                    selectedLabelColor = Color(0xFF2E7D32),
                    selectedLeadingIconColor = Color(0xFF2E7D32)
                )
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("item_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(cardAccentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = cardAccentColor
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cardAccentColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardAccentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.name.take(2).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Top Accent Stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(cardAccentColor)
                        .align(Alignment.TopCenter)
                )

                // Stock status overlay if low or out of stock
                if (isOutOfStock) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "OUT OF STOCK",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isLowStock) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.9f),
                        shape = RoundedCornerShape(bottomStart = 6.dp),
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "${item.inStock.toInt()} left",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Text Info area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.price)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    if (item.soldBy == SoldBy.WEIGHT) {
                        Text(
                            text = "/kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
            .clickable(onClick = onClick)
            .testTag("item_row_${item.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cardAccentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = {
                            Text(
                                text = item.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = cardAccentColor
                            )
                        }
                    )
                } else {
                    Text(
                        text = item.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = cardAccentColor
                    )
                }
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
                color = Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
fun LoyverseKeypadView(
    inputAmountCents: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onAddToTicket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val amountVal = (inputAmountCents.toDoubleOrNull() ?: 0.0) / 100.0
    val formattedAmount = "$${String.format(Locale.US, "%.2f", amountVal)}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Display Area
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Custom Amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        // Keypad Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "00")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (key in row) {
                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> onClear()
                                    else -> onDigit(key)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (key == "C") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (key == "C") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Backspace row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onBackspace() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }

        // Add to Ticket Button
        Button(
            onClick = onAddToTicket,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            ),
            enabled = amountVal > 0.0
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add to Ticket  ($formattedAmount)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
