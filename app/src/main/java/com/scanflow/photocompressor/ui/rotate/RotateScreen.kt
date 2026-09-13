package com.scanflow.photocompressor.ui.rotate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotateScreen(onNavigateBack: () -> Unit, viewModel: RotateViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Rotate & Flip") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { ImagePickerCard(selectedImageUri = uiState.selectedImageUri, onImageSelected = { viewModel.selectImage(it) }) }

                item {
                    Text("Rotation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.rotate90CCW() }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.AutoMirrored.Filled.RotateLeft, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("90° CCW")
                        }
                        OutlinedButton(onClick = { viewModel.rotate90CW() }, Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("90° CW")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Custom: ${uiState.rotation.toInt()}°", style = MaterialTheme.typography.bodySmall)
                    Slider(value = uiState.rotation, onValueChange = { viewModel.setRotation(it) }, valueRange = 0f..359f)
                }

                item {
                    Text("Flip", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = uiState.flipH, onClick = { viewModel.toggleFlipH() }, label = { Text("Horizontal") }, leadingIcon = { Icon(Icons.Filled.Flip, null, Modifier.size(16.dp)) }, modifier = Modifier.weight(1f))
                        FilterChip(selected = uiState.flipV, onClick = { viewModel.toggleFlipV() }, label = { Text("Vertical") }, leadingIcon = { Icon(Icons.Filled.FlipCameraAndroid, null, Modifier.size(16.dp)) }, modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Button(onClick = { viewModel.apply() }, Modifier.fillMaxWidth().height(52.dp), enabled = uiState.selectedImageUri != null && !uiState.isProcessing, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.Check, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (uiState.isProcessing) "Applying..." else "Apply", fontWeight = FontWeight.SemiBold)
                    }
                }

                uiState.error?.let { item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } } }
                uiState.result?.let { result ->
                    item { BeforeAfterPreview(originalUri = uiState.selectedImageUri, resultUri = result.outputUri, originalSize = uiState.imageInfo?.resolution ?: "", resultSize = "${result.width}x${result.height}", savedPercentage = "Rotation applied") }
                    item { OutlinedButton(onClick = { viewModel.reset() }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Rotate Another") } }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            if (uiState.isProcessing) ProcessingOverlay(isVisible = true, message = "Applying transformations...")
        }
    }
}
