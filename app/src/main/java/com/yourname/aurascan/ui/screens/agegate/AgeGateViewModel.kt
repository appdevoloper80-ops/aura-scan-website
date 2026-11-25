package com.yourname.aurascan.ui.screens.agegate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourname.aurascan.data.local.AgeGateStatus
import com.yourname.aurascan.data.local.AgePreferenceStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AgeGateViewModel(
    private val agePreferenceStore: AgePreferenceStore
) : ViewModel() {

    val ageGateStatus: StateFlow<AgeGateStatus?> = agePreferenceStore.ageGateStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun confirmAdult() {
        viewModelScope.launch {
            agePreferenceStore.setAgeGateStatus(AgeGateStatus.ADULT_18_PLUS)
        }
    }

    fun markUnder18() {
        viewModelScope.launch {
            agePreferenceStore.setAgeGateStatus(AgeGateStatus.UNDER_18)
        }
    }

    class Factory(
        private val agePreferenceStore: AgePreferenceStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AgeGateViewModel(agePreferenceStore) as T
        }
    }
}
