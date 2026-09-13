package com.scanflow.photocompressor.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.scanflow.photocompressor.ui.theme.GradientEnd
import com.scanflow.photocompressor.ui.theme.GradientStart
import java.io.File

/**
 * Modern Image Picker Component.
 * - Utilizes modern Android Photo Picker (PickVisualMedia / PickMultipleVisualMedia).
 * - Avoids requesting broad storage permissions (READ_EXTERNAL_STORAGE / READ_MEDIA_IMAGES).
 * - Implements optional Camera capture workflow (Camera -> Capture -> Temporary Uri -> Analyzer -> Processing).
 * - Requests CAMERA runtime permission strictly when the user chooses camera capture.
 */
@Composable
fun ImagePickerCard(
    selectedImageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    allowMultiple: Boolean = false,
    onMultipleImagesSelected: ((List<Uri>) -> Unit)? = null
) {
    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }

    // Modern Android Photo Picker - Single Selection (Zero storage permissions required)
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // Modern Android Photo Picker - Multiple Selection (Zero storage permissions required)
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onMultipleImagesSelected?.invoke(uris)
            onImageSelected(uris.first())
        }
    }

    // Camera Capture Launcher: writes capture to Temporary Uri
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCameraUri?.let { uri ->
                // Flow: Temporary Uri -> Analyzer -> Processing
                onImageSelected(uri)
            }
        }
    }

    fun launchCameraInternal() {
        try {
            val cacheDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
            val tempFile = File.createTempFile("camera_capture_${System.currentTimeMillis()}_", ".jpg", cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // On-demand CAMERA permission requester: requested only when user chooses camera capture
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            launchCameraInternal()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to capture photos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun requestCameraAndCapture() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraInternal()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Select Image Source") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            showSourceDialog = false
                            if (allowMultiple) {
                                multiPhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                singlePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Image, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (allowMultiple) "Photo Picker (Multiple)" else "Photo Picker (Single)")
                    }

                    OutlinedButton(
                        onClick = {
                            showSourceDialog = false
                            requestCameraAndCapture()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Take Photo with Camera")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                showSourceDialog = true
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            brush = if (selectedImageUri != null) {
                Brush.linearGradient(listOf(GradientStart, GradientEnd))
            } else {
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
        )
    ) {
        if (selectedImageUri != null) {
            // Show scaled, memory-safe, smooth preview
            SafeImagePreview(
                data = selectedImageUri,
                contentDescription = "Selected image",
                modifier = Modifier.fillMaxSize(),
                tier = com.scanflow.photocompressor.ui.preview.PreviewTier.CARD_PREVIEW,
                contentScale = ContentScale.Crop
            )
        } else {
            // Show upload prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Pick image",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (allowMultiple) "Tap to select photos" else "Tap to select a photo",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Photo Picker • Camera (JPEG, PNG, WebP)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
