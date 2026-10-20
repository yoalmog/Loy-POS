package com.example.ui.kitchen

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.util.RecipeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable KDS (Kitchen Display System) UI Component (Fully Translated to Spanish)
 *
 * Pulls incoming orders from RecipeManager and displays them as live kitchen cards
 * with their respective preparation steps and ingredient lists (BOM).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenDisplayComponent(
    modifier: Modifier = Modifier,
    initialStationFilter: KitchenStation? = null,
    language: String = "es",
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val strings = com.example.util.AppStrings.get(language)
    LaunchedEffect(Unit) {
        RecipeManager.initialize(context)
    }

    val tickets by RecipeManager.kitchenTickets.collectAsState()
    var selectedStationFilter by remember { mutableStateOf(initialStationFilter) }
    var selectedStatusFilter by remember { mutableStateOf<KitchenTicketStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredTickets = remember(tickets, selectedStationFilter, selectedStatusFilter, searchQuery) {
        tickets.filter { ticket ->
            val matchesStatus = selectedStatusFilter == null || ticket.status == selectedStatusFilter
            val matchesStation = selectedStationFilter == null || ticket.items.any { item ->
                item.recipe?.prepGuide?.station == selectedStationFilter
            }
            val matchesSearch = searchQuery.isBlank() ||
                    ticket.orderNumber.toString().contains(searchQuery) ||
                    ticket.tableName.contains(searchQuery, ignoreCase = true) ||
                    ticket.items.any { it.name.contains(searchQuery, ignoreCase = true) }

            matchesStatus && matchesStation && matchesSearch
        }
    }

    // Ticket metrics
    val activeCount = tickets.count { it.status != KitchenTicketStatus.COMPLETED }
    val newCount = tickets.count { it.status == KitchenTicketStatus.NEW }
    val preparingCount = tickets.count { it.status == KitchenTicketStatus.PREPARING }
    val readyCount = tickets.count { it.status == KitchenTicketStatus.READY }
    val urgentCount = tickets.count {
        it.status != KitchenTicketStatus.COMPLETED &&
                ((System.currentTimeMillis() - it.timestamp) / (1000 * 60)) >= 15
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live KDS Stats & Toolbar (Fixed & Fully Responsive Top Bar)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                if (isWideScreen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.OutdoorGrill,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "הזמנות פעילות במטבח ($activeCount)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("$newCount חדשות", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                                    Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("$preparingCount בהכנה", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                                    Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("$readyCount מוכנות", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                                    if (urgentCount > 0) {
                                        Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("$urgentCount באיחור!", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { RecipeManager.simulateNewIncomingOrder(context) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("הדמה הזמנה", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            if (onNavigateBack != null) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.Default.Close, contentDescription = "סגור", tint = Color.White)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.OutdoorGrill,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "מטבח ($activeCount)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { RecipeManager.simulateNewIncomingOrder(context) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("הדמה", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                if (onNavigateBack != null) {
                                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "סגור", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$newCount חדשות", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                            Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("$preparingCount בהכנה", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                            Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("$readyCount מוכנות", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                            if (urgentCount > 0) {
                                Text("•", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("$urgentCount באיחור!", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Workstation Filter Bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedStationFilter == null,
                        onClick = { selectedStationFilter = null },
                        label = { Text("כל התחנות ($activeCount)", fontWeight = if (selectedStationFilter == null) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                items(KitchenStation.values()) { station ->
                    val stationCount = tickets.count { t ->
                        t.status != KitchenTicketStatus.COMPLETED &&
                                t.items.any { it.recipe?.prepGuide?.station == station }
                    }
                    FilterChip(
                        selected = selectedStationFilter == station,
                        onClick = {
                            selectedStationFilter = if (selectedStationFilter == station) null else station
                        },
                        label = { Text("${station.labelHebrew} ($stationCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color(0xFF0F172A),
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        )
                    )
                }
            }

            // Status Filter row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    null to "הכול (${filteredTickets.size})",
                    KitchenTicketStatus.NEW to "חדשות",
                    KitchenTicketStatus.PREPARING to "בהכנה",
                    KitchenTicketStatus.READY to "מוכנות",
                    KitchenTicketStatus.COMPLETED to "הושלמו"
                ).forEach { (status, label) ->
                    val isSelected = selectedStatusFilter == status
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B),
                        modifier = Modifier
                            .clickable { selectedStatusFilter = status }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Main Incoming Orders Display (Adaptive Grid or Column)
            if (filteredTickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SoupKitchen,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "אין הזמנות ממתינות בתחנה זו",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "הזמנות חדשות מהקופה או משלוחים יופיעו כאן באופן אוטומטי",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Button(
                            onClick = { RecipeManager.simulateNewIncomingOrder(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("הדמה הזמנת מטבח חדשה")
                        }
                    }
                }
            } else if (isWideScreen) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 380.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTickets, key = { it.id }) { ticket ->
                        KdsTicketCard(
                            ticket = ticket,
                            onUpdateStatus = { newStatus ->
                                RecipeManager.updateTicketStatus(context, ticket.id, newStatus)
                            },
                            onToggleItem = { itemId ->
                                RecipeManager.toggleItemCompleted(context, ticket.id, itemId)
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTickets, key = { it.id }) { ticket ->
                        KdsTicketCard(
                            ticket = ticket,
                            onUpdateStatus = { newStatus ->
                                RecipeManager.updateTicketStatus(context, ticket.id, newStatus)
                            },
                            onToggleItem = { itemId ->
                                RecipeManager.toggleItemCompleted(context, ticket.id, itemId)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual KDS Ticket Card displaying order header, timer,
 * list of ordered items, their respective preparation steps and ingredient lists (BOM).
 */
@Composable
fun KdsTicketCard(
    ticket: KitchenOrderTicket,
    onUpdateStatus: (KitchenTicketStatus) -> Unit,
    onToggleItem: (Long) -> Unit
) {
    val elapsedMinutes = remember(ticket.timestamp) {
        ((System.currentTimeMillis() - ticket.timestamp) / (1000 * 60)).coerceAtLeast(0)
    }

    val timerColor = when {
        elapsedMinutes >= 18 -> Color(0xFFEF4444) // Red urgent
        elapsedMinutes >= 10 -> Color(0xFFF59E0B) // Amber priority
        else -> Color(0xFF10B981) // Green on-time
    }

    // Default expanded so cooks see recipes & prep steps immediately
    var expandedItemIds by remember { mutableStateOf(ticket.items.map { it.id }.toSet()) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E293B),
        border = BorderStroke(
            1.5.dp,
            when (ticket.status) {
                KitchenTicketStatus.NEW -> Color(0xFF38BDF8)
                KitchenTicketStatus.PREPARING -> Color(0xFFF59E0B)
                KitchenTicketStatus.READY -> Color(0xFF10B981)
                KitchenTicketStatus.COMPLETED -> Color(0xFF475569)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Ticket Header
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
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF334155)
                    ) {
                        Text(
                            text = "#${ticket.orderNumber}",
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Column {
                        Text(
                            text = ticket.tableName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "מלצר: ${ticket.serverName} • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ticket.timestamp))}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Live Cooking Timer Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = timerColor.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, timerColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = timerColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${elapsedMinutes} דק'",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = timerColor
                            )
                        }
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(ticket.status.colorHex).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, Color(ticket.status.colorHex))
                    ) {
                        Text(
                            text = ticket.status.labelHebrew,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(ticket.status.colorHex),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ordered Items List with Preparation Steps and Ingredient Lists
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ticket.items.forEach { item ->
                    val isExpanded = expandedItemIds.contains(item.id)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (item.isCompleted) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFF334155).copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            if (item.isCompleted) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF475569)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Item row title & checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = item.isCompleted,
                                        onCheckedChange = { onToggleItem(item.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF10B981),
                                            uncheckedColor = Color(0xFF94A3B8)
                                        )
                                    )

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${item.quantity}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (item.isCompleted) Color(0xFF94A3B8) else Color.White
                                        )
                                        item.recipe?.prepGuide?.let { guide ->
                                            Text(
                                                text = "תחנת הכנה: ${guide.station.labelHebrew} • זמן הכנה: ${guide.prepTimeMinutes} דק'",
                                                fontSize = 11.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                        if (item.notes.isNotBlank()) {
                                            Text(
                                                text = "הערות: ${item.notes}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFF59E0B)
                                            )
                                        }
                                    }
                                }

                                // Toggle Recipe and Prep Guide Button
                                if (item.recipe != null) {
                                    TextButton(
                                        onClick = {
                                            expandedItemIds = if (isExpanded) {
                                                expandedItemIds - item.id
                                            } else {
                                                expandedItemIds + item.id
                                            }
                                        }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                if (isExpanded) "הסתר מתכון" else "הצג מתכון ושלבים",
                                                fontSize = 11.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Expanded Culinary Specification (Respective Steps & BOM Ingredients)
                            AnimatedVisibility(visible = isExpanded && item.recipe != null) {
                                item.recipe?.let { r ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0F172A))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Scaled BOM Ingredients List
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "חומרי גלם (BOM) (${item.quantity} מנות):",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFFF1F5F9)
                                            )
                                            Text(
                                                text = "מתכון מקצועי",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }

                                        r.ingredients.forEach { ing ->
                                            val totalQty = ing.quantity * item.quantity
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("• ${ing.name}", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                                Text(
                                                    "${if (totalQty % 1.0 == 0.0) totalQty.toInt() else totalQty} ${ing.unit}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF38BDF8)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                                        // 2. Respective Kitchen Preparation Steps
                                        if (r.prepGuide.steps.isNotEmpty()) {
                                            Text(
                                                text = "שלבי הכנה:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFFF1F5F9)
                                            )
                                            r.prepGuide.steps.forEachIndexed { sIdx, step ->
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                        modifier = Modifier.size(18.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                "${sIdx + 1}",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 10.sp,
                                                                color = Color(0xFFF59E0B)
                                                            )
                                                        }
                                                    }
                                                    Text(step, fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                                }
                                            }
                                        }

                                        // 3. Allergens and Chef Notes
                                        if (r.prepGuide.allergens.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("אלרגנים:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                                Text(r.prepGuide.allergens.joinToString(", "), fontSize = 11.sp, color = Color(0xFFFCA5A5))
                                            }
                                        }

                                        if (r.prepGuide.chefNotes.isNotBlank()) {
                                            Text(
                                                "הערות השף: ${r.prepGuide.chefNotes}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFFCD34D)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (ticket.status == KitchenTicketStatus.NEW) {
                    Button(
                        onClick = { onUpdateStatus(KitchenTicketStatus.PREPARING) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OutdoorGrill, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("התחל הכנה", fontWeight = FontWeight.Bold)
                    }
                } else if (ticket.status == KitchenTicketStatus.PREPARING) {
                    Button(
                        onClick = { onUpdateStatus(KitchenTicketStatus.READY) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("הזמנה מוכנה!", fontWeight = FontWeight.Bold)
                    }
                } else if (ticket.status == KitchenTicketStatus.READY) {
                    Button(
                        onClick = { onUpdateStatus(KitchenTicketStatus.COMPLETED) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("נמסרה / הושלמה", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
