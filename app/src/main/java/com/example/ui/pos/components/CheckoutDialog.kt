package com.example.ui.pos.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    grandTotal: Double,
    subtotal: Double,
    taxAmount: Double,
    discountAmount: Double,
    diningOption: DiningOption,
    selectedCustomer: CustomerEntity?,
    onDismiss: () -> Unit,
    onRedeemPoints: (Double) -> Unit,
    onPaymentConfirmed: (type: PaymentType, details: String, tipAmount: Double) -> Unit
) {
    var selectedTab by remember { mutableStateOf(PaymentType.CASH) }
    var tipAmount by remember { mutableStateOf(0.0) }

    // Cash Tender State
    val finalDue = grandTotal + tipAmount
    var cashReceivedInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", finalDue)) }
    val cashReceived = cashReceivedInput.toDoubleOrNull() ?: 0.0
    val changeDue = (cashReceived - finalDue).coerceAtLeast(0.0)

    // Card Tender State
    var cardStep by remember { mutableStateOf(1) } // 1 = Ready, 2 = Authorizing, 3 = Approved

    // Split Tender State
    var splitCount by remember { mutableStateOf(2) }
    val splitAmountEach = (finalDue / splitCount)

    // Loyalty points state
    var pointsToRedeemInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = {
                        Column {
                            Text("Payment & Checkout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Total Due: $${String.format(Locale.US, "%.2f", finalDue)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )

                // Loyalty Points Redemption Banner if customer is attached
                if (selectedCustomer != null && selectedCustomer.pointsBalance > 0.0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${selectedCustomer.name} Loyalty",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.2f", selectedCustomer.pointsBalance)} pts ($${String.format(Locale.US, "%.2f", selectedCustomer.pointsBalance)} value)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    val maxRedeem = minOf(selectedCustomer.pointsBalance, grandTotal)
                                    onRedeemPoints(maxRedeem)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Redeem Points", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Payment Method Selector Tabs
                TabRow(
                    selectedTabIndex = when (selectedTab) {
                        PaymentType.CASH -> 0
                        PaymentType.CARD -> 1
                        PaymentType.SPLIT -> 2
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedTab == PaymentType.CASH,
                        onClick = { selectedTab = PaymentType.CASH },
                        text = { Text("Cash") },
                        icon = { Icon(Icons.Default.Payments, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == PaymentType.CARD,
                        onClick = { selectedTab = PaymentType.CARD },
                        text = { Text("Card") },
                        icon = { Icon(Icons.Default.CreditCard, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == PaymentType.SPLIT,
                        onClick = { selectedTab = PaymentType.SPLIT },
                        text = { Text("Split") },
                        icon = { Icon(Icons.Default.CallSplit, contentDescription = null) }
                    )
                }

                // Main Tender Content Area
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tip selection row
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Add Tip (Optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val tips = listOf(0.0 to "No Tip", 1.0 to "$1.00", 2.0 to "$2.00", 3.0 to "$3.00", 5.0 to "$5.00")
                                    tips.forEach { (amt, label) ->
                                        val isSelected = tipAmount == amt
                                        OutlinedButton(
                                            onClick = {
                                                tipAmount = amt
                                                cashReceivedInput = String.format(Locale.US, "%.2f", grandTotal + amt)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = if (isSelected) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                            else ButtonDefaults.outlinedButtonColors(),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                        ) {
                                            Text(label, fontSize = 11.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    when (selectedTab) {
                        PaymentType.CASH -> {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Big Due & Received Card
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Total Due", style = MaterialTheme.typography.labelSmall)
                                                    Text(
                                                        "$${String.format(Locale.US, "%.2f", finalDue)}",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Change Due", style = MaterialTheme.typography.labelSmall)
                                                    Text(
                                                        "$${String.format(Locale.US, "%.2f", changeDue)}",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Quick cash denomination chips
                                    Text("Suggested Cash Tender:", style = MaterialTheme.typography.labelMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val presets = listOf(
                                            finalDue to "Exact",
                                            Math.ceil(finalDue) to "$${Math.ceil(finalDue).toInt()}",
                                            20.0 to "$20",
                                            50.0 to "$50",
                                            100.0 to "$100"
                                        )
                                        presets.forEach { (presetVal, label) ->
                                            Button(
                                                onClick = {
                                                    cashReceivedInput = String.format(Locale.US, "%.2f", presetVal)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                                            ) {
                                                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = cashReceivedInput,
                                        onValueChange = { cashReceivedInput = it },
                                        label = { Text("Cash Received ($)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        PaymentType.CARD -> {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        when (cardStep) {
                                            1 -> {
                                                Icon(
                                                    Icons.Default.Contactless,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "Tap, Insert, or Swipe Card",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Terminal ready • Amount: $${String.format(Locale.US, "%.2f", finalDue)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Button(
                                                    onClick = { cardStep = 2 },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Simulate Card Tap")
                                                }
                                            }
                                            2 -> {
                                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                                Text("Processing transaction...", fontWeight = FontWeight.Bold)
                                                Text("Authorizing with payment network", style = MaterialTheme.typography.bodySmall)
                                                LaunchedEffect(Unit) {
                                                    kotlinx.coroutines.delay(1200)
                                                    cardStep = 3
                                                }
                                            }
                                            3 -> {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text("Payment Approved!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text("Visa ending in •••• 4012", style = MaterialTheme.typography.bodySmall)
                                                Text("Auth Code: #AP99410", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        PaymentType.SPLIT -> {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Number of Splits", fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (splitCount > 2) splitCount-- }) {
                                                Icon(Icons.Default.Remove, contentDescription = null)
                                            }
                                            Text(splitCount.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            IconButton(onClick = { if (splitCount < 6) splitCount++ }) {
                                                Icon(Icons.Default.Add, contentDescription = null)
                                            }
                                        }
                                    }

                                    for (i in 1..splitCount) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Payment $i ($${String.format(Locale.US, "%.2f", splitAmountEach)})", fontWeight = FontWeight.Bold)
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        if (i % 2 == 1) "Cash" else "Card",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Final Action Button
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = {
                                val details = when (selectedTab) {
                                    PaymentType.CASH -> "Cash Tendered: $${String.format(Locale.US, "%.2f", cashReceived)}, Change: $${String.format(Locale.US, "%.2f", changeDue)}"
                                    PaymentType.CARD -> "Card Payment: Visa •••• 4012 (Auth #AP99410)"
                                    PaymentType.SPLIT -> "Split into $splitCount payments of $${String.format(Locale.US, "%.2f", splitAmountEach)}"
                                }
                                onPaymentConfirmed(selectedTab, details, tipAmount)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = when (selectedTab) {
                                PaymentType.CASH -> cashReceived >= finalDue
                                PaymentType.CARD -> cardStep == 3
                                PaymentType.SPLIT -> true
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Charge $${String.format(Locale.US, "%.2f", finalDue)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptDialog(
    receipt: ReceiptEntity,
    onDismiss: () -> Unit,
    onNewSale: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var emailSentMessage by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        // Store Header
                        Text("★ BISTRO & GRILL ★", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        Text("100 Market Square, Suite 4B", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Tel: (555) 019-2834 • VAT #849201", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Receipt Details
                        Text(
                            "RECEIPT ${receipt.receiptNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        val dateFormatted = SimpleDateFormat("MMM dd, yyyy  h:mm a", Locale.US).format(Date(receipt.timestamp))
                        Text(dateFormatted, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                        Text("Server: ${receipt.cashierName} • ${receipt.diningOption.label}", style = MaterialTheme.typography.bodySmall)
                        if (receipt.customerName != null) {
                            Text("Customer: ${receipt.customerName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    item {
                        // Items Breakdown
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(receipt.itemsSummaryJson, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    item {
                        // Financial breakdown
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ReceiptRow("Subtotal", "$${String.format(Locale.US, "%.2f", receipt.subtotal)}")
                            if (receipt.discountAmount > 0) {
                                ReceiptRow("Discount / Points", "-$${String.format(Locale.US, "%.2f", receipt.discountAmount)}")
                            }
                            ReceiptRow("Sales Tax (8.25%)", "$${String.format(Locale.US, "%.2f", receipt.taxAmount)}")
                            if (receipt.tipAmount > 0) {
                                ReceiptRow("Tip", "$${String.format(Locale.US, "%.2f", receipt.tipAmount)}")
                            }
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("TOTAL PAID", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "$${String.format(Locale.US, "%.2f", receipt.totalAmount)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                "Payment: ${receipt.paymentType.label} (${receipt.paymentDetailsJson})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("Thank you for dining with us!", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp)
                    }

                    // Email & Print Receipt Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        val context = androidx.compose.ui.platform.LocalContext.current
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { com.example.util.ReceiptPrinter.printReceipt(context, receipt) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Print, contentDescription = "Print Receipt", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Print Receipt", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Send Digital Receipt via Email", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = emailInput,
                                        onValueChange = { emailInput = it },
                                        placeholder = { Text("customer@email.com") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Button(
                                        onClick = { emailSentMessage = true },
                                        enabled = emailInput.contains("@") && emailInput.contains(".")
                                    ) {
                                        Text("Send")
                                    }
                                }
                                if (emailSentMessage) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("✓ Receipt sent to $emailInput", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // New Sale Bottom Button
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = onNewSale,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start New Sale", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
    }
}
