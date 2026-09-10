package com.ledger.iou.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ledger_preferences")

class UserPreferencesRepository(private val context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_PRIVACY_MASK_ENABLED = booleanPreferencesKey("privacy_mask_enabled")
        val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val KEY_TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
    }

    val isTermsAccepted: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_TERMS_ACCEPTED] ?: false
    }

    val isPrivacyMaskEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_PRIVACY_MASK_ENABLED] ?: false
    }

    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val currencySymbol: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_SYMBOL] ?: "₱"
    }

    suspend fun setPrivacyMaskEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PRIVACY_MASK_ENABLED] = enabled
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setTermsAccepted(accepted: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_TERMS_ACCEPTED] = accepted
        }
    }
}
