package cz.obchodnik.ui.markets

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Encodes/decodes the user's saved market views to a single JSON string stored in
 * settings. Decoding is defensive: blank input yields an empty list and entries with
 * unknown category/sort enum names are skipped instead of crashing.
 */
object SavedMarketViewSerializer {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    private data class StoredView(
        val id: String,
        val name: String,
        val category: String,
        val sortMode: String,
        val query: String = "",
    )

    fun encode(views: List<SavedMarketView>): String {
        val stored = views.map { view ->
            StoredView(
                id = view.id,
                name = view.name,
                category = view.category.name,
                sortMode = view.sortMode.name,
                query = view.query,
            )
        }
        return json.encodeToString(ListSerializer(StoredView.serializer()), stored)
    }

    fun decode(raw: String): List<SavedMarketView> {
        if (raw.isBlank()) return emptyList()
        val stored = runCatching {
            json.decodeFromString(ListSerializer(StoredView.serializer()), raw)
        }.getOrNull() ?: return emptyList()
        return stored.mapNotNull { it.toSavedViewOrNull() }
    }

    private fun StoredView.toSavedViewOrNull(): SavedMarketView? {
        val category = runCatching { MarketCategory.valueOf(category) }.getOrNull() ?: return null
        val sort = runCatching { MarketSortMode.valueOf(sortMode) }.getOrNull() ?: return null
        return SavedMarketView(
            id = id,
            name = name,
            category = category,
            sortMode = sort,
            query = query,
        )
    }
}
