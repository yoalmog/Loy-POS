package com.example.ui.backoffice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
            onSave = { id, name, catId, catName, price, cost, sku, barcode, soldBy, inStock, lowThreshold, track, color ->
                viewModel.saveItem(id, name, catId, catName, price, cost, sku, barcode, soldBy, inStock, lowThreshold, track, color)
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

@Composable
fun ItemEditorDialog(
    item: ItemEntity?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, catId: Long, catName: String, price: Double, cost: Double, sku: String, barcode: String, soldBy: SoldBy, inStock: Double, lowThreshold: Double, track: Boolean, color: String) -> Unit,
    onDelete: (ItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var selectedCatId by remember { mutableStateOf(item?.categoryId ?: (categories.firstOrNull()?.id ?: 1L)) }
    var priceText by remember { mutableStateOf(item?.price?.toString() ?: "5.00") }
    var costText by remember { mutableStateOf(item?.cost?.toString() ?: "1.50") }
    var sku by remember { mutableStateOf(item?.sku ?: "2001") }
    var barcode by remember { mutableStateOf(item?.barcode ?: "200001001000") }
    var soldBy by remember { mutableStateOf(item?.soldBy ?: SoldBy.EACH) }
    var inStockText by remember { mutableStateOf(item?.inStock?.toString() ?: "20") }
    var lowThresholdText by remember { mutableStateOf(item?.lowStockThreshold?.toString() ?: "5") }
    var trackStock by remember { mutableStateOf(item?.trackStock ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Create New Item" else "Edit Item", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    Text("Category:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price ($)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("Cost ($)") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode") }, modifier = Modifier.weight(1f))
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Sold By: ", style = MaterialTheme.typography.bodySmall)
                        RadioButton(selected = soldBy == SoldBy.EACH, onClick = { soldBy = SoldBy.EACH })
                        Text("Each", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        RadioButton(selected = soldBy == SoldBy.WEIGHT, onClick = { soldBy = SoldBy.WEIGHT })
                        Text("Weight", fontSize = 12.sp)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = inStockText, onValueChange = { inStockText = it }, label = { Text("In Stock") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = lowThresholdText, onValueChange = { lowThresholdText = it }, label = { Text("Low Alert") }, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catName = categories.firstOrNull { it.id == selectedCatId }?.name ?: "General"
                    onSave(
                        item?.id ?: 0L,
                        name,
                        selectedCatId,
                        catName,
                        priceText.toDoubleOrNull() ?: 0.0,
                        costText.toDoubleOrNull() ?: 0.0,
                        sku,
                        barcode,
                        soldBy,
                        inStockText.toDoubleOrNull() ?: 0.0,
                        lowThresholdText.toDoubleOrNull() ?: 5.0,
                        trackStock,
                        item?.colorHex ?: "#2E7D32"
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
