package cz.obchodnik.ui.search

import cz.obchodnik.domain.model.Asset

data class SearchUiState(
    val query: String = "",
    val remoteResults: List<Asset> = emptyList(),
    val catalogResults: List<Asset> = emptyList(),
    val watchlistIds: Set<String> = emptySet(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
)
