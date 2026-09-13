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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.scanflow.photocompressor.ui.components.ProPaywallSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showPaywall by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    if (showPaywall) {
        ProPaywallSheet(
            onDismiss = { showPaywall = false },
            onUpgradeClicked = {
                showPaywall = false
                (context as? Activity)?.let { act ->
                    viewModel.upgradeToPro(act)
                }
            },
            onRestoreClicked = {
                showPaywall = false
                viewModel.restorePurchases()
            }
        )
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

            // Pro Membership Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isPro) 
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (uiState.isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (uiState.isPro) "ScanFlow Pro Active" else "ScanFlow Free Tier",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (uiState.isPro) "Unlimited batching & all features unlocked" else "Upgrade for unlimited batching & zero ads",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (!uiState.isPro) {
                            Button(
                                onClick = { showPaywall = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Upgrade")
                            }
                        }
                    }
                }
            }

            // About banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PhotoCamera, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("ScanFlow Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("All-in-One Image Utility Toolkit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Fast, private, and offline-first image compression, resizing, and conversion.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                            Text("Strategy when saving an output file with an existing name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            // Storage & Memory
            item { Text("Storage & Memory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }

            item {
                SettingsItem(
                    icon = Icons.Filled.Folder,
                    title = "Output Storage",
                    subtitle = "${uiState.outputFileCount} files saved (${uiState.outputDirSize})"
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

            // Privacy & Transparency
            item { Text("Privacy & Legal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }

            item {
                SettingsItem(
                    icon = Icons.Filled.Security,
                    title = "100% Offline Processing",
                    subtitle = "All compression & photo processing runs strictly on-device. Zero data collected."
                )
            }
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
