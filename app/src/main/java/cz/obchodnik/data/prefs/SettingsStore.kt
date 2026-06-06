package cz.obchodnik.data.prefs

import kotlinx.coroutines.flow.Flow

interface SettingsStore {
    val settings: Flow<AppSettings>
}
