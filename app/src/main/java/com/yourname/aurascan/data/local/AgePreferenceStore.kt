package com.yourname.aurascan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ageDataStore: DataStore<Preferences> by preferencesDataStore(name = "aurascan_prefs")

enum class AgeGateStatus {
    UNSET,
    ADULT_18_PLUS,
    UNDER_18
}

class AgePreferenceStore(private val context: Context) {

    private val ageStatusKey = stringPreferencesKey("age_gate_status")

    val ageGateStatus: Flow<AgeGateStatus> = context.ageDataStore.data.map { prefs ->
        when (prefs[ageStatusKey]) {
            AgeGateStatus.ADULT_18_PLUS.name -> AgeGateStatus.ADULT_18_PLUS
            AgeGateStatus.UNDER_18.name -> AgeGateStatus.UNDER_18
            else -> AgeGateStatus.UNSET
        }
    }

    suspend fun setAgeGateStatus(status: AgeGateStatus) {
        context.ageDataStore.edit { prefs ->
            prefs[ageStatusKey] = status.name
        }
    }
}
