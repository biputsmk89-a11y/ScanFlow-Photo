package com.scanflow.photocompressor.ui.convert

import android.content.Intent
import com.scanflow.photocompressor.util.ShareHelper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    onNavigateBack: () -> Unit,
    initialUri: android.net.Uri? = null,
    viewModel: ConvertViewModel = hiltViewModel()
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
                title = { Text("Convert Format") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ImagePickerCard(
                        selectedImageUri = uiState.selectedImageUri,
                        onImageSelected = { viewModel.selectImage(it) }
                    )
                }

                uiState.imageInfo?.let { info ->
                    item {
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Source Format: ${info.format.name} (${info.mimeType})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (info.hasAlpha) "Contains alpha channel (transparency)" else "Opaque image (no alpha)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (info.hasAlpha) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Target Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        val formats = listOf(ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            formats.forEach { fmt ->
                                FilterChip(
                                    selected = uiState.targetFormat == fmt,
                                    onClick = { viewModel.setFormat(fmt) },
                                    label = { Text(fmt.name, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Format Rules & Best Use Guidelines
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "${uiState.targetFormat.name} Guidelines",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    uiState.targetFormat.bestFor,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Transparency Handling Warning Banner
                if (uiState.showTransparencyWarning) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = "Warning",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Transparency detected.",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    "The image background will be flattened because JPEG does not support transparency.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Flattening Background Color:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BackgroundColorOption.values().forEach { option ->
                                        FilterChip(
                                            selected = uiState.selectedBgColor == option,
                                            onClick = { viewModel.setBackgroundColor(option) },
                                            label = { Text(option.label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Advanced Settings (Progressive Disclosure for Quality)
                if (uiState.targetFormat != ImageFormat.PNG) {
                    item {
                        AdvancedSettingsCard(
                            title = "Advanced Settings",
                            summary = "Quality: ${uiState.quality}%"
                        ) {
                            QualitySlider(
                                quality = uiState.quality,
                                onQualityChange = { viewModel.setQuality(it) }
                            )
                        }
                    }
                }

                // 1 Primary Action: Convert Button
                item {
                    Button(
                        onClick = { viewModel.convert() },
                        Modifier.fillMaxWidth().height(52.dp),
                        enabled = uiState.selectedImageUri != null && !uiState.isProcessing,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.SwapHoriz, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isProcessing) "Converting..." else "Convert Image", fontWeight = FontWeight.SemiBold)
                    }
                }

                uiState.error?.let {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Result Section with Strict Visual Hierarchy
                uiState.result?.let { result ->
                    item {
                        BeforeAfterPreview(
                            originalUri = uiState.selectedImageUri,
                            resultUri = result.outputUri,
                            originalSize = "${uiState.imageInfo?.format?.name ?: ""} • ${String.format("%.1f", result.originalSizeMB)} MB",
                            resultSize = "${result.format.name} • ${String.format("%.1f", result.compressedSizeMB)} MB",
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
                                    ShareHelper.shareImage(context, result.outputUri, result.format.mimeType, "Share Converted Image")
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share Converted Image", fontWeight = FontWeight.SemiBold)
                            }

                            // Secondary Action: Convert Another
                            OutlinedButton(
                                onClick = { viewModel.reset() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Convert Another")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            if (uiState.isProcessing) ProcessingOverlay(isVisible = true, message = "Converting format...")
        }
    }
}
