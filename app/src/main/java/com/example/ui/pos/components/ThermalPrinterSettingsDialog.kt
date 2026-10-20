package com.example.ui.pos.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.NetworkPrinter
import com.example.data.model.NetworkPrinterSettings
import com.example.data.model.PrinterDiscoveryState
import com.example.data.model.PrinterInterfaceType
import com.example.util.DiscoveredPrinter
import com.example.util.NetworkPrinterDiscoveryService
import com.example.util.NetworkPrinterManager
import com.example.util.PrinterConfig
import com.example.util.PrinterConnectionStatus
import com.example.util.ThermalPrinterManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalPrinterSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize Network Printer Manager
    LaunchedEffect(Unit) {
        NetworkPrinterManager.initialize(context)
    }

    // BLE Printer States
    val bleStatus by ThermalPrinterManager.status.collectAsState()
    val bleConfig by ThermalPrinterManager.config.collectAsState()
    val bleDiscoveredPrinters by ThermalPrinterManager.discoveredPrinters.collectAsState()
    val bleLastPrintedLog by ThermalPrinterManager.lastPrintedLog.collectAsState()

    // Network Printer States
    val discoveryState by NetworkPrinterDiscoveryService.discoveryState.collectAsState()
    val discoveredNetPrinters by NetworkPrinterDiscoveryService.discoveredPrinters.collectAsState()
    val savedNetPrinters by NetworkPrinterManager.savedPrinters.collectAsState()
    val netSettings by NetworkPrinterManager.settings.collectAsState()
    val activeNetPrinter by NetworkPrinterManager.activePrinter.collectAsState()
    val netPrintLog by NetworkPrinterManager.lastPrintLog.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Network (IP), 1 = Bluetooth (BLE), 2 = POS Preferences, 3 = Virtual Log
    var showAddIpDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("POS Printer & Hardware Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Network IP Discovery (Port 9100) & BLE GATT Drivers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        // Connection Status Pill
                        Surface(
                            color = if (activeNetPrinter != null) Color(0xFF2E7D32) else when (bleStatus) {
                                PrinterConnectionStatus.CONNECTED -> Color(0xFF2E7D32)
                                PrinterConnectionStatus.CONNECTING, PrinterConnectionStatus.SCANNING -> Color(0xFFED6C02)
                                else -> MaterialTheme.colorScheme.error
                            },
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeNetPrinter != null) "NET ONLINE: ${activeNetPrinter?.ipAddress}" else bleStatus.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )

                // Navigation Tabs
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Network (IP) Printers") },
                        icon = { Icon(Icons.Default.Lan, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Bluetooth (BLE)") },
                        icon = { Icon(Icons.Default.BluetoothSearching, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("POS Checkout") },
                        icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("Virtual Log") },
                        icon = { Icon(Icons.Default.Terminal, contentDescription = null) }
                    )
                }

                // Tab Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (activeTab) {
                        0 -> NetworkPrinterTab(
                            discoveryState = discoveryState,
                            discoveredPrinters = discoveredNetPrinters,
                            savedPrinters = savedNetPrinters,
                            activePrinter = activeNetPrinter,
                            onScanNetwork = { NetworkPrinterDiscoveryService.startDiscovery(context) },
                            onStopScan = { NetworkPrinterDiscoveryService.stopDiscovery() },
                            onSavePrinter = { printer, isDefault ->
                                NetworkPrinterManager.savePrinter(context, printer, isDefault)
                            },
                            onSetDefault = { printerId ->
                                NetworkPrinterManager.setDefaultPrinter(context, printerId)
                            },
                            onDeletePrinter = { printerId ->
                                NetworkPrinterManager.deletePrinter(context, printerId)
                            },
                            onTestPrint = { printer ->
                                NetworkPrinterManager.printTestReceipt(context, printer)
                            },
                            onOpenAddDialog = { showAddIpDialog = true }
                        )
                        1 -> BleScannerTab(
                            status = bleStatus,
                            config = bleConfig,
                            printers = bleDiscoveredPrinters,
                            onScan = { ThermalPrinterManager.startScan(context) },
                            onConnect = { printer -> ThermalPrinterManager.connectPrinter(context, printer) }
                        )
                        2 -> PosCheckoutPreferencesTab(
                            netSettings = netSettings,
                            bleConfig = bleConfig,
                            savedNetPrinters = savedNetPrinters,
                            onUpdateNetSettings = { updated ->
                                NetworkPrinterManager.updateSettings(context, updated)
                            },
                            onWidthChanged = { width ->
                                ThermalPrinterManager.setPaperWidth(width)
                                NetworkPrinterManager.updateSettings(context, netSettings.copy(defaultPaperWidthMm = width))
                            },
                            onAutoFeaturesChanged = { cut, drawer ->
                                ThermalPrinterManager.setAutoFeatures(cut, drawer)
                                NetworkPrinterManager.updateSettings(context, netSettings.copy(cutPaperOnPrint = cut, openDrawerOnPrint = drawer))
                            },
                            onTestPrint = {
                                NetworkPrinterManager.printTestReceipt(context)
                                ThermalPrinterManager.printTestReceipt(context)
                            },
                            onOpenDrawer = {
                                NetworkPrinterManager.openCashDrawer(context)
                                ThermalPrinterManager.openCashDrawer(context)
                            },
                            onCutPaper = {
                                NetworkPrinterManager.cutPaper(context)
                                ThermalPrinterManager.cutPaper(context)
                            }
                        )
                        3 -> CombinedVirtualLogTab(
                            netLog = netPrintLog,
                            bleLog = bleLastPrintedLog
                        )
                    }
                }
            }
        }
    }

    // Add Manual IP Printer Dialog
    if (showAddIpDialog) {
        AddManualPrinterDialog(
            defaultSubnet = NetworkPrinterDiscoveryService.getLocalSubnet(context),
            onDismiss = { showAddIpDialog = false },
            onAddPrinter = { printer ->
                NetworkPrinterManager.savePrinter(context, printer, makeDefault = true)
                showAddIpDialog = false
            }
        )
    }
}

@Composable
private fun NetworkPrinterTab(
    discoveryState: PrinterDiscoveryState,
    discoveredPrinters: List<NetworkPrinter>,
    savedPrinters: List<NetworkPrinter>,
    activePrinter: NetworkPrinter?,
    onScanNetwork: () -> Unit,
    onStopScan: () -> Unit,
    onSavePrinter: (NetworkPrinter, Boolean) -> Unit,
    onSetDefault: (String) -> Unit,
    onDeletePrinter: (String) -> Unit,
    onTestPrint: (NetworkPrinter) -> Unit,
    onOpenAddDialog: () -> Unit
) {
    val isScanning = discoveryState is PrinterDiscoveryState.Scanning

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Active / Default Network Printer Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFF2E7D32),
                                shape = CircleShape,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEFAULT POS NETWORK PRINTER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        if (activePrinter != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${activePrinter.paperWidthMm}mm ESC/POS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (activePrinter != null) {
                        Text(
                            text = activePrinter.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "IP Address: ${activePrinter.addressLabel} (TCP Port ${activePrinter.port})",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Model: ${activePrinter.model} • Latency: ${activePrinter.responseTimeMs}ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onTestPrint(activePrinter) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Receipt", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "No default network printer assigned yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Scan the local subnet or add a printer by IP to automate checkout receipts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Discovery & Subnet Scanner Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Network Subnet Discovery", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            val subnetLabel = when (discoveryState) {
                                is PrinterDiscoveryState.Scanning -> "Probing ${discoveryState.subnet}..."
                                is PrinterDiscoveryState.Finished -> "Scanned ${discoveryState.subnet}"
                                else -> "Probes Port 9100 / 631 & mDNS DNS-SD"
                            }
                            Text(subnetLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenAddDialog,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add IP")
                            }

                            Button(
                                onClick = {
                                    if (isScanning) onStopScan() else onScanNetwork()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isScanning) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stop")
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Scan Network")
                                }
                            }
                        }
                    }

                    // Progress bar during subnet sweep
                    if (discoveryState is PrinterDiscoveryState.Scanning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { discoveryState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target: ${discoveryState.currentIp}:9100", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            Text("Found: ${discoveryState.foundCount} printers", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }

        // 3. Saved Network Printers (In Settings)
        item {
            Text(
                text = "Saved in Settings (${savedPrinters.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (savedPrinters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No saved network printers in settings yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(savedPrinters) { printer ->
                val isDefault = printer.isDefault || (activePrinter?.id == printer.id)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = if (isDefault) CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E7D32)),
                        width = 2.dp
                    ) else CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDefault) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Lan,
                                        contentDescription = null,
                                        tint = if (isDefault) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(printer.name, fontWeight = FontWeight.Bold)
                                    if (isDefault) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF2E7D32),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "DEFAULT",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    "${printer.ipAddress}:${printer.port} • ${printer.paperWidthMm}mm • ${printer.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(onClick = { onTestPrint(printer) }) {
                                Icon(Icons.Default.Print, contentDescription = "Test Print", tint = MaterialTheme.colorScheme.primary)
                            }

                            if (!isDefault) {
                                Button(
                                    onClick = { onSetDefault(printer.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Make Default", fontSize = 11.sp)
                                }
                            }

                            IconButton(onClick = { onDeletePrinter(printer.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 4. Discovered Printers List
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Discovered Network Printers (${discoveredPrinters.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (discoveredPrinters.isNotEmpty()) {
                    Text(
                        text = "Tap to save & assign",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (discoveredPrinters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WifiOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No active discovery scan yet. Click 'Scan Network' to find printers via IP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(discoveredPrinters) { discovered ->
                val alreadySaved = savedPrinters.any { it.ipAddress == discovered.ipAddress }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = null,
                                tint = if (alreadySaved) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(discovered.name, fontWeight = FontWeight.Bold)
                                Text(
                                    "IP: ${discovered.ipAddress}:${discovered.port} • Response: ${discovered.responseTimeMs}ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onTestPrint(discovered) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Test", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { onSavePrinter(discovered, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(if (alreadySaved) "Set Default" else "Save & Use", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BleScannerTab(
    status: PrinterConnectionStatus,
    config: PrinterConfig,
    printers: List<DiscoveredPrinter>,
    onScan: () -> Unit,
    onConnect: (DiscoveredPrinter) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Bluetooth Printer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = config.deviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MAC: ${config.deviceAddress} | UUID: ${config.serviceUuid.substring(0, 8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onScan,
                    enabled = status != PrinterConnectionStatus.SCANNING,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (status == PrinterConnectionStatus.SCANNING) "Scanning..." else "Scan BLE")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Discovered BLE / SPP Printers (${printers.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (printers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BluetoothDisabled, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No BLE printers found. Click 'Scan BLE' to search.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(printers) { printer ->
                    val isCurrent = printer.address == config.deviceAddress
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onConnect(printer) },
                        border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E7D32)),
                            width = 2.dp
                        ) else CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(printer.name, fontWeight = FontWeight.Bold)
                                    Text("${printer.address} • RSSI: ${printer.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(
                                onClick = { onConnect(printer) },
                                colors = if (isCurrent) ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(if (isCurrent) "Connected" else "Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosCheckoutPreferencesTab(
    netSettings: NetworkPrinterSettings,
    bleConfig: PrinterConfig,
    savedNetPrinters: List<NetworkPrinter>,
    onUpdateNetSettings: (NetworkPrinterSettings) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onAutoFeaturesChanged: (Boolean, Boolean) -> Unit,
    onTestPrint: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCutPaper: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // POS Checkout Automation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("POS Checkout Automation", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Print Receipt on Sale Completion", fontWeight = FontWeight.SemiBold)
                            Text("Instantly transmit ESC/POS receipt upon checkout approval", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = netSettings.autoPrintOnCheckout,
                            onCheckedChange = {
                                onUpdateNetSettings(netSettings.copy(autoPrintOnCheckout = it))
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Paper Cut (GS V)", fontWeight = FontWeight.SemiBold)
                            Text("Send partial paper cut command after receipt print", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = netSettings.cutPaperOnPrint,
                            onCheckedChange = {
                                onUpdateNetSettings(netSettings.copy(cutPaperOnPrint = it))
                                onAutoFeaturesChanged(it, netSettings.openDrawerOnPrint)
                            }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kick Cash Drawer on Checkout", fontWeight = FontWeight.SemiBold)
                            Text("Send 24V drawer pulse (ESC p) to trigger cash drawer opening", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = netSettings.openDrawerOnPrint,
                            onCheckedChange = {
                                onUpdateNetSettings(netSettings.copy(openDrawerOnPrint = it))
                                onAutoFeaturesChanged(netSettings.cutPaperOnPrint, it)
                            }
                        )
                    }
                }
            }
        }

        // Paper Width Selector Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thermal Paper Specifications", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onWidthChanged(58) },
                            modifier = Modifier.weight(1f),
                            colors = if (netSettings.defaultPaperWidthMm == 58) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("58mm Mini", fontWeight = FontWeight.Bold)
                                Text("384 dots • 32 cols", fontSize = 10.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { onWidthChanged(80) },
                            modifier = Modifier.weight(1f),
                            colors = if (netSettings.defaultPaperWidthMm == 80) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("80mm Standard", fontWeight = FontWeight.Bold)
                                Text("576 dots • 48 cols", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open Drawer")
                }
                OutlinedButton(
                    onClick = onCutPaper,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cut Paper")
                }
                Button(
                    onClick = onTestPrint,
                    modifier = Modifier.weight(1.2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Print")
                }
            }
        }
    }
}

@Composable
private fun CombinedVirtualLogTab(
    netLog: String,
    bleLog: String
) {
    val displayLog = when {
        netLog.isNotBlank() -> netLog
        bleLog.isNotBlank() -> bleLog
        else -> "> No print output streamed yet.\n> Click 'Test Print' or complete an order to view ESC/POS stream."
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Transmission Stream & Protocol Console", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("TCP Socket Port 9100 / BLE GATT Packet Visualizer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = displayLog,
                    color = Color(0xFF81C784),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AddManualPrinterDialog(
    defaultSubnet: String,
    onDismiss: () -> Unit,
    onAddPrinter: (NetworkPrinter) -> Unit
) {
    var printerName by remember { mutableStateOf("Kitchen Receipt Printer") }
    var ipAddress by remember { mutableStateOf("$defaultSubnet.120") }
    var portStr by remember { mutableStateOf("9100") }
    var paperWidth by remember { mutableStateOf(80) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Network Printer by IP", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = printerName,
                    onValueChange = { printerName = it },
                    label = { Text("Printer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IPv4 Address (e.g. 192.168.1.150)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = portStr,
                    onValueChange = { portStr = it },
                    label = { Text("TCP Port (Standard 9100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paperWidth == 80,
                        onClick = { paperWidth = 80 },
                        label = { Text("80mm (Standard)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paperWidth == 58,
                        onClick = { paperWidth = 58 },
                        label = { Text("58mm (Mini)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portStr.toIntOrNull() ?: 9100
                    val printer = NetworkPrinter(
                        name = printerName.ifBlank { "Network Thermal ($ipAddress)" },
                        ipAddress = ipAddress.trim(),
                        port = port,
                        model = "ESC/POS Network Thermal",
                        paperWidthMm = paperWidth,
                        isOnline = true,
                        isDefault = true
                    )
                    onAddPrinter(printer)
                },
                enabled = ipAddress.isNotBlank() && ipAddress.contains("."),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
