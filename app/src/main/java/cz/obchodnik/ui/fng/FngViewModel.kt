package cz.obchodnik.ui.fng

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.obchodnik.ObchodnikApp
import cz.obchodnik.core.Result
import cz.obchodnik.data.repository.FngRepository
import cz.obchodnik.domain.model.Fng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FngUiState(
    val currentFng: Fng? = null,
    val history: List<Fng> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class FngViewModel(
    private val fngRepository: FngRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FngUiState())
    val uiState: StateFlow<FngUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentResult = fngRepository.getCurrentFng()
            val historyResult = fngRepository.getFngHistory(30)

            if (currentResult is Result.Success && historyResult is Result.Success) {
                _uiState.update {
                    it.copy(
                        currentFng = currentResult.data,
                        history = historyResult.data,
                        isLoading = false
                    )
                }
            } else {
                val errorMsg = when {
                    currentResult is Result.Error -> currentResult.message
                    historyResult is Result.Error -> historyResult.message
                    else -> "Nepodařilo se načíst data pro Fear & Greed index."
                }
                _uiState.update {
                    it.copy(
                        errorMessage = errorMsg,
                        isLoading = false
                    )
                }
            }
        }
    }

    companion object {
        fun factory(app: ObchodnikApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FngViewModel(
                        fngRepository = app.container.fngRepository,
                    ) as T
                }
            }
    }
}
