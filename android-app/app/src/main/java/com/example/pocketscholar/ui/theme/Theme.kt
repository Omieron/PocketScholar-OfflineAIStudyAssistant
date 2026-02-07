package com.example.pocketscholar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Amber80,
    onSecondary = Teal20,
    secondaryContainer = Amber30,
    onSecondaryContainer = Amber90,
    tertiary = TealGrey80,
    background = Teal20,
    onBackground = Teal90,
    surface = Teal20,
    onSurface = Teal90,
    surfaceVariant = Teal30,
    onSurfaceVariant = Teal70,
    outline = Teal50
)

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Teal90,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal20,
    secondary = Amber40,
    onSecondary = Teal90,
    secondaryContainer = Amber80,
    onSecondaryContainer = Teal20,
    tertiary = TealGrey40,
    background = Teal90.copy(alpha = 0.3f),
    onBackground = Teal20,
    surface = Teal90.copy(alpha = 0.5f),
    onSurface = Teal20,
    surfaceVariant = Teal70.copy(alpha = 0.3f),
    onSurfaceVariant = Teal30,
    outline = Teal50
)

@Composable
fun PocketScholarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
