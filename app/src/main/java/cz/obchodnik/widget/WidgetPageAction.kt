package cz.obchodnik.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class WidgetPageAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val direction = parameters[DirectionKey] ?: "next"
        val totalAssets = parameters[TotalAssetsKey] ?: 0
        val pageSize = parameters[PageSizeKey] ?: 3

        if (totalAssets <= pageSize) return

        val totalPages = (totalAssets + pageSize - 1) / pageSize
        val pageKey = intPreferencesKey("page")

        updateAppWidgetState(context, glanceId) { prefs ->
            val currentPage = prefs[pageKey] ?: 0
            val nextPage = if (direction == "next") {
                (currentPage + 1) % totalPages
            } else {
                (currentPage - 1 + totalPages) % totalPages
            }
            prefs[pageKey] = nextPage
        }
        ObchodnikWidget().update(context, glanceId)
    }

    companion object {
        val DirectionKey = ActionParameters.Key<String>("direction")
        val TotalAssetsKey = ActionParameters.Key<Int>("totalAssets")
        val PageSizeKey = ActionParameters.Key<Int>("pageSize")
    }
}
