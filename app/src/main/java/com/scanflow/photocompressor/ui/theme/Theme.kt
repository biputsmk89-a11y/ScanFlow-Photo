package com.scanflow.photocompressor.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Secondary,
    tertiary = Tertiary,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = Tertiary,
    error = Error,
    errorContainer = ErrorContainer,
    onError = androidx.compose.ui.graphics.Color.White,
    onErrorContainer = Error,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Border,
    outlineVariant = Border,
    background = Background,
    onBackground = OnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = PrimaryDark,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = PrimaryLight,
    secondary = DarkSecondary,
    onSecondary = Secondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = SecondaryLight,
    tertiary = Tertiary,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = Tertiary,
    error = Error,
    errorContainer = ErrorContainer,
    onError = androidx.compose.ui.graphics.Color.White,
    onErrorContainer = Error,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Outline,
    background = DarkSurface,
    onBackground = DarkOnSurface
)

@Composable
fun PhotoCompressorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
