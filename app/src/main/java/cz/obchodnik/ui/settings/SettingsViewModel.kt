package cz.obchodnik.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.work.WorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context?,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { settings ->
            SettingsUiState(settings = settings, isLoading = false)
        }
        .stateIn(
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

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        context = app,
                        settingsRepository = app.container.settingsRepository,
                    ) as T
                }
            }
    }
}
