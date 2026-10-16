package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.*
import java.util.Locale

/**
 * A Jetpack Compose component that displays an inventory grid of Products
 * with dynamic category filtering, search, and stock status indicators,
 * styled in the signature Loyverse emerald green theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductInventoryGrid(
    products: List<Product>,
    modifier: Modifier = Modifier,
    selectedCategory: String? = null,
    onCategorySelected: (String?) -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    onQuickStockAdjust: ((Product, Int) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }

    // Distinct categories extracted from products
    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct().sorted()
    }

    val activeCategory = selectedCategory ?: "All"

    // Filtered products by category and search
    val filteredProducts = remember(products, activeCategory, searchQuery) {
        products.filter { product ->
            val matchesCategory = (activeCategory == "All") || product.category.equals(activeCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search Header with Loyverse Green Accent
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search inventory products...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = LoyverseGreen
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LoyverseGreen,
                        focusedLabelColor = LoyverseGreen,
                        cursorColor = LoyverseGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_search_field")
                )
            }
        }

        // Category Filter Row with Loyverse Green Styling
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = (category == activeCategory)

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onCategorySelected(if (category == "All") null else category)
                    },
                    label = {
                        Text(
                            text = category,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                when (category.lowercase()) {
                                    "burgers" -> Icons.Default.Fastfood
                                    "combos" -> Icons.Default.LunchDining
                                    "sides" -> Icons.Default.DinnerDining
                                    "drinks" -> Icons.Default.LocalDrink
                                    "desserts" -> Icons.Default.Cake
                                    else -> Icons.Default.Category
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = LoyverseGreen
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LoyverseMintContainer,
                        selectedLabelColor = LoyverseGreenDark,
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) LoyverseGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        selectedBorderColor = LoyverseGreen,
                        borderWidth = if (isSelected) 1.5.dp else 1.dp
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        // Product Count & Summary Line
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredProducts.size} Products found",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val lowStockCount = filteredProducts.count { it.stockLevel in 1..5 }
            val outOfStockCount = filteredProducts.count { it.stockLevel <= 0 }

            if (outOfStockCount > 0 || lowStockCount > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (lowStockCount > 0) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$lowStockCount low",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (outOfStockCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "$outOfStockCount empty",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Product Grid Content
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No products found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try clearing the search or category filter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 155.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        onClick = { onProductClick(product) },
                        onQuickStockAdjust = onQuickStockAdjust
                    )
                }
            }
        }
    }
}

/**
 * Individual Product Card within the Grid
 */
@Composable
fun ProductGridCard(
    product: Product,
    onClick: () -> Unit,
    onQuickStockAdjust: ((Product, Int) -> Unit)? = null
) {
    val isOutOfStock = product.stockLevel <= 0
    val isLowStock = product.stockLevel in 1..5

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(205.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Product Visual / Header Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LoyverseGreen.copy(alpha = 0.15f),
                                LoyverseGreenLight.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Category Icon Avatar
                Surface(
                    shape = CircleShape,
                    color = LoyverseGreen.copy(alpha = 0.2f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (product.category.lowercase()) {
                                "burgers" -> Icons.Default.Fastfood
                                "combos" -> Icons.Default.LunchDining
                                "sides" -> Icons.Default.DinnerDining
                                "drinks" -> Icons.Default.LocalDrink
                                "desserts" -> Icons.Default.Cake
                                else -> Icons.Default.RestaurantMenu
                            },
                            contentDescription = product.name,
                            tint = LoyverseGreen,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Category Tag Top-Left
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 14.dp, bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = product.category,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Stock Level Badge Top-Right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    val badgeColor = when {
                        isOutOfStock -> MaterialTheme.colorScheme.errorContainer
                        isLowStock -> Color(0xFFFFF3E0)
                        else -> LoyverseMintContainer
                    }
                    val textColor = when {
                        isOutOfStock -> MaterialTheme.colorScheme.error
                        isLowStock -> Color(0xFFE65100)
                        else -> LoyverseGreenDark
                    }
                    val textLabel = when {
                        isOutOfStock -> "0 In Stock"
                        isLowStock -> "${product.stockLevel} Left"
                        else -> "${product.stockLevel} In Stock"
                    }

                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = textLabel,
                            color = textColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Product Details Area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Name
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Price and Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", product.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = LoyverseGreen
                    )

                    if (onQuickStockAdjust != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconButton(
                                onClick = { onQuickStockAdjust(product, -1) },
                                modifier = Modifier.size(24.dp),
                                enabled = product.stockLevel > 0,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease Stock", modifier = Modifier.size(12.dp))
                            }
                            FilledIconButton(
                                onClick = { onQuickStockAdjust(product, 1) },
                                modifier = Modifier.size(24.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = LoyverseGreen
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase Stock", modifier = Modifier.size(12.dp), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
