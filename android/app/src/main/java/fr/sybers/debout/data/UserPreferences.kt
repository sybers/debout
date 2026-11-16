package fr.sybers.debout.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "preferences")

/** Writes run in [scope] so they survive the screen that triggered them. */
class UserPreferences(context: Context, private val scope: CoroutineScope) {
    private val dataStore = context.dataStore

    private val preferences = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    val lastViewedMachineId: Flow<Long?> = preferences.map { it[LAST_VIEWED_MACHINE] }

    val wakeNotificationsEnabled: Flow<Boolean> = preferences.map { it[WAKE_NOTIFICATIONS] ?: true }

    fun setLastViewedMachine(id: Long) {
        scope.launch { dataStore.edit { it[LAST_VIEWED_MACHINE] = id } }
    }

    fun setWakeNotificationsEnabled(enabled: Boolean) {
        scope.launch { dataStore.edit { it[WAKE_NOTIFICATIONS] = enabled } }
    }

    private companion object {
        val LAST_VIEWED_MACHINE = longPreferencesKey("last_viewed_machine")
        val WAKE_NOTIFICATIONS = booleanPreferencesKey("wake_notifications")
    }
}
