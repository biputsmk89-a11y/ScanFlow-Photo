package com.scanflow.photocompressor.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.scanflow.photocompressor.domain.model.FeatureFlags
import com.scanflow.photocompressor.ui.components.ScanFlowHeader
import com.scanflow.photocompressor.ui.theme.*
import com.scanflow.photocompressor.util.AnalyticsEvent
import com.scanflow.photocompressor.util.AnalyticsLogger

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
    val context = LocalContext.current
    var isMoreToolsExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header (ScanFlow Minimal)
        ScanFlowHeader(
            title = "Home",
            subtitle = "Photo Compressor + Tools",
            onQuickActionClick = onNavigateToBatch,
            quickActionIcon = Icons.Filled.CollectionsBookmark
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 0. Privacy Badge
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Private • Offline • Simple",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Hero: Main Photo Compression Card (Compact & Focused)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            AnalyticsLogger.logEvent(
                                AnalyticsEvent.TOOL_OPENED,
                                mapOf("tool" to "compress")
                            )
                            onNavigateToCompress()
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceContainerLowest,
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Top Row: Icon + Status Indicator (Non-clickable)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Compress,
                                            contentDescription = "Compress",
                                            tint = Primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Success, CircleShape)
                                        )
                                        Text(
                                            text = "Local Engine",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Title & Description
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Compress Photos",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    text = "Make your photos smaller without complicated settings. Preserves visual detail.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )
                            }

                            // Primary CTA Button
                            Button(
                                onClick = {
                                    AnalyticsLogger.logEvent(
                                        AnalyticsEvent.TOOL_OPENED,
                                        mapOf("tool" to "compress_cta")
                                    )
                                    onNavigateToCompress()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Select Photos",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Example result indicator
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.PhotoSizeSelectSmall,
                                                    contentDescription = null,
                                                    tint = Primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "Example result",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Typical reduction ~85%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Text(
                                            text = "6.4 MB → 780 KB",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Tertiary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Quick Tools Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quick Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "One-Tap Utility",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StitchToolCard(
                            icon = Icons.Filled.AspectRatio,
                            title = "Resize",
                            subtitle = "Change pixel dimensions",
                            onClick = {
                                AnalyticsLogger.logEvent(AnalyticsEvent.TOOL_OPENED, mapOf("tool" to "resize"))
                                onNavigateToResize()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StitchToolCard(
                            icon = Icons.Filled.Transform,
                            title = "Convert",
                            subtitle = "JPG, PNG, WEBP",
                            onClick = {
                                AnalyticsLogger.logEvent(AnalyticsEvent.TOOL_OPENED, mapOf("tool" to "convert"))
                                onNavigateToConvert()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StitchToolCard(
                            icon = Icons.Filled.Crop,
                            title = "Crop",
                            subtitle = "Aspect ratios & angles",
                            onClick = {
                                AnalyticsLogger.logEvent(AnalyticsEvent.TOOL_OPENED, mapOf("tool" to "crop"))
                                onNavigateToCrop()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        StitchToolCard(
                            icon = Icons.Filled.CollectionsBookmark,
                            title = "Batch",
                            subtitle = "Process multiple photos",
                            onClick = {
                                AnalyticsLogger.logEvent(AnalyticsEvent.TOOL_OPENED, mapOf("tool" to "batch"))
                                onNavigateToBatch()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Live Visual Insight Preview (Last Processed Photo)
            val latestHistory = uiState.recentHistory.firstOrNull()
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (latestHistory != null) "Last Processed Photo" else "Photo Engine Ready",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                if (latestHistory != null) {
                                    val savedPct = latestHistory.savedPercentage.coerceAtLeast(0.0)
                                    Text(
                                        text = "Saved ${String.format("%.0f", savedPct)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Tertiary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                } else {
                                    Text(
                                        text = "100% Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Tertiary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Image Thumbnail
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                if (latestHistory?.outputUri != null) {
                                    val uri = runCatching { Uri.parse(latestHistory.outputUri) }.getOrNull()
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "Recent photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PhotoLibrary,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                if (latestHistory != null) {
                                    Text(
                                        text = latestHistory.inputFileName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = formatBytes(latestHistory.originalSize),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = formatBytes(latestHistory.resultSize),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Optimized for Web & Storage",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "No photos processed yet",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Select a tool above to start compressing or editing photos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Share Action Button
                            IconButton(
                                onClick = {
                                    val uri = latestHistory?.outputUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                                    if (uri != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/*"
                                            putExtra(Intent.EXTRA_STREAM, uri as Parcelable)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Photo"))
                                    } else {
                                        onNavigateToCompress()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Icon(
                                    imageVector = if (latestHistory != null) Icons.Filled.Share else Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = if (latestHistory != null) "Share" else "Start",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. More Tools Section (Google Stitch Workflows)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMoreToolsExpanded = !isMoreToolsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More Tools",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isMoreToolsExpanded) "Hide" else "Show All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Primary
                            )
                            Icon(
                                imageVector = if (isMoreToolsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = isMoreToolsExpanded) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            if (FeatureFlags.ENABLE_PDF) {
                                StitchMoreToolRow(
                                    icon = Icons.Filled.PictureAsPdf,
                                    iconBgColor = Color(0xFFEF4444),
                                    title = "PDF Document Maker",
                                    description = "Combine images into compressed PDF",
                                    onClick = onNavigateToPdf
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                    thickness = 0.5.dp
                                )
                            }

                            if (FeatureFlags.ENABLE_PASSPORT) {
                                StitchMoreToolRow(
                                    icon = Icons.Filled.Badge,
                                    iconBgColor = Color(0xFF4F46E5),
                                    title = "Passport & ID Studio",
                                    description = "Passport, ID & visa photo sizes",
                                    badgeText = "AI OFFLINE",
                                    badgeColor = Color(0xFF4F46E5),
                                    onClick = onNavigateToPassport
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                    thickness = 0.5.dp
                                )
                            }

                            if (FeatureFlags.ENABLE_SOCIAL) {
                                StitchMoreToolRow(
                                    icon = Icons.AutoMirrored.Filled.Feed,
                                    iconBgColor = Color(0xFFE1306C),
                                    title = "Social Media Sizer",
                                    description = "Presets for Stories, Posts & Covers",
                                    onClick = onNavigateToSocial
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                    thickness = 0.5.dp
                                )
                            }

                            if (FeatureFlags.ENABLE_WHATSAPP) {
                                StitchMoreToolRow(
                                    icon = Icons.AutoMirrored.Filled.Chat,
                                    iconBgColor = Color(0xFF10B981),
                                    title = "WhatsApp Optimizer",
                                    description = "Optimize photos for WhatsApp sharing",
                                    onClick = onNavigateToWhatsApp
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                    thickness = 0.5.dp
                                )
                            }

                            StitchMoreToolRow(
                                icon = Icons.AutoMirrored.Filled.RotateRight,
                                iconBgColor = Color(0xFF0EA5E9),
                                title = "Rotate & Flip",
                                description = "90° rotation and orientation fix",
                                onClick = onNavigateToRotate
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                thickness = 0.5.dp
                            )

                            StitchMoreToolRow(
                                icon = Icons.Filled.TextFields,
                                iconBgColor = Color(0xFFF59E0B),
                                title = "Watermark",
                                description = "Protect photos with custom text",
                                onClick = onNavigateToWatermark
                            )
                        }
                    }
                }
            }
        }

            // Space for Bottom Navigation Bar
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * 2x2 Quick Tool Card (Identik Google Stitch Layout)
 */
@Composable
private fun StitchToolCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(20.dp),
                        tint = Primary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Row inside "More Tools" card (iOS Settings & Shortcuts Aesthetic)
 */
@Composable
private fun StitchMoreToolRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color = Color.White,
    title: String,
    description: String,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBgColor,
                modifier = Modifier.size(36.dp),
                shadowElevation = 0.5.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
            modifier = Modifier.size(18.dp)
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
