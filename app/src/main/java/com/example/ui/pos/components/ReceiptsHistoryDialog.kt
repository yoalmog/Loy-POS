package com.example.ui.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.ReceiptEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsHistoryDialog(
    receipts: List<ReceiptEntity>,
    onDismiss: () -> Unit,
    onRefundReceipt: (ReceiptEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedReceiptForDetail by remember { mutableStateOf<ReceiptEntity?>(null) }
    var showRefundConfirm by remember { mutableStateOf<ReceiptEntity?>(null) }

    val filtered = remember(receipts, searchQuery) {
        if (searchQuery.isBlank()) receipts
        else receipts.filter {
            it.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                    (it.customerName?.contains(searchQuery, ignoreCase = true) == true) ||
                    it.itemsSummaryJson.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Receipts Archive", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search receipts by # or item...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { receipt ->
                        val dateFormatted = SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(receipt.timestamp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReceiptForDetail = receipt },
                            colors = CardDefaults.cardColors(
                                containerColor = if (receipt.isRefunded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            receipt.receiptNumber,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (receipt.isRefunded) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.error,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "REFUNDED",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "$dateFormatted • ${receipt.diningOption.label} • ${receipt.paymentType.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        receipt.itemsSummaryJson,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "$${String.format(Locale.US, "%.2f", receipt.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (receipt.isRefunded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    if (!receipt.isRefunded) {
                                        TextButton(
                                            onClick = { showRefundConfirm = receipt },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Refund", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedReceiptForDetail?.let { receipt ->
        ReceiptDialog(
            receipt = receipt,
            onDismiss = { selectedReceiptForDetail = null },
            onNewSale = { selectedReceiptForDetail = null }
        )
    }

    // Refund Confirmation
    showRefundConfirm?.let { receipt ->
        AlertDialog(
            onDismissRequest = { showRefundConfirm = null },
            title = { Text("Confirm Refund", fontWeight = FontWeight.Bold) },
            text = {
                Text("Issue full refund of $${String.format(Locale.US, "%.2f", receipt.totalAmount)} for receipt ${receipt.receiptNumber}? Items will be returned to inventory stock.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRefundReceipt(receipt)
                        showRefundConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Refund")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRefundConfirm = null }) { Text("Cancel") }
            }
        )
    }
}
