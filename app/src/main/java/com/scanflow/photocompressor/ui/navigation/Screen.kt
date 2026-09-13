package com.scanflow.photocompressor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class defining all navigation destinations.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    // Bottom nav destinations
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object History : Screen("history", "History", Icons.Filled.History)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    // Tool destinations
    object Compress : Screen("compress", "Compress", Icons.Filled.Compress)
    object Batch : Screen("batch", "Batch", Icons.Filled.PhotoLibrary)
    object Resize : Screen("resize", "Resize", Icons.Filled.AspectRatio)
    object Crop : Screen("crop", "Crop", Icons.Filled.Crop)
    object Rotate : Screen("rotate", "Rotate", Icons.Filled.RotateRight)
    object Watermark : Screen("watermark", "Watermark", Icons.Filled.TextFields)
    object Convert : Screen("convert", "Convert", Icons.Filled.SwapHoriz)

    // Advanced tool destinations
    object Pdf : Screen("pdf", "PDF Document", Icons.Filled.PictureAsPdf)
    object Passport : Screen("passport", "Passport Photo", Icons.Filled.Badge)
    object Social : Screen("social", "Social Media", Icons.Filled.Share)
    object WhatsApp : Screen("whatsapp", "WhatsApp Ready", Icons.Filled.Send)

    companion object {
        val bottomNavItems = listOf(Home, History, Settings)
    }
}
