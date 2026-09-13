package com.scanflow.photocompressor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.scanflow.photocompressor.ui.theme.GradientEnd
import com.scanflow.photocompressor.ui.theme.GradientStart

/**
 * Animated circular progress indicator with percentage text.
 */
@Composable
fun AnimatedCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = animatedProgress * 360f
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)

            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(this.size.width, this.size.height)
            )

            // Progress
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(GradientStart, GradientEnd, GradientStart)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = stroke,
                size = Size(this.size.width, this.size.height)
            )
        }

        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Animated linear progress bar with gradient.
 */
@Composable
fun GradientLinearProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "linearProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Track
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height.toPx() / 2)
            )
        }

        // Progress
        Canvas(
            modifier = Modifier
                .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                .fillMaxHeight()
        ) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(GradientStart, GradientEnd)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(height.toPx() / 2)
            )
        }
    }
}

/**
 * Full processing overlay adhering to Rule 37:
 * - Single: "Compressing..." with percentage display (e.g. "42%")
 * - Batch: "Processing" with count display (e.g. "43 / 100")
 * - Support "Cancel" button
 * - NO fake / unreliable ETA displays
 */
@Composable
fun ProcessingOverlay(
    isVisible: Boolean,
    progress: Float = -1f, // -1 for indeterminate
    message: String = "Processing...",
    progressText: String? = null, // e.g. "42%" or "43 / 100"
    cancelLabel: String = "Cancel",
    onCancel: (() -> Unit)? = null
) {
    if (isVisible) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (progress >= 0f) {
                    AnimatedCircularProgress(progress = progress, size = 110.dp)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 5.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (!progressText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(28.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cancelLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
