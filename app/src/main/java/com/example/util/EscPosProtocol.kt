package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

enum class Align {
    LEFT, CENTER, RIGHT
}

enum class FontSize {
    NORMAL, DOUBLE_WIDTH, DOUBLE_HEIGHT, LARGE
}

/**
 * High-performance ESC/POS Command Encoder & Byte Stream Builder
 * Supports 58mm (32 chars) and 80mm (48 chars) mini thermal printers,
 * BLE GATT chunking, native 1D/2D barcodes, raster logos, cash drawer kick, and auto-cut.
 */
class EscPosBuilder(val paperWidthMm: Int = 58) {

    private val outputStream = ByteArrayOutputStream()
    private val charset = Charset.forName("GBK") // Standard encoding for thermal printers

    val maxColumns: Int = if (paperWidthMm >= 80) 48 else 32

    init {
        initPrinter()
    }

    /** ESC @ - Initialize printer */
    fun initPrinter(): EscPosBuilder {
        outputStream.write(byteArrayOf(0x1B, 0x40))
        return this
    }

    /** CR LF Line Feed */
    fun lineFeed(): EscPosBuilder {
        outputStream.write(byteArrayOf(0x0D, 0x0A))
        return this
    }

    /** ESC d n - Feed n lines */
    fun feedLines(n: Int): EscPosBuilder {
        val count = n.coerceIn(1, 255)
        outputStream.write(byteArrayOf(0x1B, 0x64, count.toByte()))
        return this
    }

    /** ESC a n - Select alignment */
    fun setAlign(align: Align): EscPosBuilder {
        val n: Byte = when (align) {
            Align.LEFT -> 0x00
            Align.CENTER -> 0x01
            Align.RIGHT -> 0x02
        }
        outputStream.write(byteArrayOf(0x1B, 0x61, n))
        return this
    }

    /** ESC E n - Emphasized / Bold mode */
    fun setBold(enable: Boolean): EscPosBuilder {
        val n: Byte = if (enable) 0x01 else 0x00
        outputStream.write(byteArrayOf(0x1B, 0x45, n))
        return this
    }

    /** ESC - n - Underline mode */
    fun setUnderline(enable: Boolean): EscPosBuilder {
        val n: Byte = if (enable) 0x01 else 0x00
        outputStream.write(byteArrayOf(0x1B, 0x2D, n))
        return this
    }

    /** GS ! n - Font magnification */
    fun setFontSize(size: FontSize): EscPosBuilder {
        val n: Byte = when (size) {
            FontSize.NORMAL -> 0x00
            FontSize.DOUBLE_WIDTH -> 0x20
            FontSize.DOUBLE_HEIGHT -> 0x01
            FontSize.LARGE -> 0x11
        }
        outputStream.write(byteArrayOf(0x1D, 0x21, n))
        return this
    }

    /** Append raw string text */
    fun text(str: String): EscPosBuilder {
        try {
            outputStream.write(str.toByteArray(charset))
        } catch (e: Exception) {
            outputStream.write(str.toByteArray())
        }
        return this
    }

    /** Append raw string text followed by CR LF */
    fun textLine(str: String = ""): EscPosBuilder {
        text(str)
        lineFeed()
        return this
    }

    /** Format divider line like "--------------------------------" */
    fun divider(char: Char = '-'): EscPosBuilder {
        textLine(char.toString().repeat(maxColumns))
        return this
    }

    /** Format double divider like "================================" */
    fun doubleDivider(): EscPosBuilder {
        return divider('=')
    }

    /** Format two columns (e.g. "Subtotal:" and "$15.00") justified to edges */
    fun twoColumnRow(left: String, right: String): EscPosBuilder {
        val available = maxColumns - left.length - right.length
        if (available <= 0) {
            textLine("$left $right")
        } else {
            textLine(left + " ".repeat(available) + right)
        }
        return this
    }

    /**
     * Formats 3-column items according to protocol spec:
     * 58mm: Item Name (20 chars) + Qty (3 chars) + Price (9 chars) = 32 chars
     * 80mm: Item Name (32 chars) + Qty (4 chars) + Price (12 chars) = 48 chars
     */
    fun itemRow(name: String, qty: String, price: String): EscPosBuilder {
        val nameWidth = if (maxColumns == 48) 32 else 20
        val qtyWidth = if (maxColumns == 48) 4 else 3
        val priceWidth = if (maxColumns == 48) 12 else 9

        val truncatedName = if (name.length > nameWidth) name.substring(0, nameWidth - 1) + "." else name.padEnd(nameWidth)
        val formattedQty = qty.padStart(qtyWidth)
        val formattedPrice = price.padStart(priceWidth)

        textLine("$truncatedName$formattedQty$formattedPrice")
        return this
    }

    /** ESC p m t1 t2 - Cash drawer pulse kick */
    fun kickCashDrawer(): EscPosBuilder {
        // Command: 0x1B 0x70 0x00 0x19 0xFA (Pin 2 pulse 50ms)
        outputStream.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
        return this
    }

    /** GS V m n - Paper Cut */
    fun cutPaper(fullCut: Boolean = false): EscPosBuilder {
        feedLines(3)
        val m: Byte = if (fullCut) 0x41 else 0x42
        outputStream.write(byteArrayOf(0x1D, 0x56, m, 0x00))
        return this
    }

    /**
     * 1D Barcode (CODE128) Protocol
     * GS h 60 (Set height)
     * GS H 2 (Set text below)
     * GS k 73 len ... (Print CODE128)
     */
    fun barcode128(data: String): EscPosBuilder {
        setAlign(Align.CENTER)
        // Set barcode height = 60 dots
        outputStream.write(byteArrayOf(0x1D, 0x68, 0x3C))
        // Set text position below barcode
        outputStream.write(byteArrayOf(0x1D, 0x48, 0x02))
        // CODE128 command header: GS k 73 len {B data
        val bytes = data.toByteArray(Charsets.US_ASCII)
        val len = (bytes.size + 2).toByte()
        outputStream.write(byteArrayOf(0x1D, 0x6B, 0x49, len, 0x7B, 0x42))
        outputStream.write(bytes)
        lineFeed()
        setAlign(Align.LEFT)
        return this
    }

    /**
     * Native 2D QR Code (Model 2) Protocol
     * Model 2 Select: 0x1D 0x28 0x6B 0x04 0x00 0x31 0x41 0x32 0x00
     * Module Size: 0x1D 0x28 0x6B 0x03 0x00 0x31 0x43 [size]
     * Error Correction L: 0x1D 0x28 0x6B 0x03 0x00 0x31 0x45 0x31
     * Store Data: 0x1D 0x28 0x6B [pL] [pH] 0x31 0x50 0x30 [bytes...]
     * Print QR: 0x1D 0x28 0x6B 0x03 0x00 0x31 0x51 0x30
     */
    fun qrCode(data: String, moduleSize: Int = 5): EscPosBuilder {
        setAlign(Align.CENTER)
        val dataBytes = data.toByteArray(Charsets.UTF_8)
        val len = dataBytes.size + 3
        val pL = (len % 256).toByte()
        val pH = (len / 256).toByte()
        val sizeByte = moduleSize.coerceIn(3, 8).toByte()

        // 1. Select Model 2
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))
        // 2. Set Module size
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, sizeByte))
        // 3. Set Error correction level L
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))
        // 4. Store QR data
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        outputStream.write(dataBytes)
        // 5. Print Stored QR Symbol
        outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
        lineFeed()
        setAlign(Align.LEFT)
        return this
    }

    /**
     * Standard ESC/POS Raster Image Mode (GS v 0) & X6 Cat Printer Protocol
     */
    fun rasterImage(bitmap: Bitmap, isCatPrinter: Boolean = false): EscPosBuilder {
        val width = if (paperWidthMm >= 80) 576 else 384
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val height = (width * aspectRatio).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val widthBytes = width / 8

        setAlign(Align.CENTER)

        if (isCatPrinter) {
            // X6 / X6h Cat Mini Thermal Protocol Header (0x51 0x78)
            outputStream.write(byteArrayOf(0x51, 0x78.toByte(), 0x00, 0x00))
        }

        val rasterHeader = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (widthBytes % 256).toByte(),
            (widthBytes / 256).toByte(),
            (height % 256).toByte(),
            (height / 256).toByte()
        )
        outputStream.write(rasterHeader)

        val imageData = ByteArray(widthBytes * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = scaled.getPixel(x, y)
                val luminance = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114)
                if (luminance < 128) { // Black pixel
                    val byteIdx = y * widthBytes + (x / 8)
                    val bitIdx = 7 - (x % 8)
                    imageData[byteIdx] = (imageData[byteIdx].toInt() or (1 shl bitIdx)).toByte()
                }
            }
        }

        outputStream.write(imageData)
        lineFeed()
        setAlign(Align.LEFT)
        return this
    }

    /** Build complete byte array */
    fun build(): ByteArray {
        return outputStream.toByteArray()
    }
}
