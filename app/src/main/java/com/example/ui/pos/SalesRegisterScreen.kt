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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import com.example.ui.theme.*
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
    var showOcrDialog by remember { mutableStateOf(false) }
    var showKitchenScreen by remember { mutableStateOf(false) }
    var selectedDishForRecipeSheet by remember { mutableStateOf<ItemEntity?>(null) }

    LaunchedEffect(Unit) {
        com.example.util.RecipeManager.initialize(context)
    }

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
                val strings = com.example.util.LocalAppStrings.current

                Surface(
                    color = PosWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    shadowElevation = 1.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PosTokens.TopBarHeight)
                            .padding(horizontal = PosTokens.TopBarPaddingHorizontal),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Drawer button + Store identity
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosSlate100,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable(onClick = onOpenDrawer)
                                    .testTag("drawer_menu_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Menu",
                                        tint = PosSlate700,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Store brand box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosSky50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosSky100),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = PosSky600,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.clickable { viewModel.setOpenTicketsDialog(true) }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {

                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = PosSky100,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSky200)
                                    ) {
                                        Text(
                                            text = "Punto de Venta (POS)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PosSky700,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "• ${uiState.cashierName} (Cajero) (CASHIER)",
                                    fontSize = 10.sp,
                                    color = PosSlate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Center: Search input
                        if (isWideScreen) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PosSlate100,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                                modifier = Modifier
                                    .widthIn(min = 220.dp, max = 340.dp)
                                    .height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = PosSlate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = { viewModel.setSearchQuery(it) },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 12.sp,
                                            color = PosSlate800,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("appbar_search_input"),
                                        decorationBox = { innerTextField ->
                                            if (uiState.searchQuery.isEmpty()) {
                                                Text(
                                                    "Buscar por nombre, SKU o código...",
                                                    fontSize = 11.sp,
                                                    color = PosSlate400
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = PosSlate400,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { viewModel.setSearchQuery("") }
                                        )
                                    }
                                }
                            }
                        }

                        // Right: Action buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Camera Scanner
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosSlate100,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clickable { viewModel.setBarcodeDialog(true) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Escáner", tint = PosSky600, modifier = Modifier.size(16.dp))
                                    if (isWideScreen) {
                                        Text("Escáner", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PosSlate700)
                                    }
                                }
                            }

                            // AI Menu Scanner Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clickable { showOcrDialog = true },
                                shadowElevation = 2.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Brush.horizontalGradient(listOf(PosSky600, PosIndigo600)))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PosAmber200, modifier = Modifier.size(16.dp))
                                        if (isWideScreen) {
                                            Text("Escáner IA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // Paused Orders (Open Tickets)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosSlate100,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clickable { viewModel.setOpenTicketsDialog(true) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = PosAmber500, modifier = Modifier.size(16.dp))
                                    if (isWideScreen) {
                                        Text("Pausados", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PosSlate700)
                                    }
                                }
                            }

                            // KDS Shortcut
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosAmber50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosAmber200),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clickable { showKitchenScreen = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.OutdoorGrill, contentDescription = null, tint = PosAmber600, modifier = Modifier.size(16.dp))
                                    if (isWideScreen) {
                                        Text("KDS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PosAmber800)
                                    }
                                }
                            }

                            // Cloud Sync status
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isOnline) PosEmerald50 else PosRed50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isOnline) PosEmerald200 else PosRed200),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clickable {
                                        syncManager.triggerSync()
                                        viewModel.setSyncStatusDialog(true)
                                    }
                                    .testTag("sync_status_button")
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                                        contentDescription = "Sync",
                                        tint = if (isOnline) PosEmerald600 else PosRed600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Lock terminal
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = PosRed50,
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosRed200),
                                modifier = Modifier
                                    .height(36.dp)
                                    .clickable { viewModel.setShiftDialog(true) }
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Bloquear Terminal",
                                        tint = PosRed600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // More menu
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PosSlate100,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clickable { showMoreMenu = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = PosSlate700, modifier = Modifier.size(18.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (uiState.selectedCustomer != null) "Cliente: ${uiState.selectedCustomer?.name}" else "Asignar Cliente") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PosSky600) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setCustomerSelectDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (uiState.isGridLayout) "Vista Lista" else "Vista Cuadrícula") },
                                        leadingIcon = { Icon(if (uiState.isGridLayout) Icons.AutoMirrored.Filled.ListAlt else Icons.Default.GridView, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.toggleLayoutMode()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Gestión de Turnos (Shifts)") },
                                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setShiftDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ajustes de Impresora") },
                                        leadingIcon = { Icon(Icons.Default.Print, contentDescription = null, tint = PosSky600) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setPrinterSettingsDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Stock Bajo (${lowStockProducts.size})") },
                                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = PosAmber600) },
                                        onClick = {
                                            showMoreMenu = false
                                            viewModel.setLowStockDialog(true)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Abrir Back Office Online") },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = PosSky600) },
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
                        }
                    }
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
            ) {
                // Item Catalog Pane
                Column(
                    modifier = Modifier
                        .weight(if (isWideScreen) 0.62f else 1f)
                        .fillMaxHeight()
                        .background(PosSlate100)
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

                    // Mobile Search Bar (Only shown on narrow screens where header search is hidden)
                    if (!isWideScreen) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PosSlate100,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .padding(horizontal = 14.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = PosSlate400,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BasicTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontSize = 12.sp,
                                        color = PosSlate800,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("appbar_search_input_mobile"),
                                    decorationBox = { innerTextField ->
                                        if (uiState.searchQuery.isEmpty()) {
                                            Text(
                                                "Buscar por nombre, SKU o código...",
                                                fontSize = 11.sp,
                                                color = PosSlate400
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (uiState.searchQuery.isNotEmpty()) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = PosSlate400,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.setSearchQuery("") }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
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
                                    onClick = { viewModel.onItemClick(item) },
                                    onRecipeClick = { selectedDishForRecipeSheet = item }
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
                                    onClick = { viewModel.onItemClick(item) },
                                    onRecipeClick = { selectedDishForRecipeSheet = item }
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
                            onDiningOptionChange = { viewModel.setDiningOption(it) },
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
                    onDiningOptionChange = { viewModel.setDiningOption(it) },
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

    // OCR Scanner Dialog (PaddleOCR)
    if (showOcrDialog) {
        com.example.ui.pos.components.OcrScannerDialog(
            onDismiss = { showOcrDialog = false },
            onAddItemsToCart = { acceptedItems ->
                for (scanned in acceptedItems) {
                    val existing = allItems.find { it.name.equals(scanned.name, ignoreCase = true) }
                    if (existing != null) {
                        viewModel.onItemClick(existing)
                    } else {
                        val transientItem = com.example.data.model.ItemEntity(
                            id = System.currentTimeMillis() % 10000000L + (1..999).random(),
                            name = scanned.name,
                            categoryId = 1L,
                            categoryName = scanned.category,
                            price = scanned.price,
                            cost = scanned.price * 0.6,
                            sku = "OCR-${scanned.name.take(3).uppercase()}",
                            inStock = 50.0
                        )
                        viewModel.onItemClick(transientItem)
                    }
                }
            }
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

    // Dish Recipe & Kitchen Preparation Sheet (at POS order time)
    if (selectedDishForRecipeSheet != null) {
        val dish = selectedDishForRecipeSheet!!
        val recipe = remember(dish) {
            com.example.util.RecipeManager.getRecipeForItem(dish.id, dish.name, dish.categoryName)
        }
        com.example.ui.pos.components.DishRecipeSheet(
            recipe = recipe,
            onDismiss = { selectedDishForRecipeSheet = null }
        )
    }

    // Dedicated Kitchen Display Screen (KDS) Dialog
    if (showKitchenScreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showKitchenScreen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.ui.kitchen.KitchenDisplayScreen(
                    onNavigateBack = { showKitchenScreen = false }
                )
            }
        }
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
    Surface(
        color = PosWhite,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Todas las Categorías" chip
            item {
                val isAllSelected = !isKeypadSelected && !isFavoritesSelected && selectedCategoryId == null
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAllSelected) PosSky600 else PosSlate100,
                    border = if (!isAllSelected) androidx.compose.foundation.BorderStroke(1.dp, PosSlate200) else null,
                    shadowElevation = if (isAllSelected) 3.dp else 0.dp,
                    modifier = Modifier.clickable { onCategorySelect(null) }
                ) {
                    Text(
                        text = "Todas las Categorías",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isAllSelected) Color.White else PosSlate700,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            // "Favoritos" chip
            item {
                val isFav = !isKeypadSelected && isFavoritesSelected
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFav) PosAmber500 else PosAmber50,
                    border = if (!isFav) androidx.compose.foundation.BorderStroke(1.dp, PosAmber200) else null,
                    shadowElevation = if (isFav) 3.dp else 0.dp,
                    modifier = Modifier.clickable { onFavoritesSelect() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isFav) PosSlate950 else PosAmber500,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Favoritos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isFav) PosSlate950 else PosAmber800
                        )
                    }
                }
            }

            // Dynamic categories from Back Office / Room
            items(categories) { category ->
                val isSelected = !isKeypadSelected && !isFavoritesSelected && selectedCategoryId == category.id
                val chipColor = try {
                    Color(android.graphics.Color.parseColor(category.colorHex))
                } catch (e: Exception) {
                    PosSky600
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) chipColor else PosWhite,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, PosSlate200) else null,
                    shadowElevation = if (isSelected) 3.dp else 0.dp,
                    modifier = Modifier.clickable { onCategorySelect(category.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else PosSlate700
                        )
                    }
                }
            }

            // Keypad tab
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isKeypadSelected) PosSky600 else PosWhite,
                    border = if (!isKeypadSelected) androidx.compose.foundation.BorderStroke(1.dp, PosSlate200) else null,
                    shadowElevation = if (isKeypadSelected) 3.dp else 0.dp,
                    modifier = Modifier.clickable { onKeypadSelect() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Dialpad,
                            contentDescription = null,
                            tint = if (isKeypadSelected) Color.White else PosSlate700,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Teclado",
                            fontSize = 12.sp,
                            fontWeight = if (isKeypadSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isKeypadSelected) Color.White else PosSlate700
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemGridCard(
    item: ItemEntity,
    onClick: () -> Unit,
    onRecipeClick: (() -> Unit)? = null
) {
    val cardAccentColor = remember(item.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.colorHex))
        } catch (e: Exception) {
            PosSky600
        }
    }

    val isLowStock = item.trackStock && item.inStock <= item.lowStockThreshold && item.inStock > 0.0
    val isOutOfStock = item.trackStock && item.inStock <= 0.0
    val isFavorite = item.id in listOf(101L, 102L, 103L, 105L, 106L)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("item_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        color = PosWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200.copy(alpha = 0.9f)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image area (Back Office POS card layout)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(PosSlate100),
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
                                    color = PosSky600
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cardAccentColor.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.name.firstOrNull()?.toString()?.uppercase() ?: "",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(cardAccentColor.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.name.firstOrNull()?.toString()?.uppercase() ?: "",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Favorite Star Badge (Back Office top-start)
                if (isFavorite) {
                    Surface(
                        color = Color.White.copy(alpha = 0.92f),
                        shape = CircleShape,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = PosAmber500,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Quick Recipe & Preparation Button (Back Office overlay)
                if (onRecipeClick != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.92f),
                        shape = CircleShape,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .size(26.dp)
                            .clickable { onRecipeClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "מתכון והכנה",
                                tint = PosSky700,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Low Stock Tag (Back Office top-end red pill)
                if (isOutOfStock) {
                    Surface(
                        color = PosSlate950.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "AGOTADO",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else if (isLowStock) {
                    Surface(
                        color = PosRed600,
                        shape = RoundedCornerShape(100.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "STOCK BAJO (${item.inStock.toInt()})",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Card Body (Back Office text hierarchy)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Category tag uppercase
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.categoryName.uppercase(),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PosSky600,
                            letterSpacing = 0.6.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.modifierIds.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PosSky50,
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosSky100)
                        ) {
                            Text(
                                text = "Variantes",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosSky600,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Product Name
                Text(
                    text = item.name,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosSlate900,
                        lineHeight = 17.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 34.dp)
                )

                // Divider line
                HorizontalDivider(
                    color = PosSlate100,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Price and Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", item.price)}",
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = PosSlate900
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosSky50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSky100),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add to cart",
                                tint = PosSky700,
                                modifier = Modifier.size(16.dp)
                            )
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
    onClick: () -> Unit,
    onRecipeClick: (() -> Unit)? = null
) {
    val cardAccentColor = remember(item.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.colorHex))
        } catch (e: Exception) {
            PosSky600
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("item_row_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        color = PosWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200.copy(alpha = 0.8f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PosSlate100),
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
                                fontWeight = FontWeight.Black,
                                color = PosSky600,
                                fontSize = 20.sp
                            )
                        }
                    )
                } else {
                    Text(
                        text = item.name.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = PosSky600,
                        fontSize = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PosSlate900
                )
                Text(
                    "${item.categoryName} • ${if (item.trackStock) "Stock: ${item.inStock.toInt()}" else "Stock no controlado"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PosSlate500
                )
            }

            Text(
                "$${String.format(Locale.US, "%.2f", item.price)}${if (item.soldBy == SoldBy.WEIGHT) "/kg" else ""}",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = PosSlate900
            )

            if (onRecipeClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PosSky50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosSky100),
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onRecipeClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "מתכון ואופן הכנה",
                            tint = PosSky700,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
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
        color = PosWhite,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
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
                            Badge(containerColor = PosSky600, contentColor = Color.White) {
                                Text(uiState.itemCount.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosSky50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSky100),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = PosSky600, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${uiState.currentTicketName} • ${uiState.diningOption.label}",
                        style = TextStyle(fontSize = 11.sp, color = PosSlate500, fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Black, color = PosSlate900)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .height(46.dp)
                    .testTag("charge_button")
                    .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onCharge),
                shadowElevation = if (uiState.cartItems.isNotEmpty()) 4.dp else 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (uiState.cartItems.isNotEmpty())
                                Brush.horizontalGradient(listOf(PosSky600, PosIndigo600))
                            else
                                androidx.compose.ui.graphics.SolidColor(PosSlate200)
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = if (uiState.cartItems.isNotEmpty()) Color.White else PosSlate400,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Cobrar $${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = if (uiState.cartItems.isNotEmpty()) Color.White else PosSlate400
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TicketCartPanel(
    uiState: PosUiState,
    openTicketsCount: Int,
    onDiningOptionChange: (DiningOption) -> Unit = {},
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
            .background(PosWhite)
    ) {
        // Back Office Cart Header (slate-50 background, blue shopping bag, dining pills)
        Surface(
            color = PosSlate50,
            border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PosSky600,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = uiState.currentTicketName,
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, color = PosSlate900)
                        )
                        Text(
                            text = "${uiState.cartItems.size} items • Carrito Activo",
                            style = TextStyle(fontSize = 11.sp, color = PosSlate500, fontWeight = FontWeight.Medium)
                        )
                    }
                }

                // Dining Option Segmented Pills (Comer Aquí, Para Llevar, A Domicilio)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = PosSlate200,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(modifier = Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        DiningOption.values().forEach { opt ->
                            val isSelected = uiState.diningOption == opt
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PosWhite else Color.Transparent,
                                shadowElevation = if (isSelected) 1.dp else 0.dp,
                                modifier = Modifier.clickable { onDiningOptionChange(opt) }
                            ) {
                                Text(
                                    text = when (opt) {
                                        DiningOption.DINE_IN -> "Comer Aquí"
                                        DiningOption.TAKEOUT -> "Para Llevar"
                                        DiningOption.DELIVERY -> "A Domicilio"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) PosSlate900 else PosSlate600,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Customer Selection Bar (sky-50/50 with User icon)
        Surface(
            color = PosSky50.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.clickable { onOpenTicketsClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = PosSky600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (uiState.selectedCustomer != null) uiState.selectedCustomer.name else "Asignar Cliente (+)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosSky700
                    )
                }

                if (uiState.selectedCustomer != null) {
                    Text(
                        text = "★ ${String.format(Locale.US, "%.1f", uiState.selectedCustomer.pointsBalance)} pts",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PosAmber600
                    )
                }
            }
        }

        // Ticket Items List
        if (uiState.cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = PosSlate400.copy(alpha = 0.25f)
                    )
                    Text(
                        text = "El carrito está vacío. Seleccione un producto.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosSlate500,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Escáner con Cámara o elija del catálogo",
                        fontSize = 11.sp,
                        color = PosSlate400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.cartItems) { index, item ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PosSlate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200.copy(alpha = 0.8f)),
                        shadowElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.itemName,
                                        style = TextStyle(
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PosSlate900
                                        )
                                    )
                                    if (item.selectedModifiersJson.isNotBlank()) {
                                        Text(
                                            text = "• ${item.selectedModifiersJson}",
                                            style = TextStyle(fontSize = 10.sp, color = PosSlate500)
                                        )
                                    }
                                    if (item.comment.isNotBlank()) {
                                        Text(
                                            text = "* ${item.comment}",
                                            style = TextStyle(fontSize = 10.sp, color = PosAmber700, fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.lineTotal)}",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PosSlate900
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quantity Stepper and Delete button (Back Office layout)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Minus button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PosWhite,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onUpdateQuantity(index, item.quantity - 1.0) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = PosSlate700, modifier = Modifier.size(13.dp))
                                        }
                                    }

                                    Box(
                                        modifier = Modifier.widthIn(min = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.2f", item.quantity),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = PosSlate900
                                        )
                                    }

                                    // Plus button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = PosWhite,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clickable { onUpdateQuantity(index, item.quantity + 1.0) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase", tint = PosSlate700, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }

                                // Delete Trash Button
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PosRed50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosRed200),
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { onRemoveItem(index) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = PosRed600,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary Calculations & Action Buttons (Back Office Cart Footer)
        Surface(
            color = PosWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Subtotal
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 12.sp, color = PosSlate500)
                    Text("$${String.format(Locale.US, "%.2f", uiState.subtotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PosSlate800)
                }

                if (uiState.discountAmount > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Descuento ${if (uiState.ticketDiscountPercent > 0) "(${uiState.ticketDiscountPercent.toInt()}%)" else ""}",
                            fontSize = 12.sp,
                            color = PosEmerald600,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "-$${String.format(Locale.US, "%.2f", uiState.discountAmount)}",
                            fontSize = 12.sp,
                            color = PosEmerald600,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Impuestos (8.25%)", fontSize = 12.sp, color = PosSlate500)
                    Text("$${String.format(Locale.US, "%.2f", uiState.taxAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PosSlate800)
                }

                HorizontalDivider(color = PosSlate200, modifier = Modifier.padding(vertical = 2.dp))

                // Three Action Buttons Grid (Pausar Pedido, Descuento, Vaciar Carrito)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pausar Pedido (Save / Hold)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PosWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onSaveTicketClick)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = PosAmber500, modifier = Modifier.size(16.dp))
                            Text("Pausar Pedido", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosSlate700)
                        }
                    }

                    // Descuento (Discount)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PosWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onDiscountClick)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Percent, contentDescription = null, tint = PosEmerald600, modifier = Modifier.size(16.dp))
                            Text("Descuento", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosSlate700)
                        }
                    }

                    // Vaciar Carrito (Clear Cart)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PosWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosSlate200),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onClearCart)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = PosRed600, modifier = Modifier.size(16.dp))
                            Text("Vaciar Carrito", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PosRed600)
                        }
                    }
                }

                // Big Gradient CTA Checkout Button (Cobrar S/ 0.00 / $X.XX)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable(enabled = uiState.cartItems.isNotEmpty(), onClick = onChargeClick),
                    shadowElevation = if (uiState.cartItems.isNotEmpty()) 6.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (uiState.cartItems.isNotEmpty())
                                    Brush.horizontalGradient(listOf(PosSky600, PosIndigo600))
                                else
                                    androidx.compose.ui.graphics.SolidColor(PosSlate200)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = if (uiState.cartItems.isNotEmpty()) Color.White else PosSlate400,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Cobrar $${String.format(Locale.US, "%.2f", uiState.grandTotal)}",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (uiState.cartItems.isNotEmpty()) Color.White else PosSlate400
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductPlaceholderTile(name: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        color.copy(alpha = 0.85f),
                        color
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
