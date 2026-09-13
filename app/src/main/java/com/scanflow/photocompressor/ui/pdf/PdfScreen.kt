package com.scanflow.photocompressor.ui.pdf

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.domain.model.*
import com.scanflow.photocompressor.ui.components.ImagePickerCard
import com.scanflow.photocompressor.ui.components.SafeThumbnail
import com.scanflow.photocompressor.ui.theme.Success
import com.scanflow.photocompressor.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfScreen(
    onNavigateBack: () -> Unit,
    viewModel: PdfViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Document Creator") },
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
            // 1. SELECT IMAGES
            item {
                Text(
                    text = "1. Select Images",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ImagePickerCard(
                    selectedImageUri = uiState.selectedImages.firstOrNull()?.uri,
                    onImageSelected = { viewModel.addImages(listOf(it)) },
                    allowMultiple = true,
                    onMultipleImagesSelected = { viewModel.addImages(it) }
                )
            }

            // 2. REORDER IMAGES
            if (uiState.selectedImages.isNotEmpty()) {
                item {
                    Text(
                        text = "2. Reorder Pages (${uiState.selectedImages.size} pages)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                itemsIndexed(uiState.selectedImages) { index, image ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(28.dp)
                            )
                            SafeThumbnail(
                                data = image.uri,
                                contentDescription = image.fileName,
                                modifier = Modifier
                                    .size(48.dp)
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
                            IconButton(
                                onClick = { viewModel.moveImageUp(index) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, "Move Up", modifier = Modifier.size(20.dp))
                            }
                            IconButton(
                                onClick = { viewModel.moveImageDown(index) },
                                enabled = index < uiState.selectedImages.size - 1
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, "Move Down", modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.removeImage(index) }) {
                                Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // 3. PAGE SIZE
                item {
                    Text(
                        text = "3. Page Size",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(PdfPageSize.values().toList()) { size ->
                            FilterChip(
                                selected = uiState.config.pageSize == size,
                                onClick = { viewModel.updatePageSize(size) },
                                label = {
                                    Text(
                                        text = size.displayName,
                                        fontWeight = if (uiState.config.pageSize == size) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // 4. MARGINS
                item {
                    Text(
                        text = "4. Margins",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfMargin.values().forEach { margin ->
                            FilterChip(
                                selected = uiState.config.margin == margin,
                                onClick = { viewModel.updateMargin(margin) },
                                label = { Text(margin.displayName, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 5. ORIENTATION
                item {
                    Text(
                        text = "5. Orientation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfOrientation.values().forEach { orient ->
                            FilterChip(
                                selected = uiState.config.orientation == orient,
                                onClick = { viewModel.updateOrientation(orient) },
                                label = { Text(orient.displayName, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 6. QUALITY
                item {
                    Text(
                        text = "6. Quality",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfQuality.values().forEach { qual ->
                            FilterChip(
                                selected = uiState.config.quality == qual,
                                onClick = { viewModel.updateQuality(qual) },
                                label = { Text(qual.displayName, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // 7. GENERATE BUTTON
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.generatePdf() },
                        enabled = !uiState.isGenerating && uiState.selectedImages.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating PDF document...")
                        } else {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("7. Generate PDF (${uiState.selectedImages.size} pages)", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SUCCESS CARD
                val genFile = uiState.generatedFile
                if (genFile != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
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
                                    text = "PDF Generated Successfully!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Size: ${String.format("%.2f", genFile.length() / (1024.0 * 1024.0))} MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            genFile
                                        )
                                        ShareHelper.shareImage(context, uri, "application/pdf", "Share PDF")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Share, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share PDF")
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
