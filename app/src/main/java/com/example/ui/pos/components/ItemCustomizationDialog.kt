package com.example.ui.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ItemEntity
import com.example.data.model.ModifierGroupEntity
import com.example.data.model.ModifierOption
import com.example.data.model.SoldBy
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCustomizationDialog(
    item: ItemEntity,
    availableModifierGroups: List<ModifierGroupEntity>,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, modifiersText: String, modifiersCost: Double, comment: String) -> Unit
) {
    var quantity by remember { mutableStateOf(if (item.soldBy == SoldBy.WEIGHT) 0.50 else 1.0) }
    var weightInputString by remember { mutableStateOf("0.50") }
    var comment by remember { mutableStateOf("") }

    // Parse item modifier groups
    val relevantGroupIds = remember(item.modifierIds) {
        if (item.modifierIds.isBlank()) emptySet()
        else item.modifierIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }
    val relevantGroups = remember(availableModifierGroups, relevantGroupIds) {
        availableModifierGroups.filter { it.id in relevantGroupIds }
    }

    // Selected options: map from group to set of selected options
    val selectedOptions = remember { mutableStateMapOf<String, MutableSet<ModifierOption>>() }

    // Calculate total modifier cost
    val totalModifiersCost = remember(selectedOptions.values.toList()) {
        selectedOptions.values.flatten().sumOf { it.price }
    }

    val unitPriceWithModifiers = item.price + totalModifiersCost
    val calculatedTotal = unitPriceWithModifiers * quantity

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
                // Dialog Header
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${item.categoryName} • $${String.format(Locale.US, "%.2f", item.price)} ${if (item.soldBy == SoldBy.WEIGHT) "/ kg" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sold by Weight Keypad Section
                    if (item.soldBy == SoldBy.WEIGHT) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Weight Scale Entry (kg)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "$weightInputString kg",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    // Quick weight chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("0.25", "0.50", "0.75", "1.00", "1.50").forEach { preset ->
                                            OutlinedButton(
                                                onClick = {
                                                    weightInputString = preset
                                                    quantity = preset.toDoubleOrNull() ?: 1.0
                                                },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 2.dp)
                                            ) {
                                                Text(preset, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Quantity Stepper
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Quantity",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FilledIconButton(
                                            onClick = { if (quantity > 1.0) quantity -= 1.0 },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                        }
                                        Text(
                                            text = quantity.toInt().toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        FilledIconButton(
                                            onClick = { quantity += 1.0 },
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Increase")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Modifier Groups
                    relevantGroups.forEach { group ->
                        item {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Parse options e.g. "Cheddar:1.00,Bacon:1.75"
                        val options = group.optionsJson.split(",").mapNotNull { part ->
                            val tokens = part.split(":")
                            if (tokens.size == 2) {
                                ModifierOption(tokens[0].trim(), tokens[1].trim().toDoubleOrNull() ?: 0.0)
                            } else null
                        }

                        items(options) { opt ->
                            val currentSet = selectedOptions.getOrPut(group.name) { mutableSetOf() }
                            val isSelected = currentSet.any { it.name == opt.name }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        val updatedSet = currentSet.toMutableSet()
                                        if (isSelected) {
                                            updatedSet.removeAll { it.name == opt.name }
                                        } else {
                                            updatedSet.add(opt)
                                        }
                                        selectedOptions[group.name] = updatedSet
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = opt.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = if (opt.price > 0) "+$${String.format(Locale.US, "%.2f", opt.price)}" else "Free",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (opt.price > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Special Instructions / Comment
                    item {
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Special Instructions / Kitchen Note") },
                            placeholder = { Text("e.g. No onions, extra crispy, sauce on side") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            maxLines = 2
                        )
                    }
                }

                // Bottom Action Bar
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Item Cost",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", calculatedTotal)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                val allChosen = selectedOptions.values.flatten()
                                val modifiersText = allChosen.joinToString(", ") {
                                    if (it.price > 0) "${it.name} (+$${String.format(Locale.US, "%.2f", it.price)})"
                                    else it.name
                                }
                                onConfirm(quantity, modifiersText, totalModifiersCost, comment)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Add to Ticket", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
