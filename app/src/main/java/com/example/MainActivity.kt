package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.ScanDatabase
import com.example.data.repository.ScanRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.CropEditScreen
import com.example.ui.screens.DocumentDetailScreen
import com.example.ui.screens.FolderManagerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.ScanProTheme
import com.example.ui.viewmodel.ScanViewModel
import com.example.ui.viewmodel.ScanViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob SDK
        com.example.util.AdManager.initialize(applicationContext)

        val database = ScanDatabase.getDatabase(applicationContext)
        val repository = ScanRepository(database.scanDao())
        val factory = ScanViewModelFactory(repository)

        setContent {
            val viewModel: ScanViewModel = viewModel(factory = factory)
            val settings by viewModel.settings.collectAsState()

            val isDarkTheme = when (settings.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            ScanProTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Listen for Toast Messages from ViewModel and Handle Deep Link Verification
                LaunchedEffect(Unit) {
                    val dataUri = intent?.data
                    if (dataUri != null && (dataUri.scheme == "scanpro" || dataUri.host == "scanpro.app")) {
                        val verifiedEmail = dataUri.getQueryParameter("email") ?: "waqasahmad08766@gmail.com"
                        val formattedName = verifiedEmail.substringBefore("@").replaceFirstChar { 
                            if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() 
                        }
                        viewModel.loginUser(formattedName, verifiedEmail)
                        viewModel.setOnboardingCompleted(true)
                        viewModel.showToast("Email Verified via Email Link! Welcome to ScanPro AI")
                        navController.navigate("home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }

                    viewModel.uiMessages.collectLatest { msg ->
                        snackbarHostState.showSnackbar(msg.message)
                    }
                }

                val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("splash") {
                            SplashScreen(
                                onFinishSplash = {
                                    val target = if (hasCompletedOnboarding) "home" else "onboarding"
                                    navController.navigate(target) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("onboarding") {
                            OnboardingScreen(
                                onFinishOnboarding = {
                                    navController.navigate("auth") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("auth") {
                            AuthScreen(
                                onLoginSuccess = { name, email ->
                                    viewModel.loginUser(name, email)
                                    viewModel.setOnboardingCompleted(true)
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onGuestContinue = {
                                    viewModel.logoutUser()
                                    viewModel.setOnboardingCompleted(true)
                                    navController.navigate("home") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                },
                                onShowToast = { msg ->
                                    viewModel.showToast(msg)
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) },
                                onOpenDocument = { docId ->
                                    viewModel.openDocumentDetail(docId)
                                    navController.navigate("doc_detail")
                                },
                                onOpenScan = { navController.navigate("scan") },
                                onOpenFolder = { folderName ->
                                    viewModel.selectedFolderName.value = folderName
                                    navController.navigate("folders")
                                }
                            )
                        }

                        composable("scan") {
                            CameraScanScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onProceedToEdit = { navController.navigate("crop_edit") }
                            )
                        }

                        composable("crop_edit") {
                            CropEditScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onProceedToSave = { viewModel.showSaveModal.value = true }
                            )

                            if (viewModel.showSaveModal.collectAsState().value) {
                                val folders by viewModel.allFolders.collectAsState()
                                com.example.ui.components.SaveExportModal(
                                    folders = folders,
                                    onDismiss = { viewModel.showSaveModal.value = false },
                                    onSave = { title, folderName, format ->
                                        viewModel.saveScanDocument(title, folderName, format)
                                        com.example.util.AdManager.showInterstitial(this@MainActivity) {
                                            navController.navigate("doc_detail") {
                                                popUpTo("home")
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        composable("doc_detail") {
                            DocumentDetailScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("folders") {
                            FolderManagerScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) },
                                onOpenDocument = { docId ->
                                    viewModel.openDocumentDetail(docId)
                                    navController.navigate("doc_detail")
                                },
                                onOpenScan = { navController.navigate("scan") }
                            )
                        }

                        composable("tools") {
                            ToolsScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) },
                                onOpenScan = { navController.navigate("scan") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigate = { route -> navController.navigate(route) },
                                onOpenScan = { navController.navigate("scan") }
                            )
                        }
                    }
                }
            }
        }
    }
}
