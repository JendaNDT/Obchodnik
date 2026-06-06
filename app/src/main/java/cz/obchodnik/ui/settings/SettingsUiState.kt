package cz.obchodnik.ui.settings

import cz.obchodnik.data.prefs.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
)
