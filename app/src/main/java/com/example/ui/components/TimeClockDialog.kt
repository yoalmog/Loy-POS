package com.example.ui.components

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Timecard
import com.example.ui.theme.LoyverseGreen
import com.example.ui.theme.LoyverseGreenDark
import com.example.ui.theme.LoyverseMintContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeClockDialog(
    currentEmployeeName: String,
    timecards: List<Timecard>,
    onDismiss: () -> Unit,
    onClockIn: (employeeName: String, callback: (Timecard) -> Unit) -> Unit,
    onClockOut: (employeeName: String, callback: (Timecard?) -> Unit) -> Unit
) {
    var selectedEmployee by remember { mutableStateOf(currentEmployeeName) }
    val employeeOptions = listOf("Alex (Cashier)", "Sarah (Manager)", "Marcus (Supervisor)")

    val activeSession = remember(timecards, selectedEmployee) {
        timecards.firstOrNull { it.employeeName == selectedEmployee && it.isClockedIn }
    }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }

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
                // Header
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = LoyverseGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Employee Time Clock", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Employee Selector
                    item {
                        Text("Select Employee Session:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            employeeOptions.forEach { emp ->
                                val isSelected = emp == selectedEmployee
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedEmployee = emp
                                        feedbackMessage = null
                                    },
                                    label = { Text(emp, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LoyverseMintContainer,
                                        selectedLabelColor = LoyverseGreenDark
                                    )
                                )
                            }
                        }
                    }

                    // Current Clock In Status Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeSession != null) LoyverseMintContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (activeSession != null) LoyverseGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            if (activeSession != null) Icons.Default.PunchClock else Icons.Default.TimerOff,
                                            contentDescription = null,
                                            tint = if (activeSession != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = selectedEmployee,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (activeSession != null) {
                                        val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(activeSession.clockInTime))
                                        "Currently CLOCKED IN (since $timeStr)"
                                    } else "Currently CLOCKED OUT",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (activeSession != null) LoyverseGreenDark else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Clock In / Clock Out Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onClockIn(selectedEmployee) { tc ->
                                                val tStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(tc.clockInTime))
                                                feedbackMessage = "✓ $selectedEmployee clocked in at $tStr"
                                            }
                                        },
                                        enabled = activeSession == null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("clock_in_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = LoyverseGreen)
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CLOCK IN", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            onClockOut(selectedEmployee) { tc ->
                                                if (tc != null) {
                                                    val tStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(tc.clockOutTime ?: System.currentTimeMillis()))
                                                    feedbackMessage = "✓ $selectedEmployee clocked out at $tStr (${tc.totalHours} hrs worked)"
                                                }
                                            }
                                        },
                                        enabled = activeSession != null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("clock_out_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("CLOCK OUT", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (feedbackMessage != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = LoyverseMintContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = feedbackMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LoyverseGreenDark,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Recent Timecards Table
                    item {
                        Text("Recent Timecard History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    val history = timecards.filter { it.employeeName == selectedEmployee }
                    if (history.isEmpty()) {
                        item {
                            Text("No timecard punches recorded for $selectedEmployee", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(history) { tc ->
                            val inStr = SimpleDateFormat("MMM dd, h:mm a", Locale.US).format(Date(tc.clockInTime))
                            val outStr = tc.clockOutTime?.let { SimpleDateFormat("h:mm a", Locale.US).format(Date(it)) } ?: "Active Session"

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("In: $inStr", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text("Out: $outStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Surface(
                                        color = if (tc.isClockedIn) LoyverseMintContainer else MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (tc.isClockedIn) "Active" else "${tc.totalHours} hrs",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (tc.isClockedIn) LoyverseGreenDark else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
}
