package com.zaidxme.whatsappcleaner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class StoreData(val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "store_data")
        val IS_GRID_VIEW_KEY = booleanPreferencesKey("is_grid_view")
    }

    suspend fun set(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    suspend fun get(key: String): String? = context.dataStore.data.first().get(
        stringPreferencesKey(key)
    )

    suspend fun setGridViewPreference(isGridView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRID_VIEW_KEY] = isGridView
        }
    }

    val isGridViewFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_GRID_VIEW_KEY] ?: true
        }
}
