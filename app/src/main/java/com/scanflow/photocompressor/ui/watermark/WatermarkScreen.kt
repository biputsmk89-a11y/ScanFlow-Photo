package com.scanflow.photocompressor.ui.watermark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.scanflow.photocompressor.util.ShareHelper
import com.scanflow.photocompressor.domain.model.WatermarkPosition
import com.scanflow.photocompressor.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatermarkScreen(
    onNavigateBack: () -> Unit,
    initialUri: android.net.Uri? = null,
    viewModel: WatermarkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(initialUri) {
        if (initialUri != null && uiState.selectedImageUri != initialUri) {
            viewModel.selectImage(initialUri)
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Watermark") }, navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { ImagePickerCard(selectedImageUri = uiState.selectedImageUri, onImageSelected = { viewModel.selectImage(it) }) }

                item {
                    OutlinedTextField(
                        value = uiState.text, onValueChange = { viewModel.setText(it) },
                        label = { Text("Watermark Text") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.TextFields, null) }
                    )
                }

                item {
                    Text("Position", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(WatermarkPosition.values()) { pos ->
                            FilterChip(selected = uiState.position == pos, onClick = { viewModel.setPosition(pos) }, label = { Text(pos.label, style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }

                item {
                    Column {
                        Text("Opacity: ${(uiState.opacity * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                        Slider(value = uiState.opacity, onValueChange = { viewModel.setOpacity(it) }, valueRange = 0.1f..1f)
                    }
                }

                item {
                    Column {
                        Text("Font Size: ${uiState.fontSize.toInt()}sp", style = MaterialTheme.typography.titleSmall)
                        Slider(value = uiState.fontSize, onValueChange = { viewModel.setFontSize(it) }, valueRange = 8f..72f)
                    }
                }

                item {
                    Button(onClick = { viewModel.apply() }, Modifier.fillMaxWidth().height(52.dp), enabled = uiState.selectedImageUri != null && !uiState.isProcessing && uiState.text.isNotBlank(), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.TextFields, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (uiState.isProcessing) "Applying..." else "Add Watermark", fontWeight = FontWeight.SemiBold)
                    }
                }

                uiState.error?.let { item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(it, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } } }
                uiState.result?.let { result ->
                    item { BeforeAfterPreview(originalUri = uiState.selectedImageUri, resultUri = result.outputUri, originalSize = "Original", resultSize = "Watermarked", savedPercentage = "Watermark applied") }
                    item {
                        Button(
                            onClick = { ShareHelper.shareImage(context, result.outputUri, "image/*", "Share Watermarked Image") },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share Watermarked Image", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    item { OutlinedButton(onClick = { viewModel.reset() }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Add to Another") } }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
            if (uiState.isProcessing) ProcessingOverlay(isVisible = true, message = "Adding watermark...")
        }
    }
}
