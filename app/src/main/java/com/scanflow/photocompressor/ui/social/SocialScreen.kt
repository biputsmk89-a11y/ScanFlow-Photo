package com.scanflow.photocompressor.ui.social

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanflow.photocompressor.domain.model.SocialContentType
import com.scanflow.photocompressor.domain.model.SocialPlatform
import com.scanflow.photocompressor.ui.components.ImagePickerCard
import com.scanflow.photocompressor.ui.components.SafeImagePreview
import com.scanflow.photocompressor.ui.preview.PreviewTier
import com.scanflow.photocompressor.ui.theme.Success
import com.scanflow.photocompressor.util.ReductionCalculator
import com.scanflow.photocompressor.util.ShareHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialScreen(
    onNavigateBack: () -> Unit,
    viewModel: SocialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social Media Ready") },
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
            // 1. SELECT IMAGE
            item {
                Text(
                    text = "1. Select Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ImagePickerCard(
                    selectedImageUri = uiState.selectedImageUri,
                    onImageSelected = { viewModel.selectImage(it) }
                )
            }

            if (uiState.selectedImageUri != null) {
                // 2. PLATFORM SELECTOR: Instagram, Facebook, TikTok, YouTube, LinkedIn, Custom
                item {
                    Text(
                        text = "2. Select Platform",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ScrollableTabRow(
                        selectedTabIndex = SocialPlatform.values().indexOf(uiState.selectedPlatform),
                        edgePadding = 0.dp
                    ) {
                        SocialPlatform.values().forEach { platform ->
                            Tab(
                                selected = uiState.selectedPlatform == platform,
                                onClick = { viewModel.selectPlatform(platform) },
                                text = {
                                    Text(
                                        text = platform.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (uiState.selectedPlatform == platform) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }

                // 3. CONTENT TYPE SELECTOR: Post, Story, Cover, Thumbnail, Profile
                // Do NOT force user to know pixel dimensions - configuration is internal preset
                item {
                    Text(
                        text = "3. Select Content Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SocialContentType.values()) { type ->
                            FilterChip(
                                selected = uiState.selectedType == type,
                                onClick = { viewModel.selectType(type) },
                                label = {
                                    Text(
                                        text = type.displayName,
                                        fontWeight = if (uiState.selectedType == type) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // 4. PREVIEW
                item {
                    Text(
                        text = "4. Format Preview (${uiState.selectedPlatform.displayName} • ${uiState.selectedType.displayName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        SafeImagePreview(
                            data = uiState.selectedImageUri,
                            contentDescription = "Social Source",
                            modifier = Modifier.fillMaxSize(),
                            tier = PreviewTier.CARD_PREVIEW,
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // 5. COMPRESS QUALITY
                item {
                    Text(
                        text = "5. Quality: ${uiState.quality}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Slider(
                        value = uiState.quality.toFloat(),
                        onValueChange = { viewModel.updateQuality(it.toInt()) },
                        valueRange = 10f..100f,
                        steps = 17
                    )
                }

                // 6. PROCESS BUTTON
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.processSocialImage() },
                        enabled = !uiState.isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Processing via Image Pipeline...")
                        } else {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Process for ${uiState.selectedPlatform.displayName} ${uiState.selectedType.displayName}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // RESULT CARD
                val activeResult = uiState.result
                if (activeResult != null) {
                    item {
                        val result = activeResult
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
                                    text = "Ready for ${uiState.selectedPlatform.displayName}!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ReductionCalculator.formatBytes(result.outputBytes)} • -${String.format("%.1f", result.reductionPercent)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        ShareHelper.shareImage(
                                            context,
                                            result.outputUri,
                                            "image/jpeg",
                                            "Share to ${uiState.selectedPlatform.displayName}"
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Share, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Share to ${uiState.selectedPlatform.displayName}")
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
