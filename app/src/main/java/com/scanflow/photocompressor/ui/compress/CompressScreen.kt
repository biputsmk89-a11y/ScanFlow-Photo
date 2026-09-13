package com.scanflow.photocompressor.ui.compress

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.ui.components.*
import com.scanflow.photocompressor.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    onNavigateToEdit: (() -> Unit)? = null,
    initialUris: List<android.net.Uri>? = null,
    viewModel: CompressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialUris) {
        if (!initialUris.isNullOrEmpty()) {
            viewModel.selectImages(initialUris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compress Photos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onNavigateToHistory != null && uiState.result == null) {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Filled.History, contentDescription = "History")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Check if we are displaying RESULT SCREEN (Rule 38)
                val activeResult = uiState.result
                if (activeResult != null) {
                    val result = activeResult

                    // 1. Result Header: Compression Complete
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Success.copy(alpha = 0.15f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Success,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Compression Complete",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (uiState.isMultiple) "${uiState.selectedCount} photos compressed" else "Photo successfully compressed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 2. Metrics 2x2 Grid (Original, New Size, Saved, Reduction)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricCard(
                                    label = "Original",
                                    value = formatBytes(result.originalSize),
                                    modifier = Modifier.weight(1f)
                                )
                                MetricCard(
                                    label = "New Size",
                                    value = formatBytes(result.compressedSize),
                                    modifier = Modifier.weight(1f),
                                    highlight = true
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricCard(
                                    label = "Saved",
                                    value = formatBytes(result.savedBytes),
                                    modifier = Modifier.weight(1f),
                                    valueColor = Success
                                )
                                MetricCard(
                                    label = "Reduction",
                                    value = "${String.format("%.1f", result.savedPercentage)}%",
                                    modifier = Modifier.weight(1f),
                                    valueColor = Success
                                )
                            }
                        }
                    }

                    // 3. Before/After visual preview for single photo
                    if (!uiState.isMultiple && uiState.selectedImageUri != null) {
                        item {
                            BeforeAfterPreview(
                                originalUri = uiState.selectedImageUri,
                                resultUri = result.outputUri,
                                originalSize = formatBytes(result.originalSize),
                                resultSize = formatBytes(result.compressedSize),
                                savedPercentage = String.format("%.1f%%", result.savedPercentage)
                            )
                        }
                    }

                    // 4. Result Actions (1 Primary + Secondary Actions)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Primary Action: SAVE
                            Button(
                                onClick = {
                                    viewModel.saveResult()
                                    Toast.makeText(context, "Saved to device storage", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isSavedToGallery) Success else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    if (uiState.isSavedToGallery) Icons.Filled.Check else Icons.Filled.SaveAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isSavedToGallery) "SAVED" else "SAVE",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            // Secondary Action: SHARE
                            OutlinedButton(
                                onClick = {
                                    com.scanflow.photocompressor.util.AnalyticsLogger.logEvent(
                                        com.scanflow.photocompressor.util.AnalyticsEvent.SHARE_CLICKED,
                                        mapOf("count" to if (uiState.outputUris.size > 1) uiState.outputUris.size else 1)
                                    )
                                    val outputUris = uiState.outputUris
                                    if (outputUris.size > 1) {
                                        com.scanflow.photocompressor.util.ShareHelper.shareImages(
                                            context = context,
                                            uris = outputUris,
                                            mimeType = "image/*",
                                            title = "Share Compressed Photos"
                                        )
                                    } else {
                                        com.scanflow.photocompressor.util.ShareHelper.shareImage(
                                            context = context,
                                            uri = result.outputUri,
                                            mimeType = result.format.mimeType,
                                            title = "Share Compressed Photo"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.outputUris.size > 1) "SHARE (${uiState.outputUris.size} PHOTOS)" else "SHARE")
                            }

                            // Secondary Action: COMPRESS AGAIN
                            OutlinedButton(
                                onClick = { viewModel.reset() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPRESS AGAIN")
                            }

                            // Secondary Action: EDIT
                            if (onNavigateToEdit != null) {
                                OutlinedButton(
                                    onClick = onNavigateToEdit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("EDIT")
                                }
                            }
                        }
                    }
                } else {
                    // CONFIGURATION SCREEN

                    // 1. [ Select Photos ] Card
                    item {
                        ImagePickerCard(
                            selectedImageUri = uiState.selectedImageUri,
                            onImageSelected = { viewModel.selectImage(it) },
                            allowMultiple = true,
                            onMultipleImagesSelected = { viewModel.selectImages(it) }
                        )
                    }

                    // 2. Selected Count Banner
                    if (uiState.selectedCount > 0) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "Selected:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (uiState.selectedCount > 1) {
                                            "${uiState.selectedCount} photos"
                                        } else {
                                            uiState.imageInfo?.let {
                                                "1 photo • ${it.fileName} (${it.resolution}, ${String.format("%.1f", it.fileSizeMB)} MB)"
                                            } ?: "1 photo"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 3. Mode Selection
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Option: Small
                            val isSmall = uiState.mode == CompressionMode.QUICK && uiState.quickPreset == QuickPreset.SMALL
                            ModeOptionCard(
                                title = "Small",
                                subtitle = "Maximum compression for quick sharing (~60% quality)",
                                isSelected = isSmall,
                                onClick = { viewModel.setQuickPreset(QuickPreset.SMALL) }
                            )

                            // Option: Balanced (Default)
                            val isBalanced = uiState.mode == CompressionMode.QUICK && uiState.quickPreset == QuickPreset.BALANCED
                            ModeOptionCard(
                                title = "Balanced",
                                subtitle = "Best balance of clarity & reduced size (~75% quality)",
                                isSelected = isBalanced,
                                isDefault = true,
                                onClick = { viewModel.setQuickPreset(QuickPreset.BALANCED) }
                            )

                            // Option: High Quality
                            val isHighQuality = uiState.mode == CompressionMode.QUICK && uiState.quickPreset == QuickPreset.HIGH_QUALITY
                            ModeOptionCard(
                                title = "High Quality",
                                subtitle = "Minimal compression preserving maximum details (~85% quality)",
                                isSelected = isHighQuality,
                                onClick = { viewModel.setQuickPreset(QuickPreset.HIGH_QUALITY) }
                            )

                            // Option: Target Size
                            val isTargetSize = uiState.mode == CompressionMode.TARGET_SIZE
                            ModeOptionCard(
                                title = "Target Size",
                                subtitle = "Compress strictly below a specified file size",
                                isSelected = isTargetSize,
                                onClick = { viewModel.setMode(CompressionMode.TARGET_SIZE) }
                            )

                            // Sub-settings for Target Size
                            if (isTargetSize) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Choose target limit:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                TargetSizePreset.SIZE_100_KB,
                                                TargetSizePreset.SIZE_250_KB,
                                                TargetSizePreset.SIZE_500_KB,
                                                TargetSizePreset.SIZE_1_MB
                                            ).forEach { preset ->
                                                FilterChip(
                                                    selected = uiState.targetSizePreset == preset,
                                                    onClick = { viewModel.setTargetSizePreset(preset) },
                                                    label = { Text(preset.label) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                TargetSizePreset.SIZE_2_MB,
                                                TargetSizePreset.SIZE_5_MB,
                                                TargetSizePreset.CUSTOM
                                            ).forEach { preset ->
                                                FilterChip(
                                                    selected = uiState.targetSizePreset == preset,
                                                    onClick = { viewModel.setTargetSizePreset(preset) },
                                                    label = { Text(preset.label) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        if (uiState.targetSizePreset == TargetSizePreset.CUSTOM) {
                                            OutlinedTextField(
                                                value = uiState.customTargetSizeKB,
                                                onValueChange = { viewModel.setCustomTargetSizeKB(it) },
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("Enter size in KB (e.g. 750)") },
                                                singleLine = true,
                                                trailingIcon = { Text("KB", modifier = Modifier.padding(end = 12.dp)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Advanced (Progressive Disclosure)
                    item {
                        AdvancedSettingsCard(
                            title = "Advanced",
                            summary = "${uiState.format.name} • ${if (uiState.maxWidth > 0) "${uiState.maxWidth}px" else "Original Size"} • ${uiState.metadataOption.label}"
                        ) {
                            // Sub 1: Format
                            Column {
                                Text(
                                    text = "Format",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ImageFormat.values().forEach { format ->
                                        FilterChip(
                                            selected = uiState.format == format,
                                            onClick = { viewModel.setFormat(format) },
                                            label = { Text(format.name) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Sub 2: Resize Constraints
                            Column {
                                Text(
                                    text = "Resize Constraint",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val resizeOptions = listOf(
                                    Pair("Original", 0),
                                    Pair("1080p", 1920),
                                    Pair("2K", 2560),
                                    Pair("4K", 3840)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    resizeOptions.forEach { (label, dim) ->
                                        FilterChip(
                                            selected = uiState.maxWidth == dim,
                                            onClick = { viewModel.setMaxDimensions(dim, dim) },
                                            label = { Text(label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Sub 3: Metadata
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Metadata & Privacy",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                MetadataOption.values().forEach { option ->
                                    val isSelected = uiState.metadataOption == option
                                    OutlinedCard(
                                        onClick = { viewModel.setMetadataOption(option) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { viewModel.setMetadataOption(option) }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = option.label,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = option.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. 1 Primary Action: [ COMPRESS X PHOTOS ]
                    item {
                        val buttonText = when {
                            uiState.selectedCount > 1 -> "COMPRESS ${uiState.selectedCount} PHOTOS"
                            uiState.selectedCount == 1 -> "COMPRESS PHOTO"
                            else -> "COMPRESS PHOTOS"
                        }
                        val semanticDescription = when {
                            uiState.selectedCount > 1 -> "Compress ${uiState.selectedCount} selected photos"
                            uiState.selectedCount == 1 -> "Compress selected photo"
                            else -> "Compress selected photos"
                        }

                        Button(
                            onClick = { viewModel.compress() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .semantics {
                                    contentDescription = semanticDescription
                                },
                            enabled = uiState.selectedCount > 0 && !uiState.isProcessing,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Filled.Compress,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Error message (Rule 53: Error UX without stack traces)
                    uiState.userErrorMessage?.let { errorMsg ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = errorMsg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Processing Overlay adhering to Rule 37
            if (uiState.isProcessing) {
                if (uiState.isMultiple) {
                    ProcessingOverlay(
                        isVisible = true,
                        progress = uiState.currentProgress,
                        message = "Processing",
                        progressText = "${uiState.processedCount} / ${uiState.totalToProcess}",
                        cancelLabel = "Cancel",
                        onCancel = { viewModel.cancelCompression() }
                    )
                } else {
                    ProcessingOverlay(
                        isVisible = true,
                        progress = uiState.currentProgress,
                        message = "Compressing...",
                        progressText = "${(uiState.currentProgress.coerceIn(0f, 1f) * 100).toInt()}%",
                        cancelLabel = "Cancel",
                        onCancel = { viewModel.cancelCompression() }
                    )
                }
            }
        }
    }
}

/**
 * Metric Card for Rule 38 Result Screen
 */
@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else valueColor
            )
        }
    }
}

/**
 * Mode Option selectable card with Radio button
 */
@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isDefault: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    )
                    if (isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Default", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
