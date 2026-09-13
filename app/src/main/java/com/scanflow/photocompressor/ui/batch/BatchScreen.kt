package com.scanflow.photocompressor.ui.batch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.ui.components.*
import com.scanflow.photocompressor.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchScreen(
    onNavigateBack: () -> Unit,
    initialUris: List<android.net.Uri>? = null,
    viewModel: BatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(initialUris) {
        if (!initialUris.isNullOrEmpty()) {
            viewModel.addImages(initialUris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Compress") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.batchJob == null) {
                    item {
                        ImagePickerCard(
                            selectedImageUri = uiState.selectedImages.firstOrNull()?.uri,
                            onImageSelected = { viewModel.addImages(listOf(it)) },
                            allowMultiple = true,
                            onMultipleImagesSelected = { viewModel.addImages(it) }
                        )
                    }

                    if (uiState.selectedImages.isNotEmpty()) {
                        item {
                            Text(
                                "${uiState.selectedImages.size} images selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        itemsIndexed(uiState.selectedImages) { index, image ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SafeThumbnail(
                                        data = image.uri,
                                        contentDescription = image.fileName,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(image.fileName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        Text(
                                            "${image.resolution} • ${String.format("%.1f", image.fileSizeMB)} MB",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { viewModel.removeImage(index) }) {
                                        Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Preset selector
                    if (uiState.presets.isNotEmpty()) {
                        item {
                            Text("Select Preset", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.presets.take(3).forEach { preset ->
                                    FilterChip(
                                        selected = uiState.selectedPreset?.name == preset.name,
                                        onClick = { viewModel.selectPreset(preset) },
                                        label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Start button
                    item {
                        Button(
                            onClick = { viewModel.startBatch() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = uiState.selectedImages.isNotEmpty() && !uiState.isProcessing,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isProcessing) "Processing..." else "Start Batch", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    val job = uiState.batchJob ?: return@LazyColumn

                    // Progress Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                GradientLinearProgress(progress = job.progressPercentage)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${job.completedCount}/${job.totalCount} completed",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (job.failedCount > 0) {
                                            Text(
                                                "${job.failedCount} failed",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        if (job.cancelledCount > 0) {
                                            Text(
                                                "${job.cancelledCount} cancelled",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                                if (job.totalSavedBytes > 0) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "💾 Total saved: ${formatBytes(job.totalSavedBytes)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Success,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Main Action Buttons
                    if (uiState.isProcessing) {
                        item {
                            OutlinedButton(
                                onClick = { viewModel.cancelBatch() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Cancel Batch")
                            }
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (job.failedCount > 0 || job.cancelledCount > 0) {
                                    // 1 Primary Action: Retry Failed Items
                                    Button(
                                        onClick = { viewModel.retryBatch() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Retry Failed Items (${job.failedCount + job.cancelledCount})",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Secondary Action: Start New Batch
                                    OutlinedButton(
                                        onClick = { viewModel.reset() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Start New Batch")
                                    }
                                } else {
                                    // 1 Primary Action: Start New Batch when all succeed
                                    Button(
                                        onClick = { viewModel.reset() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Start New Batch", fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                val completedUris = job.items.mapNotNull { it.outputUri }
                                if (completedUris.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            com.scanflow.photocompressor.util.ShareHelper.shareImages(
                                                context = context,
                                                uris = completedUris,
                                                mimeType = "image/*",
                                                title = "Share Batch Photos"
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Share Completed Photos (${completedUris.size})")
                                    }
                                }
                            }
                        }
                    }

                    // Section: Batch Items with Per-Item Status and Retry
                    item {
                        Text(
                            "Batch Items (${job.items.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    itemsIndexed(job.items, key = { _, item -> item.id }) { index, item ->
                        val fileName = item.sourceUri.lastPathSegment?.substringAfterLast('/') ?: "Image #${index + 1}"
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        when (item.status) {
                                            com.scanflow.photocompressor.domain.model.BatchItemStatus.SUCCESS -> {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "Success",
                                                    tint = Success,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                val sizeInfo = if (item.originalBytes > 0 && item.outputBytes > 0) {
                                                    " • ${formatBytes(item.originalBytes)} → ${formatBytes(item.outputBytes)}"
                                                } else ""
                                                Text(
                                                    "Completed$sizeInfo",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Success
                                                )
                                            }
                                            com.scanflow.photocompressor.domain.model.BatchItemStatus.FAILED -> {
                                                Icon(
                                                    Icons.Filled.Error,
                                                    contentDescription = "Failed",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    item.error?.userFacingMessage ?: "Failed",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    maxLines = 1
                                                )
                                            }
                                            com.scanflow.photocompressor.domain.model.BatchItemStatus.CANCELLED -> {
                                                Icon(
                                                    Icons.Filled.Cancel,
                                                    contentDescription = "Cancelled",
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Cancelled",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                            com.scanflow.photocompressor.domain.model.BatchItemStatus.PROCESSING -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(14.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "Processing...",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            com.scanflow.photocompressor.domain.model.BatchItemStatus.QUEUED -> {
                                                Icon(
                                                    Icons.Filled.Schedule,
                                                    contentDescription = "Queued",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "Queued",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Per-item retry button for failed/cancelled items when not actively processing
                                if (!uiState.isProcessing && (item.status == com.scanflow.photocompressor.domain.model.BatchItemStatus.FAILED || item.status == com.scanflow.photocompressor.domain.model.BatchItemStatus.CANCELLED)) {
                                    OutlinedButton(
                                        onClick = { viewModel.retryItem(item.id) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Refresh, contentDescription = "Retry", modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            if (uiState.isProcessing) {
                val job = uiState.batchJob
                ProcessingOverlay(
                    isVisible = true,
                    progress = job?.progressPercentage ?: -1f,
                    message = "Processing ${(job?.completedCount ?: 0) + (job?.processingCount ?: 0)}/${job?.totalCount ?: 0}...",
                    onCancel = { viewModel.cancelBatch() }
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
