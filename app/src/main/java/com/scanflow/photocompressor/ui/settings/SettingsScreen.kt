package com.scanflow.photocompressor.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.scanflow.photocompressor.R
import com.scanflow.photocompressor.domain.model.ConflictStrategy
import com.scanflow.photocompressor.domain.model.ImageFormat
import com.scanflow.photocompressor.domain.model.ThemeMode

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            // Unified About & Free Utility Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.PhotoCamera,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("ScanFlow Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("All-in-One Image Utility Toolkit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Fast, private, and 100% offline-first. All tools & compression features are completely free with zero tracking or telemetry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Theme Setting (Rule 78)
            item { 
                Text(
                    text = stringResource(R.string.theme_appearance_title), 
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.SemiBold, 
                    color = MaterialTheme.colorScheme.primary
                ) 
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.theme_mode), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.values().forEach { mode ->
                                val selected = uiState.preferences.theme == mode
                                val modeLabel = when (mode) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                }
                                val modeIcon = when (mode) {
                                    ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
                                    ThemeMode.LIGHT -> Icons.Filled.LightMode
                                    ThemeMode.DARK -> Icons.Filled.DarkMode
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    leadingIcon = {
                                        Icon(modeIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text(modeLabel) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .semantics {
                                            contentDescription = "$modeLabel theme option"
                                        }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val currentDesc = when (uiState.preferences.theme) {
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system_desc)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light_desc)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark_desc)
                        }
                        Text(
                            text = currentDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Default Quality & Format
            item { Text("Compression & Export Defaults", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HighQuality, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Default Quality: ${uiState.preferences.defaultQuality}%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = uiState.preferences.defaultQuality.toFloat(),
                            onValueChange = { viewModel.setDefaultQuality(it.toInt()) },
                            valueRange = 10f..100f,
                            steps = 17
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Image, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Default Format", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(ImageFormat.JPEG, ImageFormat.PNG, ImageFormat.WEBP).forEach { fmt ->
                                val selected = uiState.preferences.defaultFormat == fmt
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setDefaultFormat(fmt) },
                                    label = { Text(fmt.name) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Default Behavior
            item { Text("Default Behavior & Policies", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Preserve EXIF Metadata", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Retain camera settings, date & orientation in output files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.preferences.behavior.preserveExif,
                                onCheckedChange = { viewModel.setPreserveExif(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Maintain Aspect Ratio", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Avoid image distortion when resizing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.preferences.behavior.keepAspectRatio,
                                onCheckedChange = { viewModel.setKeepAspectRatio(it) }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Column {
                            Text("Collision Resolution Strategy", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Applied when saving output files that share an existing name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ConflictStrategy.values().forEach { strategy ->
                                    val selected = uiState.preferences.behavior.conflictStrategy == strategy
                                    FilterChip(
                                        selected = selected,
                                        onClick = { viewModel.setConflictStrategy(strategy) },
                                        label = { Text(strategy.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // File Naming & Scan Templates
            item { Text("Scan & Output File Naming", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Filename Prefix Template", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            com.scanflow.photocompressor.domain.model.NamingPrefixType.values().forEach { prefixType ->
                                val selected = uiState.preferences.namingConfig.prefixType == prefixType
                                val shortLabel = when (prefixType) {
                                    com.scanflow.photocompressor.domain.model.NamingPrefixType.ORIGINAL -> "Orig"
                                    com.scanflow.photocompressor.domain.model.NamingPrefixType.SCAN -> "SCAN_"
                                    com.scanflow.photocompressor.domain.model.NamingPrefixType.IMG -> "IMG_"
                                    com.scanflow.photocompressor.domain.model.NamingPrefixType.DOC -> "DOC_"
                                    com.scanflow.photocompressor.domain.model.NamingPrefixType.CUSTOM -> "Custom"
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setNamingPrefixType(prefixType) },
                                    label = { Text(shortLabel, style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (uiState.preferences.namingConfig.prefixType == com.scanflow.photocompressor.domain.model.NamingPrefixType.CUSTOM) {
                            OutlinedTextField(
                                value = uiState.preferences.namingConfig.customPrefixText,
                                onValueChange = { viewModel.setCustomPrefixText(it) },
                                label = { Text("Custom Prefix Text") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Include Timestamp (Date/Time)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("Appends YYYYMMDD_HHMMSS for archival organization", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.preferences.namingConfig.includeTimestamp,
                                onCheckedChange = { viewModel.setIncludeTimestamp(it) }
                            )
                        }

                        // Live Naming Preview
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val sampleBase = uiState.preferences.namingConfig.getEffectivePrefix("photo")
                            val timestampStr = if (uiState.preferences.namingConfig.includeTimestamp) "_20260920_200000" else ""
                            val ext = uiState.preferences.defaultFormat.extension
                            val previewName = "${sampleBase}${timestampStr}_compressed.$ext"
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Label, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Preview: $previewName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Storage & Memory
            item { Text("Storage & Memory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }

            item {
                SettingsItem(
                    icon = Icons.Filled.Folder,
                    title = "Output Storage",
                    subtitle = "${uiState.outputFileCount} files saved (${uiState.outputDirSize}) • Tap to open Gallery",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.SdCard,
                    title = "Available Free Space",
                    subtitle = uiState.availableStorage
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Filled.CleaningServices,
                    title = "Clear Cache & Temporary Processing Files",
                    subtitle = "Temporary files: ${uiState.cacheSize}",
                    onClick = { viewModel.clearCache() }
                )
            }

            // Privacy & Legal
            item { Text("Privacy & Legal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }

            item {
                SettingsItem(
                    icon = Icons.Filled.Policy,
                    title = "Privacy Policy",
                    subtitle = "View full privacy policy on GitHub Pages",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://biputsmk89-a11y.github.io/ScanFlow-Photo/privacy-policy.html"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback if browser is unavailable
                        }
                    }
                )
            }

            // Reset Settings
            item {
                OutlinedButton(
                    onClick = { viewModel.resetDefaults() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.RestartAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset All Preferences to Defaults")
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClick != null) {
                Icon(Icons.Filled.ChevronRight, "Action", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
