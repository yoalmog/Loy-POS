package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.repository.PosRepository
import com.example.ui.backoffice.BackOfficeScreen
import com.example.ui.backoffice.BackOfficeTab
import com.example.ui.backoffice.BackOfficeViewModel
import com.example.ui.backoffice.BackOfficeViewModelFactory
import com.example.ui.pos.PosViewModel
import com.example.ui.pos.PosViewModelFactory
import com.example.ui.pos.SalesRegisterScreen
import com.example.ui.theme.LoyverseTheme
import kotlinx.coroutines.launch

enum class AppDestination {
    SALES_REGISTER,
    BACK_OFFICE,
    KITCHEN_DISPLAY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local network printer manager & settings
        com.example.util.NetworkPrinterManager.initialize(this)

        setContent {
            LoyverseTheme {
                LoyversePosApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyversePosApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { PosRepository(database) }

    val application = context.applicationContext as android.app.Application
    val posViewModel: PosViewModel = viewModel(factory = PosViewModelFactory(application, repository))
    val backOfficeViewModel: BackOfficeViewModel = viewModel(factory = BackOfficeViewModelFactory(repository))
    val posUiState by posViewModel.uiState.collectAsState()

    var currentScreen by remember { mutableStateOf(AppDestination.SALES_REGISTER) }
    var currentCashier by remember { mutableStateOf("Alex (Cashier)") }
    var showPinDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Handle Android system back press
    BackHandler(enabled = currentScreen != AppDestination.SALES_REGISTER || drawerState.isOpen) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else if (currentScreen != AppDestination.SALES_REGISTER) {
            currentScreen = AppDestination.SALES_REGISTER
        }
    }

    com.example.util.LocalizationProvider(language = posUiState.language) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTabletLandscape = maxWidth >= 960.dp

        if (isTabletLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Persistent Sidebar matching Back Office desktop/tablet view
                Surface(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight(),
                    color = com.example.ui.theme.PosSlate900
                ) {
                    AppSidebar(
                        currentScreen = currentScreen,
                        onNavigate = { currentScreen = it },
                        currentCashier = currentCashier,
                        onOpenReceipts = { posViewModel.setReceiptsDialog(true) },
                        onOpenShifts = { posViewModel.setShiftDialog(true) },
                        onOpenTickets = { posViewModel.setOpenTicketsDialog(true) },
                        onOpenBackup = { posViewModel.setDatabaseBackupDialog(true) },
                        onOpenPinDialog = { showPinDialog = true }
                    )
                }

                // Main screen area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (currentScreen) {
                        AppDestination.SALES_REGISTER -> {
                            SalesRegisterScreen(
                                viewModel = posViewModel,
                                onOpenDrawer = { /* Sidebar is permanently pinned on wide screen */ }
                            )
                        }
                        AppDestination.BACK_OFFICE -> {
                            BackOfficeScreen(
                                viewModel = backOfficeViewModel,
                                onNavigateToPos = { currentScreen = AppDestination.SALES_REGISTER }
                            )
                        }
                        AppDestination.KITCHEN_DISPLAY -> {
                            com.example.ui.kitchen.KitchenDisplayScreen(
                                onNavigateBack = { currentScreen = AppDestination.SALES_REGISTER }
                            )
                        }
                    }
                }
            }
        } else {
            // Compact mobile screen uses slide-out ModalNavigationDrawer
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = com.example.ui.theme.PosSlate900,
                        drawerContentColor = com.example.ui.theme.PosSlate300,
                        modifier = Modifier.width(310.dp)
                    ) {
                        AppSidebar(
                            currentScreen = currentScreen,
                            onNavigate = {
                                currentScreen = it
                                coroutineScope.launch { drawerState.close() }
                            },
                            currentCashier = currentCashier,
                            onOpenReceipts = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    posViewModel.setReceiptsDialog(true)
                                }
                            },
                            onOpenShifts = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    posViewModel.setShiftDialog(true)
                                }
                            },
                            onOpenTickets = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    posViewModel.setOpenTicketsDialog(true)
                                }
                            },
                            onOpenBackup = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    posViewModel.setDatabaseBackupDialog(true)
                                }
                            },
                            onOpenPinDialog = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    showPinDialog = true
                                }
                            }
                        )
                    }
                }
            ) {
                when (currentScreen) {
                    AppDestination.SALES_REGISTER -> {
                        SalesRegisterScreen(
                            viewModel = posViewModel,
                            onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                        )
                    }
                    AppDestination.BACK_OFFICE -> {
                        BackOfficeScreen(
                            viewModel = backOfficeViewModel,
                            onNavigateToPos = { currentScreen = AppDestination.SALES_REGISTER }
                        )
                    }
                    AppDestination.KITCHEN_DISPLAY -> {
                        com.example.ui.kitchen.KitchenDisplayScreen(
                            onNavigateBack = { currentScreen = AppDestination.SALES_REGISTER }
                        )
                    }
                }
            }
        }
    }
}

    // PIN Switch Dialog (Section 5.4 & 5.6 of Loyverse User Manual)
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        val cashiers = listOf("Alex (Cashier)", "Sarah (Manager)", "Marcus (Supervisor)")

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Switch Employee PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select cashier or enter 4-digit PIN:")
                    cashiers.forEach { c ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentCashier == c) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentCashier = c
                                    showPinDialog = false
                                }
                        ) {
                            Text(c, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AppSidebar(
    currentScreen: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    currentCashier: String,
    onOpenReceipts: () -> Unit,
    onOpenShifts: () -> Unit,
    onOpenTickets: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenPinDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.example.util.AppStrings.get(com.example.util.LocalAppLanguage.current)
    val drawerColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = com.example.ui.theme.PosSky600,
        unselectedContainerColor = Color.Transparent,
        selectedTextColor = Color.White,
        unselectedTextColor = com.example.ui.theme.PosSlate300,
        selectedIconColor = Color.White,
        unselectedIconColor = com.example.ui.theme.PosSlate400
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(com.example.ui.theme.PosSlate900)
    ) {
        // Sidebar Brand Header (Back Office Loy-POS)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = com.example.ui.theme.PosSky600,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "AuraPOS Bistro & Bar",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Portal de Control y Gestión",
                                style = MaterialTheme.typography.labelSmall,
                                color = com.example.ui.theme.PosSky400,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Switch employee lock button
                    IconButton(onClick = onOpenPinDialog) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Switch Employee PIN",
                            tint = com.example.ui.theme.PosSlate400
                        )
                    }
                }

                // POS Active Mode Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = com.example.ui.theme.PosSky600.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PosSky500.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.PosEmerald400)
                            )
                            Text(
                                text = "Punto de Venta (POS)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = com.example.ui.theme.PosSky600
                        ) {
                            Text(
                                text = "POS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Cloud status bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = com.example.ui.theme.PosSlate950.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PosSlate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = com.example.ui.theme.PosEmerald400,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Nube Conectada",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = com.example.ui.theme.PosSlate300
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = com.example.ui.theme.PosEmerald500.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PosEmerald500.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Online",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.PosEmerald400,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Navigation Items List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                label = { Text(strings.salesRegister, fontWeight = FontWeight.Bold) },
                selected = currentScreen == AppDestination.SALES_REGISTER,
                colors = drawerColors,
                onClick = { onNavigate(AppDestination.SALES_REGISTER) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                label = { Text(strings.receiptsArchive) },
                selected = false,
                colors = drawerColors,
                onClick = onOpenReceipts,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                label = { Text(strings.shiftManagement) },
                selected = false,
                colors = drawerColors,
                onClick = onOpenShifts,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                label = { Text(strings.openTickets) },
                selected = false,
                colors = drawerColors,
                onClick = onOpenTickets,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.OutdoorGrill, contentDescription = null, tint = com.example.ui.theme.PosAmber500) },
                label = { Text(strings.kds, fontWeight = FontWeight.Bold) },
                selected = currentScreen == AppDestination.KITCHEN_DISPLAY,
                colors = drawerColors,
                onClick = { onNavigate(AppDestination.KITCHEN_DISPLAY) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp), color = com.example.ui.theme.PosSlate800)

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                label = { Text(strings.backOffice, fontWeight = FontWeight.Bold) },
                selected = currentScreen == AppDestination.BACK_OFFICE,
                colors = drawerColors,
                onClick = { onNavigate(AppDestination.BACK_OFFICE) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                label = { Text(strings.backupSync, fontWeight = FontWeight.SemiBold) },
                selected = false,
                colors = drawerColors,
                onClick = onOpenBackup,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        // Staff & Lock Footer (Back Office sidebar footer)
        Surface(
            color = com.example.ui.theme.PosSlate900,
            border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PosSlate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.PosSky600),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentCashier.firstOrNull()?.toString()?.uppercase() ?: "C",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentCashier,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Cajero (CASHIER)",
                            fontSize = 10.sp,
                            color = com.example.ui.theme.PosSlate400
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = com.example.ui.theme.PosSlate800,
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable(onClick = onOpenPinDialog)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = com.example.ui.theme.PosAmber400,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bloquear",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.example.ui.theme.PosSlate300
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = com.example.ui.theme.PosRed600.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, com.example.ui.theme.PosRed600.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(34.dp)
                            .clickable(onClick = onOpenShifts)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ExitToApp,
                                contentDescription = "Cerrar Turno",
                                tint = com.example.ui.theme.PosRed400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
