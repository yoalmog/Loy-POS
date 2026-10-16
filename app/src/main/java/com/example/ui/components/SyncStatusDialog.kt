package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.SyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusDialog(
    isOnline: Boolean,
    connectionType: String,
    syncState: SyncState,
    lastSyncTime: Long,
    pendingSyncCount: Int,
    onTriggerSync: () -> Unit,
    onOpenBackupDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    val statusColor = when {
        !isOnline -> Color(0xFFE65100) // Orange/Amber
        syncState == SyncState.SYNCING -> Color(0xFF0288D1) // Blue
        syncState == SyncState.SYNCED -> Color(0xFF2E7D32) // Green
        else -> Color(0xFFD32F2F) // Red
    }

    val formattedLastSync = remember(lastSyncTime) {
        if (lastSyncTime <= 0) "Never"
        else SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(lastSyncTime))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "System Sync & Connectivity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOnline) "Connected ($connectionType)" else "Offline Mode Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Main Status Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(12.dp),
                            shape = CircleShape,
                            color = statusColor
                        ) {}

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    !isOnline -> "Local Storage Mode (100% Offline Functional)"
                                    syncState == SyncState.SYNCING -> "Synchronizing POS Data..."
                                    syncState == SyncState.SYNCED -> "Online • Real-Time Database Synced"
                                    else -> "Sync Warning"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = statusColor
                            )
                            Text(
                                text = if (!isOnline)
                                    "All transactions, products, and shifts are recorded to local Room DB."
                                else
                                    "Last sync completed at $formattedLastSync.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Details List
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Database Engine:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("SQLite Room (Local Persistent)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Network Type:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(connectionType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pending Sync Queue:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$pendingSyncCount records", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (pendingSyncCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32))
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Last Sync Timestamp:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formattedLastSync, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Quick Backup Action Notice
                OutlinedCard(
                    onClick = onOpenBackupDialog,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_backup_dialog_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text("Database Backup & Restore", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Export/Import catalog, sales, & shifts to JSON", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onTriggerSync,
                enabled = syncState != SyncState.SYNCING && isOnline,
                modifier = Modifier.testTag("trigger_sync_button")
            ) {
                Icon(
                    Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (syncState == SyncState.SYNCING) "Syncing..." else "Sync Now")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_sync_dialog_button")
            ) {
                Text("Close")
            }
        }
    )
}
