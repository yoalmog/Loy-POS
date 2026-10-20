package com.example.data.model

import java.util.UUID

enum class PrinterInterfaceType(val label: String) {
    NETWORK("Network (LAN / Wi-Fi)"),
    BLUETOOTH("Bluetooth BLE / SPP"),
    SYSTEM("Android System Print")
}

data class NetworkPrinter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val ipAddress: String,
    val port: Int = 9100,
    val model: String = "ESC/POS Network Thermal",
    val paperWidthMm: Int = 80,
    val isOnline: Boolean = true,
    val responseTimeMs: Long = 0L,
    val isDefault: Boolean = false,
    val isKitchenPrinter: Boolean = false,
    val macAddress: String? = null,
    val isSimulated: Boolean = false
) {
    val addressLabel: String
        get() = "$ipAddress:$port"

    val description: String
        get() = "$model • ${paperWidthMm}mm"
}

data class NetworkPrinterSettings(
    val defaultPrinterId: String? = null,
    val defaultPrinterIp: String? = "192.168.1.120",
    val defaultPrinterPort: Int = 9100,
    val autoPrintOnCheckout: Boolean = true,
    val openDrawerOnPrint: Boolean = true,
    val cutPaperOnPrint: Boolean = true,
    val defaultPaperWidthMm: Int = 80,
    val printKitchenTicket: Boolean = false,
    val kitchenPrinterIp: String? = null,
    val kitchenPrinterPort: Int = 9100,
    val preferredInterface: PrinterInterfaceType = PrinterInterfaceType.NETWORK
)

sealed class PrinterDiscoveryState {
    object Idle : PrinterDiscoveryState()
    data class Scanning(
        val progress: Float,
        val subnet: String,
        val currentIp: String,
        val foundCount: Int
    ) : PrinterDiscoveryState()
    data class Finished(val foundCount: Int, val subnet: String) : PrinterDiscoveryState()
    data class Error(val message: String) : PrinterDiscoveryState()
}

sealed class PrintJobStatus {
    object Idle : PrintJobStatus()
    data class Connecting(val target: String) : PrintJobStatus()
    data class Sending(val target: String, val bytesTotal: Int) : PrintJobStatus()
    data class Success(val target: String, val message: String, val timestamp: Long = System.currentTimeMillis()) : PrintJobStatus()
    data class Failed(val target: String, val error: String) : PrintJobStatus()
}
