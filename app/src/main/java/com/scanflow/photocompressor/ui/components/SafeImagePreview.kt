package com.scanflow.photocompressor.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.size.Scale
import com.scanflow.photocompressor.ui.preview.ImagePreviewStrategy
import com.scanflow.photocompressor.ui.preview.PreviewTier

/**
 * Memory-safe, scaled, smooth image preview composable.
 *
 * Guarantees:
 * - Scaled: Explicitly constrained by [tier] to prevent full-resolution decodes.
 * - Memory safe: Uses Coil downsampling with Precision.INEXACT and hardware bitmap allocation.
 * - Smooth: Smooth crossfade transitions with placeholder and fallback states.
 */
@Composable
fun SafeImagePreview(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tier: PreviewTier = PreviewTier.CARD_PREVIEW,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center
) {
    val context = LocalContext.current
    val imageRequest = remember(data, tier, contentScale) {
        data?.let {
            ImagePreviewStrategy.buildPreviewRequest(
                context = context,
                data = it,
                tier = tier,
                scale = if (contentScale == ContentScale.Fit) Scale.FIT else Scale.FILL
            )
        }
    }

    if (imageRequest != null) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Smooth subtle placeholder box without blocking UI
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrokenImage,
                        contentDescription = "Error loading preview",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    }
}

/**
 * Convenient micro-thumbnail wrapper for list items, batch selection rows, etc.
 * Uses [PreviewTier.THUMBNAIL] (max 384px, RGB_565 config).
 */
@Composable
fun SafeThumbnail(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    SafeImagePreview(
        data = data,
        contentDescription = contentDescription,
        modifier = modifier,
        tier = PreviewTier.THUMBNAIL,
        contentScale = contentScale
    )
}
