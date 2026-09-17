package com.scanflow.photocompressor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class defining all navigation destinations aligned with Google Stitch.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    // Bottom nav destinations (Identik Google Stitch)
    object Home : Screen("home", "Home", Icons.Filled.Compress)
    object History : Screen("history", "History", Icons.Filled.History)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    // Tool destinations
    object Compress : Screen("compress", "Compress", Icons.Filled.Compress)
    object Batch : Screen("batch", "Batch", Icons.Filled.CollectionsBookmark)
    object Resize : Screen("resize", "Resize", Icons.Filled.AspectRatio)
    object Crop : Screen("crop", "Crop", Icons.Filled.Crop)
    object Rotate : Screen("rotate", "Rotate", Icons.AutoMirrored.Filled.RotateRight)
    object Watermark : Screen("watermark", "Watermark", Icons.Filled.TextFields)
    object Convert : Screen("convert", "Convert", Icons.Filled.Transform)

    // Advanced tool destinations
    object Pdf : Screen("pdf", "PDF Document Maker", Icons.Filled.PictureAsPdf)
    object Passport : Screen("passport", "Passport & ID Studio", Icons.Filled.Badge)
    object Social : Screen("social", "Social Media Sizer", Icons.Filled.Feed)
    object WhatsApp : Screen("whatsapp", "WhatsApp Optimizer", Icons.AutoMirrored.Filled.Send)

    companion object {
        val bottomNavItems = listOf(Home, History, Settings)
    }
}
