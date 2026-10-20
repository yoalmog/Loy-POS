package com.example.ui.backoffice.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.util.RecipeManager

val ALLERGEN_OPTIONS = listOf(
    "גלוטן", "חלב / לקטוז", "ביצים", "בוטנים ואגוזים", "דגים", "פירות ים", "סויה", "שומשום"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeEditorDialog(
    recipe: DishRecipe,
    onDismiss: () -> Unit,
    onSave: (DishRecipe) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // Ingredients state
    var ingredients by remember { mutableStateOf(recipe.ingredients) }
    var newIngName by remember { mutableStateOf("") }
    var newIngQty by remember { mutableStateOf("") }
    var newIngUnit by remember { mutableStateOf("גרם") }
    var newIngCost by remember { mutableStateOf("") }
    var showAddIngForm by remember { mutableStateOf(false) }

    // Kitchen guide state
    var prepTime by remember { mutableStateOf(recipe.prepGuide.prepTimeMinutes.toString()) }
    var selectedStation by remember { mutableStateOf(recipe.prepGuide.station) }
    var steps by remember { mutableStateOf(recipe.prepGuide.steps) }
    var newStepText by remember { mutableStateOf("") }
    var selectedAllergens by remember { mutableStateOf(recipe.prepGuide.allergens.toSet()) }
    var chefNotes by remember { mutableStateOf(recipe.prepGuide.chefNotes) }
    var platingGuide by remember { mutableStateOf(recipe.prepGuide.platingGuide) }

    val totalCost = remember(ingredients) {
        ingredients.sumOf { it.quantity * it.costPerUnit }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "מתכון ואופן הכנה: ${recipe.itemName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = recipe.categoryName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text("•", color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = "עלות מזון משוערת: S/ ${"%.2f".format(totalCost)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "סגור")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    indicator = {},
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("חומרי גלם ומצרכים (BOM)", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(imageVector = Icons.Default.OutdoorGrill, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("אופן הכנה למטבח (Chef Guide)", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Viewport
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // TAB 0: Ingredients (BOM)
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "רשימת חומרי גלם וכמויות למנה אחת (${ingredients.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Button(
                                    onClick = { showAddIngForm = !showAddIngForm },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (showAddIngForm) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showAddIngForm) Icons.Default.Close else Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showAddIngForm) "סגור טופס" else "הוסף חומר גלם")
                                }
                            }

                            // Add Ingredient Form Card
                            AnimatedVisibility(visible = showAddIngForm) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("הוספת מצרך חדש למתכון", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = newIngName,
                                                onValueChange = { newIngName = it },
                                                label = { Text("שם חומר הגלם") },
                                                placeholder = { Text("למשל: בשר אנטרקוט, גבינת מוצרלה") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1.5f)
                                            )

                                            OutlinedTextField(
                                                value = newIngQty,
                                                onValueChange = { newIngQty = it },
                                                label = { Text("כמות") },
                                                placeholder = { Text("200") },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.7f)
                                            )

                                            OutlinedTextField(
                                                value = newIngUnit,
                                                onValueChange = { newIngUnit = it },
                                                label = { Text("יחידה") },
                                                placeholder = { Text("גרם / מ\"ל / יח'") },
                                                singleLine = true,
                                                modifier = Modifier.weight(0.8f)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            OutlinedTextField(
                                                value = newIngCost,
                                                onValueChange = { newIngCost = it },
                                                label = { Text("עלות ליחידה (₪ / S/)") },
                                                placeholder = { Text("0.05") },
                                                singleLine = true,
                                                modifier = Modifier.width(180.dp)
                                            )

                                            Button(
                                                onClick = {
                                                    val qty = newIngQty.toDoubleOrNull() ?: 0.0
                                                    val cost = newIngCost.toDoubleOrNull() ?: 0.0
                                                    if (newIngName.isNotBlank() && qty > 0) {
                                                        ingredients = ingredients + RecipeIngredient(
                                                            name = newIngName.trim(),
                                                            quantity = qty,
                                                            unit = newIngUnit.trim(),
                                                            costPerUnit = cost,
                                                            deductStock = true
                                                        )
                                                        newIngName = ""
                                                        newIngQty = ""
                                                        newIngCost = ""
                                                        showAddIngForm = false
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("הוסף למתכון")
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Ingredients List
                            if (ingredients.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(imageVector = Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                        Text("אין חומרי גלם מוגדרים עדיין למנה זו", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(ingredients, key = { _, ing -> ing.id }) { index, ing ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "${index + 1}",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                    }
                                                    Column {
                                                        Text(text = ing.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text(
                                                                text = "${ing.quantity} ${ing.unit}",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                            if (ing.costPerUnit > 0) {
                                                                Text(
                                                                    text = "עלות: S/ ${"%.2f".format(ing.quantity * ing.costPerUnit)}",
                                                                    fontSize = 12.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable {
                                                            ingredients = ingredients.map {
                                                                if (it.id == ing.id) it.copy(deductStock = !it.deductStock) else it
                                                            }
                                                        }
                                                    ) {
                                                        Checkbox(
                                                            checked = ing.deductStock,
                                                            onCheckedChange = { checked ->
                                                                ingredients = ingredients.map {
                                                                    if (it.id == ing.id) it.copy(deductStock = checked) else it
                                                                }
                                                            }
                                                        )
                                                        Text("גרע מלאי", fontSize = 11.sp)
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            ingredients = ingredients.filter { it.id != ing.id }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "מחק",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 1: Kitchen Prep Guide
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Section 1: Station & Prep Time
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("תחנת עבודה וזמני ביצוע", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = prepTime,
                                                onValueChange = { prepTime = it },
                                                label = { Text("זמן הכנה (בדקות)") },
                                                singleLine = true,
                                                modifier = Modifier.width(140.dp)
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("תחנת מטבח:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    KitchenStation.values().take(4).forEach { station ->
                                                        FilterChip(
                                                            selected = selectedStation == station,
                                                            onClick = { selectedStation = station },
                                                            label = { Text(station.labelHebrew, fontSize = 11.sp) }
                                                        )
                                                    }
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    KitchenStation.values().drop(4).forEach { station ->
                                                        FilterChip(
                                                            selected = selectedStation == station,
                                                            onClick = { selectedStation = station },
                                                            label = { Text(station.labelHebrew, fontSize = 11.sp) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Section 2: Step-by-Step Cooking Steps
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "שלבי בישול והוראות הכנה מדויקות (${steps.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = newStepText,
                                            onValueChange = { newStepText = it },
                                            placeholder = { Text("הזן שלב בישול חדש (למשל: צריבת הבקר בווק 90 שניות...)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                if (newStepText.isNotBlank()) {
                                                    steps = steps + newStepText.trim()
                                                    newStepText = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("הוסף שלב")
                                        }
                                    }
                                }
                            }

                            itemsIndexed(steps) { idx, step ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(text = step, fontSize = 13.sp)
                                        }

                                        IconButton(
                                            onClick = {
                                                steps = steps.filterIndexed { i, _ -> i != idx }
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "מחק", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            // Section 3: Allergens
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("אלרגנים במנה:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ALLERGEN_OPTIONS.forEach { allergen ->
                                            val isSelected = selectedAllergens.contains(allergen)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    selectedAllergens = if (isSelected) {
                                                        selectedAllergens - allergen
                                                    } else {
                                                        selectedAllergens + allergen
                                                    }
                                                },
                                                label = { Text(allergen, fontSize = 11.sp) },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }

                            // Section 4: Chef Notes & Plating
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = chefNotes,
                                        onValueChange = { chefNotes = it },
                                        label = { Text("הערות שף ודגשי ביצוע למטבח") },
                                        placeholder = { Text("למשל: לא לשרוף את השום, לוודא שהבטטה מזוגגת חמה...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )

                                    OutlinedTextField(
                                        value = platingGuide,
                                        onValueChange = { platingGuide = it },
                                        label = { Text("הנחיות צלחות והגשה (Plating)") },
                                        placeholder = { Text("למשל: צלחת קרמיקה כחולה, ערימת תירס קלוי מעל...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ביטול")
                    }

                    Button(
                        onClick = {
                            val updatedRecipe = recipe.copy(
                                ingredients = ingredients,
                                prepGuide = KitchenPrepGuide(
                                    prepTimeMinutes = prepTime.toIntOrNull() ?: 10,
                                    station = selectedStation,
                                    steps = steps,
                                    allergens = selectedAllergens.toList(),
                                    chefNotes = chefNotes.trim(),
                                    platingGuide = platingGuide.trim()
                                )
                            )
                            RecipeManager.saveRecipe(context, updatedRecipe)
                            onSave(updatedRecipe)
                            onDismiss()
                        },
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("שמור מתכון ואופן הכנה")
                    }
                }
            }
        }
    }
}
