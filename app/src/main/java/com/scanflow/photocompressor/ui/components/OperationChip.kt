package com.scanflow.photocompressor.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scanflow.photocompressor.domain.model.OperationType

/**
 * Chip for displaying operation type with color-coded background.
 */
@Composable
fun OperationChip(
    operationType: OperationType,
    modifier: Modifier = Modifier
) {
    val (containerColor, contentColor) = getOperationColors(operationType)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = operationType.displayName,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

private fun getOperationColors(type: OperationType): Pair<Color, Color> {
    return when (type) {
        OperationType.COMPRESS -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        OperationType.RESIZE -> Pair(Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        OperationType.CROP -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        OperationType.ROTATE -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00))
        OperationType.FLIP -> Pair(Color(0xFFFCE4EC), Color(0xFFC62828))
        OperationType.WATERMARK -> Pair(Color(0xFFE0F2F1), Color(0xFF00695C))
        OperationType.CONVERT -> Pair(Color(0xFFF1F8E9), Color(0xFF558B2F))
        OperationType.BATCH -> Pair(Color(0xFFEDE7F6), Color(0xFF4527A0))
    }
}
