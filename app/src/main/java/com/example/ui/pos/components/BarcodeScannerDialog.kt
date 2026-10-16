package com.example.ui.pos.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    var manualBarcodeInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scan Barcode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Simulated Camera Viewfinder
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E2320))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(100.dp)
                    )

                    // Animated scan laser
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = (laserOffset - 90).dp)
                            .background(Color(0xFF00E676))
                    )

                    Text(
                        text = "Point camera at barcode",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = "Quick Demo Scans (Click to simulate):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onBarcodeScanned("200001001000") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Bistro Burger", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onBarcodeScanned("021008003500") }, // 350 grams roast beef embedded
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Beef 350g (Weight)", fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { onBarcodeScanned("LOY8801") }, // Sarah Connor loyalty card
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("Loyalty Card", fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Manual input field
                OutlinedTextField(
                    value = manualBarcodeInput,
                    onValueChange = { manualBarcodeInput = it },
                    label = { Text("Or Type Barcode / SKU") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = {
                        if (manualBarcodeInput.isNotBlank()) {
                            onBarcodeScanned(manualBarcodeInput.trim())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = manualBarcodeInput.isNotBlank()
                ) {
                    Text("Submit Barcode")
                }
            }
        }
    }
}
