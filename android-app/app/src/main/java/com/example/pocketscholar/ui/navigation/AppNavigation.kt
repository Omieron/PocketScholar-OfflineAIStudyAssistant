package com.example.pocketscholar.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppScreen(
    val title: String,
    val icon: ImageVector
) {
    Documents("Belgeler", Icons.Default.Description),
    Chat("Sohbet", Icons.Default.Chat),
    Stats("İstatistik", Icons.Default.BarChart)
}
