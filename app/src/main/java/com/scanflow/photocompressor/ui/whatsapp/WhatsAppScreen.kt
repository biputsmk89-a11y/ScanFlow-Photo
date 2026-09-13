package com.scanflow.photocompressor.ui.whatsapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.domain.model.WhatsAppTier
import com.scanflow.photocompressor.ui.components.BeforeAfterPreview
import com.scanflow.photocompressor.ui.components.ImagePickerCard
import com.scanflow.photocompressor.ui.theme.Success
import com.scanflow.photocompressor.util.ReductionCalculator
import com.scanflow.photocompressor.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppScreen(
    onNavigateBack: () -> Unit,
    viewModel: WhatsAppViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsApp Ready") },
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
                // 2. PRESETS: Small, Balanced, High Quality, Custom (No absolute file size promises)
                item {
                    Text(
                        text = "2. Select WhatsApp Preset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WhatsAppTier.values().forEach { tier ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.selectTier(tier) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (uiState.config.tier == tier) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = uiState.config.tier == tier,
                                        onClick = { viewModel.selectTier(tier) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = tier.displayName,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = tier.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. CUSTOM SETTINGS (if Custom is selected)
                if (uiState.config.tier == WhatsAppTier.CUSTOM) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Max Dimension: ${uiState.config.customMaxDimension} px",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = uiState.config.customMaxDimension.toFloat(),
                                    onValueChange = { viewModel.updateCustomMaxDimension(it.toInt()) },
                                    valueRange = 640f..2560f,
                                    steps = 11
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Quality: ${uiState.config.customQuality}%",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Slider(
                                    value = uiState.config.customQuality.toFloat(),
                                    onValueChange = { viewModel.updateCustomTargetSize(it.toInt()) },
                                    valueRange = 10f..100f,
                                    steps = 17
                                )
                            }
                        }
                    }
                }

                // 4. PROCESS BUTTON
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.processWhatsAppImage() },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .semantics {
                                contentDescription = "Optimize photo for WhatsApp sharing (${uiState.config.tier.displayName})"
                            },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Optimizing for WhatsApp...")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Optimize for WhatsApp (${uiState.config.tier.displayName})", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 5. RESULT SECTION: Displays Before, After, Saved, Reduction
                val activeResult = uiState.result
                if (activeResult != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            val result = activeResult
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
                                    text = "WhatsApp Ready!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // 4 Core Metrics: Before, After, Saved, Reduction
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricCard(
                                        label = "Before",
                                        value = ReductionCalculator.formatBytes(result.originalBytes),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    MetricCard(
                                        label = "After",
                                        value = ReductionCalculator.formatBytes(result.outputBytes),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricCard(
                                        label = "Saved",
                                        value = ReductionCalculator.formatBytes(result.savedBytes),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    MetricCard(
                                        label = "Reduction",
                                        value = "-${String.format("%.1f", result.reductionPercent)}%",
                                        modifier = Modifier.weight(1f),
                                        highlightColor = Success
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Before / After interactive preview
                                BeforeAfterPreview(
                                    originalUri = uiState.selectedImageUri,
                                    resultUri = result.outputUri,
                                    originalSize = ReductionCalculator.formatBytes(result.originalBytes),
                                    resultSize = ReductionCalculator.formatBytes(result.outputBytes),
                                    savedPercentage = "-${String.format("%.1f", result.reductionPercent)}%"
                                )

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        ShareHelper.shareImage(
                                            context,
                                            result.outputUri,
                                            "image/jpeg",
                                            "Share to WhatsApp"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share to WhatsApp")
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

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightColor: androidx.compose.ui.graphics.Color? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = highlightColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
