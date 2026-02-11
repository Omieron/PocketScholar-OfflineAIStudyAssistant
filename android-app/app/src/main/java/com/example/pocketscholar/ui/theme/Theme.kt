package com.example.pocketscholar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/*
 * Paul Rand Tema
 * ──────────────
 * "Everything is design. Everything!"
 *
 * Light: Sıcak kağıt beyazı, siyah metin, teal vurgu.
 * Dark:  Koyu kömür, açık metin, teal vurgu.
 * Her iki temada da aynı teal — tutarlı marka.
 */

private val DarkColorScheme = darkColorScheme(
    primary = RandTealMuted,
    onPrimary = RandBlack,
    primaryContainer = RandTealDark,
    onPrimaryContainer = RandTealLight,
    secondary = RandAmber,
    onSecondary = RandBlack,
    secondaryContainer = Color(0xFF3D3000),
    onSecondaryContainer = RandAmberLight,
    tertiary = RandTealMuted,
    background = RandCharcoal,
    onBackground = RandOffWhite,
    surface = RandBlack,
    onSurface = RandOffWhite,
    surfaceVariant = RandDarkGrey,
    onSurfaceVariant = RandGrey,
    outline = RandDarkGrey,
    error = RandRed,
    onError = RandWhite,
    errorContainer = Color(0xFF3D0A00),
    onErrorContainer = RandRedLight
)

private val LightColorScheme = lightColorScheme(
    primary = RandTeal,
    onPrimary = RandWhite,
    primaryContainer = RandTealLight,
    onPrimaryContainer = RandTealDark,
    secondary = RandAmber,
    onSecondary = RandWhite,
    secondaryContainer = RandAmberLight,
    onSecondaryContainer = Color(0xFF3D3000),
    tertiary = RandTeal,
    background = RandWhite,
    onBackground = RandBlack,
    surface = RandWhite,
    onSurface = RandBlack,
    surfaceVariant = RandOffWhite,
    onSurfaceVariant = RandDarkGrey,
    outline = RandLightGrey,
    error = RandRed,
    onError = RandWhite,
    errorContainer = RandRedLight,
    onErrorContainer = RandRed
)

@Composable
fun PocketScholarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

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
