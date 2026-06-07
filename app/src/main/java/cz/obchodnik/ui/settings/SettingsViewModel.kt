package cz.obchodnik.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.data.backup.BackupImportPreview
import cz.obchodnik.data.backup.BackupRepository
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.work.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val context: Context?,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository? = null,
) : ViewModel() {

    private var pendingImportText: String? = null
    private val importPreview = MutableStateFlow<BackupImportPreview?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        importPreview,
    ) { settings, preview ->
        SettingsUiState(
            settings = settings,
            isLoading = false,
            importPreview = preview,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(isLoading = true),
    )

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setAccent(accent: String) {
        viewModelScope.launch {
            settingsRepository.setAccent(accent)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.setCurrency(currency)
        }
    }

    fun setRefreshIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setRefreshIntervalMinutes(minutes)
            context?.let { ctx ->
                try {
                    WorkScheduler.scheduleRefresh(ctx, minutes)
                } catch (e: Exception) {
                    android.util.Log.e("SettingsViewModel", "Failed to reschedule refresh worker", e)
                }
            }
        }
    }

    fun setDefaultChart(defaultChart: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultChart(defaultChart)
        }
    }

    fun setDensity(density: String) {
        viewModelScope.launch {
            settingsRepository.setDensity(density)
        }
    }

    fun setShowFngOnWidget(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowFngOnWidget(show)
        }
    }

    fun setCoinGeckoKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setCoinGeckoKey(key)
        }
    }

    fun setAlphaVantageKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setAlphaVantageKey(key)
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            settingsRepository.setGeminiApiKey(key)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingDone(false)
        }
    }

    fun exportData(uri: Uri, includeApiKeys: Boolean = false) {
        val repo = backupRepository ?: return
        viewModelScope.launch {
            val result = runCatching {
                val jsonText = withContext(Dispatchers.Default) {
                    repo.exportToJson(includeApiKeys)
                }
                withContext(Dispatchers.IO) {
                    val ctx = context ?: error("Kontext nedostupný")
                    ctx.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonText.toByteArray(Charsets.UTF_8))
                    } ?: error("Nelze otevřít soubor pro zápis")
                }
            }
            toast(
                result.fold(
                    onSuccess = {
                        if (includeApiKeys) {
                            "Záloha uložena včetně API klíčů"
                        } else {
                            "Záloha uložena bez API klíčů"
                        }
                    },
                    onFailure = { "Export selhal: ${it.message ?: "neznámá chyba"}" },
                ),
            )
        }
    }

    fun previewImportData(uri: Uri) {
        val repo = backupRepository ?: return
        viewModelScope.launch {
            val result = runCatching {
                val text = readText(uri)
                val preview = withContext(Dispatchers.Default) {
                    repo.previewImport(text)
                }
                pendingImportText = text
                importPreview.value = preview
            }
            result.exceptionOrNull()?.let {
                pendingImportText = null
                importPreview.value = null
                toast("Náhled importu selhal: ${it.message ?: "neplatný soubor"}")
            }
        }
    }

    fun dismissImportPreview() {
        pendingImportText = null
        importPreview.value = null
    }

    fun confirmImportData() {
        val repo = backupRepository ?: return
        val text = pendingImportText ?: run {
            toast("Vyberte nejdřív soubor zálohy")
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    repo.importFromJson(text)
                }
            }
            pendingImportText = null
            importPreview.value = null
            toast(
                result.fold(
                    onSuccess = { "Import dokončen: ${it.assets} aktiv, ${it.holdings} pozic, ${it.alerts} alertů" },
                    onFailure = { "Import selhal: ${it.message ?: "neplatný soubor"}" },
                ),
            )
        }
    }

    private suspend fun readText(uri: Uri): String =
        withContext(Dispatchers.IO) {
            val ctx = context ?: error("Kontext nedostupný")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: error("Nelze otevřít soubor pro čtení")
        }

    private fun toast(message: String) {
        context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
    }

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        context = app,
                        settingsRepository = app.container.settingsRepository,
                        backupRepository = app.container.backupRepository,
                    ) as T
                }
            }
    }
}
