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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.LoyverseGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockAlertDialog(
    lowStockProducts: List<Product>,
    currentThreshold: Int,
    onThresholdChange: (Int) -> Unit,
    onRestockProduct: (Product, Int) -> Unit,
    onRunInventoryCheck: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("All") } // "All", "Out of Stock", "Low Stock"

    val filteredProducts = remember(lowStockProducts, searchQuery, filterMode) {
        lowStockProducts.filter { prod ->
            val matchesSearch = searchQuery.isBlank() ||
                    prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.category.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (filterMode) {
                "Out of Stock" -> prod.stockLevel <= 0
                "Low Stock" -> prod.stockLevel in 1..currentThreshold
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val outOfStockCount = remember(lowStockProducts) {
        lowStockProducts.count { it.stockLevel <= 0 }
    }
    val lowStockCount = remember(lowStockProducts, currentThreshold) {
        lowStockProducts.count { it.stockLevel in 1..currentThreshold }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(8.dp)
            .testTag("low_stock_alert_dialog"),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Warning Badge
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
                            color = Color(0xFFFFF3E0),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Low Stock Warning",
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Inventory Stock Warning",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${lowStockProducts.size} item(s) require attention",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Threshold Selector Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Warning Threshold:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(3, 5, 10, 15, 20).forEach { thresholdVal ->
                                FilterChip(
                                    selected = currentThreshold == thresholdVal,
                                    onClick = { onThresholdChange(thresholdVal) },
                                    label = { Text("≤ $thresholdVal", fontSize = 11.sp) },
                                    modifier = Modifier.height(32.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter & Search Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search low stock items...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Quick status filter chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = filterMode == "All",
                            onClick = { filterMode = "All" },
                            label = { Text("All (${lowStockProducts.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = filterMode == "Out of Stock",
                            onClick = { filterMode = "Out of Stock" },
                            label = { Text("Out ($outOfStockCount)", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product List
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = LoyverseGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No items matching threshold (≤ $currentThreshold)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "All product inventory levels are healthy!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            LowStockProductRow(
                                product = product,
                                threshold = currentThreshold,
                                onRestock = { addedQty -> onRestockProduct(product, addedQty) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRunInventoryCheck,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Stock Levels", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = LoyverseGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LowStockProductRow(
    product: Product,
    threshold: Int,
    onRestock: (Int) -> Unit
) {
    val isOutOfStock = product.stockLevel <= 0
    val isLowStock = product.stockLevel in 1..threshold

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Product Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", product.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stock Badge
                Surface(
                    color = if (isOutOfStock) MaterialTheme.colorScheme.error else Color(0xFFE65100),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isOutOfStock) "OUT OF STOCK (0 left)" else "LOW STOCK (${product.stockLevel} left)",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Quick Restock Action Buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Quick Restock:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onRestock(5) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("+5", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { onRestock(20) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("+20", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onRestock(50) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LoyverseGreen),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("+50", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
