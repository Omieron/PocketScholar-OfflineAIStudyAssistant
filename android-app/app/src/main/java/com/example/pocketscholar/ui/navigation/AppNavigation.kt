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
    Documents("Documents", Icons.Default.Description),
    Chat("Chat", Icons.Default.Chat),
    Stats("Stats", Icons.Default.BarChart)
}
