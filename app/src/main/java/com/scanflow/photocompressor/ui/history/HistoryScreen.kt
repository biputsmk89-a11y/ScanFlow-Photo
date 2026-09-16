package com.scanflow.photocompressor.ui.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.scanflow.photocompressor.ui.components.OperationChip
import com.scanflow.photocompressor.ui.components.StatCard
import com.scanflow.photocompressor.ui.theme.Success
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToEdit: ((android.net.Uri) -> Unit)? = null,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<com.scanflow.photocompressor.domain.model.ProcessingHistory?>(null) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all processing history?") },
            confirmButton = { TextButton(onClick = { viewModel.clearAll(); showClearDialog = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    selectedEntry?.let { entry ->
        ModalBottomSheet(
            onDismissRequest = { selectedEntry = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isPdf = entry.operation == com.scanflow.photocompressor.domain.model.OperationType.PDF || entry.outputFileName.endsWith(".pdf", ignoreCase = true)
                val mimeType = if (isPdf) "application/pdf" else "image/*"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val previewUri = entry.outputUri ?: entry.inputUri
                    if (isPdf) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.PictureAsPdf,
                                    contentDescription = "PDF Document",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    } else if (!previewUri.isNullOrEmpty()) {
                        com.scanflow.photocompressor.ui.components.SafeThumbnail(
                            data = previewUri,
                            contentDescription = entry.inputFileName,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = entry.outputFileName.ifEmpty { entry.inputFileName },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                        OperationChip(operationType = entry.operation)
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Original Size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatBytes(entry.originalSize), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Result Size", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatBytes(entry.resultSize), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    if (entry.savedPercentage > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reduction", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${String.format("%.1f", entry.savedPercentage)}% (${formatBytes(entry.savedBytes)})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Success)
                        }
                    }
                    if (entry.width > 0 && entry.height > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Dimensions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${entry.width} × ${entry.height} px", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatTimestamp(entry.timestamp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val uriStr = entry.outputUri ?: entry.inputUri
                            if (!uriStr.isNullOrEmpty()) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                        setDataAndType(android.net.Uri.parse(uriStr), mimeType)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "No app available to open this file", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                android.widget.Toast.makeText(context, "File URI not available", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPdf) "Open PDF" else "Open")
                    }

                    OutlinedButton(
                        onClick = {
                            val uriStr = entry.outputUri ?: entry.inputUri
                            if (!uriStr.isNullOrEmpty()) {
                                try {
                                    com.scanflow.photocompressor.util.ShareHelper.shareImage(
                                        context = context,
                                        uri = android.net.Uri.parse(uriStr),
                                        mimeType = mimeType,
                                        title = if (isPdf) "Share PDF" else "Share Image"
                                    )
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Could not share file", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteEntry(entry.id)
                            selectedEntry = null
                        }
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (!isPdf && onNavigateToEdit != null) {
                    val editUriStr = entry.outputUri ?: entry.inputUri
                    if (!editUriStr.isNullOrEmpty()) {
                        OutlinedButton(
                            onClick = {
                                val uriToEdit = android.net.Uri.parse(editUriStr)
                                selectedEntry = null
                                onNavigateToEdit(uriToEdit)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Edit / Further Compress Photo")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                if (uiState.historyList.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) { Icon(Icons.Filled.DeleteSweep, "Clear all") }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(icon = Icons.Filled.SaveAlt, value = formatBytes(uiState.totalSavedBytes), label = "Saved", modifier = Modifier.weight(1f), gradient = true)
                StatCard(icon = Icons.Filled.CheckCircle, value = "${uiState.totalOperations}", label = "Total Ops", modifier = Modifier.weight(1f))
            }
        }

        if (uiState.historyList.isEmpty() && !uiState.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.History, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.height(16.dp))
                        Text("No history yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Start compressing images to see history here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }

        items(uiState.historyList, key = { it.id }) { entry ->
            Card(
                onClick = { selectedEntry = entry },
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val previewUri = entry.outputUri ?: entry.inputUri
                    val isPdf = entry.operation == com.scanflow.photocompressor.domain.model.OperationType.PDF || entry.outputFileName.endsWith(".pdf", ignoreCase = true)
                    if (isPdf) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.PictureAsPdf,
                                    contentDescription = "PDF Document",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                    } else if (!previewUri.isNullOrEmpty()) {
                        com.scanflow.photocompressor.ui.components.SafeThumbnail(
                            data = previewUri,
                            contentDescription = entry.inputFileName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(entry.inputFileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OperationChip(operationType = entry.operation)
                            Text("${formatBytes(entry.originalSize)} → ${formatBytes(entry.resultSize)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(formatTimestamp(entry.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (entry.savedPercentage > 0) {
                            Text("-${String.format("%.0f", entry.savedPercentage)}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Success)
                        }
                        IconButton(onClick = { viewModel.deleteEntry(entry.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatTimestamp(ts: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
