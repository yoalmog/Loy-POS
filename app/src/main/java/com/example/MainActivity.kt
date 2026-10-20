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
import com.example.ui.theme.PosDesignTokens
import kotlinx.coroutines.launch

enum class AppDestination {
    SALES_REGISTER,
    BACK_OFFICE
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

    val posViewModel: PosViewModel = viewModel(factory = PosViewModelFactory(repository))
    val backOfficeViewModel: BackOfficeViewModel = viewModel(factory = BackOfficeViewModelFactory(repository))

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = PosDesignTokens.NavDarkBg,
                drawerContentColor = PosDesignTokens.NavTextSecondary,
                modifier = Modifier.width(300.dp)
            ) {
                // 1. Brand Header (Matching Back Office Dark Navy Sidebar)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PosDesignTokens.NavDarkBg)
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = PosDesignTokens.BrandSky600,
                                    shadowElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Bistro & Grill #1",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Online Cloud Portal",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = PosDesignTokens.BrandSky500
                                    )
                                }
                            }

                            // Switch employee lock button
                            IconButton(
                                onClick = { showPinDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PosDesignTokens.NavDarkCard, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Switch Employee PIN",
                                    tint = PosDesignTokens.AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Active POS Banner (Matching Back Office Sidebar Banner)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PosDesignTokens.BrandSky600.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosDesignTokens.BrandSky600.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PosDesignTokens.AccentEmerald)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sales Register",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = PosDesignTokens.BrandSky600
                        ) {
                            Text(
                                "POS",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = PosDesignTokens.NavBorder, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                val navColors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = PosDesignTokens.BrandSky600,
                    unselectedContainerColor = Color.Transparent,
                    selectedIconColor = Color.White,
                    unselectedIconColor = PosDesignTokens.NavTextMuted,
                    selectedTextColor = Color.White,
                    unselectedTextColor = PosDesignTokens.NavTextSecondary
                )

                // 3. Navigation Items (Matching Back Office nav)
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                    label = { Text(stringResource(R.string.sales_register), fontWeight = FontWeight.Bold) },
                    selected = currentScreen == AppDestination.SALES_REGISTER,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        currentScreen = AppDestination.SALES_REGISTER
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    label = { Text(stringResource(R.string.receipts_archive), fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setReceiptsDialog(true)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text(stringResource(R.string.shift_management), fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setShiftDialog(true)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                    label = { Text(stringResource(R.string.open_tickets), fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setOpenTicketsDialog(true)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                HorizontalDivider(color = PosDesignTokens.NavBorder, modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text(stringResource(R.string.back_office), fontWeight = FontWeight.Bold) },
                    selected = currentScreen == AppDestination.BACK_OFFICE,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        currentScreen = AppDestination.BACK_OFFICE
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text(stringResource(R.string.customer_loyalty), fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        currentScreen = AppDestination.BACK_OFFICE
                        backOfficeViewModel.selectTab(BackOfficeTab.CUSTOMERS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text(stringResource(R.string.inventory_adjustments), fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        currentScreen = AppDestination.BACK_OFFICE
                        backOfficeViewModel.selectTab(BackOfficeTab.ITEMS_INVENTORY)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                    label = { Text(stringResource(R.string.backup_sync), fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    colors = navColors,
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setDatabaseBackupDialog(true)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Footer: Cashier info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PosDesignTokens.NavDarkDeep)
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PosDesignTokens.BrandSky600,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    currentCashier.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Column {
                            Text(
                                currentCashier,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Cashier on Duty",
                                style = MaterialTheme.typography.labelSmall,
                                color = PosDesignTokens.NavTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
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
        }
    }

    // PIN Switch Dialog
    if (showPinDialog) {
        val cashiers = listOf("Alex (Cashier)", "Sarah (Manager)", "Marcus (Supervisor)")

        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Switch Employee PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select cashier or enter 4-digit PIN:")
                    cashiers.forEach { c ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentCashier == c) PosDesignTokens.BrandSky100 else PosDesignTokens.BackgroundLight,
                            border = if (currentCashier == c) androidx.compose.foundation.BorderStroke(1.dp, PosDesignTokens.BrandSky600) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentCashier = c
                                    showPinDialog = false
                                }
                        ) {
                            Text(
                                c,
                                modifier = Modifier.padding(14.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (currentCashier == c) PosDesignTokens.BrandSky800 else PosDesignTokens.TextPrimary
                            )
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
