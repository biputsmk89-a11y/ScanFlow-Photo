package com.scanflow.photocompressor.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.scanflow.photocompressor.ui.theme.Success
import com.scanflow.photocompressor.ui.theme.SuccessContainer

enum class ComparisonDisplayMode {
    BEFORE_AFTER_TOGGLE,
    SIDE_BY_SIDE
}

enum class ActivePreviewTarget {
    BEFORE,
    AFTER
}

/**
 * Memory-efficient Before/After preview component.
 *
 * Constraints respected:
 * - Downsamples preview bitmaps to a small bounded resolution (max 512x512)
 *   so two full-resolution multi-megapixel Bitmaps are never held simultaneously in memory.
 * - Supports interactive Before ↔ After comparison toggle with smooth crossfade.
 * - Supports Side-by-Side comparison mode.
 */
@Composable
fun BeforeAfterPreview(
    originalUri: Uri?,
    resultUri: Uri?,
    originalSize: String,
    resultSize: String,
    savedPercentage: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var displayMode by remember { mutableStateOf(ComparisonDisplayMode.BEFORE_AFTER_TOGGLE) }
    var activeTarget by remember { mutableStateOf(ActivePreviewTarget.AFTER) }

    // Memory-safe scaled ImageRequests (bounded via ImagePreviewStrategy, eliminating full-res decodes)
    val originalImageRequest = remember(originalUri) {
        originalUri?.let {
            com.scanflow.photocompressor.ui.preview.ImagePreviewStrategy.buildPreviewRequest(
                context = context,
                data = it,
                tier = com.scanflow.photocompressor.ui.preview.PreviewTier.CARD_PREVIEW
            )
        }
    }

    val resultImageRequest = remember(resultUri) {
        resultUri?.let {
            com.scanflow.photocompressor.ui.preview.ImagePreviewStrategy.buildPreviewRequest(
                context = context,
                data = it,
                tier = com.scanflow.photocompressor.ui.preview.PreviewTier.CARD_PREVIEW
            )
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Comparison Mode Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Before ↔ After Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Mode switch: Toggle vs Side-by-Side
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { displayMode = ComparisonDisplayMode.BEFORE_AFTER_TOGGLE },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SwapHoriz,
                                contentDescription = "Before ↔ After Toggle",
                                tint = if (displayMode == ComparisonDisplayMode.BEFORE_AFTER_TOGGLE) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { displayMode = ComparisonDisplayMode.SIDE_BY_SIDE },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ViewColumn,
                                contentDescription = "Side by Side",
                                tint = if (displayMode == ComparisonDisplayMode.SIDE_BY_SIDE) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (displayMode) {
                // 1. BEFORE ↔ AFTER TOGGLE MODE
                ComparisonDisplayMode.BEFORE_AFTER_TOGGLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Quick Toggle Chips: Before vs After
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeTarget == ActivePreviewTarget.BEFORE,
                                onClick = { activeTarget = ActivePreviewTarget.BEFORE },
                                label = { Text("Before ($originalSize)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = activeTarget == ActivePreviewTarget.AFTER,
                                onClick = { activeTarget = ActivePreviewTarget.AFTER },
                                label = { Text("After ($resultSize)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Interactive image viewport (Tap to flip Before ↔ After)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.05f))
                                .clickable {
                                    activeTarget = if (activeTarget == ActivePreviewTarget.BEFORE) {
                                        ActivePreviewTarget.AFTER
                                    } else {
                                        ActivePreviewTarget.BEFORE
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = activeTarget,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "previewTransition"
                            ) { target ->
                                val request = if (target == ActivePreviewTarget.BEFORE) originalImageRequest else resultImageRequest
                                if (request != null) {
                                    AsyncImage(
                                        model = request,
                                        contentDescription = target.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No preview available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Active Badge pill overlay at bottom
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.7f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (activeTarget == ActivePreviewTarget.BEFORE) "BEFORE • $originalSize (Tap to switch)" else "AFTER • $resultSize (Tap to switch)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. SIDE BY SIDE MODE
                ComparisonDisplayMode.SIDE_BY_SIDE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Original (Before)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Before",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (originalImageRequest != null) {
                                AsyncImage(
                                    model = originalImageRequest,
                                    contentDescription = "Original",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = originalSize,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Compressed (After)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "After",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (resultImageRequest != null) {
                                AsyncImage(
                                    model = resultImageRequest,
                                    contentDescription = "Compressed",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resultSize,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val cleanPercentStr = savedPercentage.replace("%", "").trim()
            val percentVal = cleanPercentStr.toDoubleOrNull()

            val (bannerText, bannerColor, bannerBg) = when {
                percentVal == null -> {
                    Triple(
                        savedPercentage,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                }
                percentVal > 0.05 -> {
                    Triple(
                        "💾 Saved ${String.format(java.util.Locale.US, "%.1f", percentVal)}%",
                        Success,
                        SuccessContainer
                    )
                }
                percentVal < -0.05 -> {
                    val increaseVal = kotlin.math.abs(percentVal)
                    Triple(
                        "📈 Size increased by ${String.format(java.util.Locale.US, "%.1f", increaseVal)}%",
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                else -> {
                    Triple(
                        "⚖️ Size unchanged",
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            // Summary banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = bannerBg
            ) {
                Text(
                    text = bannerText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = bannerColor
                )
            }
        }
    }
}
