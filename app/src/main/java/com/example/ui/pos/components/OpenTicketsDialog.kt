package com.example.ui.pos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TicketEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenTicketsDialog(
    openTickets: List<TicketEntity>,
    onDismiss: () -> Unit,
    onSelectTicket: (TicketEntity) -> Unit,
    onMergeTickets: (targetId: Long, tickets: List<TicketEntity>) -> Unit,
    onVoidTicket: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isMergeMode by remember { mutableStateOf(false) }
    val selectedTicketIds = remember { mutableStateListOf<Long>() }

    val filteredTickets = remember(openTickets, searchQuery) {
        if (searchQuery.isBlank()) openTickets
        else openTickets.filter {
            it.ticketName.contains(searchQuery, ignoreCase = true) ||
                    it.tableName.contains(searchQuery, ignoreCase = true) ||
                    it.serverName.contains(searchQuery, ignoreCase = true)
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
                    title = {
                        Text(
                            text = if (isMergeMode) "Select Tickets to Merge" else "Open Tickets (${openTickets.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (!isMergeMode && openTickets.size >= 2) {
                            TextButton(onClick = { isMergeMode = true }) {
                                Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Merge")
                            }
                        } else if (isMergeMode) {
                            TextButton(onClick = {
                                isMergeMode = false
                                selectedTicketIds.clear()
                            }) {
                                Text("Cancel")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by table, name or server...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (filteredTickets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No open tickets found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTickets) { ticket ->
                            val isSelectedForMerge = selectedTicketIds.contains(ticket.id)
                            val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(ticket.createdAt))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isSelectedForMerge) 2.dp else 1.dp,
                                        color = if (isSelectedForMerge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (isMergeMode) {
                                            if (isSelectedForMerge) selectedTicketIds.remove(ticket.id)
                                            else selectedTicketIds.add(ticket.id)
                                        } else {
                                            onSelectTicket(ticket)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelectedForMerge) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (isMergeMode) {
                                        Checkbox(
                                            checked = isSelectedForMerge,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedTicketIds.add(ticket.id)
                                                else selectedTicketIds.remove(ticket.id)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = ticket.ticketName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (ticket.tableName.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = ticket.tableName,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Created $timeStr • Server: ${ticket.serverName} • ${ticket.diningOption.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (ticket.customerName != null) {
                                            Text(
                                                text = "Customer: ${ticket.customerName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    if (!isMergeMode) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onVoidTicket(ticket.id) }) {
                                                Icon(
                                                    Icons.Default.DeleteOutline,
                                                    contentDescription = "Void Ticket",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // If in merge mode, bottom merge confirmation bar
                if (isMergeMode && selectedTicketIds.size >= 2) {
                    Surface(
                        tonalElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedTicketIds.size} tickets selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = {
                                    val targetId = selectedTicketIds.first()
                                    val ticketsToMerge = openTickets.filter { it.id in selectedTicketIds }
                                    onMergeTickets(targetId, ticketsToMerge)
                                    isMergeMode = false
                                    selectedTicketIds.clear()
                                }
                            ) {
                                Text("Combine into One")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SaveTicketDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (ticketName: String, tableName: String) -> Unit
) {
    var ticketName by remember { mutableStateOf(initialName) }
    var selectedTable by remember { mutableStateOf("") }
    val predefinedTables = listOf("Table 1", "Table 2", "Table 3", "Table 4", "Table 5", "Table 6", "Patio 1", "Patio 2", "Bar 1", "Bar 2")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Open Ticket", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ticketName,
                    onValueChange = { ticketName = it },
                    label = { Text("Ticket Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Assign Predefined Table (Optional):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(160.dp)
                ) {
                    items(predefinedTables) { table ->
                        val isSelected = selectedTable == table
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedTable = if (isSelected) "" else table
                                    if (selectedTable.isNotBlank()) {
                                        ticketName = selectedTable
                                    }
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = table,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(ticketName, selectedTable) },
                enabled = ticketName.isNotBlank()
            ) {
                Text("Save Ticket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
