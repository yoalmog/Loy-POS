package com.example.data.model

import java.util.UUID

enum class OcrEngineType(val label: String, val subtitle: String) {
    PADDLE_OCR("PaddleOCR (PP-OCRv4)", "Self-Hosted / Open-Source API"),
    HEURISTIC_PARSER("Smart Heuristic Parser", "Offline Regex & Spatial Fallback")
}

data class OcrBoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class PaddleOcrDetectedLine(
    val text: String,
    val confidence: Float,
    val bounds: OcrBoundingBox? = null
)

data class PaddleOcrScanResult(
    val success: Boolean,
    val lines: List<PaddleOcrDetectedLine>,
    val fullText: String,
    val latencyMs: Long,
    val engine: OcrEngineType,
    val warning: String? = null
)

data class ParsedOcrMenuItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val price: Double,
    val description: String = "",
    val confidence: Float = 0.95f,
    val isSelected: Boolean = true
)

data class OcrEngineSettings(
    val endpointUrl: String = "http://10.0.2.2:8866/predict/ocr_system",
    val timeoutMs: Long = 8000L,
    val language: String = "spa+eng",
    val autoFallbackToHeuristic: Boolean = true
)
