package com.scanflow.photocompressor.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryFixed,
    onPrimaryContainer = PrimaryDark,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = Tertiary,
    error = Error,
    errorContainer = ErrorContainer,
    onError = OnError,
    onErrorContainer = OnErrorContainer,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Border,
    outlineVariant = OutlineVariant,
    background = Background,
    onBackground = OnSurface,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest
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
    tertiary = SuccessLight,
    onTertiary = DarkSurface,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = TertiaryLight,
    error = Error,
    errorContainer = ErrorContainer,
    onError = OnError,
    onErrorContainer = OnErrorContainer,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Outline,
    outlineVariant = DarkSurfaceVariant,
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
