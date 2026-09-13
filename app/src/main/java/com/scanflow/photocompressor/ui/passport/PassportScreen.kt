package com.scanflow.photocompressor.ui.passport

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Passport & ID Photo") },
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
                // 2. FACE GUIDANCE & PREVIEW
                item {
                    Text(
                        text = "2. Face Guidance Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Align face within oval: eyes on upper line, chin on lower line (70–80% photo height).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        SafeImagePreview(
                            data = uiState.selectedImageUri,
                            contentDescription = "Passport Source",
                            modifier = Modifier.fillMaxSize(),
                            tier = PreviewTier.FULL_PREVIEW,
                            contentScale = ContentScale.Fit
                        )

                        // Biometric Face Guidance Overlay (Oval, Eye line, Chin line)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val centerX = w / 2f
                            val centerY = h / 2f

                            val ovalW = w * 0.42f
                            val ovalH = h * 0.62f

                            val dashStroke = Stroke(
                                width = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            )
                            val guideColor = Color.Yellow.copy(alpha = 0.85f)

                            // Head oval
                            drawOval(
                                color = guideColor,
                                topLeft = Offset(centerX - (ovalW / 2f), centerY - (ovalH / 2f) - (h * 0.04f)),
                                size = Size(ovalW, ovalH),
                                style = dashStroke
                            )

                            // Eye level line
                            val eyeY = centerY - (h * 0.10f)
                            drawLine(
                                color = guideColor,
                                start = Offset(centerX - (ovalW * 0.6f), eyeY),
                                end = Offset(centerX + (ovalW * 0.6f), eyeY),
                                strokeWidth = 2f
                            )

                            // Chin level line
                            val chinY = centerY + (ovalH / 2f) - (h * 0.04f)
                            drawLine(
                                color = guideColor,
                                start = Offset(centerX - (ovalW * 0.4f), chinY),
                                end = Offset(centerX + (ovalW * 0.4f), chinY),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                // DISCLAIMER NOTICE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.config.disclaimer,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // 3. PRESET SPECIFICATION: 2 × 3, 3 × 4, 4 × 6, Custom
                item {
                    Text(
                        text = "3. Select Preset",
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

                // 4. BACKGROUND COLOR
                item {
                    Text(
                        text = "4. Background Color",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PassportBackground.values().forEach { bg ->
                            FilterChip(
                                selected = uiState.config.background == bg,
                                onClick = { viewModel.updateBackground(bg) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (bg.colorInt != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(bg.colorInt))
                                                    .border(1.dp, Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(bg.displayName, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. PRINT LAYOUT: 1, 2, 4, 6, 8, 9, 12
                item {
                    Text(
                        text = "5. Print Layout (Copies per sheet)",
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
                                        text = layout.displayName,
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

                // 6. PROCESS BUTTON
                item {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            Text("Generating passport photo...")
                        } else {
                            Icon(Icons.Filled.Badge, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Passport Photo", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 7. RESULT CARD
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
                                    text = "Passport Photo Ready!",
                                    style = MaterialTheme.typography.titleSmall,
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
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    tier = PreviewTier.CARD_PREVIEW,
                                    contentScale = ContentScale.Fit
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        ShareHelper.shareImage(
                                            context,
                                            activeResult.outputUri,
                                            "image/jpeg",
                                            "Share Passport Photo"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
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
