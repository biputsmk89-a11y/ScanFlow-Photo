package com.scanflow.photocompressor.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.ui.components.OperationChip
import com.scanflow.photocompressor.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToCompress: () -> Unit,
    onNavigateToBatch: () -> Unit,
    onNavigateToResize: () -> Unit,
    onNavigateToCrop: () -> Unit,
    onNavigateToRotate: () -> Unit,
    onNavigateToWatermark: () -> Unit,
    onNavigateToConvert: () -> Unit,
    onNavigateToPdf: () -> Unit = {},
    onNavigateToPassport: () -> Unit = {},
    onNavigateToSocial: () -> Unit = {},
    onNavigateToWhatsApp: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isMoreToolsExpanded by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Photo Compressor / Image Tools
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Photo Compressor",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Image Tools",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Minimalist Offline Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "100% Offline",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2. Primary Hero: [ Compress Photos ]
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                            com.scanflow.photocompressor.util.AnalyticsEvent.TOOL_OPENED,
                            mapOf("tool" to "compress")
                        )
                        onNavigateToCompress()
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Compress,
                                contentDescription = "Compress",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compress Photos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Reduce file size with high visual quality",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. Priority Visual Grid (2x2): Resize, Convert, Batch, Crop
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryGridCard(
                        icon = Icons.Filled.AspectRatio,
                        title = "Resize",
                        subtitle = "Scale dimensions",
                        onClick = {
                            com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                                com.scanflow.photocompressor.util.AnalyticsEvent.TOOL_OPENED,
                                mapOf("tool" to "resize")
                            )
                            onNavigateToResize()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryGridCard(
                        icon = Icons.Filled.SwapHoriz,
                        title = "Convert",
                        subtitle = "JPG • PNG • WEBP",
                        onClick = {
                            com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                                com.scanflow.photocompressor.util.AnalyticsEvent.TOOL_OPENED,
                                mapOf("tool" to "convert")
                            )
                            onNavigateToConvert()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrimaryGridCard(
                        icon = Icons.Filled.PhotoLibrary,
                        title = "Batch",
                        subtitle = "Multi-photo queue",
                        onClick = {
                            com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                                com.scanflow.photocompressor.util.AnalyticsEvent.TOOL_OPENED,
                                mapOf("tool" to "batch")
                            )
                            onNavigateToBatch()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryGridCard(
                        icon = Icons.Filled.Crop,
                        title = "Crop",
                        subtitle = "Trim & aspect ratio",
                        onClick = {
                            com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                                com.scanflow.photocompressor.util.AnalyticsEvent.TOOL_OPENED,
                                mapOf("tool" to "crop")
                            )
                            onNavigateToCrop()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Progressive Disclosure: More Tools (PDF • Passport • Social • WhatsApp)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMoreToolsExpanded = !isMoreToolsExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "More Tools",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PDF • Passport • Social • WhatsApp",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val rotationAngle by animateFloatAsState(
                            targetValue = if (isMoreToolsExpanded) 180f else 0f,
                            label = "chevron_rotation"
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (isMoreToolsExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(rotationAngle)
                        )
                    }

                    // Expandable content
                    AnimatedVisibility(
                        visible = isMoreToolsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

                            if (com.scanflow.photocompressor.domain.model.FeatureFlags.ENABLE_PDF) {
                                SecondaryToolRow(
                                    icon = Icons.Filled.PictureAsPdf,
                                    title = "PDF Document",
                                    description = "Convert multiple photos into a PDF file",
                                    onClick = onNavigateToPdf
                                )
                            }

                            if (com.scanflow.photocompressor.domain.model.FeatureFlags.ENABLE_PASSPORT) {
                                SecondaryToolRow(
                                    icon = Icons.Filled.Badge,
                                    title = "Passport & ID Photo Studio",
                                    description = "Framing, background color & print layouts",
                                    onClick = onNavigateToPassport
                                )
                            }

                            if (com.scanflow.photocompressor.domain.model.FeatureFlags.ENABLE_SOCIAL) {
                                SecondaryToolRow(
                                    icon = Icons.Filled.Share,
                                    title = "Social Media Sizes",
                                    description = "Instagram, Facebook, YouTube, TikTok presets",
                                    onClick = onNavigateToSocial
                                )
                            }

                            if (com.scanflow.photocompressor.domain.model.FeatureFlags.ENABLE_WHATSAPP) {
                                SecondaryToolRow(
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    title = "WhatsApp Ready",
                                    description = "Small, Balanced, HD & custom optimization",
                                    onClick = onNavigateToWhatsApp
                                )
                            }

                            SecondaryToolRow(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                title = "Rotate & Flip",
                                description = "90° rotation and orientation fix",
                                onClick = onNavigateToRotate
                            )

                            SecondaryToolRow(
                                icon = Icons.Filled.TextFields,
                                title = "Watermark",
                                description = "Protect photos with custom text",
                                onClick = onNavigateToWatermark
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // 5. Recent Activity (Clean & Non-crowded)
        if (uiState.recentHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.totalSavedBytes > 0) {
                        Text(
                            text = "Saved ${formatBytes(uiState.totalSavedBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            items(uiState.recentHistory.take(3)) { history ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = history.inputFileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OperationChip(operationType = history.operation)
                                Text(
                                    text = "${formatBytes(history.originalSize)} → ${formatBytes(history.resultSize)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (history.savedPercentage > 0) {
                            Text(
                                text = "-${String.format("%.0f", history.savedPercentage)}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Success
                            )
                        }
                    }
                }
            }
        }

        // Bottom space for bottom navigation bar
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

/**
 * 2x2 Priority Grid Card:
 * Modern, Minimal, Surface #FFFFFF, Border #E2E8F0, Primary Accent #2563EB.
 */
@Composable
private fun PrimaryGridCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(105.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Secondary tool row inside progressive disclosure.
 */
@Composable
private fun SecondaryToolRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
