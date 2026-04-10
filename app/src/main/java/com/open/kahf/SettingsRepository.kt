package com.open.kahf

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val PREVENT_CHANGE = booleanPreferencesKey("prevent_change")
        val PREVENT_UNINSTALL = booleanPreferencesKey("prevent_uninstall")
        val DISABLE_REQUEST_TIME = longPreferencesKey("disable_request_time")
    }

    val preventChange: Flow<Boolean> = context.dataStore.data.map { it[PREVENT_CHANGE] ?: false }
    val preventUninstall: Flow<Boolean> = context.dataStore.data.map { it[PREVENT_UNINSTALL] ?: false }
    val disableRequestTime: Flow<Long> = context.dataStore.data.map { it[DISABLE_REQUEST_TIME] ?: 0L }

    suspend fun setPreventChange(enabled: Boolean) {
        context.dataStore.edit { it[PREVENT_CHANGE] = enabled }
    }

    suspend fun setPreventUninstall(enabled: Boolean) {
        context.dataStore.edit { it[PREVENT_UNINSTALL] = enabled }
    }

    suspend fun setDisableRequestTime(time: Long) {
        context.dataStore.edit { it[DISABLE_REQUEST_TIME] = time }
    }
}
