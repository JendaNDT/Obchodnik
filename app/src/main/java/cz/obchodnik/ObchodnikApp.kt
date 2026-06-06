package cz.obchodnik

import android.app.Application
import cz.obchodnik.di.AppContainer
import cz.obchodnik.work.WorkScheduler
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Application entry point with a manual dependency container.
 */
class ObchodnikApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Schedule background refresh based on saved preferences
        MainScope().launch {
            val settings = container.settingsRepository.settings.firstOrNull()
            settings?.let {
                WorkScheduler.scheduleRefresh(this@ObchodnikApp, it.refreshIntervalMinutes)
            }
        }
    }
}
