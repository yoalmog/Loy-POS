package com.example.ui.backoffice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.backoffice.components.ALLERGEN_OPTIONS
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.ProductInventoryGrid
import com.example.ui.theme.PosDesignTokens
import com.example.util.AppConfig
import com.example.util.RecipeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackOfficeScreen(
    viewModel: BackOfficeViewModel,
    onNavigateToPos: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val items by viewModel.items.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val receipts by viewModel.receipts.collectAsState()
    val products by viewModel.products.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val syncManager = remember { com.example.util.SyncManager(context, viewModel.repository) }
    val database = remember { com.example.data.db.AppDatabase.getDatabase(context) }

    val isOnline by syncManager.networkMonitor.isOnline.collectAsState()
    val connectionType by syncManager.networkMonitor.connectionType.collectAsState()
    val syncState by syncManager.syncState.collectAsState()
    val lastSyncTime by syncManager.lastSyncTimestamp.collectAsState()
    val pendingSyncCount by syncManager.pendingSyncCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissStatusMessage()
        }
    }

    LaunchedEffect(Unit) {
        RecipeManager.initialize(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Loyverse Back Office", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(uiState.storeName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToPos) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Register")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setSyncStatusDialog(true) },
                        modifier = Modifier.testTag("backoffice_sync_status_button")
                    ) {
                        BadgedBox(
                            badge = {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = CircleShape,
                                    color = if (isOnline) PosDesignTokens.AccentEmerald else PosDesignTokens.AccentAmber
                                ) {}
                            }
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = "Sync Status",
                                tint = if (isOnline) PosDesignTokens.AccentEmerald else PosDesignTokens.AccentAmber
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onNavigateToPos,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open POS")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                BackOfficeTab.values().forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                when (tab) {
                                    BackOfficeTab.SALES_SUMMARY -> Icons.Default.Analytics
                                    BackOfficeTab.ITEMS_INVENTORY -> Icons.Default.Inventory2
                                    BackOfficeTab.CUSTOMERS -> Icons.Default.People
                                    BackOfficeTab.SHIFTS -> Icons.Default.History
                                    BackOfficeTab.SETTINGS -> Icons.Default.Settings
                                },
                                contentDescription = tab.label
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                BackOfficeTab.SALES_SUMMARY -> SalesSummaryTab(receipts = receipts, uiState = uiState)
                BackOfficeTab.ITEMS_INVENTORY -> ItemsInventoryTab(
                    items = items,
                    products = products,
                    categories = categories,
                    filterQuery = uiState.itemFilterQuery,
                    onQueryChange = { viewModel.setItemFilterQuery(it) },
                    onEditItem = { viewModel.openItemEditor(it) },
                    onAddNewItem = { viewModel.openItemEditor(null) },
                    onAdjustStock = { viewModel.openStockAdjustment(it) },
                    onQuickStockAdjust = { prod, delta -> viewModel.quickAdjustProductStock(prod, delta) }
                )
                BackOfficeTab.CUSTOMERS -> CustomersTab(
                    customers = customers,
                    onAddCustomer = { viewModel.openAddCustomer() },
                    onViewHistory = { viewModel.viewCustomerReceipts(it) }
                )
                BackOfficeTab.SHIFTS -> ShiftsTab(shifts = shifts)
                BackOfficeTab.SETTINGS -> SettingsTab(
                    uiState = uiState,
                    isOnline = isOnline,
                    connectionType = connectionType,
                    onOpenSyncStatus = { viewModel.setSyncStatusDialog(true) },
                    onOpenBackup = { viewModel.setDatabaseBackupDialog(true) }
                )
            }
        }
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

    // Add / Edit Item Dialog
    if (uiState.showItemEditorDialog) {
        ItemEditorDialog(
            item = uiState.editingItem,
            categories = categories,
            onDismiss = { viewModel.closeItemEditor() },
            onSave = { id, name, catId, catName, price, cost, sku, barcode, soldBy, inStock, lowThreshold, track, color, recipe ->
                viewModel.saveItem(id, name, catId, catName, price, cost, sku, barcode, soldBy, inStock, lowThreshold, track, color, recipe, context)
            },
            onDelete = { item -> viewModel.deleteItem(item) }
        )
    }

    // Stock Adjustment Dialog
    if (uiState.showStockAdjustDialog && uiState.adjustingItem != null) {
        StockAdjustmentDialog(
            item = uiState.adjustingItem!!,
            onDismiss = { viewModel.closeStockAdjustment() },
            onConfirm = { diff, reason -> viewModel.applyStockAdjustment(uiState.adjustingItem!!.id, diff, reason) }
        )
    }

    // Add Customer Dialog
    if (uiState.showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { viewModel.closeAddCustomer() },
            onConfirm = { name, phone, email, note -> viewModel.saveCustomer(name, phone, email, note) }
        )
    }

    // Customer Purchase History Dialog
    uiState.viewingCustomerReceipts?.let { customer ->
        val customerReceipts = remember(receipts, customer.id) {
            receipts.filter { it.customerId == customer.id }
        }
        AlertDialog(
            onDismissRequest = { viewModel.viewCustomerReceipts(null) },
            title = { Text("${customer.name}'s Purchase History", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 350.dp)) {
                    Text("Total Visits: ${customer.totalVisits} • Total Spent: $${String.format(Locale.US, "%.2f", customer.totalSpent)} • Points: ${String.format(Locale.US, "%.1f", customer.pointsBalance)}")
                    Divider()
                    if (customerReceipts.isEmpty()) {
                        Text("No prior receipts on record", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(customerReceipts) { r ->
                                val dateStr = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US).format(Date(r.timestamp))
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(r.receiptNumber, fontWeight = FontWeight.Bold)
                                            Text("$${String.format(Locale.US, "%.2f", r.totalAmount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$dateStr • ${r.diningOption.label}", style = MaterialTheme.typography.bodySmall)
                                        Text(r.itemsSummaryJson, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.viewCustomerReceipts(null) }) { Text("Close") }
            }
        )
    }
}

@Composable
fun SalesSummaryTab(receipts: List<ReceiptEntity>, uiState: BackOfficeUiState) {
    val totalGrossSales = remember(receipts) { receipts.filter { !it.isRefunded }.sumOf { it.totalAmount } }
    val totalRefunds = remember(receipts) { receipts.filter { it.isRefunded }.sumOf { it.totalAmount } }
    val totalDiscounts = remember(receipts) { receipts.sumOf { it.discountAmount } }
    val netSales = (totalGrossSales - totalRefunds - totalDiscounts).coerceAtLeast(0.0)
    val estimatedCogs = netSales * 0.35 // estimated 35% food cost
    val grossProfit = netSales - estimatedCogs

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Sales Performance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Aggregated across all registers & dining options", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // KPI Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(title = "Gross Sales", value = "$${String.format(Locale.US, "%.2f", totalGrossSales)}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                KpiCard(title = "Net Sales", value = "$${String.format(Locale.US, "%.2f", netSales)}", modifier = Modifier.weight(1f), color = Color(0xFF00796B))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(title = "Discounts", value = "$${String.format(Locale.US, "%.2f", totalDiscounts)}", modifier = Modifier.weight(1f), color = Color(0xFFF57F17))
                KpiCard(title = "Refunds", value = "$${String.format(Locale.US, "%.2f", totalRefunds)}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiCard(title = "Est. Gross Profit", value = "$${String.format(Locale.US, "%.2f", grossProfit)}", modifier = Modifier.weight(1f), color = PosDesignTokens.AccentEmerald)
                KpiCard(title = "Total Receipts", value = receipts.size.toString(), modifier = Modifier.weight(1f), color = Color(0xFF1976D2))
            }
        }

        // Analytics & Data Visualization Charts
        item {
            com.example.ui.components.AnalyticsDataVisualizationScreen(receipts = receipts)
        }

        // Recent Receipts breakdown
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recent Transaction Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(receipts) { receipt ->
            val dateStr = SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(receipt.timestamp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(receipt.receiptNumber, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("(${receipt.diningOption.label})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$dateStr • ${receipt.cashierName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(receipt.itemsSummaryJson, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "$${String.format(Locale.US, "%.2f", receipt.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = if (receipt.isRefunded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(receipt.paymentType.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(title: String, value: String, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun ItemsInventoryTab(
    items: List<ItemEntity>,
    products: List<Product>,
    categories: List<CategoryEntity>,
    filterQuery: String,
    onQueryChange: (String) -> Unit,
    onEditItem: (ItemEntity) -> Unit,
    onAddNewItem: () -> Unit,
    onAdjustStock: (ItemEntity) -> Unit,
    onQuickStockAdjust: (Product, Int) -> Unit
) {
    var viewMode by remember { mutableStateOf(0) } // 0 = Product Grid (Loyverse UI), 1 = Table List
    var editingRecipeItem by remember { mutableStateOf<ItemEntity?>(null) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab selector for view modes
        TabRow(
            selectedTabIndex = viewMode,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(
                selected = viewMode == 0,
                onClick = { viewMode = 0 },
                text = { Text("Product Grid (Loyverse)") },
                icon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = viewMode == 1,
                onClick = { viewMode = 1 },
                text = { Text("Inventory Table") },
                icon = { Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (viewMode == 0) {
            // Render the requested ProductInventoryGrid Compose component
            ProductInventoryGrid(
                products = products,
                modifier = Modifier.weight(1f),
                onProductClick = { prod ->
                    // Optionally link to edit
                },
                onQuickStockAdjust = onQuickStockAdjust
            )
        } else {
            val filtered = remember(items, filterQuery) {
                if (filterQuery.isBlank()) items
                else items.filter { it.name.contains(filterQuery, ignoreCase = true) || it.sku.contains(filterQuery) }
            }

            val lowStockCount = items.count { it.trackStock && it.inStock <= it.lowStockThreshold }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Items & Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (lowStockCount > 0) {
                        Text("$lowStockCount items low or out of stock", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Button(onClick = onAddNewItem, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }

            OutlinedTextField(
                value = filterQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Filter items...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            items(filtered) { item ->
                val isLow = item.trackStock && item.inStock <= item.lowStockThreshold

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${item.categoryName} • SKU: ${item.sku} • Price: $${String.format(Locale.US, "%.2f", item.price)} (Cost: $${String.format(Locale.US, "%.2f", item.cost)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.trackStock) {
                                    Surface(
                                        color = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Stock: ${if (item.soldBy == SoldBy.WEIGHT) String.format(Locale.US, "%.2f kg", item.inStock) else item.inStock.toInt().toString()}",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text("Stock not tracked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { onAdjustStock(item) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Supply", style = MaterialTheme.typography.labelMedium)
                            }
                                Spacer(modifier = Modifier.width(4.dp))
                                FilledTonalButton(
                                    onClick = { editingRecipeItem = item },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFEF3C7), contentColor = Color(0xFFB45309))
                                ) {
                                    Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("מתכון והכנה", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { onEditItem(item) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editingRecipeItem != null) {
        val currentRecipe = remember(editingRecipeItem) {
            RecipeManager.getRecipeForItem(editingRecipeItem!!.id, editingRecipeItem!!.name, editingRecipeItem!!.categoryName)
        }
        com.example.ui.backoffice.components.RecipeEditorDialog(
            recipe = currentRecipe,
            onDismiss = { editingRecipeItem = null },
            onSave = { updated ->
                RecipeManager.saveRecipe(context, updated)
                editingRecipeItem = null
            }
        )
    }
}

@Composable
fun CustomersTab(
    customers: List<CustomerEntity>,
    onAddCustomer: () -> Unit,
    onViewHistory: (CustomerEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Customer Loyalty Base", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${customers.size} registered customers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onAddCustomer, shape = RoundedCornerShape(8.dp)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Customer")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(customers) { customer ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${customer.phone} • ${customer.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Loyalty Code: ${customer.customerCode} • Total Visits: ${customer.totalVisits} • Spent: $${String.format(Locale.US, "%.2f", customer.totalSpent)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${String.format(Locale.US, "%.1f", customer.pointsBalance)} pts",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { onViewHistory(customer) }) {
                                Text("Purchases", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftsTab(shifts: List<ShiftEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Register Shifts History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Cash drawer open/close records and discrepancy tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(shifts) { shift ->
            val openStr = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US).format(Date(shift.openedAt))
            val closeStr = shift.closedAt?.let { SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(it)) } ?: "Still Open"

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Shift #${shift.id} • ${shift.cashierName}", fontWeight = FontWeight.Bold)
                        Surface(
                            color = if (shift.isOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (shift.isOpen) "OPEN" else "CLOSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text("Opened: $openStr\nClosed: $closeStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Starting Float: $${String.format(Locale.US, "%.2f", shift.startingCash)}", style = MaterialTheme.typography.labelSmall)
                        Text("Cash Sales: $${String.format(Locale.US, "%.2f", shift.cashPayments)}", style = MaterialTheme.typography.labelSmall)
                        Text("Expected: $${String.format(Locale.US, "%.2f", shift.expectedCash)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    uiState: BackOfficeUiState,
    isOnline: Boolean,
    connectionType: String,
    onOpenSyncStatus: () -> Unit,
    onOpenBackup: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Store & System Administration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("General Store Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Store Name: ${uiState.storeName}")
                    Text("Currency: USD ($)")
                    Text("Sales Tax: 8.25% (Added to ticket)")
                    Text("Dining Options: Dine in, Takeout, Delivery (All Active)")
                    Text("Loyalty Program: 1% points earned on every dollar spent")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Online Web Back Office", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Access your full online Back Office website from any desktop, tablet, or phone browser to manage sales, products, inventory, customers, shifts, and reports.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val context = LocalContext.current
                    val webUrl = AppConfig.BACK_OFFICE_URL
                    
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("open_web_back_office_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Online Back Office Website")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = if (isOnline) PosDesignTokens.AccentEmerald else PosDesignTokens.AccentAmber
                            )
                            Text("Network & Sync Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (isOnline) PosDesignTokens.AccentEmerald.copy(alpha = 0.15f) else PosDesignTokens.AccentAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                if (isOnline) "Online ($connectionType)" else "Offline Mode",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) PosDesignTokens.AccentEmerald else PosDesignTokens.AccentAmber
                            )
                        }
                    }
                    Text("AuraPOS utilizes local SQLite Room database storage ensuring zero downtime offline operation.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = onOpenSyncStatus,
                        modifier = Modifier.fillMaxWidth().testTag("view_sync_details_button")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Sync & Connection Details")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Database Backup & Safety (.json)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Export or restore products catalog, sales receipts, shift records, and customer loyalty balances to JSON files.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = onOpenBackup,
                        modifier = Modifier.fillMaxWidth().testTag("open_database_backup_manager_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Database Backup & Restore")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemEditorDialog(
    item: ItemEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        catId: Long,
        catName: String,
        price: Double,
        cost: Double,
        sku: String,
        barcode: String,
        soldBy: SoldBy,
        inStock: Double,
        lowThreshold: Double,
        track: Boolean,
        color: String,
        recipe: DishRecipe?
    ) -> Unit,
    onDelete: (ItemEntity) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // 1. General Item info state
    var name by remember { mutableStateOf(item?.name ?: "") }
    var selectedCatId by remember { mutableStateOf(item?.categoryId ?: (categories.firstOrNull()?.id ?: 1L)) }
    var priceText by remember { mutableStateOf(item?.price?.toString() ?: "12.00") }
    var costText by remember { mutableStateOf(item?.cost?.toString() ?: "3.50") }
    var sku by remember { mutableStateOf(item?.sku ?: "2001") }
    var barcode by remember { mutableStateOf(item?.barcode ?: "200001001000") }
    var soldBy by remember { mutableStateOf(item?.soldBy ?: SoldBy.EACH) }
    var inStockText by remember { mutableStateOf(item?.inStock?.toString() ?: "20") }
    var lowThresholdText by remember { mutableStateOf(item?.lowStockThreshold?.toString() ?: "5") }
    var trackStock by remember { mutableStateOf(item?.trackStock ?: true) }

    // 2. Load initial Recipe / BOM data for this dish
    val initialRecipe = remember(item) {
        if (item != null) {
            RecipeManager.getRecipeForItem(item.id, item.name, item.categoryName)
        } else {
            DishRecipe(
                itemId = 0L,
                itemName = "",
                categoryName = categories.firstOrNull()?.name ?: "כללי",
                ingredients = emptyList(),
                prepGuide = KitchenPrepGuide(
                    prepTimeMinutes = 10,
                    station = KitchenStation.GENERAL,
                    steps = emptyList(),
                    allergens = emptyList(),
                    chefNotes = "",
                    platingGuide = ""
                )
            )
        }
    }

    // 3. BOM / Recipe Ingredients state
    var ingredients by remember { mutableStateOf(initialRecipe.ingredients) }
    var newIngName by remember { mutableStateOf("") }
    var newIngQty by remember { mutableStateOf("") }
    var newIngUnit by remember { mutableStateOf("גרם") }
    var newIngCost by remember { mutableStateOf("") }
    var newIngDeductStock by remember { mutableStateOf(true) }
    var showAddIngForm by remember { mutableStateOf(false) }

    // 4. Kitchen Prep & Cooking Instructions state
    var prepTimeMinutes by remember { mutableStateOf(initialRecipe.prepGuide.prepTimeMinutes.toString()) }
    var selectedStation by remember { mutableStateOf(initialRecipe.prepGuide.station) }
    var steps by remember { mutableStateOf(initialRecipe.prepGuide.steps) }
    var newStepText by remember { mutableStateOf("") }
    var selectedAllergens by remember { mutableStateOf(initialRecipe.prepGuide.allergens.toSet()) }
    var chefNotes by remember { mutableStateOf(initialRecipe.prepGuide.chefNotes) }
    var platingGuide by remember { mutableStateOf(initialRecipe.prepGuide.platingGuide) }

    val totalBomCost = remember(ingredients) {
        ingredients.sumOf { it.quantity * it.costPerUnit }
    }
    val currentPrice = priceText.toDoubleOrNull() ?: 0.0
    val grossProfit = currentPrice - totalBomCost
    val marginPercent = if (currentPrice > 0.0) ((grossProfit / currentPrice) * 100).toInt() else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (item == null) "הוספת מנה חדשה ועץ מוצר (New Item & BOM)" else "עריכת מנה ועץ מוצר (${name.ifBlank { "מנה" }})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ניהול פרטי מוצר, עץ מוצר (BOM) והוראות הכנה למטבח (KDS)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item != null) {
                            IconButton(onClick = { onDelete(item) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "מחק מנה",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "סגור")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Three-tab navigation inside product editor
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("פרטי פריט", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("עץ מוצר ומצרכים (BOM)", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                if (ingredients.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${ingredients.size}", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.OutdoorGrill, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("הוראות הכנה למטבח (KDS)", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Viewport Content based on selected tab
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> {
                            // TAB 0: General Item Details
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        label = { Text("שם המנה / מוצר *") },
                                        placeholder = { Text("למשל: המבורגר אנטרקוט כפול") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    Text("קטגוריה:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(categories) { cat ->
                                            FilterChip(
                                                selected = selectedCatId == cat.id,
                                                onClick = { selectedCatId = cat.id },
                                                label = { Text(cat.name, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = priceText,
                                            onValueChange = { priceText = it },
                                            label = { Text("מחיר מכירה ($ / ₪)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        OutlinedTextField(
                                            value = costText,
                                            onValueChange = { costText = it },
                                            label = { Text("עלות ($ / ₪)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Quick BOM Cost sync helper
                                    if (ingredients.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        TextButton(
                                            onClick = {
                                                costText = String.format(Locale.US, "%.2f", totalBomCost)
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "השתמש בעלות חומרי הגלם המחושבת מעץ המוצר: $${String.format(Locale.US, "%.2f", totalBomCost)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                item {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = sku,
                                            onValueChange = { sku = it },
                                            label = { Text("מק\"ט (SKU)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = barcode,
                                            onValueChange = { barcode = it },
                                            label = { Text("ברקוד (Barcode)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("אופן מכירה: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            RadioButton(selected = soldBy == SoldBy.EACH, onClick = { soldBy = SoldBy.EACH })
                                            Text("לפי יחידה (Each)", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            RadioButton(selected = soldBy == SoldBy.WEIGHT, onClick = { soldBy = SoldBy.WEIGHT })
                                            Text("לפי משקל (Weight / kg)", fontSize = 12.sp)
                                        }
                                    }
                                }

                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("מעקב מלאי פריט", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("מעקב יחידות מוכנות במלאי (בנוסף לגריעת חומרי גלם ממתכון)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                                            }

                                            if (trackStock) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedTextField(
                                                        value = inStockText,
                                                        onValueChange = { inStockText = it },
                                                        label = { Text("כמות במלאי") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    OutlinedTextField(
                                                        value = lowThresholdText,
                                                        onValueChange = { lowThresholdText = it },
                                                        label = { Text("התראת מלאי נמוך") },
                                                        singleLine = true,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // TAB 1: BOM (Bill of Materials) & Ingredients Section
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Live Cost & Margin Analysis Header
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("עלות חומרי גלם (Food Cost):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "$${String.format(Locale.US, "%.2f", totalBomCost)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Column {
                                            Text("מחיר מכירה:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "$${String.format(Locale.US, "%.2f", currentPrice)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Column {
                                            Text("רווח גולמי:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "$${String.format(Locale.US, "%.2f", grossProfit)} ($marginPercent%)",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (grossProfit >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "רשימת חומרי גלם וכמויות למנה (${ingredients.size}):",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Button(
                                        onClick = { showAddIngForm = !showAddIngForm },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (showAddIngForm) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showAddIngForm) Icons.Default.Close else Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (showAddIngForm) "סגור" else "הוסף חומר גלם")
                                    }
                                }

                                // Add Ingredient Form
                                AnimatedVisibility(visible = showAddIngForm) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("הגדרת חומר גלם חדש למנה (BOM):", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = newIngName,
                                                    onValueChange = { newIngName = it },
                                                    label = { Text("שם חומר הגלם") },
                                                    placeholder = { Text("למשל: קציצת בקר, גבינה") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.5f)
                                                )
                                                OutlinedTextField(
                                                    value = newIngQty,
                                                    onValueChange = { newIngQty = it },
                                                    label = { Text("כמות") },
                                                    placeholder = { Text("150") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(0.7f)
                                                )
                                                OutlinedTextField(
                                                    value = newIngUnit,
                                                    onValueChange = { newIngUnit = it },
                                                    label = { Text("יחידה") },
                                                    placeholder = { Text("גרם") },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(0.8f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = newIngCost,
                                                    onValueChange = { newIngCost = it },
                                                    label = { Text("עלות ליחידה ($ / ₪)") },
                                                    placeholder = { Text("0.05") },
                                                    singleLine = true,
                                                    modifier = Modifier.width(160.dp)
                                                )

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(checked = newIngDeductStock, onCheckedChange = { newIngDeductStock = it })
                                                    Text("גרע מלאי אוטומטית", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        val qty = newIngQty.toDoubleOrNull() ?: 0.0
                                                        val cost = newIngCost.toDoubleOrNull() ?: 0.0
                                                        if (newIngName.isNotBlank() && qty > 0) {
                                                            ingredients = ingredients + RecipeIngredient(
                                                                name = newIngName.trim(),
                                                                quantity = qty,
                                                                unit = newIngUnit.trim(),
                                                                costPerUnit = cost,
                                                                deductStock = newIngDeductStock
                                                            )
                                                            newIngName = ""
                                                            newIngQty = ""
                                                            newIngCost = ""
                                                            showAddIngForm = false
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("הוסף")
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // List of Ingredients
                                if (ingredients.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                            Text("אין חומרי גלם מוגדרים עדיין למנה זו", color = MaterialTheme.colorScheme.outline)
                                            Text("לחץ על 'הוסף חומר גלם' להגדרת עץ המוצר (BOM)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        itemsIndexed(ingredients) { index, ing ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                            }
                                                        }

                                                        Column {
                                                            Text(ing.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Text("${ing.quantity} ${ing.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                if (ing.costPerUnit > 0) {
                                                                    Text("• עלות: $${String.format(Locale.US, "%.2f", ing.quantity * ing.costPerUnit)}", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                                                }
                                                                if (ing.deductStock) {
                                                                    Text("• גריעה אוטומטית", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            ingredients = ingredients.filterIndexed { i, _ -> i != index }
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "מחק", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // TAB 2: Kitchen Prep Instructions & Station (KDS) Section
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Workstation selector
                                item {
                                    Text("תחנת עבודה ייעודית במטבח (KDS Workstation):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(KitchenStation.values()) { station ->
                                            FilterChip(
                                                selected = selectedStation == station,
                                                onClick = { selectedStation = station },
                                                label = { Text(station.labelHebrew, fontSize = 12.sp) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = when (station) {
                                                            KitchenStation.GRILL -> Icons.Default.OutdoorGrill
                                                            KitchenStation.WOK -> Icons.Default.SoupKitchen
                                                            KitchenStation.OVEN -> Icons.Default.LocalPizza
                                                            KitchenStation.SALAD -> Icons.Default.EggAlt
                                                            KitchenStation.BAR -> Icons.Default.LocalBar
                                                            KitchenStation.PASTRY -> Icons.Default.Cake
                                                            KitchenStation.GENERAL -> Icons.Default.Kitchen
                                                        },
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                // Prep time
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("זמן הכנה משוער במטבח:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("לשימוש במדדי שעון עצר במסך המטבח (KDS)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        val cur = prepTimeMinutes.toIntOrNull() ?: 10
                                                        prepTimeMinutes = (cur - 1).coerceAtLeast(1).toString()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "הפחת")
                                                }
                                                Text(
                                                    text = "$prepTimeMinutes דק'",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val cur = prepTimeMinutes.toIntOrNull() ?: 10
                                                        prepTimeMinutes = (cur + 1).toString()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "הוסף")
                                                }
                                            }
                                        }
                                    }
                                }

                                // Step-by-Step Cooking Instructions for KDS
                                item {
                                    Text("שלבי ביצוע והכנה למטבח (Step-by-Step Directions):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newStepText,
                                            onValueChange = { newStepText = it },
                                            label = { Text("שלב ביצוע חדש...") },
                                            placeholder = { Text("למשל: לצרוב את הבקר בווק 90 שניות...") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                if (newStepText.isNotBlank()) {
                                                    steps = steps + newStepText.trim()
                                                    newStepText = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("הוסף")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (steps.isEmpty()) {
                                        Text("טרם הוגדרו שלבי הכנה למנה זו", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    } else {
                                        steps.forEachIndexed { sIdx, step ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFFF59E0B),
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Text("${sIdx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                            }
                                                        }
                                                        Text(step, fontSize = 12.sp)
                                                    }
                                                    IconButton(
                                                        onClick = { steps = steps.filterIndexed { i, _ -> i != sIdx } },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "מחק", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Allergens chips
                                item {
                                    Text("סימון אלרגנים למנה:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        ALLERGEN_OPTIONS.forEach { allergen ->
                                            val isSelected = selectedAllergens.contains(allergen)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedAllergens = if (isSelected) selectedAllergens - allergen else selectedAllergens + allergen
                                                },
                                                label = { Text(allergen, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                // Chef Notes & Plating guide
                                item {
                                    OutlinedTextField(
                                        value = chefNotes,
                                        onValueChange = { chefNotes = it },
                                        label = { Text("הערות שף ודגשי בישול למטבח") },
                                        placeholder = { Text("למשל: לשמור על חום גבוה, לא לייבש את הבשר") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = platingGuide,
                                        onValueChange = { platingGuide = it },
                                        label = { Text("הנחיות צלחות והגשה (Plating)") },
                                        placeholder = { Text("למשל: צלחת חרס מלבנית, קישוט כוסברה מעל") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ביטול")
                    }

                    Button(
                        onClick = {
                            val catName = categories.firstOrNull { it.id == selectedCatId }?.name ?: "כללי"
                            val parsedCost = costText.toDoubleOrNull() ?: totalBomCost

                            val recipeToSave = DishRecipe(
                                itemId = item?.id ?: 0L,
                                itemName = name,
                                categoryName = catName,
                                ingredients = ingredients,
                                prepGuide = KitchenPrepGuide(
                                    prepTimeMinutes = prepTimeMinutes.toIntOrNull() ?: 10,
                                    station = selectedStation,
                                    steps = steps,
                                    allergens = selectedAllergens.toList(),
                                    chefNotes = chefNotes,
                                    platingGuide = platingGuide
                                )
                            )

                            onSave(
                                item?.id ?: 0L,
                                name,
                                selectedCatId,
                                catName,
                                priceText.toDoubleOrNull() ?: 0.0,
                                parsedCost,
                                sku,
                                barcode,
                                soldBy,
                                inStockText.toDoubleOrNull() ?: 0.0,
                                lowThresholdText.toDoubleOrNull() ?: 5.0,
                                trackStock,
                                item?.colorHex ?: "#2E7D32",
                                recipeToSave
                            )
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (item == null) "צור מנה ועץ מוצר" else "שמור מנה ועץ מוצר (Save Item & BOM)")
                    }
                }
            }
        }
    }
}

@Composable
fun StockAdjustmentDialog(
    item: ItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (diff: Double, reason: String) -> Unit
) {
    var diffText by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("Receive Items") }
    val reasons = listOf("Receive Items", "Inventory Count", "Damage / Loss")
    val parsedDiff = diffText.toDoubleOrNull() ?: 0.0
    val newStock = (item.inStock + parsedDiff).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (selectedReason == "Receive Items") Icons.Default.LocalShipping else Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedReason == "Receive Items") "Receive Supply: ${item.name}" else "Adjust Stock: ${item.name}",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (item.soldBy == SoldBy.WEIGHT) String.format(Locale.US, "%.2f kg", item.inStock) else item.inStock.toInt().toString(),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (diffText.isNotBlank() && diffText.toDoubleOrNull() != null) {
                            Text("➔", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Column(horizontalAlignment = Alignment.End) {
                                Text("New Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (item.soldBy == SoldBy.WEIGHT) String.format(Locale.US, "%.2f kg", newStock) else newStock.toInt().toString(),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Text("Reason:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                reasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                    ) {
                        RadioButton(selected = selectedReason == r, onClick = { selectedReason = r })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (r == "Receive Items") "Receive Items / Supply (קבלת אספקה)" else r)
                    }
                }

                OutlinedTextField(
                    value = diffText,
                    onValueChange = { diffText = it },
                    label = { Text(if (selectedReason == "Receive Items") "Supply Quantity (+)" else "Adjustment Quantity (+ or -)") },
                    placeholder = { Text("e.g. 10, 50, 100") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (selectedReason == "Receive Items") {
                    Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 25, 50, 100).forEach { qty ->
                            FilledTonalButton(
                                onClick = {
                                    val current = diffText.toDoubleOrNull() ?: 0.0
                                    diffText = (current + qty).toInt().toString()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("+$qty", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val diff = diffText.toDoubleOrNull() ?: 0.0
                    onConfirm(diff, selectedReason)
                },
                enabled = diffText.toDoubleOrNull() != null && (diffText.toDoubleOrNull() ?: 0.0) != 0.0
            ) {
                Text(if (selectedReason == "Receive Items") "Add Supply" else "Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Loyalty Customer", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name*") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Customer Notes") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, email, note) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
