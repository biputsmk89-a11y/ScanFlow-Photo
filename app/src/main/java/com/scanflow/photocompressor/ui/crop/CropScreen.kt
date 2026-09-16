package com.scanflow.photocompressor.ui.crop

import android.content.Intent
import com.scanflow.photocompressor.util.ShareHelper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.scanflow.photocompressor.domain.model.AspectRatioPreset
import com.scanflow.photocompressor.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropScreen(
    onNavigateBack: () -> Unit,
    initialUri: android.net.Uri? = null,
    viewModel: CropViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialUri) {
        if (initialUri != null && uiState.selectedImageUri != initialUri) {
            viewModel.selectImage(initialUri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crop Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.selectedImageUri != null) {
                        IconButton(onClick = { viewModel.rotate90() }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate 90°")
                        }
                        IconButton(onClick = { viewModel.resetTransform() }) {
                            Icon(Icons.Filled.RestartAlt, "Reset")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ImagePickerCard(
                        selectedImageUri = uiState.selectedImageUri,
                        onImageSelected = { viewModel.selectImage(it) }
                    )
                }

                if (uiState.selectedImageUri != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Interactive Viewport (Pinch / Drag)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (uiState.rotationDegrees != 0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                "${uiState.rotationDegrees}°",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Interactive Crop Canvas
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                        .clipToBounds()
                                        .onSizeChanged { size ->
                                            viewModel.setViewportSize(size.width, size.height)
                                        }
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                viewModel.updateTransform(zoom, pan.x, pan.y)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val cropPreviewRequest = remember(uiState.selectedImageUri) {
                                        uiState.selectedImageUri?.let {
                                            com.scanflow.photocompressor.ui.preview.ImagePreviewStrategy.buildPreviewRequest(
                                                context = context,
                                                data = it,
                                                tier = com.scanflow.photocompressor.ui.preview.PreviewTier.FULL_PREVIEW,
                                                scale = coil.size.Scale.FIT
                                            )
                                        }
                                    }
                                    AsyncImage(
                                        model = cropPreviewRequest,
                                        contentDescription = "Crop target",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = uiState.zoomScale
                                                scaleY = uiState.zoomScale
                                                translationX = uiState.panOffsetX
                                                translationY = uiState.panOffsetY
                                                rotationZ = uiState.rotationDegrees.toFloat()
                                            },
                                        contentScale = ContentScale.Fit
                                    )

                                    // Rule of Thirds Crop Overlay
                                    val currentAspectRatio = uiState.selectedAspectRatio
                                    val customRatioX = uiState.customRatioX.toFloatOrNull() ?: 1f
                                    val customRatioY = uiState.customRatioY.toFloatOrNull() ?: 1f
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height

                                        val targetAspect = when {
                                            currentAspectRatio == AspectRatioPreset.CUSTOM -> {
                                                if (customRatioY > 0f) customRatioX / customRatioY else 1f
                                            }
                                            currentAspectRatio.ratio != null -> currentAspectRatio.ratio ?: 1f
                                            else -> canvasWidth / canvasHeight
                                        }

                                        val (boxWidth, boxHeight) = if (canvasWidth / canvasHeight > targetAspect) {
                                            val h = canvasHeight * 0.9f
                                            Pair(h * targetAspect, h)
                                        } else {
                                            val w = canvasWidth * 0.9f
                                            Pair(w, w / targetAspect)
                                        }

                                        val left = (canvasWidth - boxWidth) / 2f
                                        val top = (canvasHeight - boxHeight) / 2f

                                        // Darkened backdrop around crop frame
                                        drawRect(Color(0x77000000))
                                        // Clear crop box center
                                        drawRect(
                                            color = Color.Transparent,
                                            topLeft = Offset(left, top),
                                            size = Size(boxWidth, boxHeight)
                                        )

                                        // White border
                                        drawRect(
                                            color = Color.White,
                                            topLeft = Offset(left, top),
                                            size = Size(boxWidth, boxHeight),
                                            style = Stroke(width = 2.dp.toPx())
                                        )

                                        // Rule of Thirds Grid Lines
                                        val thirdW = boxWidth / 3f
                                        val thirdH = boxHeight / 3f
                                        val gridColor = Color(0x66FFFFFF)
                                        val gridStroke = Stroke(width = 1.dp.toPx())

                                        // Vertical lines
                                        drawLine(gridColor, Offset(left + thirdW, top), Offset(left + thirdW, top + boxHeight), gridStroke.width)
                                        drawLine(gridColor, Offset(left + 2 * thirdW, top), Offset(left + 2 * thirdW, top + boxHeight), gridStroke.width)

                                        // Horizontal lines
                                        drawLine(gridColor, Offset(left, top + thirdH), Offset(left + boxWidth, top + thirdH), gridStroke.width)
                                        drawLine(gridColor, Offset(left, top + 2 * thirdH), Offset(left + boxWidth, top + 2 * thirdH), gridStroke.width)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Zoom Control Slider & Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom", modifier = Modifier.size(20.dp))
                                    Slider(
                                        value = uiState.zoomScale,
                                        onValueChange = { viewModel.setZoom(it) },
                                        valueRange = 1f..5f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        String.format("%.1fx", uiState.zoomScale),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Aspect Ratio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            val presets = listOf(
                                AspectRatioPreset.FREE,
                                AspectRatioPreset.SQUARE,
                                AspectRatioPreset.RATIO_4_5,
                                AspectRatioPreset.RATIO_3_4,
                                AspectRatioPreset.RATIO_4_3,
                                AspectRatioPreset.RATIO_16_9,
                                AspectRatioPreset.RATIO_9_16,
                                AspectRatioPreset.CUSTOM
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(presets) { preset ->
                                    FilterChip(
                                        selected = uiState.selectedAspectRatio == preset,
                                        onClick = { viewModel.setAspectRatio(preset) },
                                        label = { Text(preset.label) }
                                    )
                                }
                            }

                            if (uiState.selectedAspectRatio == AspectRatioPreset.CUSTOM) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = uiState.customRatioX,
                                        onValueChange = { viewModel.setCustomRatio(it, uiState.customRatioY) },
                                        label = { Text("Ratio X") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    Text(":", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    OutlinedTextField(
                                        value = uiState.customRatioY,
                                        onValueChange = { viewModel.setCustomRatio(uiState.customRatioX, it) },
                                        label = { Text("Ratio Y") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "Target Crop Resolution: ${uiState.cropWidth} × ${uiState.cropHeight} px",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Advanced Settings (Progressive Disclosure)
                    item {
                        AdvancedSettingsCard(
                            title = "Advanced Settings",
                            summary = "Output Quality: ${uiState.quality}%"
                        ) {
                            QualitySlider(
                                quality = uiState.quality,
                                onQualityChange = { viewModel.setQuality(it) }
                            )
                        }
                    }

                    // 1 Primary Action: Crop Button
                    item {
                        Button(
                            onClick = { viewModel.crop() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !uiState.isProcessing,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Crop, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isProcessing) "Cropping..." else "Crop Image", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                uiState.error?.let { err ->
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(err, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Result Section with Strict Visual Hierarchy
                uiState.result?.let { result ->
                    item {
                        BeforeAfterPreview(
                            originalUri = uiState.selectedImageUri,
                            resultUri = result.outputUri,
                            originalSize = "${uiState.imageInfo?.resolution ?: ""}",
                            resultSize = "${result.width}x${result.height}",
                            savedPercentage = String.format("%.1f%%", result.savedPercentage)
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Primary Action: Share
                            Button(
                                onClick = {
                                    ShareHelper.shareImage(context, result.outputUri, "image/*", "Share Cropped Image")
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Cropped Image", fontWeight = FontWeight.SemiBold)
                            }

                            // Secondary Action: Crop Another
                            OutlinedButton(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crop Another")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            if (uiState.isProcessing) ProcessingOverlay(isVisible = true, message = "Cropping image...")
        }
    }
}
