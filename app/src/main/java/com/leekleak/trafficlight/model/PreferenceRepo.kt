package com.leekleak.trafficlight.model


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PreferenceRepo (
    private val context: Context
) {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    val data get() = context.dataStore.data

    val modeAOD: Flow<Boolean> = context.dataStore.data.map { it[MODE_AOD] ?: false }
    fun setModeAOD(value: Boolean) = scope.launch { context.dataStore.edit { it[MODE_AOD] = value } }

    val bigIcon: Flow<Boolean> = context.dataStore.data.map { it[BIG_ICON] ?: false }
    fun setBigIcon(value: Boolean) = scope.launch { context.dataStore.edit { it[BIG_ICON] = value } }
    private companion object {
        val MODE_AOD = booleanPreferencesKey("mode_aod")
        val BIG_ICON = booleanPreferencesKey("big_icon")
    }
}