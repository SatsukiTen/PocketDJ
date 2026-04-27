package com.djapp.data.source

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dj_app_settings"
)

/**
 * T-007: DataStore（設定値永続化）のデータソース。
 */
@Singleton
class SettingsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.settingsDataStore

    private object Keys {
        val CROSSFADER_CURVE = stringPreferencesKey("crossfader_curve")
        val SCRATCH_MODE     = stringPreferencesKey("scratch_mode")
    }

    fun getCrossfaderCurve(): Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.CROSSFADER_CURVE] ?: "EQUAL_POWER" }

    suspend fun setCrossfaderCurve(curveKey: String) {
        dataStore.edit { prefs -> prefs[Keys.CROSSFADER_CURVE] = curveKey }
    }

    fun getScratchMode(): Flow<String> =
        dataStore.data.map { prefs -> prefs[Keys.SCRATCH_MODE] ?: "CHOP_PAD" }

    suspend fun setScratchMode(modeKey: String) {
        dataStore.edit { prefs -> prefs[Keys.SCRATCH_MODE] = modeKey }
    }
}
