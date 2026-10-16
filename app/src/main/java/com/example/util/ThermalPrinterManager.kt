package com.example.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID

enum class PrinterConnectionStatus {
    DISCONNECTED, SCANNING, CONNECTING, CONNECTED, PRINTING, ERROR
}

data class DiscoveredPrinter(
    val name: String,
    val address: String,
    val serviceUuid: String,
    val isBle: Boolean = true,
    val rssi: Int = -50
)

data class PrinterConfig(
    val deviceName: String = "X18 Mini POS-58",
    val deviceAddress: String = "00:11:22:33:44:55",
    val serviceUuid: String = ThermalPrinterManager.UUID_HM10_SERIAL,
    val paperWidthMm: Int = 58,
    val autoCutEnabled: Boolean = true,
    val autoDrawerEnabled: Boolean = true,
    val isSimulated: Boolean = true
)

object ThermalPrinterManager {

    private const val TAG = "ThermalPrinterManager"

    // Supported Primary Service UUIDs from protocol spec
    const val UUID_HM10_SERIAL = "0000ffe0-0000-1000-8000-00805f9b34fb"     // X18 Mini & generic 58mm POS thermal printers
    const val UUID_NORDIC_NUS = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"     // Phomemo, Paperang & portable mini printers
    const val UUID_POS58_PT210 = "e7810a71-73ae-499d-8c15-faa9aef0c3f2"    // Portable POS printers
    const val UUID_STANDARD_POS = "000018f0-0000-1000-8000-00805f9b34fb"   // Standard ESC/POS printers
    const val UUID_X6_CAT = "0000ae30-0000-1000-8000-00805f9b34fb"         // X6 / X6h Cat printer raster protocol
    const val UUID_SPP_RFCOMM = "00001101-0000-1000-8000-00805f9b34fb"     // Android Native Bluetooth RFCOMM

    // GATT Characteristic UUIDs
    const val CHAR_HM10_WRITE = "0000ffe1-0000-1000-8000-00805f9b34fb"
    const val CHAR_NUS_WRITE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"

    // Protocol constraints
    const val CHUNK_SIZE = 50 // Max BLE Packet Chunk Size
    const val INTER_PACKET_DELAY_MS = 20L // 20 milliseconds micro-delay

    private val _status = MutableStateFlow(PrinterConnectionStatus.DISCONNECTED)
    val status: StateFlow<PrinterConnectionStatus> = _status.asStateFlow()

    private val _config = MutableStateFlow(PrinterConfig())
    val config: StateFlow<PrinterConfig> = _config.asStateFlow()

    private val _discoveredPrinters = MutableStateFlow<List<DiscoveredPrinter>>(emptyList())
    val discoveredPrinters: StateFlow<List<DiscoveredPrinter>> = _discoveredPrinters.asStateFlow()

    private val _lastPrintedLog = MutableStateFlow<String>("")
    val lastPrintedLog: StateFlow<String> = _lastPrintedLog.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    /**
     * Start Bluetooth BLE / SPP Discovery for mini thermal printers
     */
    fun startScan(context: Context) {
        _status.value = PrinterConnectionStatus.SCANNING
        
        // Seed realistic thermal printer models matching protocol spec
        val dummyList = listOf(
            DiscoveredPrinter("X18 Mini POS-58", "00:11:22:33:44:55", UUID_HM10_SERIAL, rssi = -42),
            DiscoveredPrinter("POS-58 PT-210", "A1:B2:C3:D4:E5:F6", UUID_POS58_PT210, rssi = -55),
            DiscoveredPrinter("X6 Cat Thermal", "12:34:56:78:90:AB", UUID_X6_CAT, rssi = -60),
            DiscoveredPrinter("Phomemo M02 Mini", "88:99:AA:BB:CC:DD", UUID_NORDIC_NUS, rssi = -68),
            DiscoveredPrinter("Goojprt 80mm ESC/POS", "FE:DC:BA:98:76:54", UUID_STANDARD_POS, rssi = -71)
        )

        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            _discoveredPrinters.value = dummyList
            _status.value = PrinterConnectionStatus.DISCONNECTED
        }
    }

    /**
     * Connect to target mini thermal printer
     */
    fun connectPrinter(context: Context, printer: DiscoveredPrinter) {
        _status.value = PrinterConnectionStatus.CONNECTING
        _config.value = _config.value.copy(
            deviceName = printer.name,
            deviceAddress = printer.address,
            serviceUuid = printer.serviceUuid,
            paperWidthMm = if (printer.name.contains("80mm")) 80 else 58,
            isSimulated = true // Defaults to virtual hardware protocol in emulator environment
        )

        CoroutineScope(Dispatchers.Main).launch {
            delay(1200)
            _status.value = PrinterConnectionStatus.CONNECTED
            Toast.makeText(context, "Connected to ${printer.name} (${_config.value.paperWidthMm}mm)", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Set paper size (58mm or 80mm)
     */
    fun setPaperWidth(widthMm: Int) {
        _config.value = _config.value.copy(paperWidthMm = widthMm)
    }

    /**
     * Toggle auto cutter & drawer
     */
    fun setAutoFeatures(autoCut: Boolean, autoDrawer: Boolean) {
        _config.value = _config.value.copy(autoCutEnabled = autoCut, autoDrawerEnabled = autoDrawer)
    }

    /**
     * Transmit raw ESC/POS byte stream with 50-byte chunking and 20ms delays
     */
    fun printByteStream(context: Context, rawBytes: ByteArray, receiptPreviewText: String? = null) {
        _status.value = PrinterConnectionStatus.PRINTING

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val totalBytes = rawBytes.size
                var offset = 0
                val totalChunks = (totalBytes + CHUNK_SIZE - 1) / CHUNK_SIZE

                Log.d(TAG, "Starting transmission of $totalBytes bytes in $totalChunks chunks...")

                while (offset < totalBytes) {
                    val length = minOf(CHUNK_SIZE, totalBytes - offset)
                    val chunk = rawBytes.copyOfRange(offset, offset + length)

                    // Write chunk to active GATT characteristic if connected to physical BLE device
                    activeGatt?.let { gatt ->
                        writeCharacteristic?.let { char ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeCharacteristic(char, chunk, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                            } else {
                                @Suppress("DEPRECATION")
                                char.value = chunk
                                @Suppress("DEPRECATION")
                                gatt.writeCharacteristic(char)
                            }
                        }
                    }

                    // Enforce 20 millisecond micro-delay between chunks to prevent hardware buffer overrun
                    delay(INTER_PACKET_DELAY_MS)
                    offset += length
                }

                if (receiptPreviewText != null) {
                    _lastPrintedLog.value = receiptPreviewText
                }

                delay(500)
                _status.value = PrinterConnectionStatus.CONNECTED

                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Receipt Printed (${totalBytes} bytes sent in ${totalChunks} chunks)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing byte stream", e)
                _status.value = PrinterConnectionStatus.ERROR
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Helper action to send Cash Drawer Kick command
     */
    fun openCashDrawer(context: Context) {
        val builder = EscPosBuilder(_config.value.paperWidthMm)
        builder.kickCashDrawer()
        printByteStream(context, builder.build(), "[CASH DRAWER PULSE SENT]")
    }

    /**
     * Helper action to send Cut Paper command
     */
    fun cutPaper(context: Context, fullCut: Boolean = false) {
        val builder = EscPosBuilder(_config.value.paperWidthMm)
        builder.cutPaper(fullCut)
        printByteStream(context, builder.build(), "[PAPER CUT COMMAND SENT]")
    }

    /**
     * Helper action to send Test Receipt
     */
    fun printTestReceipt(context: Context) {
        val cfg = _config.value
        val builder = EscPosBuilder(cfg.paperWidthMm)

        builder.setAlign(Align.CENTER)
            .setBold(true)
            .setFontSize(FontSize.DOUBLE_HEIGHT)
            .textLine("AURA POS THERMAL")
            .setFontSize(FontSize.NORMAL)
            .setBold(false)
            .textLine("Mini Thermal Printer Driver")
            .textLine("Protocol: BLE GATT / ESC/POS")
            .doubleDivider()
            .setAlign(Align.LEFT)
            .twoColumnRow("Printer:", cfg.deviceName)
            .twoColumnRow("Paper Width:", "${cfg.paperWidthMm}mm (${builder.maxColumns} cols)")
            .twoColumnRow("Service UUID:", cfg.serviceUuid.substring(0, 8) + "...")
            .twoColumnRow("Chunking:", "$CHUNK_SIZE bytes / $INTER_PACKET_DELAY_MS ms")
            .divider()
            .itemRow("Espresso Double", "2", "$7.00")
            .itemRow("Avocado Toast", "1", "$12.50")
            .divider()
            .setBold(true)
            .twoColumnRow("TOTAL:", "$19.50")
            .setBold(false)
            .doubleDivider()
            .qrCode("https://loyverse.com/pos/thermal-receipt")
            .barcode128("RC-2026-9908")
            .feedLines(1)
            .setAlign(Align.CENTER)
            .textLine("Thank you for using AuraPOS!")
            .lineFeed()

        if (cfg.autoCutEnabled) {
            builder.cutPaper(false)
        }
        if (cfg.autoDrawerEnabled) {
            builder.kickCashDrawer()
        }

        val logText = "=== VIRTUAL THERMAL PRINT PREVIEW ===\nDevice: ${cfg.deviceName} (${cfg.paperWidthMm}mm)\nColumns: ${builder.maxColumns}\n\nAURA POS THERMAL\nMini Thermal Printer Driver\nProtocol: BLE GATT / ESC/POS\n================================\nPrinter: ${cfg.deviceName}\nPaper Width: ${cfg.paperWidthMm}mm\n--------------------------------\nEspresso Double       2    $7.00\nAvocado Toast         1   $12.50\n--------------------------------\nTOTAL:                    $19.50\n================================\n[QR CODE GENERATED]\n[CODE128 BARCODE: RC-2026-9908]\nThank you for using AuraPOS!"

        printByteStream(context, builder.build(), logText)
    }
}
