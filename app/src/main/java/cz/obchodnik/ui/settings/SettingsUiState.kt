package cz.obchodnik.ui.settings

import cz.obchodnik.data.backup.BackupImportPreview
import cz.obchodnik.data.prefs.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = false,
    val importPreview: BackupImportPreview? = null,
)
