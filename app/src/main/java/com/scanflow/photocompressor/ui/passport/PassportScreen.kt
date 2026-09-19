package com.scanflow.photocompressor.ui.passport

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.scanflow.photocompressor.R
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.ui.components.ScanFlowHeader
import com.scanflow.photocompressor.ui.theme.*
import com.scanflow.photocompressor.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportScreen(
    onNavigateBack: () -> Unit,
    initialUri: Uri? = null,
    viewModel: PassportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var areGuidesVisible by remember { mutableStateOf(true) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectImage(uri)
        }
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null && uiState.selectedImageUri != initialUri) {
            viewModel.selectImage(initialUri)
        }
    }

    var dismissedResultUri by remember { mutableStateOf<Uri?>(null) }
    val activeResultUri = uiState.result?.outputUri?.takeIf { it != dismissedResultUri }

    // Modal result preview dialog
    activeResultUri?.let { resUri ->
        AlertDialog(
            onDismissRequest = { dismissedResultUri = resUri },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Tertiary)
                    Text("Passport Sheet Ready", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = resUri,
                            contentDescription = "Result Sheet",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = "Saved to device ready for 4×6 photo lab printing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(resUri, "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No gallery app available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Open Image")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        ShareHelper.shareImage(context, resUri, "image/jpeg", "Share Passport Photo")
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header (Google Stitch)
        ScanFlowHeader(
            title = "Passport & ID Studio",
            subtitle = "ScanFlow Foto",
            onNavigateBack = onNavigateBack
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 76.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Step Progress Indicator (Stitch)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Primary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("2", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "Step 2 of 4: Photo Size & Frame",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "50% Complete",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // 4-segment progress bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape).background(Primary))
                            Box(modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape).background(Primary))
                            Box(modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                            Box(modifier = Modifier.weight(1f).height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1. Position", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Medium)
                            Text("2. Size", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                            Text("3. Backdrop", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("4. Layout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                // Interactive Passport Frame Viewport (Stitch)
                item {
                    val previewBgColor = uiState.config.effectiveBackgroundColor?.let { Color(it) } ?: Color.White
                    val isStudioBackgroundActive = uiState.config.background != PassportBackground.ORIGINAL
                    val displayImageUri = when (uiState.qaPreviewMode) {
                        QaPreviewMode.ORIGINAL -> uiState.selectedImageUri
                        QaPreviewMode.CHECKERBOARD, QaPreviewMode.COMPOSITE -> {
                            if (isStudioBackgroundActive && uiState.cutoutUri != null) uiState.cutoutUri else uiState.selectedImageUri
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    when {
                                        uiState.qaPreviewMode == QaPreviewMode.CHECKERBOARD -> Color(0xFFE2E8F0)
                                        isStudioBackgroundActive -> previewBgColor
                                        else -> Color.Transparent
                                    }
                                )
                                .clipToBounds()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        viewModel.updateTransform(zoom, pan.x, pan.y)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Subject Photo
                            if (displayImageUri != null) {
                                AsyncImage(
                                    model = displayImageUri,
                                    contentDescription = "Passport subject",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = uiState.config.zoom
                                            scaleY = uiState.config.zoom
                                            translationX = uiState.config.panX
                                            translationY = uiState.config.panY
                                            rotationZ = uiState.config.rotationDegrees
                                        }
                                )
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.clickable {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Select photo",
                                        tint = Primary,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "Tap to choose portrait photo",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // AI Background Removal Progress Indicator
                            if (uiState.isSegmenting || uiState.isProcessing) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Primary
                                        )
                                        Text(
                                            text = if (uiState.stageProgressText.isNotEmpty()) {
                                                uiState.stageProgressText
                                            } else {
                                                "Menyiapkan Studio Cutout (AI)..."
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Biometric SVG Overlay Guides
                            if (areGuidesVisible) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val centerX = w / 2f
                                    val centerY = h / 2f

                                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                    val guideBlue = Primary

                                    // Top of Head Guide Line
                                    val topHeadY = h * 0.18f
                                    drawLine(
                                        color = guideBlue,
                                        start = Offset(w * 0.1f, topHeadY),
                                        end = Offset(w * 0.9f, topHeadY),
                                        strokeWidth = 2f,
                                        pathEffect = dashEffect
                                    )

                                    // Eye Level Guide Line
                                    val eyeLevelY = h * 0.40f
                                    drawLine(
                                        color = guideBlue,
                                        start = Offset(w * 0.1f, eyeLevelY),
                                        end = Offset(w * 0.9f, eyeLevelY),
                                        strokeWidth = 2.5f,
                                        pathEffect = dashEffect
                                    )

                                    // Chin Base Guide Line
                                    val chinBaseY = h * 0.68f
                                    drawLine(
                                        color = guideBlue,
                                        start = Offset(w * 0.1f, chinBaseY),
                                        end = Offset(w * 0.9f, chinBaseY),
                                        strokeWidth = 2f,
                                        pathEffect = dashEffect
                                    )

                                    // Center Oval for Face Guide
                                    val ovalWidth = w * 0.52f
                                    val ovalHeight = h * 0.54f
                                    drawOval(
                                        color = guideBlue,
                                        topLeft = Offset(centerX - ovalWidth / 2f, centerY - ovalHeight / 2f - (h * 0.04f)),
                                        size = Size(ovalWidth, ovalHeight),
                                        style = Stroke(width = 2.5f, pathEffect = dashEffect)
                                    )

                                    // Corner Frame Marks
                                    val markLen = 24f
                                    val markPad = 16f
                                    // Top Left
                                    drawLine(guideBlue, Offset(markPad, markPad), Offset(markPad + markLen, markPad), 3f)
                                    drawLine(guideBlue, Offset(markPad, markPad), Offset(markPad, markPad + markLen), 3f)
                                    // Top Right
                                    drawLine(guideBlue, Offset(w - markPad, markPad), Offset(w - markPad - markLen, markPad), 3f)
                                    drawLine(guideBlue, Offset(w - markPad, markPad), Offset(w - markPad, markPad + markLen), 3f)
                                    // Bottom Left
                                    drawLine(guideBlue, Offset(markPad, h - markPad), Offset(markPad + markLen, h - markPad), 3f)
                                    drawLine(guideBlue, Offset(markPad, h - markPad), Offset(markPad, h - markPad - markLen), 3f)
                                    // Bottom Right
                                    drawLine(guideBlue, Offset(w - markPad, h - markPad), Offset(w - markPad - markLen, h - markPad), 3f)
                                    drawLine(guideBlue, Offset(w - markPad, h - markPad), Offset(w - markPad, h - markPad - markLen), 3f)
                                }
                            }

                            // Top Floating Dimension Pill & Toggle
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(9999.dp),
                                    color = InverseSurface.copy(alpha = 0.85f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = Tertiary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${uiState.config.spec.displayName} (${uiState.config.spec.targetWidthPx}×${uiState.config.spec.targetHeightPx} px)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = InverseSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.clickable { areGuidesVisible = !areGuidesVisible }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (areGuidesVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = "Toggle Guides",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = InverseSurface.copy(alpha = 0.85f),
                                    modifier = Modifier.clickable {
                                        val next = when (uiState.qaPreviewMode) {
                                            QaPreviewMode.COMPOSITE -> QaPreviewMode.ORIGINAL
                                            QaPreviewMode.ORIGINAL -> QaPreviewMode.CHECKERBOARD
                                            QaPreviewMode.CHECKERBOARD -> QaPreviewMode.COMPOSITE
                                        }
                                        viewModel.updateQaPreviewMode(next)
                                    }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        val icon = when (uiState.qaPreviewMode) {
                                            QaPreviewMode.COMPOSITE -> Icons.Filled.Palette
                                            QaPreviewMode.ORIGINAL -> Icons.Filled.Image
                                            QaPreviewMode.CHECKERBOARD -> Icons.Filled.GridOn
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "QA Preview Mode",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom Floating Viewport Controls Pill
                            Surface(
                                shape = RoundedCornerShape(9999.dp),
                                color = InverseSurface.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.updateZoom((uiState.config.zoom - 0.1f).coerceAtLeast(0.7f)) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateZoom((uiState.config.zoom + 0.1f).coerceAtMost(2.5f)) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.updateRotation((uiState.config.rotationDegrees + 90f) % 360f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Rotate90DegreesCw, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(9999.dp),
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable {
                                            viewModel.autoCenter()
                                            viewModel.resetPosition()
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            Text("Center", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Standard Preset Sizes (Stitch Cards)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
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
                                    Icon(Icons.Filled.Crop, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Text("Standard Preset Sizes", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                                Text("ICAO Compliant", style = MaterialTheme.typography.labelSmall, color = Primary, fontWeight = FontWeight.Bold)
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val specs = PassportSpec.values()
                                items(specs) { spec ->
                                    val isSelected = uiState.config.spec == spec
                                    Card(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.updateSpec(spec) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceContainerLow
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = spec.displayName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isSelected) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text(
                                                text = "${spec.ratioX}:${spec.ratioY} ratio (${spec.targetWidthPx}×${spec.targetHeightPx})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Studio Background Tone (Stitch 5-Color Row)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
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
                                    Icon(Icons.Filled.Palette, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Text("Studio Background Tone", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    val badgeText = when {
                                        uiState.isSegmenting -> "AI Removing BG..."
                                        uiState.cutoutUri != null -> "AI Studio Matte ✓"
                                        else -> "100% Offline AI"
                                    }
                                    Text(badgeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Text(
                                text = "Pure vector matte applied on-device. No cloud replacement or alteration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // 5 Colors: White, Sky Blue, Royal Blue, Light Gray, Soft Red
                            val bgOptions = listOf(
                                Triple(PassportBackground.WHITE, Color.White, "White"),
                                Triple(PassportBackground.BLUE, Color(0xFF1D4ED8), "Royal Blue"),
                                Triple(PassportBackground.RED, Color(0xFFDC2626), "Red"),
                                Triple(PassportBackground.GRAY, Color(0xFFE2E8F0), "Gray"),
                                Triple(PassportBackground.ORIGINAL, Color.Transparent, "Original")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                bgOptions.forEach { (bg, color, label) ->
                                    val isSelected = uiState.config.background == bg
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 0.dp,
                                            if (isSelected) Primary else Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.updateBackground(bg) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (bg == PassportBackground.ORIGINAL) MaterialTheme.colorScheme.surfaceVariant else color)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = if (color == Color.White) Primary else Color.White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Visual Border & Cut Marks (Stitch)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
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
                                    Icon(Icons.Filled.CropFree, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                    Text("Visual Border & Cut Marks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                                Text("Print Guide", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val frames = listOf(
                                    IdFrameStyle.NONE to "None",
                                    IdFrameStyle.THIN to "0.5pt Border",
                                    IdFrameStyle.PROFESSIONAL to "Corner Marks"
                                )
                                frames.forEach { (frame, label) ->
                                    val isSelected = uiState.config.frameStyle == frame
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.updateFrameStyle(frame) }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 100% Offline Honest Notice
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
                            Icon(Icons.Filled.Verified, contentDescription = null, tint = Tertiary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Processed 100% locally on device. No cloud upload, biometrics storage, or external tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Fixed Sticky Bottom Bar
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val nextLayout = if (uiState.config.printLayout == PassportPrintLayout.COPIES_4) {
                                PassportPrintLayout.COPIES_8
                            } else {
                                PassportPrintLayout.COPIES_4
                            }
                            viewModel.updatePrintLayout(nextLayout)
                            Toast.makeText(context, "Layout: ${nextLayout.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("4×6 Sheet", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            if (uiState.selectedImageUri != null) {
                                viewModel.processPassport()
                            } else {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier
                            .weight(2f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Processing Sheet...", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Continue to Print", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
