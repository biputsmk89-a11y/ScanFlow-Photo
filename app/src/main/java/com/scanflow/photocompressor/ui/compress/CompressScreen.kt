package com.scanflow.photocompressor.ui.compress

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.scanflow.photocompressor.R
import com.scanflow.photocompressor.util.ReductionCalculator.formatBytes
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.ui.components.ScanFlowHeader
import com.scanflow.photocompressor.ui.theme.*
import com.scanflow.photocompressor.util.AnalyticsEvent
import com.scanflow.photocompressor.util.AnalyticsLogger
import com.scanflow.photocompressor.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    onNavigateToEdit: ((Uri) -> Unit)? = null,
    initialUris: List<Uri>? = null,
    viewModel: CompressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectImage(uri)
        }
    }

    LaunchedEffect(initialUris) {
        if (!initialUris.isNullOrEmpty()) {
            viewModel.selectImages(initialUris)
        }
    }

    val activeResult = uiState.result

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header (Google Stitch)
        ScanFlowHeader(
            title = if (activeResult != null) "Comparison Preview" else "Compress Editor",
            subtitle = "ScanFlow Foto",
            onNavigateBack = {
                if (activeResult != null) {
                    viewModel.reset()
                } else {
                    onNavigateBack()
                }
            },
            actions = {
                if (activeResult != null && onNavigateToHistory != null) {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        if (activeResult != null) {
            // ==========================================
            // RESULT SCREEN: Comparison Preview (Stitch)
            // ==========================================
            StitchCompressResultContent(
                result = activeResult,
                uiState = uiState,
                onHomeClick = onNavigateBack,
                onResetClick = { viewModel.reset() },
                onShareClick = {
                    AnalyticsLogger.logEvent(
                        AnalyticsEvent.SHARE_CLICKED,
                        mapOf("count" to if (uiState.outputUris.size > 1) uiState.outputUris.size else 1)
                    )
                    val outputUris = uiState.outputUris
                    if (outputUris.size > 1) {
                        ShareHelper.shareImages(
                            context = context,
                            uris = outputUris,
                            mimeType = "image/*",
                            title = "Share Compressed Photos"
                        )
                    } else {
                        ShareHelper.shareImage(
                            context = context,
                            uri = activeResult.outputUri,
                            mimeType = activeResult.format.mimeType,
                            title = "Share Compressed Photo"
                        )
                    }
                },
                onOpenClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(activeResult.outputUri, activeResult.format.mimeType.ifEmpty { "image/*" })
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No gallery app found", Toast.LENGTH_SHORT).show()
                    }
                },
                onEditClick = onNavigateToEdit?.let { edit -> { edit(activeResult.outputUri) } }
            )
        } else {
            // ==========================================
            // EDITOR SCREEN: Compress & Target Size (Stitch)
            // ==========================================
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error Alert Banner (if compression fails)
                    if (uiState.error != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Compression Issue",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = uiState.error ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    IconButton(onClick = { viewModel.clearError() }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Context Indicator
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (uiState.selectedCount > 0) "${uiState.selectedCount} photo selected" else "1 photo ready",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OfflinePin,
                                        contentDescription = null,
                                        tint = Tertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "100% Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Tertiary
                                    )
                                }
                            }
                        }
                    }

                    // Image Preview Card with Overlay Pills
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                ) {
                                    if (uiState.selectedImageUri != null) {
                                        AsyncImage(
                                            model = uiState.selectedImageUri,
                                            contentDescription = "Selected photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AddPhotoAlternate,
                                                contentDescription = "Pick photo",
                                                tint = Primary,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Tap to choose a photo",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Dynamic Size Delta Pill on Preview
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val targetBadgeText = when (uiState.mode) {
                                            CompressionMode.TARGET_SIZE -> {
                                                val label = if (uiState.targetSizePreset == TargetSizePreset.CUSTOM) {
                                                    "${uiState.customTargetSizeKB.ifEmpty { "500" }} KB"
                                                } else {
                                                    uiState.targetSizePreset.label
                                                }
                                                "Target: $label"
                                            }
                                            CompressionMode.QUICK -> "Profile: ${uiState.quickPreset.label}"
                                            CompressionMode.QUALITY -> "Quality: ${uiState.quality}%"
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = InverseSurface.copy(alpha = 0.85f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ArrowDownward,
                                                    contentDescription = null,
                                                    tint = TertiaryFixed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = targetBadgeText,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TertiaryFixed
                                                )
                                            }
                                        }

                                        val resText = uiState.imageInfo?.resolution ?: "Ready"
                                        Surface(
                                            shape = RoundedCornerShape(9999.dp),
                                            color = InverseSurface.copy(alpha = 0.85f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.AspectRatio,
                                                    contentDescription = null,
                                                    tint = InverseOnSurface,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = resText,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = InverseOnSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                // Metadata bar below image
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = uiState.imageInfo?.fileName ?: "No photo selected",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = "Original uncompressed capture",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Original",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val origSizeText = uiState.imageInfo?.let { formatBytes(it.fileSize) } ?: "0 KB"
                                            Text(
                                                text = origSizeText,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 1: Optimization Profile (3 Stitch Cards in a row)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OPTIMIZATION PROFILE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pick fidelity balance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isSmall = uiState.mode == CompressionMode.QUICK && uiState.quickPreset == QuickPreset.SMALL
                                val isBalanced = uiState.mode == CompressionMode.QUICK && uiState.quickPreset == QuickPreset.BALANCED
                                val isHigh = uiState.mode == CompressionMode.QUICK && uiState.quickPreset == QuickPreset.HIGH_QUALITY

                                val origBytes = uiState.imageInfo?.fileSize ?: 0L
                                val smallTag = if (origBytes > 0L) "~${formatBytes((origBytes * 0.25).toLong().coerceAtLeast(10240L))}" else "~300 KB"
                                val balancedTag = "Recommended"
                                val fidelityTag = if (origBytes > 0L) "~${formatBytes((origBytes * 0.80).toLong().coerceAtLeast(10240L))}" else "~1.5 MB"

                                StitchProfileCard(
                                    title = "Small",
                                    subtitle = "Quick send",
                                    tag = smallTag,
                                    icon = Icons.Filled.Compress,
                                    isSelected = isSmall,
                                    onClick = { viewModel.setQuickPreset(QuickPreset.SMALL) },
                                    modifier = Modifier.weight(1f)
                                )

                                StitchProfileCard(
                                    title = "Balanced",
                                    subtitle = "Best clarity",
                                    tag = balancedTag,
                                    icon = Icons.Filled.Tune,
                                    isSelected = isBalanced,
                                    onClick = { viewModel.setQuickPreset(QuickPreset.BALANCED) },
                                    modifier = Modifier.weight(1f)
                                )

                                StitchProfileCard(
                                    title = "Fidelity",
                                    subtitle = "Fine details",
                                    tag = fidelityTag,
                                    icon = Icons.Filled.HighQuality,
                                    isSelected = isHigh,
                                    onClick = { viewModel.setQuickPreset(QuickPreset.HIGH_QUALITY) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Section 2: Target File Size Chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TARGET FILE SIZE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoFixHigh,
                                        contentDescription = null,
                                        tint = Tertiary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "High Reduction",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Tertiary
                                    )
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val presets = listOf(
                                    TargetSizePreset.SIZE_100_KB,
                                    TargetSizePreset.SIZE_250_KB,
                                    TargetSizePreset.SIZE_500_KB,
                                    TargetSizePreset.SIZE_1_MB,
                                    TargetSizePreset.SIZE_2_MB,
                                    TargetSizePreset.CUSTOM
                                )
                                items(presets) { preset ->
                                    val isSelected = uiState.targetSizePreset == preset && uiState.mode == CompressionMode.TARGET_SIZE
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.setMode(CompressionMode.TARGET_SIZE)
                                                viewModel.setTargetSizePreset(preset)
                                            }
                                    ) {
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                        )
                                    }
                                }
                            }

                            if (uiState.targetSizePreset == TargetSizePreset.CUSTOM && uiState.mode == CompressionMode.TARGET_SIZE) {
                                OutlinedTextField(
                                    value = uiState.customTargetSizeKB,
                                    onValueChange = { viewModel.setCustomTargetSizeKB(it) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Target size in KB (e.g. 600)") },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    trailingIcon = { Text("KB", modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }

                    // Section 3: Advanced Controls Accordion
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Tune,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Advanced Controls",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.format.name} • ${uiState.quality}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = if (isAdvancedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isAdvancedExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                        // Format Selector
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "Target File Format",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(ImageFormat.JPEG, ImageFormat.WEBP, ImageFormat.PNG).forEach { fmt ->
                                                    val isSelected = uiState.format == fmt
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else MaterialTheme.colorScheme.surfaceContainerLow,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isSelected) Primary else Color.Transparent
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .clickable { viewModel.setFormat(fmt) }
                                                    ) {
                                                        Text(
                                                            text = fmt.name,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(vertical = 10.dp),
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Preserve EXIF Switch
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Preserve EXIF Metadata",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Retain camera, geolocation GPS & timestamp",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            val preserveExif = uiState.metadataOption != MetadataOption.REMOVE_ALL
                                            Switch(
                                                checked = preserveExif,
                                                onCheckedChange = { preserve ->
                                                    viewModel.setMetadataOption(
                                                        if (preserve) MetadataOption.KEEP_METADATA else MetadataOption.REMOVE_ALL
                                                    )
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Primary
                                                )
                                            )
                                        }

                                        // Quality Percentage Slider
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Compression Quality Cap",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${uiState.quality}%",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Primary
                                                )
                                            }
                                            Slider(
                                                value = uiState.quality.toFloat(),
                                                onValueChange = { viewModel.setQuality(it.toInt()) },
                                                valueRange = 40f..95f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Primary,
                                                    activeTrackColor = Primary
                                                )
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Heavy (40%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text("Balanced (80%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                                Text("Lossless-like (95%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Space Savings Preview Banner (Dynamic, 100% Real Math)
                    item {
                        val savings = remember(uiState.mode, uiState.quickPreset, uiState.targetSizePreset, uiState.customTargetSizeKB, uiState.quality, uiState.imageInfo?.fileSize) {
                            calculateSavingsPreview(
                                mode = uiState.mode,
                                quickPreset = uiState.quickPreset,
                                targetSizePreset = uiState.targetSizePreset,
                                customTargetSizeKB = uiState.customTargetSizeKB,
                                quality = uiState.quality,
                                originalBytes = uiState.imageInfo?.fileSize ?: 0L
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (savings.isPositive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (savings.isPositive) Primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (savings.isPositive) Icons.Filled.SaveAlt else Icons.Filled.Info,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = savings.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = savings.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Sticky Bottom Bar: Shutter Primary Action
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                if (uiState.selectedImageUri != null) {
                                    viewModel.compress()
                                } else {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            enabled = !uiState.isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Primary,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (uiState.isProcessing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Compressing...",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val ctaText = if (uiState.selectedImageUri != null) {
                                    when (uiState.mode) {
                                        CompressionMode.TARGET_SIZE -> {
                                            val label = if (uiState.targetSizePreset == TargetSizePreset.CUSTOM) {
                                                "${uiState.customTargetSizeKB.ifEmpty { "500" }} KB"
                                            } else {
                                                uiState.targetSizePreset.label
                                            }
                                            "Compress Photo • $label"
                                        }
                                        CompressionMode.QUICK -> "Compress Photo • ${uiState.quickPreset.label}"
                                        CompressionMode.QUALITY -> "Compress Photo • ${uiState.quality}%"
                                    }
                                } else {
                                    "Select Photo to Compress"
                                }
                                Text(
                                    text = ctaText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Tertiary)
                            )
                            Text(
                                text = "Local Android Canvas Engine • Instant Process",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3-Column Preset Mode Card (Stitch)
 */
@Composable
private fun StitchProfileCard(
    title: String,
    subtitle: String,
    tag: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(16.dp)
                ) {
                    if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Result Content (Comparison Preview Identik Stitch)
 */
@Composable
private fun StitchCompressResultContent(
    result: CompressionResult,
    uiState: CompressUiState,
    onHomeClick: () -> Unit,
    onResetClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenClick: () -> Unit,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var comparisonMode by remember { mutableStateOf("slider") } // "slider", "before", "after"
    var splitFraction by remember { mutableFloatStateOf(0.5f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sub-bar: Home Button Pill & Completion Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.clickable(onClick = onHomeClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Compression Complete",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Prominent Result Bento Card (Stitch)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "STORAGE RECLAIMED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val displayPct = result.savedPercentage.coerceAtLeast(0.0)
                                val statusLabel = if (result.savedPercentage > 0.0) "smaller" else "optimized"
                                Text(
                                    text = "${String.format("%.1f", displayPct)}%",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary
                                )
                                Text(
                                    text = statusLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Tertiary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = Tertiary
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Saved ${formatBytes(result.savedBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Transformation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = formatBytes(result.originalSize),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = formatBytes(result.compressedSize),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Lossless Perception",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Tertiary
                        )
                    }

                    // Visual Progress Weight Bar
                    val ratio = (result.compressedSize.toFloat() / result.originalSize.toFloat().coerceAtLeast(1f)).coerceIn(0.05f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(9999.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .clip(RoundedCornerShape(9999.dp))
                                .background(Primary)
                        )
                    }
                }
            }
        }

        // Interactive Before / After Visual Comparison
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Compare,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Visual Fidelity",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Mode Toggle Pills
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                listOf("slider" to "Split", "before" to "Orig", "after" to "New").forEach { (mode, label) ->
                                    val isSelected = comparisonMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                                        modifier = Modifier.clickable { comparisonMode = mode }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Viewport Box
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        val widthPx = constraints.maxWidth.toFloat()

                        when (comparisonMode) {
                            "before" -> {
                                AsyncImage(
                                    model = uiState.selectedImageUri,
                                    contentDescription = "Original",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            "after" -> {
                                AsyncImage(
                                    model = result.outputUri,
                                    contentDescription = "Optimized",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            else -> {
                                // Split Slider Mode
                                AsyncImage(
                                    model = result.outputUri,
                                    contentDescription = "After",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(splitFraction)
                                        .clip(RoundedCornerShape(0.dp))
                                ) {
                                    AsyncImage(
                                        model = uiState.selectedImageUri,
                                        contentDescription = "Before",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Draggable Handle
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .offset(x = (maxWidth * splitFraction) - 16.dp)
                                        .width(32.dp)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                val newFraction = (splitFraction + (dragAmount.x / widthPx)).coerceIn(0.05f, 0.95f)
                                                splitFraction = newFraction
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .background(Color.White)
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.UnfoldMore,
                                                contentDescription = null,
                                                tint = Primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Badges
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = InverseSurface.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "Before: ${formatBytes(result.originalSize)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Primary.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "After: ${formatBytes(result.compressedSize)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Subtext
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ZoomIn,
                                contentDescription = null,
                                tint = Tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "100% Crisp Edges",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Drag handle horizontally",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Storage confirmation card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved to Device",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = result.outputFileName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("File Path", result.outputFileName)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "File name copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy Path",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Primary & Secondary Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary CTA: Open Result
                Button(
                    onClick = onOpenClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Open Result", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                // Secondary Row: Share Photo + Fullscreen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onShareClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Share Photo", style = MaterialTheme.typography.labelMedium)
                    }

                    if (onEditClick != null) {
                        OutlinedButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Fine-tune", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(imageVector = Icons.Filled.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Fullscreen", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Tertiary: Compress Another Photo
                TextButton(
                    onClick = onResetClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = "Compress Another Photo",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
        }
    }
}

private data class SavingsPreviewData(
    val title: String,
    val subtitle: String,
    val isPositive: Boolean
)

private fun calculateSavingsPreview(
    mode: CompressionMode,
    quickPreset: QuickPreset,
    targetSizePreset: TargetSizePreset,
    customTargetSizeKB: String,
    quality: Int,
    originalBytes: Long
): SavingsPreviewData {
    if (originalBytes <= 0L) {
        return SavingsPreviewData(
            title = "Select a photo to preview savings",
            subtitle = "100% on-device offline processing",
            isPositive = true
        )
    }

    return when (mode) {
        CompressionMode.TARGET_SIZE -> {
            val targetBytes = if (targetSizePreset == TargetSizePreset.CUSTOM) {
                (customTargetSizeKB.toLongOrNull() ?: 500L) * 1024L
            } else {
                targetSizePreset.bytes
            }
            if (targetBytes < originalBytes) {
                val savedBytes = originalBytes - targetBytes
                val pct = (savedBytes.toDouble() / originalBytes * 100).coerceIn(1.0, 99.0)
                SavingsPreviewData(
                    title = "Saving ~${formatBytes(savedBytes)} space (${String.format("%.0f", pct)}%)",
                    subtitle = "Ideal for email sharing and messaging apps",
                    isPositive = true
                )
            } else {
                SavingsPreviewData(
                    title = "Target (${formatBytes(targetBytes)}) is larger than photo (${formatBytes(originalBytes)})",
                    subtitle = "Will compress at maximum clarity without increasing size",
                    isPositive = false
                )
            }
        }
        CompressionMode.QUICK -> {
            val ratio = when (quickPreset) {
                QuickPreset.SMALL -> 0.25
                QuickPreset.BALANCED -> 0.50
                QuickPreset.HIGH_QUALITY -> 0.80
            }
            val estOutput = (originalBytes * ratio).toLong()
            val savedBytes = (originalBytes - estOutput).coerceAtLeast(0L)
            val pct = ((1.0 - ratio) * 100).coerceIn(5.0, 95.0)
            SavingsPreviewData(
                title = "Saving ~${formatBytes(savedBytes)} space (~${String.format("%.0f", pct)}%)",
                subtitle = "Preset: ${quickPreset.label} • Balanced for quality and storage",
                isPositive = true
            )
        }
        CompressionMode.QUALITY -> {
            val ratio = (quality.toDouble() / 100.0).coerceIn(0.2, 0.95)
            val estOutput = (originalBytes * ratio).toLong()
            val savedBytes = (originalBytes - estOutput).coerceAtLeast(0L)
            val pct = ((1.0 - ratio) * 100).coerceIn(5.0, 80.0)
            SavingsPreviewData(
                title = "Saving ~${formatBytes(savedBytes)} space (~${String.format("%.0f", pct)}%)",
                subtitle = "Manual quality cap at $quality%",
                isPositive = true
            )
        }
    }
}

