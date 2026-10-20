package com.example.ui.pos

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.*
import com.example.ui.pos.components.*
import com.example.ui.theme.PosDesignTokens
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

                Column(modifier = Modifier.fillMaxWidth().background(PosDesignTokens.SurfaceLight)) {
                    TopAppBar(
                        title = {
                            if (uiState.isSearchActive && !isWideScreen) {
                                TextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = {
                                        Text(
                                            "Search menu, barcode or SKU...",
                                            color = PosDesignTokens.TextMuted,
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = PosDesignTokens.TextSecondary)
                                            }
                                        }
                                    },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = PosDesignTokens.BackgroundLight,
                                        unfocusedContainerColor = PosDesignTokens.BackgroundLight,
                                        focusedTextColor = PosDesignTokens.TextPrimary,
                                        unfocusedTextColor = PosDesignTokens.TextPrimary,
                                        cursorColor = PosDesignTokens.BrandSky600,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("appbar_search_input")
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Store Brand Icon (Matching Back Office)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = PosDesignTokens.BrandSky50,
                                        border = BorderStroke(1.dp, PosDesignTokens.BrandSky100),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = PosDesignTokens.BrandSky600,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Ticket & Cashier Information
                                    Column(
                                        modifier = Modifier.clickable { viewModel.setOpenTicketsDialog(true) }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = uiState.currentTicketName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = PosDesignTokens.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = PosDesignTokens.BrandSky100,
                                                shape = RoundedCornerShape(999.dp)
                                            ) {
                                                Text(
                                                    text = "POS",
                                                    color = PosDesignTokens.BrandSky800,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 9.sp
                                                )
                                            }
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch Ticket",
                                                tint = PosDesignTokens.TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "${uiState.cashierName} • ${if (currentShift?.isOpen == true) "Turno Abierto" else "Turno Cerrado"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PosDesignTokens.TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(38.dp)
                                    .background(PosDesignTokens.BackgroundLight, RoundedCornerShape(10.dp))
                                    .testTag("drawer_menu_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PosDesignTokens.TextPrimary)
                            }
                        },
                        actions = {
                            // Integrated Wide Screen Search Input (Back Office Style)
                            if (isWideScreen) {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    placeholder = { Text("Search menu, barcode or SKU...", fontSize = 12.sp, color = PosDesignTokens.TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PosDesignTokens.TextMuted, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        if (uiState.searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = PosDesignTokens.TextSecondary, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = PosDesignTokens.BackgroundLight,
                                        unfocusedContainerColor = PosDesignTokens.BackgroundLight,
                                        focusedBorderColor = PosDesignTokens.BrandSky600,
                                        unfocusedBorderColor = PosDesignTokens.BorderLight,
                                        focusedTextColor = PosDesignTokens.TextPrimary,
                                        unfocusedTextColor = PosDesignTokens.TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(40.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                // Search Toggle on Compact Screens
                                IconButton(
                                    onClick = { viewModel.setSearchActive(!uiState.isSearchActive) },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(PosDesignTokens.BackgroundLight, RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = PosDesignTokens.TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Barcode Scanner Action Button (Back Office Treatment)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosDesignTokens.BackgroundLight,
                                border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                                modifier = Modifier
                                    .clickable { viewModel.setBarcodeDialog(true) }
                                    .height(38.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barcode", tint = PosDesignTokens.BrandSky600, modifier = Modifier.size(18.dp))
                                    if (isWideScreen) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Barcode", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            // AI Menu Scanner Action Button (Gradient Sparkles treatment)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosDesignTokens.BrandSky600,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .clickable { viewModel.setBarcodeDialog(true) }
                                    .height(38.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(PosDesignTokens.BrandGradient)
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Scanner", tint = Color(0xFFFFD54F), modifier = Modifier.size(16.dp))
                                        if (isWideScreen) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AI Scan", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            // Held Orders Badge (Amber)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosDesignTokens.BackgroundLight,
                                border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                                modifier = Modifier
                                    .clickable { viewModel.setOpenTicketsDialog(true) }
                                    .height(38.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (activeTickets.size > 0) {
                                                Badge(containerColor = PosDesignTokens.AccentAmber, contentColor = Color.Black) {
                                                    Text(activeTickets.size.toString(), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = "Held Orders", tint = PosDesignTokens.AccentAmberText, modifier = Modifier.size(18.dp))
                                    }
                                    if (isWideScreen) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Held", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            // Online / Sync Pill (Emerald)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isOnline) PosDesignTokens.AccentEmeraldBg else PosDesignTokens.AccentRedBg,
                                border = BorderStroke(1.dp, if (isOnline) PosDesignTokens.AccentEmeraldBorder else PosDesignTokens.AccentRedBorder),
                                modifier = Modifier
                                    .clickable {
                                        syncManager.triggerSync()
                                        viewModel.setSyncStatusDialog(true)
                                    }
                                    .height(38.dp)
                                    .testTag("sync_status_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = "Sync",
                                        tint = if (isOnline) PosDesignTokens.AccentEmeraldText else PosDesignTokens.AccentRedText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    if (pendingSyncCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            color = PosDesignTokens.AccentAmber,
                                            shape = RoundedCornerShape(999.dp)
                                        ) {
                                            Text(
                                                pendingSyncCount.toString(),
                                                color = Color.Black,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            // View Mode Toggle (Grid vs List)
                            IconButton(
                                onClick = { viewModel.toggleLayoutMode() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(PosDesignTokens.BackgroundLight, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    if (uiState.isGridLayout) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle View",
                                    tint = PosDesignTokens.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            // More Options Dropdown Menu
                            Box {
                                IconButton(
                                    onClick = { showMoreMenu = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(PosDesignTokens.BackgroundLight, RoundedCornerShape(10.dp))
                                ) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = PosDesignTokens.TextSecondary, modifier = Modifier.size(18.dp))
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (uiState.selectedCustomer != null) "Customer: ${uiState.selectedCustomer?.name}" else "Select Customer") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PosDesignTokens.BrandSky600) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setCustomerSelectDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Shift Management") },
                                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = PosDesignTokens.BrandSky600) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setShiftDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Thermal Printer Settings") },
                                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = PosDesignTokens.BrandSky600) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setPrinterSettingsDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Review Low Stock (${lowStockProducts.size})") },
                                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = PosDesignTokens.AccentAmber) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setLowStockDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open Online Back Office") },
                                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PosDesignTokens.BrandSky600) },
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
                            Spacer(modifier = Modifier.width(6.dp))
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = PosDesignTokens.SurfaceLight,
                            titleContentColor = PosDesignTokens.TextPrimary,
                            navigationIconContentColor = PosDesignTokens.TextPrimary,
                            actionIconContentColor = PosDesignTokens.TextPrimary
                        )
                    )
                    HorizontalDivider(color = PosDesignTokens.BorderLight)
                }
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
                    .background(PosDesignTokens.BackgroundLight)
            ) {
                // Item Catalog Pane
                Column(
                    modifier = Modifier
                        .weight(if (isWideScreen) 0.62f else 1f)
                        .fillMaxHeight()
                        .background(PosDesignTokens.BackgroundLight)
                ) {
                    // Category Chips Bar (Back Office Styling)
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
                            color = PosDesignTokens.AccentAmberBg,
                            border = BorderStroke(1.dp, PosDesignTokens.AccentAmberBorder),
                            shape = RoundedCornerShape(12.dp),
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
                                        tint = PosDesignTokens.AccentAmberText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Low Stock Warning (${lowStockProducts.size} items)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PosDesignTokens.AccentAmberText
                                        )
                                        val firstTwo = lowStockProducts.take(2).joinToString(", ") { "${it.name} (${it.stockLevel} left)" }
                                        Text(
                                            text = "$firstTwo${if (lowStockProducts.size > 2) "..." else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = PosDesignTokens.TextSecondary,
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
                                        Text("Review", fontWeight = FontWeight.Bold, color = PosDesignTokens.AccentAmberText, fontSize = 12.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.dismissLowStockBanner() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Dismiss banner",
                                            modifier = Modifier.size(16.dp),
                                            tint = PosDesignTokens.TextSecondary
                                        )
                                    }
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
                                    CircularProgressIndicator(color = PosDesignTokens.BrandSky600)
                                    Text(
                                        text = "Connecting to Back Office & loading catalog...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PosDesignTokens.TextSecondary
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
                                        tint = PosDesignTokens.AccentRed,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Unable to connect to Back Office",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PosDesignTokens.AccentRed
                                    )
                                    Text(
                                        text = "Check network connection or tap retry to re-sync.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PosDesignTokens.TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { syncManager.triggerSync() },
                                        colors = ButtonDefaults.buttonColors(containerColor = PosDesignTokens.BrandSky600)
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
                                    Icon(Icons.Default.Search, contentDescription = null, tint = PosDesignTokens.TextMuted, modifier = Modifier.size(40.dp))
                                    Text(
                                        text = "No items match '${uiState.searchQuery}'",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PosDesignTokens.TextSecondary,
                                        fontWeight = FontWeight.Bold
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
                                        tint = PosDesignTokens.AccentAmber,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = "No favorite items yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PosDesignTokens.TextSecondary
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
                                        color = PosDesignTokens.TextSecondary
                                    )
                                }
                            }
                        }
                    } else if (uiState.isGridLayout) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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

                // Wide Screen Ticket Pane (Back Office Cart Column)
                if (isWideScreen) {
                    Box(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight()
                            .border(width = 1.dp, color = PosDesignTokens.BorderLight)
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
                            onChargeClick = { viewModel.setCheckoutSheet(true) },
                            onDiningOptionChange = { viewModel.setDiningOption(it) }
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
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = PosDesignTokens.SurfaceLight
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
                    },
                    onDiningOptionChange = { viewModel.setDiningOption(it) }
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
                Button(
                    onClick = {
                        val pct = discountInput.toDoubleOrNull() ?: 0.0
                        viewModel.applyTicketDiscount(pct)
                        showDiscountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosDesignTokens.BrandSky600)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// CATEGORY FILTER BAR (1:1 with Web Back Office Chip Buttons)
// -----------------------------------------------------------------------------------------
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
    Surface(
        color = PosDesignTokens.SurfaceLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Items chip
                item {
                    val isAllSelected = !isKeypadSelected && !isFavoritesSelected && selectedCategoryId == null
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAllSelected) PosDesignTokens.BrandSky600 else PosDesignTokens.BackgroundLight,
                        shadowElevation = if (isAllSelected) 3.dp else 0.dp,
                        modifier = Modifier
                            .clickable { onCategorySelect(null) }
                            .height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "All Items",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = if (isAllSelected) Color.White else PosDesignTokens.TextSecondary
                            )
                        }
                    }
                }

                // Favorites chip
                item {
                    val isFavSelected = !isKeypadSelected && isFavoritesSelected
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFavSelected) PosDesignTokens.AccentAmber else PosDesignTokens.AccentAmberBg,
                        border = if (!isFavSelected) BorderStroke(1.dp, PosDesignTokens.AccentAmberBorder) else null,
                        shadowElevation = if (isFavSelected) 3.dp else 0.dp,
                        modifier = Modifier
                            .clickable { onFavoritesSelect() }
                            .height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isFavSelected) Color.Black else PosDesignTokens.AccentAmberText
                            )
                            Text(
                                text = "Favorites",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isFavSelected) Color.Black else PosDesignTokens.AccentAmberText
                            )
                        }
                    }
                }

                // Dynamic Categories from Back Office
                items(categories) { category ->
                    val isSelected = !isKeypadSelected && !isFavoritesSelected && selectedCategoryId == category.id
                    val chipColor = try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (e: Exception) {
                        PosDesignTokens.BrandSky600
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) chipColor else PosDesignTokens.SurfaceLight,
                        border = if (!isSelected) BorderStroke(1.dp, PosDesignTokens.BorderLight) else null,
                        shadowElevation = if (isSelected) 3.dp else 0.dp,
                        modifier = Modifier
                            .clickable { onCategorySelect(category.id) }
                            .height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            if (!isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(chipColor)
                                )
                            }
                            Text(
                                text = category.name,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else PosDesignTokens.TextPrimary
                            )
                        }
                    }
                }

                // Keypad tab
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isKeypadSelected) PosDesignTokens.BrandSky600 else PosDesignTokens.BackgroundLight,
                        shadowElevation = if (isKeypadSelected) 3.dp else 0.dp,
                        modifier = Modifier
                            .clickable { onKeypadSelect() }
                            .height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Default.Dialpad,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isKeypadSelected) Color.White else PosDesignTokens.TextSecondary
                            )
                            Text(
                                text = "Keypad",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isKeypadSelected) Color.White else PosDesignTokens.TextSecondary
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = PosDesignTokens.BorderLight)
        }
    }
}

// -----------------------------------------------------------------------------------------
// PRODUCT GRID CARD (1:1 with Web Back Office Catalog Card)
// -----------------------------------------------------------------------------------------
@Composable
fun ItemGridCard(
    item: ItemEntity,
    onClick: () -> Unit
) {
    val cardAccentColor = remember(item.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.colorHex))
        } catch (e: Exception) {
            PosDesignTokens.BrandSky600
        }
    }

    val isLowStock = item.trackStock && item.inStock <= item.lowStockThreshold && item.inStock > 0.0
    val isOutOfStock = item.trackStock && item.inStock <= 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PosDesignTokens.SurfaceLight),
        border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image / Monogram Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(cardAccentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = cardAccentColor)
                            }
                        },
                        error = {
                            ProductPlaceholderTile(name = item.name, color = cardAccentColor)
                        }
                    )
                } else {
                    ProductPlaceholderTile(name = item.name, color = cardAccentColor)
                }

                // Favorite Star Indicator (Top Start Frosted Circle)
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = PosDesignTokens.AccentAmber,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Low Stock / Out of Stock Badge (Top End Pill)
                if (isOutOfStock) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isLowStock) {
                    Surface(
                        color = PosDesignTokens.AccentRed,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "LOW (${item.inStock.toInt()})",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Text Info & Action Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                // Category Tag
                Text(
                    text = item.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PosDesignTokens.BrandSky600,
                    letterSpacing = 0.6.sp,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))

                // Product Name (2 Lines max)
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PosDesignTokens.TextPrimary,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Price and Add Button Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.price)}${if (item.soldBy == SoldBy.WEIGHT) "/kg" else ""}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = PosDesignTokens.TextPrimary
                    )

                    // Dedicated Plus Button (Back Office 32x32 rounded-xl)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosDesignTokens.BrandSky50,
                        border = BorderStroke(1.dp, PosDesignTokens.BrandSky100),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add to Cart",
                                tint = PosDesignTokens.BrandSky700,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// PRODUCT LIST ROW
// -----------------------------------------------------------------------------------------
@Composable
fun ItemListRow(
    item: ItemEntity,
    onClick: () -> Unit
) {
    val cardAccentColor = remember(item.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.colorHex))
        } catch (e: Exception) {
            PosDesignTokens.BrandSky600
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("item_row_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PosDesignTokens.SurfaceLight),
        border = BorderStroke(1.dp, PosDesignTokens.BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardAccentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.name.take(1).uppercase(),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = PosDesignTokens.BrandSky600,
                    fontSize = 9.sp
                )
                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = PosDesignTokens.TextPrimary)
                Text(
                    "${if (item.trackStock) "Stock: ${item.inStock.toInt()}" else "Stock not tracked"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = PosDesignTokens.TextSecondary
                )
            }
            Text(
                "$${String.format(Locale.US, "%.2f", item.price)}${if (item.soldBy == SoldBy.WEIGHT) "/kg" else ""}",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                color = PosDesignTokens.TextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PosDesignTokens.BrandSky50,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = PosDesignTokens.BrandSky700, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// LOYVERSE KEYPAD VIEW
// -----------------------------------------------------------------------------------------
@Composable
fun LoyverseKeypadView(
    inputAmountCents: String,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onAddToTicket: () -> Unit
) {
    val cents = inputAmountCents.toLongOrNull() ?: 0L
    val displayDollars = cents / 100.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = PosDesignTokens.SurfaceLight,
            border = BorderStroke(1.dp, PosDesignTokens.BorderLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Custom Amount", style = MaterialTheme.typography.labelMedium, color = PosDesignTokens.TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$${String.format(Locale.US, "%.2f", displayDollars)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = PosDesignTokens.BrandSky600
                )
            }
        }

        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9),
            listOf(-1, 0, -2) // -1 is clear, -2 is backspace
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { digit ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                when (digit) {
                                    -1 -> onClear()
                                    -2 -> onBackspace()
                                    else -> onDigit(digit.toString())
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = PosDesignTokens.SurfaceLight,
                        border = BorderStroke(1.dp, PosDesignTokens.BorderLight)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            when (digit) {
                                -1 -> Text("C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PosDesignTokens.AccentRed)
                                -2 -> Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = PosDesignTokens.TextSecondary)
                                else -> Text(digit.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onAddToTicket,
            enabled = cents > 0L,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PosDesignTokens.BrandSky600)
        ) {
            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add to Ticket", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// -----------------------------------------------------------------------------------------
// COMPACT CART BOTTOM BAR (Mobile Phone Mode)
// -----------------------------------------------------------------------------------------
@Composable
fun CompactCartBottomBar(
    uiState: PosUiState,
    onViewCart: () -> Unit,
    onCharge: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = PosDesignTokens.SurfaceLight,
        border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            Badge(containerColor = PosDesignTokens.AccentRed, contentColor = Color.White) {
                                Text(uiState.itemCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosDesignTokens.BrandSky50,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = PosDesignTokens.BrandSky600, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Ticket (${uiState.diningOption.label})", style = MaterialTheme.typography.labelSmall, color = PosDesignTokens.TextSecondary)
                    Text(
                        "$${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = PosDesignTokens.TextPrimary
                    )
                }
            }

            // Primary Gradient Charge Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = if (uiState.cartItems.isNotEmpty()) 4.dp else 0.dp,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("charge_button")
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (uiState.cartItems.isNotEmpty()) PosDesignTokens.BrandGradient else Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                        )
                        .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onCharge)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "CHARGE $${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// TICKET CART PANEL (1:1 with Web Back Office Cart Column)
// -----------------------------------------------------------------------------------------
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
    onChargeClick: () -> Unit,
    onDiningOptionChange: (DiningOption) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PosDesignTokens.SurfaceLight)
    ) {
        // 1. Cart Header
        Surface(
            color = PosDesignTokens.SurfaceLightSubtle,
            border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PosDesignTokens.BrandSky600,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Column {
                            Text(
                                text = uiState.currentTicketName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = PosDesignTokens.TextPrimary
                            )
                            Text(
                                text = "${uiState.itemCount} items in ticket",
                                style = MaterialTheme.typography.labelSmall,
                                color = PosDesignTokens.TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Open tickets button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PosDesignTokens.BackgroundLight,
                            border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                            modifier = Modifier
                                .clickable(onClick = onOpenTicketsClick)
                                .height(30.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = PosDesignTokens.TextSecondary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tickets ($openTicketsCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                            }
                        }

                        if (uiState.cartItems.isNotEmpty()) {
                            IconButton(onClick = onClearCart, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Ticket", tint = PosDesignTokens.AccentRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Dining Option Segmented Pills (Matching Back Office)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PosDesignTokens.BackgroundLight, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DiningOption.values().forEach { opt ->
                        val isSelected = uiState.diningOption == opt
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color.White else Color.Transparent,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onDiningOptionChange(opt) }
                                .height(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = opt.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) PosDesignTokens.TextPrimary else PosDesignTokens.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Customer Loyalty Row
        Surface(
            color = PosDesignTokens.BrandSky50.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PosDesignTokens.BrandSky600, modifier = Modifier.size(16.dp))
                    Text(
                        text = uiState.selectedCustomer?.name ?: "Customer (+)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PosDesignTokens.BrandSky700
                    )
                }

                if (uiState.selectedCustomer != null) {
                    Text(
                        text = "★ ${uiState.selectedCustomer.pointsBalance.toInt()} pts",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = PosDesignTokens.AccentAmberText
                    )
                }
            }
        }

        // 3. Cart Items List
        if (uiState.cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = PosDesignTokens.TextMuted.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Empty ticket",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PosDesignTokens.TextSecondary
                    )
                    Text(
                        text = "Tap products from the catalog to add",
                        style = MaterialTheme.typography.labelSmall,
                        color = PosDesignTokens.TextMuted
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
                        colors = CardDefaults.cardColors(containerColor = PosDesignTokens.SurfaceLightSubtle),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PosDesignTokens.BorderLight)
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
                                        fontWeight = FontWeight.Bold,
                                        color = PosDesignTokens.TextPrimary
                                    )
                                    if (item.selectedModifiersJson.isNotBlank()) {
                                        Text(
                                            text = item.selectedModifiersJson,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PosDesignTokens.TextSecondary
                                        )
                                    }
                                    if (item.comment.isNotBlank()) {
                                        Text(
                                            text = "* ${item.comment}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PosDesignTokens.AccentAmberText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = PosDesignTokens.TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quantity controls & delete button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PosDesignTokens.SurfaceLight,
                                        border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onUpdateQuantity(index, item.quantity - 1.0) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp), tint = PosDesignTokens.TextPrimary)
                                        }
                                    }
                                    Text(
                                        text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.2f", item.quantity),
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PosDesignTokens.TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PosDesignTokens.SurfaceLight,
                                        border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onUpdateQuantity(index, item.quantity + 1.0) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp), tint = PosDesignTokens.TextPrimary)
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveItem(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = PosDesignTokens.TextMuted, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Summary Calculations & Action Buttons
        Surface(
            color = PosDesignTokens.SurfaceLightSubtle,
            border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = PosDesignTokens.TextSecondary)
                    Text("$${String.format(Locale.US, "%.2f", uiState.subtotal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                }

                if (uiState.discountAmount > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Discounts ${if (uiState.ticketDiscountPercent > 0) "(${uiState.ticketDiscountPercent.toInt()}%)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosDesignTokens.AccentRed
                        )
                        Text(
                            "-$${String.format(Locale.US, "%.2f", uiState.discountAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PosDesignTokens.AccentRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Sales Tax (8.25%)", style = MaterialTheme.typography.bodySmall, color = PosDesignTokens.TextSecondary)
                    Text("$${String.format(Locale.US, "%.2f", uiState.taxAmount)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                }

                HorizontalDivider(color = PosDesignTokens.BorderLight, modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = PosDesignTokens.TextPrimary)
                    Text(
                        "$${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = PosDesignTokens.BrandSky600
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 3-Column Action Buttons: Park, Discount, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosDesignTokens.SurfaceLight,
                        border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onSaveTicketClick)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp), tint = PosDesignTokens.AccentAmber)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Park", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosDesignTokens.SurfaceLight,
                        border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onDiscountClick)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp), tint = PosDesignTokens.AccentEmerald)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PosDesignTokens.TextPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosDesignTokens.SurfaceLight,
                        border = BorderStroke(1.dp, PosDesignTokens.BorderLight),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onClearCart)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = PosDesignTokens.AccentRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PosDesignTokens.AccentRed)
                        }
                    }
                }

                // Primary Gradient Charge Button (Matching Back Office)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = if (uiState.cartItems.isNotEmpty()) 4.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (uiState.cartItems.isNotEmpty()) PosDesignTokens.BrandGradient else Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8)))
                            )
                            .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onChargeClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "CHARGE $${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// PRODUCT PLACEHOLDER TILE (Monogram Initial with Category Background, matching Web Back Office)
// -----------------------------------------------------------------------------------------
@Composable
fun ProductPlaceholderTile(name: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 32.sp
        )
    }
}
