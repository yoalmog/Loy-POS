package com.example.ui.pos.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.OcrEngineSettings
import com.example.data.model.OcrEngineType
import com.example.data.model.ParsedOcrMenuItem
import com.example.util.OcrMenuParser
import com.example.util.PaddleOcrClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScannerDialog(
    onDismiss: () -> Unit,
    onAddItemsToCart: (List<ParsedOcrMenuItem>) -> Unit,
    onImportToCatalog: ((List<ParsedOcrMenuItem>) -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var ocrSettings by remember { mutableStateOf(OcrEngineSettings()) }
    var isProcessing by remember { mutableStateOf(false) }
    var currentEngine by remember { mutableStateOf(OcrEngineType.PADDLE_OCR) }
    var latencyMs by remember { mutableLongStateOf(0L) }
    var warningMessage by remember { mutableStateOf<String?>(null) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<String?>(null) }

    var scannedItems by remember { mutableStateOf<List<ParsedOcrMenuItem>>(emptyList()) }

    fun runScan(bitmap: Bitmap) {
        coroutineScope.launch {
            isProcessing = true
            warningMessage = null
            connectionStatus = null

            val result = PaddleOcrClient.scanBitmap(bitmap, ocrSettings)
            latencyMs = result.latencyMs
            currentEngine = result.engine
            warningMessage = result.warning

            // Parse lines into structured items
            val rawLines = result.lines.map { it.text }
            val parsed = OcrMenuParser.parseLines(rawLines)
            scannedItems = parsed

            isProcessing = false
        }
    }

    fun generateSampleMenuBitmap(): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val paint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 28f
            isAntiAlias = true
        }

        val lines = OcrMenuParser.getSampleMenuText()
        var y = 60f
        for (line in lines) {
            canvas.drawText(line, 40f, y, paint)
            y += 42f
        }
        return bitmap
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
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
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "OCR Scanner",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Escáner OCR de Menús y Boletas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentEngine == OcrEngineType.PADDLE_OCR)
                                        Color(0xFFE0F2FE) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = currentEngine.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentEngine == OcrEngineType.PADDLE_OCR)
                                            Color(0xFF0369A1) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (latencyMs > 0) {
                                    Text(
                                        text = "${latencyMs}ms",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showSettingsModal = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Configurar OCR",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Warning / Fallback Notice
                warningMessage?.let { warn ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = warn,
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                // Action Bar: Scan Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val demoBitmap = generateSampleMenuBitmap()
                            runScan(demoBitmap)
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reconociendo...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escanear Menú Demo")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                connectionStatus = "Probando conexión con ${ocrSettings.endpointUrl}..."
                                val (ok, latency) = PaddleOcrClient.testConnection(ocrSettings.endpointUrl)
                                connectionStatus = if (ok) {
                                    "PaddleOCR conectado con éxito (${latency}ms)"
                                } else {
                                    "Servidor PaddleOCR inaccesible. Modo respaldo activo."
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lan,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Probar Red")
                    }
                }

                connectionStatus?.let { status ->
                    Text(
                        text = status,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Results Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Platos y Precios Detectados (${scannedItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (scannedItems.isNotEmpty()) {
                        val allSelected = scannedItems.all { it.isSelected }
                        TextButton(
                            onClick = {
                                scannedItems = scannedItems.map { it.copy(isSelected = !allSelected) }
                            }
                        ) {
                            Text(if (allSelected) "Deseleccionar todos" else "Seleccionar todos")
                        }
                    }
                }

                // Items List
                if (scannedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusWeak,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(52.dp)
                            )
                            Text(
                                text = "Presione 'Escanear Menú Demo' o configure su servidor PaddleOCR",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "El motor PaddleOCR detectará platos, precios y categorías automáticamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(scannedItems, key = { it.id }) { item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    if (item.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scannedItems = scannedItems.map {
                                            if (it.id == item.id) it.copy(isSelected = !it.isSelected)
                                            else it
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { checked ->
                                            scannedItems = scannedItems.map {
                                                if (it.id == item.id) it.copy(isSelected = checked)
                                                else it
                                            }
                                        }
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (item.description.isNotBlank()) {
                                            Text(
                                                text = item.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "S/ ${"%.2f".format(item.price)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${(item.confidence * 100).toInt()}% conf.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                val selectedCount = scannedItems.count { it.isSelected }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val accepted = scannedItems.filter { it.isSelected }
                            if (accepted.isNotEmpty()) {
                                onAddItemsToCart(accepted)
                                onDismiss()
                            }
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier.weight(0.6f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar al Ticket ($selectedCount)")
                    }
                }
            }
        }
    }

    // Settings Modal
    if (showSettingsModal) {
        var tempUrl by remember { mutableStateOf(ocrSettings.endpointUrl) }
        var tempLang by remember { mutableStateOf(ocrSettings.language) }

        AlertDialog(
            onDismissRequest = { showSettingsModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                    Text("Configuración de PaddleOCR")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingrese la URL del endpoint del microservicio PaddleOCR (FastAPI / PP-OCR / HubServing):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("URL del Servidor PaddleOCR") },
                        placeholder = { Text("http://192.168.1.100:8866/predict/ocr_system") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempLang,
                        onValueChange = { tempLang = it },
                        label = { Text("Modelos de Idioma") },
                        placeholder = { Text("spa+eng") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "• Si el servidor no responde o no está disponible, el sistema cambia automáticamente al analizador heurístico inteligente.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ocrSettings = ocrSettings.copy(
                            endpointUrl = tempUrl.trim(),
                            language = tempLang.trim()
                        )
                        showSettingsModal = false
                    }
                ) {
                    Text("Guardar Configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsModal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
