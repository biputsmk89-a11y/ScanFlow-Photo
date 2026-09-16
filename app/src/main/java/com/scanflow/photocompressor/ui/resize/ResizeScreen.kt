package com.scanflow.photocompressor.ui.resize

import android.content.Intent
import com.scanflow.photocompressor.util.ShareHelper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.domain.model.ResizePreset
import com.scanflow.photocompressor.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResizeScreen(
    onNavigateBack: () -> Unit,
    initialUri: android.net.Uri? = null,
    viewModel: ResizeViewModel = hiltViewModel()
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
                title = { Text("Resize Image") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
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

                item {
                    TabRow(
                        selectedTabIndex = uiState.activeTab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Tab(
                            selected = uiState.activeTab == ResizeTab.PRESET,
                            onClick = { viewModel.setActiveTab(ResizeTab.PRESET) },
                            text = { Text("Presets") }
                        )
                        Tab(
                            selected = uiState.activeTab == ResizeTab.DIMENSIONS,
                            onClick = { viewModel.setActiveTab(ResizeTab.DIMENSIONS) },
                            text = { Text("Dimensions") }
                        )
                        Tab(
                            selected = uiState.activeTab == ResizeTab.PERCENTAGE,
                            onClick = { viewModel.setActiveTab(ResizeTab.PERCENTAGE) },
                            text = { Text("Scale %") }
                        )
                        Tab(
                            selected = uiState.activeTab == ResizeTab.MAX_DIMENSION,
                            onClick = { viewModel.setActiveTab(ResizeTab.MAX_DIMENSION) },
                            text = { Text("Max Dim") }
                        )
                    }
                }

                when (uiState.activeTab) {
                    ResizeTab.PRESET -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Standard Resolution Presets",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val presets = listOf(
                                    ResizePreset.P_1080,
                                    ResizePreset.P_1440,
                                    ResizePreset.P_1600,
                                    ResizePreset.P_1920,
                                    ResizePreset.P_2560,
                                    ResizePreset.CUSTOM
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(presets) { preset ->
                                        FilterChip(
                                            selected = uiState.selectedPreset == preset,
                                            onClick = { viewModel.setPreset(preset) },
                                            label = { Text(preset.label) }
                                        )
                                    }
                                }
                                if (uiState.selectedPreset != com.scanflow.photocompressor.domain.model.ResizePreset.CUSTOM) {
                                    Text(
                                        "Constrains maximum dimension to ${uiState.selectedPreset.dimension}px (maintains aspect ratio). Target: ${uiState.targetWidth} × ${uiState.targetHeight} px",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        "Custom preset allows freeform width and height configuration.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    ResizeTab.DIMENSIONS -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = uiState.targetWidth,
                                        onValueChange = { viewModel.setTargetWidth(it) },
                                        label = { Text("Width") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    IconButton(onClick = { viewModel.toggleAspectLock() }) {
                                        Icon(
                                            if (uiState.lockAspectRatio) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                            "Aspect ratio lock",
                                            tint = if (uiState.lockAspectRatio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedTextField(
                                        value = uiState.targetHeight,
                                        onValueChange = { viewModel.setTargetHeight(it) },
                                        label = { Text("Height") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                                Text(
                                    if (uiState.lockAspectRatio) "Aspect ratio locked (proportional scaling)" else "Freeform scaling (aspect ratio unlocked)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    ResizeTab.PERCENTAGE -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Scale: ${(uiState.percentage * 100).toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                                Slider(value = uiState.percentage, onValueChange = { viewModel.setPercentage(it) }, valueRange = 0.1f..1f)
                                uiState.imageInfo?.let { info ->
                                    val w = (info.width * uiState.percentage).toInt()
                                    val h = (info.height * uiState.percentage).toInt()
                                    Text("Estimated output resolution: $w × $h px", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    ResizeTab.MAX_DIMENSION -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = uiState.maxDimensionInput,
                                    onValueChange = { viewModel.setMaxDimensionInput(it) },
                                    label = { Text("Maximum Dimension (px)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                Text(
                                    "Constrains the longer edge to this dimension while maintaining aspect ratio.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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

                // 1 Primary Action: Resize Button
                item {
                    Button(
                        onClick = { viewModel.resize() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState.selectedImageUri != null && !uiState.isProcessing,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.AspectRatio, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isProcessing) "Resizing..." else "Resize Image", fontWeight = FontWeight.SemiBold)
                    }
                }

                uiState.error?.let { err ->
                    item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(err, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
                }

                // Result Section with Strict Visual Hierarchy
                uiState.result?.let { result ->
                    item {
                        BeforeAfterPreview(
                            originalUri = uiState.selectedImageUri, resultUri = result.outputUri,
                            originalSize = "${uiState.imageInfo?.resolution ?: ""} • ${String.format("%.1f", result.originalSizeMB)} MB",
                            resultSize = "${result.width}x${result.height} • ${String.format("%.1f", result.compressedSizeMB)} MB",
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
                                    ShareHelper.shareImage(context, result.outputUri, "image/*", "Share Resized Image")
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share Resized Image", fontWeight = FontWeight.SemiBold)
                            }

                            // Secondary Action: Resize Another
                            OutlinedButton(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Resize Another")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
            if (uiState.isProcessing) ProcessingOverlay(isVisible = true, message = "Resizing image...")
        }
    }
}
