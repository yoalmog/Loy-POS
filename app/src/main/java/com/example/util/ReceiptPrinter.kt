package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.widget.Toast
import com.example.data.model.ReceiptEntity
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPrinter {

    /**
     * Builds ESC/POS binary byte stream for mini thermal printer (58mm/80mm)
     */
    fun generateEscPosReceipt(receipt: ReceiptEntity, paperWidthMm: Int = 58): ByteArray {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = dateFormat.format(Date(receipt.timestamp))

        val builder = EscPosBuilder(paperWidthMm)

        // 1. Header
        builder.setAlign(Align.CENTER)
            .setBold(true)
            .setFontSize(FontSize.DOUBLE_HEIGHT)
            .textLine("AURA BISTRO & GRILL")
            .setFontSize(FontSize.NORMAL)
            .setBold(false)
            .textLine("100 Market Square, Suite 4B")
            .textLine("Tel: (555) 019-2834")
            .doubleDivider()

        // 2. Metadata
        builder.setAlign(Align.LEFT)
            .twoColumnRow("Receipt #:", receipt.receiptNumber)
            .twoColumnRow("Date:", dateStr)
            .twoColumnRow("Cashier:", receipt.cashierName)
            .twoColumnRow("Option:", receipt.diningOption.label)

        if (!receipt.customerName.isNullOrBlank()) {
            builder.twoColumnRow("Customer:", receipt.customerName)
        }

        builder.divider()
            .twoColumnRow("ITEM / QTY", "PRICE")
            .divider()

        // 3. Items
        val itemLines = receipt.itemsSummaryJson.split(",")
        for (line in itemLines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                // Parse "2x Cappuccino ($9.00)" or similar string format
                val match = Regex("""(\d+)x?\s+(.+?)\s+\(\$(.+?)\)""").find(trimmed)
                if (match != null) {
                    val qty = match.groupValues[1]
                    val name = match.groupValues[2]
                    val price = "$${match.groupValues[3]}"
                    builder.itemRow(name, qty, price)
                } else {
                    builder.textLine(trimmed)
                }
            }
        }

        // 4. Totals
        builder.divider()
            .twoColumnRow("Subtotal:", "$${String.format(Locale.US, "%.2f", receipt.subtotal)}")

        if (receipt.discountAmount > 0) {
            builder.twoColumnRow("Discount:", "-$${String.format(Locale.US, "%.2f", receipt.discountAmount)}")
        }

        builder.twoColumnRow("Tax (8.25%):", "$${String.format(Locale.US, "%.2f", receipt.taxAmount)}")

        if (receipt.tipAmount > 0) {
            builder.twoColumnRow("Tip:", "$${String.format(Locale.US, "%.2f", receipt.tipAmount)}")
        }

        builder.doubleDivider()
            .setBold(true)
            .setFontSize(FontSize.DOUBLE_WIDTH)
            .twoColumnRow("TOTAL:", "$${String.format(Locale.US, "%.2f", receipt.totalAmount)}")
            .setFontSize(FontSize.NORMAL)
            .setBold(false)
            .twoColumnRow("Payment Method:", receipt.paymentType.label)

        if (receipt.paymentDetailsJson.isNotBlank()) {
            builder.twoColumnRow("Details:", receipt.paymentDetailsJson)
        }

        builder.doubleDivider()

        // 5. Barcode & QR Code
        builder.qrCode("https://loyverse.com/receipt/${receipt.receiptNumber}")
            .barcode128(receipt.receiptNumber)
            .feedLines(1)
            .setAlign(Align.CENTER)
            .textLine("Thank you for your visit!")
            .textLine("*** Loyverse AuraPOS ***")
            .lineFeed()

        // 6. Cut & Drawer Pulse
        builder.cutPaper(false)
            .kickCashDrawer()

        return builder.build()
    }

    /**
     * Formats ticket/receipt details into a clean, monospace thermal receipt text layout.
     */
    fun formatReceiptText(
        receiptNumber: String,
        timestamp: Long,
        cashierName: String,
        diningOption: String,
        customerName: String?,
        itemsSummary: String,
        subtotal: Double,
        taxAmount: Double,
        discountAmount: Double,
        tipAmount: Double,
        totalAmount: Double,
        paymentType: String,
        paymentDetails: String,
        paperWidthMm: Int = 58
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val dateStr = dateFormat.format(Date(timestamp))

        val width = if (paperWidthMm >= 80) 48 else 32

        fun center(text: String): String {
            if (text.length >= width) return text
            val padding = (width - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun lineRow(left: String, right: String): String {
            val spaceAvailable = width - left.length - right.length
            if (spaceAvailable <= 0) {
                return "$left $right"
            }
            return left + " ".repeat(spaceAvailable) + right
        }

        val divider = "-".repeat(width)
        val doubleDivider = "=".repeat(width)

        val sb = StringBuilder()
        sb.appendLine(center("BISTRO & GRILL POS"))
        sb.appendLine(center("100 Market Square, Suite 4B"))
        sb.appendLine(center("Tel: (555) 019-2834"))
        sb.appendLine(doubleDivider)
        sb.appendLine(lineRow("Receipt #:", receiptNumber))
        sb.appendLine(lineRow("Date:", dateStr))
        sb.appendLine(lineRow("Cashier:", cashierName))
        sb.appendLine(lineRow("Option:", diningOption))
        if (!customerName.isNullOrBlank()) {
            sb.appendLine(lineRow("Customer:", customerName))
        }
        sb.appendLine(divider)
        sb.appendLine(lineRow("ITEM / QTY", "PRICE"))
        sb.appendLine(divider)

        // Parse items summary or render lines
        val itemLines = itemsSummary.split(",")
        for (line in itemLines) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                sb.appendLine(trimmed)
            }
        }

        sb.appendLine(divider)
        sb.appendLine(lineRow("Subtotal:", "$${String.format(Locale.US, "%.2f", subtotal)}"))
        if (discountAmount > 0) {
            sb.appendLine(lineRow("Discount:", "-$${String.format(Locale.US, "%.2f", discountAmount)}"))
        }
        sb.appendLine(lineRow("Tax (8.25%):", "$${String.format(Locale.US, "%.2f", taxAmount)}"))
        if (tipAmount > 0) {
            sb.appendLine(lineRow("Tip:", "$${String.format(Locale.US, "%.2f", tipAmount)}"))
        }
        sb.appendLine(doubleDivider)
        sb.appendLine(lineRow("TOTAL PAID:", "$${String.format(Locale.US, "%.2f", totalAmount)}"))
        sb.appendLine(lineRow("Payment Method:", paymentType))
        if (paymentDetails.isNotBlank()) {
            sb.appendLine(lineRow("Details:", paymentDetails))
        }
        sb.appendLine(doubleDivider)
        sb.appendLine(center("Thank you for your visit!"))
        sb.appendLine(center("*** Loyverse POS ***"))
        sb.appendLine()

        return sb.toString()
    }

    fun formatReceiptFromEntity(receipt: ReceiptEntity, paperWidthMm: Int = 58): String {
        return formatReceiptText(
            receiptNumber = receipt.receiptNumber,
            timestamp = receipt.timestamp,
            cashierName = receipt.cashierName,
            diningOption = receipt.diningOption.label,
            customerName = receipt.customerName,
            itemsSummary = receipt.itemsSummaryJson,
            subtotal = receipt.subtotal,
            taxAmount = receipt.taxAmount,
            discountAmount = receipt.discountAmount,
            tipAmount = receipt.tipAmount,
            totalAmount = receipt.totalAmount,
            paymentType = receipt.paymentType.label,
            paymentDetails = receipt.paymentDetailsJson,
            paperWidthMm = paperWidthMm
        )
    }

    /**
     * Triggers Thermal Printer transmission and/or Android PrintManager dialog
     */
    fun printReceipt(context: Context, receipt: ReceiptEntity) {
        val config = ThermalPrinterManager.config.value
        val escPosBytes = generateEscPosReceipt(receipt, config.paperWidthMm)
        val textLayout = formatReceiptFromEntity(receipt, config.paperWidthMm)

        // 1. Always transmit to BLE GATT Thermal Printer
        ThermalPrinterManager.printByteStream(context, escPosBytes, textLayout)

        // 2. Also trigger Android PrintManager for PDF or standard printer dialog
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager != null) {
            try {
                val printAdapter = TextPrintDocumentAdapter(context, textLayout, "Receipt_${receipt.receiptNumber}")
                printManager.print("Receipt_${receipt.receiptNumber}", printAdapter, PrintAttributes.Builder().build())
            } catch (e: Exception) {
                // Ignore fallback exception
            }
        }
    }
}

class TextPrintDocumentAdapter(
    private val context: Context,
    private val textContent: String,
    private val documentName: String
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)

        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(documentName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()

        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val page = pdfDocument?.startPage(0) ?: run {
            callback.onWriteFailed("Could not create PDF page")
            return
        }

        if (cancellationSignal?.isCanceled == true) {
            pdfDocument?.close()
            pdfDocument = null
            callback.onWriteCancelled()
            return
        }

        val canvas: Canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.MONOSPACE
        }

        var y = 40f
        val lines = textContent.split("\n")
        for (line in lines) {
            canvas.drawText(line, 40f, y, paint)
            y += 18f
        }

        pdfDocument?.finishPage(page)

        try {
            FileOutputStream(destination.fileDescriptor).use { output ->
                pdfDocument?.writeTo(output)
            }
        } catch (e: IOException) {
            callback.onWriteFailed(e.toString())
            return
        } finally {
            pdfDocument?.close()
            pdfDocument = null
        }

        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }
}
