package com.example.ui.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ShiftEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementDialog(
    currentShift: ShiftEntity?,
    onDismiss: () -> Unit,
    onOpenShift: (startingCash: Double) -> Unit,
    onCashAction: (isPayIn: Boolean, amount: Double, comment: String) -> Unit,
    onCloseShift: (actualCash: Double) -> Unit
) {
    var showPayInDialog by remember { mutableStateOf(false) }
    var showPayOutDialog by remember { mutableStateOf(false) }
    var showCloseShiftConfirm by remember { mutableStateOf(false) }
    var startingCashInput by remember { mutableStateOf("150.00") }
    var actualCashInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentShift != null && currentShift.isOpen) "Current Shift Management" else "Open New Shift",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )

                if (currentShift == null || !currentShift.isOpen) {
                    // Open Shift Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.LockClock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Active Shift Open",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter the starting cash float in the drawer to begin selling.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                        )

                        OutlinedTextField(
                            value = startingCashInput,
                            onValueChange = { startingCashInput = it },
                            label = { Text("Starting Cash Float ($)") },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                val amount = startingCashInput.toDoubleOrNull() ?: 100.0
                                onOpenShift(amount)
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Shift", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    // Active Shift Dashboard & Z-Report
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            val openTime = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US).format(Date(currentShift.openedAt))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Active Register Shift",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Opened: $openTime\nCashier: ${currentShift.cashierName}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text("Cash Drawer Balance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ShiftRow("Starting Cash Float", "$${String.format(Locale.US, "%.2f", currentShift.startingCash)}")
                                    ShiftRow("Cash Payments (Sales)", "+$${String.format(Locale.US, "%.2f", currentShift.cashPayments)}")
                                    ShiftRow("Cash Refunds", "-$${String.format(Locale.US, "%.2f", currentShift.cashRefunds)}")
                                    ShiftRow("Paid In", "+$${String.format(Locale.US, "%.2f", currentShift.cashPaidIn)}")
                                    ShiftRow("Paid Out", "-$${String.format(Locale.US, "%.2f", currentShift.cashPaidOut)}")
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Expected Cash in Drawer", fontWeight = FontWeight.Bold)
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", currentShift.expectedCash)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    ShiftRow("Card Payments (Direct)", "$${String.format(Locale.US, "%.2f", currentShift.cardPayments)}")
                                }
                            }
                        }

                        // Pay In & Pay Out Actions
                        item {
                            Text("Cash Management", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showPayInDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay In")
                                }
                                OutlinedButton(
                                    onClick = { showPayOutDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pay Out")
                                }
                            }
                        }
                    }

                    // Close Shift Button
                    Surface(
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick = {
                                    actualCashInput = String.format(Locale.US, "%.2f", currentShift.expectedCash)
                                    showCloseShiftConfirm = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close Shift & Print Z-Report", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Pay In Dialog
    if (showPayInDialog) {
        CashActionSubDialog(
            title = "Pay In (Add Cash)",
            isPayIn = true,
            onDismiss = { showPayInDialog = false },
            onConfirm = { amt, comment ->
                onCashAction(true, amt, comment)
                showPayInDialog = false
            }
        )
    }

    // Pay Out Dialog
    if (showPayOutDialog) {
        CashActionSubDialog(
            title = "Pay Out (Remove Cash)",
            isPayIn = false,
            onDismiss = { showPayOutDialog = false },
            onConfirm = { amt, comment ->
                onCashAction(false, amt, comment)
                showPayOutDialog = false
            }
        )
    }

    // Close Shift Reconcile Dialog
    if (showCloseShiftConfirm && currentShift != null) {
        val actual = actualCashInput.toDoubleOrNull() ?: 0.0
        val diff = actual - currentShift.expectedCash

        AlertDialog(
            onDismissRequest = { showCloseShiftConfirm = false },
            title = { Text("Close Shift Reconciliation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Expected Cash in Drawer: $${String.format(Locale.US, "%.2f", currentShift.expectedCash)}")

                    OutlinedTextField(
                        value = actualCashInput,
                        onValueChange = { actualCashInput = it },
                        label = { Text("Actual Cash Counted ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Surface(
                        color = if (Math.abs(diff) < 0.01) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (Math.abs(diff) < 0.01) "Balanced (No cash discrepancy)"
                            else "Discrepancy: ${if (diff > 0) "+$${String.format(Locale.US, "%.2f", diff)} (Over)" else "-$${String.format(Locale.US, "%.2f", -diff)} (Short)"}",
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCloseShift(actual)
                        showCloseShiftConfirm = false
                        onDismiss()
                    }
                ) {
                    Text("Confirm & Close Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseShiftConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CashActionSubDialog(
    title: String,
    isPayIn: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, comment: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var commentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Reason / Note") },
                    placeholder = { Text(if (isPayIn) "e.g. Additional coin change" else "e.g. Milk grocery expense, bank deposit") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, commentText)
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ShiftRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}
