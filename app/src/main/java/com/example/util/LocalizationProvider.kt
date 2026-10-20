package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalAppLanguage = compositionLocalOf { "es" }
val LocalAppStrings = compositionLocalOf { AppStrings.get("es") }

/**
 * Global localization provider that detects the current language setting and
 * ensures all UI components, including the KDS and Top Bar, display strings
 * dynamically based on the active language.
 */
@Composable
fun LocalizationProvider(
    language: String,
    content: @Composable () -> Unit
) {
    val strings = AppStrings.get(language)
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalAppStrings provides strings,
        content = content
    )
}
