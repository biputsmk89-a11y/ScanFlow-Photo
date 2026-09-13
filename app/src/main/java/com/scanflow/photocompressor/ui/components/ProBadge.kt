package com.scanflow.photocompressor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProBadge(
    modifier: Modifier = Modifier
) {
    val goldGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFFB300), // Amber Gold
            Color(0xFFFF8F00)  // Deep Amber
        )
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(goldGradient)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = "Pro",
            tint = Color.White,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "PRO",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )
    }
}
