package com.example.ui.theme

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
    primary = TrekSignalGreen,
    onPrimary = TrekDarkBackground,
    primaryContainer = TrekDarkSurfaceVariant,
    onPrimaryContainer = TrekSignalGreen,
    secondary = TrekBlazeOrange,
    onSecondary = TrekDarkBackground,
    secondaryContainer = TrekDarkSurfaceVariant,
    onSecondaryContainer = TrekBlazeOrange,
    tertiary = TrekAlpineCyan,
    onTertiary = TrekDarkBackground,
    background = TrekDarkBackground,
    onBackground = TrekTextPrimary,
    surface = TrekDarkSurface,
    onSurface = TrekTextPrimary,
    surfaceVariant = TrekDarkSurfaceVariant,
    onSurfaceVariant = TrekTextSecondary,
    outline = TrekDarkSurfaceBorder,
    error = TrekSosCrimson,
    onError = TrekTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = TrekDeepGreen,
    onPrimary = TrekLightSurface,
    primaryContainer = TrekLightSurfaceVariant,
    onPrimaryContainer = TrekLightTextPrimary,
    secondary = TrekBlazeOrange,
    onSecondary = TrekLightSurface,
    tertiary = TrekAlpineCyan,
    background = TrekLightBackground,
    onBackground = TrekLightTextPrimary,
    surface = TrekLightSurface,
    onSurface = TrekLightTextPrimary,
    surfaceVariant = TrekLightSurfaceVariant,
    onSurfaceVariant = TrekLightTextSecondary,
    error = TrekSosCrimson
)

@Composable
fun TrekSyncTheme(
    darkTheme: Boolean = true, // Default to rugged dark mode for high outdoor contrast
    dynamicColor: Boolean = false, // Keep high-contrast tactical styling
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
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
