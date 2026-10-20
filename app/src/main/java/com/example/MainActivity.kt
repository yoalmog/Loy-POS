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
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .statusBarsPadding()
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }

                            // Switch employee lock button (Loyverse 5.4)
                            IconButton(onClick = { showPinDialog = true }) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Switch Employee PIN",
                                    tint = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Bistro & Grill #1",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = currentCashier,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                    label = { Text(stringResource(R.string.sales_register), fontWeight = FontWeight.SemiBold) },
                    selected = currentScreen == AppDestination.SALES_REGISTER,
                    onClick = {
                        currentScreen = AppDestination.SALES_REGISTER
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    label = { Text(stringResource(R.string.receipts_archive)) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setReceiptsDialog(true)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                    label = { Text(stringResource(R.string.shift_management)) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setShiftDialog(true)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                    label = { Text(stringResource(R.string.open_tickets)) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setOpenTicketsDialog(true)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text(stringResource(R.string.back_office), fontWeight = FontWeight.Bold) },
                    selected = currentScreen == AppDestination.BACK_OFFICE,
                    onClick = {
                        currentScreen = AppDestination.BACK_OFFICE
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text(stringResource(R.string.customer_loyalty)) },
                    selected = false,
                    onClick = {
                        currentScreen = AppDestination.BACK_OFFICE
                        backOfficeViewModel.selectTab(BackOfficeTab.CUSTOMERS)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Tune, contentDescription = null) },
                    label = { Text(stringResource(R.string.inventory_adjustments)) },
                    selected = false,
                    onClick = {
                        currentScreen = AppDestination.BACK_OFFICE
                        backOfficeViewModel.selectTab(BackOfficeTab.ITEMS_INVENTORY)
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Backup, contentDescription = null) },
                    label = { Text(stringResource(R.string.backup_sync), fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            posViewModel.setDatabaseBackupDialog(true)
                        }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
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
