package com.example.util

import com.example.data.model.ParsedOcrMenuItem
import java.util.UUID

object OcrMenuParser {

    private val CATEGORY_KEYWORDS = mapOf(
        "ENTRADA" to "Entradas y Piqueos",
        "ENTRADAS" to "Entradas y Piqueos",
        "PIQUEO" to "Entradas y Piqueos",
        "PIQUEOS" to "Entradas y Piqueos",
        "ENSALADA" to "Ensaladas",
        "FONDO" to "Platos de Fondo",
        "FONDOS" to "Platos de Fondo",
        "SEGUNDO" to "Platos de Fondo",
        "SEGUNDOS" to "Platos de Fondo",
        "PLATO" to "Platos de Fondo",
        "PLATOS" to "Platos de Fondo",
        "CARNE" to "Hamburguesas y Carnes",
        "CARNES" to "Hamburguesas y Carnes",
        "HAMBURGUESA" to "Hamburguesas y Carnes",
        "HAMBURGUESAS" to "Hamburguesas y Carnes",
        "BURGER" to "Hamburguesas y Carnes",
        "POLLO" to "Pollos y Brasas",
        "POLLOS" to "Pollos y Brasas",
        "PIZZA" to "Pizzas y Empanadas",
        "PIZZAS" to "Pizzas y Empanadas",
        "PASTA" to "Pastas e Italianos",
        "PASTAS" to "Pastas e Italianos",
        "PESCADO" to "Pescados y Mariscos",
        "CEVICHE" to "Pescados y Mariscos",
        "BEBIDA" to "Bebidas y Refrescos",
        "BEBIDAS" to "Bebidas y Refrescos",
        "REFRESCO" to "Bebidas y Refrescos",
        "CERVEZA" to "Bebidas y Licores",
        "POSTRE" to "Postres y Dulces",
        "POSTRES" to "Postres y Dulces",
        "CAFE" to "Cafetería"
    )

    private val PRICE_REGEX = Regex(
        """(?:(?:s\/?\.?|\$|€|usd|pen)\s*)?(\d{1,4}(?:[.,]\d{1,2})?)(?:\s*(?:s\/?\.?|\$|€|usd|pen))?(?:\s*$|\s*[|/)]|\s*$)""",
        RegexOption.IGNORE_CASE
    )

    fun parseLines(rawLines: List<String>): List<ParsedOcrMenuItem> {
        val result = mutableListOf<ParsedOcrMenuItem>()
        var currentCategory = "General"

        val cleanedLines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        var i = 0

        while (i < cleanedLines.size) {
            val line = cleanedLines[i]

            // 1. Detect Category Header
            val headerCandidate = line.replace(Regex("""[=*\-_#:~]"""), "").trim().uppercase()
            if (headerCandidate.length in 3..35) {
                for ((kw, cat) in CATEGORY_KEYWORDS) {
                    if (headerCandidate == kw || headerCandidate.startsWith("$kw ") || headerCandidate.endsWith(" $kw")) {
                        currentCategory = cat
                        break
                    }
                }
            }

            // 2. Sanitize noise
            val sanitized = line
                .replace(Regex("""\.{2,}"""), " ")
                .replace(Regex("""-{2,}"""), " ")
                .replace(Regex("""_{2,}"""), " ")
                .replace(Regex("""[|~]"""), " ")
                .trim()

            // 3. Match Price
            val match = PRICE_REGEX.find(sanitized)
            if (match != null && match.groups[1] != null) {
                val rawPriceStr = match.groups[1]!!.value.replace(',', '.')
                val price = rawPriceStr.toDoubleOrNull() ?: 0.0

                val nameCandidate = sanitized.replace(match.value, "")
                    .replace(Regex("""[.,:;\\/]+$"""), "")
                    .trim()

                if (nameCandidate.length >= 2 && price > 0.0 && price < 10000.0) {
                    var description = ""
                    // Lookahead for optional description on next line
                    if (i + 1 < cleanedLines.size) {
                        val nextLine = cleanedLines[i + 1].trim()
                        if (!PRICE_REGEX.containsMatchIn(nextLine) && nextLine.length in 6..140) {
                            description = nextLine
                            i++ // consume next line
                        }
                    }

                    result.add(
                        ParsedOcrMenuItem(
                            id = "ocr_${System.currentTimeMillis()}_${result.size + 1}",
                            name = nameCandidate,
                            category = currentCategory,
                            price = price,
                            description = description,
                            confidence = if (match.value.contains("S/", ignoreCase = true) || match.value.contains("$")) 0.98f else 0.90f,
                            isSelected = true
                        )
                    )
                }
            }
            i++
        }

        return result
    }

    /**
     * Sample fallback menu text for testing or simulation
     */
    fun getSampleMenuText(): List<String> = listOf(
        "ENTRADAS Y PIQUEOS",
        "Ceviche Clásico de Pescado S/ 34.00",
        "Pesca del día marinada en zumo de limón fresco con camote glaseado, choclo y canchita",
        "Tequeños de Queso con Guacamole S/ 18.50",
        "Masa crujiente rellena de queso andino acompañada de salsa guacamole fresca",
        "PLATOS DE FONDO",
        "Lomo Saltado Criollo S/ 38.50",
        "Tiras de lomo fino salteadas al wok con cebolla, tomate, papas fritas y arroz",
        "Hamburguesa Artesanal Doble Queso S/ 28.00",
        "200g carne de res angus, queso cheddar derretido, cebolla caramelizada y salsa de la casa",
        "Pizza Margherita Rústica S/ 36.00",
        "BEBIDAS Y REFRESCOS",
        "Chicha Morada Tradicional 500ml S/ 8.50",
        "Limonada Frozen con Hierbabuena S/ 9.00"
    )
}
