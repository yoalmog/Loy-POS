package com.example.ui.kitchen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * Full-screen Kitchen Display Screen (KDS)
 * Wraps the modular KitchenDisplayComponent for standalone navigation.
 */
@Composable
fun KitchenDisplayScreen(
    onNavigateBack: () -> Unit
) {
    KitchenDisplayComponent(
        modifier = Modifier.fillMaxSize(),
        onNavigateBack = onNavigateBack
    )
}
