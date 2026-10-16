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
import com.example.util.DiscoveredPrinter
import com.example.util.PrinterConfig
import com.example.util.PrinterConnectionStatus
import com.example.util.ThermalPrinterManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalPrinterSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val status by ThermalPrinterManager.status.collectAsState()
    val config by ThermalPrinterManager.config.collectAsState()
    val discoveredPrinters by ThermalPrinterManager.discoveredPrinters.collectAsState()
    val lastPrintedLog by ThermalPrinterManager.lastPrintedLog.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Scanner & Hardware, 1 = Config, 2 = Print Log

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
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
                                Text("Mini Thermal Printer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("BLE GATT & ESC/POS Protocol Manager", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            color = when (status) {
                                PrinterConnectionStatus.CONNECTED -> Color(0xFF2E7D32) // Signature Loyverse Green
                                PrinterConnectionStatus.CONNECTING, PrinterConnectionStatus.SCANNING -> Color(0xFFED6C02)
                                PrinterConnectionStatus.PRINTING -> Color(0xFF0288D1)
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
                                    text = status.name,
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
                        text = { Text("Devices & Scan") },
                        icon = { Icon(Icons.Default.BluetoothSearching, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Paper & Peripherals") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
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
                        0 -> ScannerTab(
                            status = status,
                            config = config,
                            printers = discoveredPrinters,
                            onScan = { ThermalPrinterManager.startScan(context) },
                            onConnect = { printer -> ThermalPrinterManager.connectPrinter(context, printer) }
                        )
                        1 -> SettingsTab(
                            config = config,
                            onWidthChanged = { width -> ThermalPrinterManager.setPaperWidth(width) },
                            onAutoFeaturesChanged = { cut, drawer -> ThermalPrinterManager.setAutoFeatures(cut, drawer) },
                            onTestPrint = { ThermalPrinterManager.printTestReceipt(context) },
                            onOpenDrawer = { ThermalPrinterManager.openCashDrawer(context) },
                            onCutPaper = { ThermalPrinterManager.cutPaper(context) }
                        )
                        2 -> VirtualLogTab(lastPrintedLog)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerTab(
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
                        text = "Active Printer",
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Loyverse Green
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
                    Text("No printers found. Click 'Scan BLE' to search.", color = Color.Gray)
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
                        border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E7D32)), width = 2.dp) else CardDefaults.outlinedCardBorder()
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
private fun SettingsTab(
    config: PrinterConfig,
    onWidthChanged: (Int) -> Unit,
    onAutoFeaturesChanged: (Boolean, Boolean) -> Unit,
    onTestPrint: () -> Unit,
    onOpenDrawer: () -> Unit,
    onCutPaper: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Paper Width Selector
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
                        colors = if (config.paperWidthMm == 58) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("58mm Mini", fontWeight = FontWeight.Bold)
                            Text("384 dots • 32 cols", fontSize = 10.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = { onWidthChanged(80) },
                        modifier = Modifier.weight(1f),
                        colors = if (config.paperWidthMm == 80) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("80mm Standard", fontWeight = FontWeight.Bold)
                            Text("576 dots • 48 cols", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Hardware Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Peripherals & Automation", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto-Cut Paper", fontWeight = FontWeight.SemiBold)
                        Text("Send GS V partial cut command after printing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.autoCutEnabled,
                        onCheckedChange = { onAutoFeaturesChanged(it, config.autoDrawerEnabled) }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Kick Cash Drawer", fontWeight = FontWeight.SemiBold)
                        Text("Send ESC p pulse to Pin 2 on checkout", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = config.autoDrawerEnabled,
                        onCheckedChange = { onAutoFeaturesChanged(config.autoCutEnabled, it) }
                    )
                }
            }
        }

        // Test Hardware Actions
        Spacer(modifier = Modifier.weight(1f))
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) // Loyverse Green
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Print")
            }
        }
    }
}

@Composable
private fun VirtualLogTab(logText: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Transmission Stream & Protocol Console", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("Chunking: 50 bytes per packet • 20ms inter-packet delay", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Box(modifier = Modifier.padding(14.dp)) {
                if (logText.isBlank()) {
                    Text(
                        text = "> No print output streamed yet.\n> Click 'Test Print' or complete an order to view ESC/POS stream.",
                        color = Color(0xFF81C784),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = logText,
                        color = Color(0xFF81C784),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
