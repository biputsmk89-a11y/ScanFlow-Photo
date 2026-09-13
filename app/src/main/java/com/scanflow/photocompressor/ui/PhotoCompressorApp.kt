package com.scanflow.photocompressor.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scanflow.photocompressor.ui.batch.BatchScreen
import com.scanflow.photocompressor.ui.compress.CompressScreen
import com.scanflow.photocompressor.ui.convert.ConvertScreen
import com.scanflow.photocompressor.ui.crop.CropScreen
import com.scanflow.photocompressor.ui.history.HistoryScreen
import com.scanflow.photocompressor.ui.home.HomeScreen
import com.scanflow.photocompressor.ui.navigation.Screen
import com.scanflow.photocompressor.ui.passport.PassportScreen
import com.scanflow.photocompressor.ui.pdf.PdfScreen
import com.scanflow.photocompressor.ui.resize.ResizeScreen
import com.scanflow.photocompressor.ui.rotate.RotateScreen
import com.scanflow.photocompressor.ui.settings.SettingsScreen
import com.scanflow.photocompressor.ui.social.SocialScreen
import com.scanflow.photocompressor.ui.watermark.WatermarkScreen
import com.scanflow.photocompressor.ui.whatsapp.WhatsAppScreen

/**
 * Root composable with NavHost and BottomNavigation.
 * Seamlessly handles incoming external share intents (ACTION_SEND and ACTION_SEND_MULTIPLE).
 */
@Composable
fun PhotoCompressorApp(
    incomingSharedUris: List<Uri>? = null,
    onSharedUrisHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var sharedUrisToHandle by remember { mutableStateOf<List<Uri>?>(null) }

    LaunchedEffect(incomingSharedUris) {
        if (!incomingSharedUris.isNullOrEmpty()) {
            sharedUrisToHandle = incomingSharedUris
            if (incomingSharedUris.size == 1) {
                navController.navigate(Screen.Compress.route) {
                    launchSingleTop = true
                }
            } else {
                navController.navigate(Screen.Batch.route) {
                    launchSingleTop = true
                }
            }
            onSharedUrisHandled()
        }
    }

    // Show bottom nav only for main tabs
    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                screen.icon?.let {
                                    Icon(it, contentDescription = screen.title)
                                }
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Bottom nav destinations
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToCompress = { navController.navigate(Screen.Compress.route) },
                    onNavigateToBatch = { navController.navigate(Screen.Batch.route) },
                    onNavigateToResize = { navController.navigate(Screen.Resize.route) },
                    onNavigateToCrop = { navController.navigate(Screen.Crop.route) },
                    onNavigateToRotate = { navController.navigate(Screen.Rotate.route) },
                    onNavigateToWatermark = { navController.navigate(Screen.Watermark.route) },
                    onNavigateToConvert = { navController.navigate(Screen.Convert.route) },
                    onNavigateToPdf = { navController.navigate(Screen.Pdf.route) },
                    onNavigateToPassport = { navController.navigate(Screen.Passport.route) },
                    onNavigateToSocial = { navController.navigate(Screen.Social.route) },
                    onNavigateToWhatsApp = { navController.navigate(Screen.WhatsApp.route) }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            // Tool destinations
            composable(Screen.Compress.route) {
                CompressScreen(
                    initialUris = sharedUrisToHandle,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onNavigateToEdit = { navController.navigate(Screen.Resize.route) }
                )
            }

            composable(Screen.Batch.route) {
                BatchScreen(
                    initialUris = sharedUrisToHandle,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Resize.route) {
                ResizeScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Crop.route) {
                CropScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Rotate.route) {
                RotateScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Watermark.route) {
                WatermarkScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Convert.route) {
                ConvertScreen(onNavigateBack = { navController.popBackStack() })
            }

            // Advanced tool destinations
            composable(Screen.Pdf.route) {
                PdfScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Passport.route) {
                PassportScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Social.route) {
                SocialScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.WhatsApp.route) {
                WhatsAppScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
