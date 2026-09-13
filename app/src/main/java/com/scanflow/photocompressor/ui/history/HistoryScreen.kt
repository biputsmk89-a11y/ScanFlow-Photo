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
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("Are you sure you want to clear all processing history?") },
            confirmButton = { TextButton(onClick = { viewModel.clearAll(); showClearDialog = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
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
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val previewUri = entry.outputUri ?: entry.inputUri
                    if (!previewUri.isNullOrEmpty()) {
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
