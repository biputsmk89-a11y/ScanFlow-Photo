package com.scanflow.photocompressor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scanflow.photocompressor.ui.theme.GradientEnd
import com.scanflow.photocompressor.ui.theme.GradientStart

/**
 * Dashboard stat display card with icon, value, and label.
 */
@Composable
fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    gradient: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (gradient)
                Color.Transparent
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = if (gradient) {
                Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = if (gradient) Color.White.copy(alpha = 0.9f)
                    else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (gradient) Color.White
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (gradient) Color.White.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
