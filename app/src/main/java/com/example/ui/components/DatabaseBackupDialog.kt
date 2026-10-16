package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.AppDatabase
import com.example.util.DatabaseBackupManager
import com.example.util.DatabaseStats
import com.example.util.ImportMode
import com.example.util.ImportResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseBackupDialog(
    database: AppDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import
    var dbStats by remember { mutableStateOf(DatabaseStats()) }
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    var pasteJsonText by remember { mutableStateOf("") }
    var importMode by remember { mutableStateOf(ImportMode.MERGE) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }
    var importResult by remember { mutableStateOf<ImportResult?>(null) }
    var showConfirmImportModal by remember { mutableStateOf(false) }

    // SAF Launchers
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                val success = DatabaseBackupManager.exportToFile(context, database, uri)
                isLoading = false
                if (success) {
                    Toast.makeText(context, "Database backup saved successfully to JSON!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to save backup file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                isLoading = true
                val res = DatabaseBackupManager.importFromFile(context, database, uri, importMode)
                isLoading = false
                importResult = res
                // Refresh stats
                dbStats = DatabaseBackupManager.getDatabaseStats(database)
            }
        }
    }

    LaunchedEffect(Unit) {
        dbStats = DatabaseBackupManager.getDatabaseStats(database)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Database Backup & Safety",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "JSON Export/Import for products, sales, and shifts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Export Backup") },
                        icon = { Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Import / Restore") },
                        icon = { Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (selectedTab == 0) {
                    // Export Tab
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Local Room Database Statistics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Products & Catalog Items:", fontSize = 12.sp)
                                Text("${dbStats.productCount} products / ${dbStats.itemCount} items", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Categories:", fontSize = 12.sp)
                                Text("${dbStats.categoryCount} categories", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Completed Sales Receipts:", fontSize = 12.sp)
                                Text("${dbStats.receiptCount} receipts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Shift Sessions & Records:", fontSize = 12.sp)
                                Text("${dbStats.shiftCount} shifts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Customers & Loyalty Records:", fontSize = 12.sp)
                                Text("${dbStats.customerCount} customers", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Divider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Persistent Records:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${dbStats.totalRecords} records", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val defaultFileName = "AuraPOS_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                                exportFileLauncher.launch(defaultFileName)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_backup_json_file_button")
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Backup to JSON File")
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    val json = DatabaseBackupManager.exportToJsonString(database)
                                    isLoading = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("AuraPOS Backup JSON", json)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Full Backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("copy_backup_json_clipboard_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Backup JSON to Clipboard")
                        }
                    }
                } else {
                    // Import Tab
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Import Mode Options", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = importMode == ImportMode.MERGE,
                                    onClick = { importMode = ImportMode.MERGE }
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text("Merge & Update (Recommended)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Inserts new records and updates matching IDs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = importMode == ImportMode.OVERWRITE,
                                    onClick = { importMode = ImportMode.OVERWRITE }
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text("Overwrite Database Catalog", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    Text("Appends imported items as new catalog entries", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { importFileLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("select_json_backup_file_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Backup JSON File")
                    }

                    OutlinedTextField(
                        value = pasteJsonText,
                        onValueChange = { pasteJsonText = it },
                        label = { Text("Or Paste Backup JSON Code Below") },
                        placeholder = { Text("{\n  \"metadata\": { ... },\n  \"products\": [ ... ]\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("paste_backup_json_text_field"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        maxLines = 6
                    )

                    if (pasteJsonText.isNotBlank()) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    val res = DatabaseBackupManager.importFromJsonString(database, pasteJsonText, importMode)
                                    isLoading = false
                                    importResult = res
                                    dbStats = DatabaseBackupManager.getDatabaseStats(database)
                                    pasteJsonText = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_pasted_json_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore from Pasted JSON")
                        }
                    }

                    importResult?.let { res ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (res.success) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        if (res.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                        contentDescription = null,
                                        tint = if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        if (res.success) "Restore Successful!" else "Restore Failed",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (res.success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(res.message, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_backup_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}
