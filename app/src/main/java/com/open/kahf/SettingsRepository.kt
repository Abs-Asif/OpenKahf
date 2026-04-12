package com.open.kahf

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val PREVENT_CHANGE = booleanPreferencesKey("prevent_change")
        val PREVENT_UNINSTALL = booleanPreferencesKey("prevent_uninstall")
        val PIN = stringPreferencesKey("pin")
        val IS_PIN_SET = booleanPreferencesKey("is_pin_set")
    }

    val preventChange: Flow<Boolean> = context.dataStore.data.map { it[PREVENT_CHANGE] ?: false }
    val preventUninstall: Flow<Boolean> = context.dataStore.data.map { it[PREVENT_UNINSTALL] ?: false }
    val pin: Flow<String?> = context.dataStore.data.map { it[PIN] }
    val isPinSet: Flow<Boolean> = context.dataStore.data.map { it[IS_PIN_SET] ?: false }

    suspend fun setPreventChange(enabled: Boolean) {
        context.dataStore.edit { it[PREVENT_CHANGE] = enabled }
    }

    suspend fun setPreventUninstall(enabled: Boolean) {
        context.dataStore.edit { it[PREVENT_UNINSTALL] = enabled }
    }

    suspend fun setPin(pin: String) {
        context.dataStore.edit {
            it[PIN] = pin
            it[IS_PIN_SET] = true
        }
    }
}
