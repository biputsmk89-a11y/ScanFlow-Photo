package com.scanflow.photocompressor.ui.passport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.ui.components.ImagePickerCard
import com.scanflow.photocompressor.ui.components.SafeImagePreview
import com.scanflow.photocompressor.ui.preview.PreviewTier
import com.scanflow.photocompressor.ui.theme.Success
import com.scanflow.photocompressor.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportScreen(
    onNavigateBack: () -> Unit,
    viewModel: PassportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAdvancedOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passport & ID Photo Studio") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SELECT IMAGE
            item {
                Text(
                    text = "1. Select Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ImagePickerCard(
                    selectedImageUri = uiState.selectedImageUri,
                    onImageSelected = { viewModel.selectImage(it) }
                )
            }

            if (uiState.selectedImageUri != null) {
                // 2. FACE & POSITION GUIDANCE PREVIEW
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Face & Position Guidance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.autoCenter() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.FilterCenterFocus, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Center", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { viewModel.resetPosition() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Position face within guidelines: eyes on upper line, chin on lower line, shoulders balanced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preview Container with Simulated Background & Frame
                    val previewBgColor = uiState.config.effectiveBackgroundColor?.let { Color(it) } ?: Color.Black
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(previewBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        SafeImagePreview(
                            data = uiState.selectedImageUri,
                            contentDescription = "Passport Source",
                            modifier = Modifier.fillMaxSize(),
                            tier = PreviewTier.FULL_PREVIEW,
                            contentScale = ContentScale.Fit
                        )

                        // Professional Geometric Face & Position Guide Overlay
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val centerX = w / 2f
                            val centerY = h / 2f

                            val ovalW = w * 0.40f
                            val ovalH = h * 0.58f

                            val dashStroke = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                            val guideColor = Color.Cyan.copy(alpha = 0.85f)
                            val lineGuideColor = Color.White.copy(alpha = 0.7f)

                            // Top Margin Line
                            val topMarginY = centerY - (ovalH / 2f) - (h * 0.08f)
                            drawLine(
                                color = lineGuideColor,
                                start = Offset(w * 0.15f, topMarginY),
                                end = Offset(w * 0.85f, topMarginY),
                                strokeWidth = 1.5f,
                                pathEffect = dashStroke.pathEffect
                            )

                            // Center Vertical Line
                            drawLine(
                                color = lineGuideColor.copy(alpha = 0.4f),
                                start = Offset(centerX, topMarginY),
                                end = Offset(centerX, h * 0.95f),
                                strokeWidth = 1f,
                                pathEffect = dashStroke.pathEffect
                            )

                            // Head / Face Oval Zone
                            drawOval(
                                color = guideColor,
                                topLeft = Offset(centerX - (ovalW / 2f), centerY - (ovalH / 2f) - (h * 0.04f)),
                                size = Size(ovalW, ovalH),
                                style = dashStroke
                            )

                            // Eye Level Line
                            val eyeY = centerY - (h * 0.10f)
                            drawLine(
                                color = guideColor,
                                start = Offset(centerX - (ovalW * 0.65f), eyeY),
                                end = Offset(centerX + (ovalW * 0.65f), eyeY),
                                strokeWidth = 2f
                            )

                            // Chin Level Line
                            val chinY = centerY + (ovalH / 2f) - (h * 0.04f)
                            drawLine(
                                color = guideColor,
                                start = Offset(centerX - (ovalW * 0.45f), chinY),
                                end = Offset(centerX + (ovalW * 0.45f), chinY),
                                strokeWidth = 2f
                            )

                            // Shoulder Zone Guide (Arch / Baseline)
                            val shoulderY = chinY + (h * 0.14f)
                            drawLine(
                                color = lineGuideColor,
                                start = Offset(w * 0.10f, shoulderY),
                                end = Offset(w * 0.90f, shoulderY),
                                strokeWidth = 2f,
                                pathEffect = dashStroke.pathEffect
                            )
                        }

                        // ID Frame Preview Overlay
                        when (uiState.config.frameStyle) {
                            IdFrameStyle.NONE -> {}
                            IdFrameStyle.THIN -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                                )
                            }
                            IdFrameStyle.CLASSIC -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(6.dp, Color.White, RoundedCornerShape(12.dp))
                                        .padding(6.dp)
                                        .border(1.dp, Color.LightGray)
                                )
                            }
                            IdFrameStyle.PROFESSIONAL -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .border(8.dp, Color.White, RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                        .border(1.5.dp, Color(0xFFD0D0D0))
                                )
                            }
                        }
                    }
                }

                // 100% OFFLINE HONESTY & TRANSPARENCY NOTICE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "100% Offline & Private Studio",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Foto diproses lokal tanpa model AI atau cloud upload. Gunakan alat framing dan warna latar untuk menyiapkan foto identitas standar secara instan.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. PRESET SPECIFICATION: 2 × 3, 3 × 4, 4 × 6, Custom
                item {
                    Text(
                        text = "3. Photo Size",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PassportSpec.values().forEach { spec ->
                            FilterChip(
                                selected = uiState.config.spec == spec,
                                onClick = { viewModel.updateSpec(spec) },
                                label = {
                                    Text(
                                        text = spec.displayName,
                                        fontWeight = if (uiState.config.spec == spec) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 4. BACKGROUND COLOR SIMULATION (PREVIEW & COMPOSITION)
                item {
                    Text(
                        text = "4. Background Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PassportBackground.values().filter { it != PassportBackground.CUSTOM }.forEach { bg ->
                            FilterChip(
                                selected = uiState.config.background == bg,
                                onClick = { viewModel.updateBackground(bg) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (bg.colorInt != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(bg.colorInt))
                                                    .border(1.dp, Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(bg.displayName, style = MaterialTheme.typography.labelMedium)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. PROFESSIONAL ID FRAME
                item {
                    Text(
                        text = "5. ID Frame Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IdFrameStyle.values().forEach { frame ->
                            FilterChip(
                                selected = uiState.config.frameStyle == frame,
                                onClick = { viewModel.updateFrameStyle(frame) },
                                label = {
                                    Text(
                                        text = frame.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (uiState.config.frameStyle == frame) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 6. COLLAPSIBLE ADVANCED OPTIONS (Zoom, Rotation, Custom Color)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAdvancedOptions = !showAdvancedOptions },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Tune, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Advanced Options", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                }
                                Icon(
                                    if (showAdvancedOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(visible = showAdvancedOptions) {
                                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Zoom
                                    Text("Zoom: ${(uiState.config.zoom * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = uiState.config.zoom,
                                        onValueChange = { viewModel.updateZoom(it) },
                                        valueRange = 0.8f..2.5f
                                    )

                                    // Rotation
                                    Text("Rotation: ${uiState.config.rotationDegrees.toInt()}°", style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = uiState.config.rotationDegrees,
                                        onValueChange = { viewModel.updateRotation(it) },
                                        valueRange = -15f..15f
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. PRINT LAYOUT: 1, 2, 4, 6, 8, 9, 12
                item {
                    Text(
                        text = "6. Print Layout (Copies per sheet)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PassportPrintLayout.values().toList()) { layout ->
                            FilterChip(
                                selected = uiState.config.printLayout == layout,
                                onClick = { viewModel.updatePrintLayout(layout) },
                                label = {
                                    Text(
                                        text = "${layout.displayName} ${if (layout.copies == 1) "Copy" else "Copies"}",
                                        fontWeight = if (uiState.config.printLayout == layout) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            )
                        }
                    }

                    if (uiState.config.printLayout != PassportPrintLayout.COPIES_1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.config.addCutMarks,
                                onCheckedChange = { viewModel.updateAddCutMarks(it) }
                            )
                            Text("Add cut guide marks (dashed lines)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // 8. PROCESS BUTTON
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.processPassport() },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Rendering ID photo sheet...")
                        } else {
                            Icon(Icons.Filled.Badge, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate ID Photo Sheet", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 9. RESULT CARD
                val activeResult = uiState.result
                if (activeResult != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Success,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ID Photo Ready!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeResult.width} × ${activeResult.height} px • ${String.format("%.1f", activeResult.compressedSize / 1024.0)} KB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                SafeImagePreview(
                                    data = activeResult.outputUri,
                                    contentDescription = "Passport Result",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    tier = PreviewTier.CARD_PREVIEW,
                                    contentScale = ContentScale.Fit
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        ShareHelper.shareImage(
                                            context,
                                            activeResult.outputUri,
                                            "image/jpeg",
                                            "Share Passport Photo"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Filled.Share, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share / Print Photo")
                                }
                            }
                        }
                    }
                }

                // ERROR MESSAGE
                val error = uiState.errorMessage
                if (error != null) {
                    item {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

