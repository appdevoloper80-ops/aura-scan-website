package com.yourname.aurascan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yourname.aurascan.data.local.AgeGateStatus
import com.yourname.aurascan.ui.navigation.AuraScanNavGraph
import com.yourname.aurascan.ui.screens.agegate.AgeCheckDialog
import com.yourname.aurascan.ui.screens.agegate.AgeGateViewModel
import com.yourname.aurascan.ui.screens.agegate.UnderAgeBlockedScreen
import com.yourname.aurascan.ui.theme.AuraScanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val appContainer = (application as AuraScanApplication).container

        setContent {
            AuraScanTheme {
                val ageGateViewModel: AgeGateViewModel = viewModel(
                    factory = AgeGateViewModel.Factory(appContainer.agePreferenceStore)
                )
                val ageStatus by ageGateViewModel.ageGateStatus.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (ageStatus) {
                        null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        AgeGateStatus.UNSET -> {
                            AuraScanNavGraph(appContainer = appContainer)
                            AgeCheckDialog(
                                onConfirmAdult = { ageGateViewModel.confirmAdult() },
                                onUnder18 = { ageGateViewModel.markUnder18() }
                            )
                        }
                        AgeGateStatus.UNDER_18 -> {
                            UnderAgeBlockedScreen(
                                onCloseApp = { finishAffinity() }
                            )
                        }
                        AgeGateStatus.ADULT_18_PLUS -> {
                            AuraScanNavGraph(appContainer = appContainer)
                        }
                    }
                }
            }
        }
    }
}
