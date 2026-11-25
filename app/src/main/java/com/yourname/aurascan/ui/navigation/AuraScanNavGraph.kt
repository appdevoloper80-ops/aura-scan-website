package com.yourname.aurascan.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yourname.aurascan.AppContainer
import com.yourname.aurascan.ui.screens.document.DocumentScreen
import com.yourname.aurascan.ui.screens.document.DocumentViewModel
import com.yourname.aurascan.ui.screens.home.HomeScreen
import com.yourname.aurascan.ui.screens.home.HomeViewModel
import com.yourname.aurascan.ui.screens.imagestopdf.ImagesToPdfScreen
import com.yourname.aurascan.ui.screens.imagestopdf.ImagesToPdfViewModel
import com.yourname.aurascan.ui.screens.ocr.OcrScreen
import com.yourname.aurascan.ui.screens.ocr.OcrViewModel
import com.yourname.aurascan.ui.screens.resizer.ResizerScreen
import com.yourname.aurascan.ui.screens.resizer.ResizerViewModel
import com.yourname.aurascan.ui.screens.settings.SettingsScreen

@Composable
fun AuraScanNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext

    NavHost(
        navController = navController,
        startDestination = AuraScanRoutes.HOME
    ) {
        composable(AuraScanRoutes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(appContainer.documentRepository)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onOpenDocument = { docId ->
                    navController.navigate(AuraScanRoutes.document(docId))
                },
                onOpenImagesToPdf = {
                    navController.navigate(AuraScanRoutes.IMAGES_TO_PDF)
                },
                onOpenResizer = {
                    navController.navigate(AuraScanRoutes.RESIZER)
                },
                onOpenOcr = {
                    navController.navigate(AuraScanRoutes.ocrStandalone())
                },
                onOpenSettings = {
                    navController.navigate(AuraScanRoutes.SETTINGS)
                }
            )
        }

        composable(
            route = AuraScanRoutes.DOCUMENT,
            arguments = listOf(
                navArgument("documentId") { type = NavType.StringType }
            )
        ) {
            val documentViewModel: DocumentViewModel = viewModel(
                factory = DocumentViewModel.Factory(appContainer.documentRepository)
            )
            DocumentScreen(
                viewModel = documentViewModel,
                onBack = { navController.popBackStack() },
                onOpenOcr = { docId ->
                    navController.navigate(AuraScanRoutes.ocrForDocument(docId))
                }
            )
        }

        composable(
            route = AuraScanRoutes.OCR,
            arguments = listOf(
                navArgument("documentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("documentId")
            val ocrViewModel: OcrViewModel = viewModel(
                factory = OcrViewModel.Factory(
                    appContext = appContext,
                    documentRepository = appContainer.documentRepository,
                    ocrRepository = appContainer.ocrRepository
                )
            )
            OcrScreen(
                viewModel = ocrViewModel,
                isStandaloneImageMode = docId.isNullOrBlank(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(AuraScanRoutes.IMAGES_TO_PDF) {
            val imagesToPdfViewModel: ImagesToPdfViewModel = viewModel(
                factory = ImagesToPdfViewModel.Factory(
                    appContext = appContext,
                    documentRepository = appContainer.documentRepository
                )
            )
            ImagesToPdfScreen(
                viewModel = imagesToPdfViewModel,
                onBack = { navController.popBackStack() },
                onPdfCreated = { docId ->
                    navController.navigate(AuraScanRoutes.document(docId)) {
                        popUpTo(AuraScanRoutes.IMAGES_TO_PDF) { inclusive = true }
                    }
                }
            )
        }

        composable(AuraScanRoutes.RESIZER) {
            val resizerViewModel: ResizerViewModel = viewModel(
                factory = ResizerViewModel.Factory(appContext = appContext)
            )
            ResizerScreen(
                viewModel = resizerViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(AuraScanRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
