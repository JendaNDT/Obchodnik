package cz.obchodnik.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cz.obchodnik.core.crypto.KeystoreCrypto
import cz.obchodnik.core.crypto.NoOpStringCrypto
import cz.obchodnik.core.crypto.StringCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.obchodnikSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "obchodnik_settings",
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val crypto: StringCrypto = NoOpStringCrypto,
) : SettingsStore {
    constructor(context: Context) : this(context.obchodnikSettingsDataStore, KeystoreCrypto())

    override val settings: Flow<AppSettings> =
        dataStore.data.map { prefs ->
            AppSettings(
                theme = prefs[Keys.theme] ?: "terminal",
                accent = prefs[Keys.accent] ?: "blue",
                currency = prefs[Keys.currency] ?: "usd",
                refreshIntervalMinutes = prefs[Keys.refreshIntervalMinutes] ?: 30,
                defaultChart = prefs[Keys.defaultChart] ?: "line",
                density = prefs[Keys.density] ?: "normal",
                showFngOnWidget = prefs[Keys.showFngOnWidget] ?: true,
                coingeckoKey = crypto.decrypt(prefs[Keys.coingeckoKey] ?: ""),
                alphaVantageKey = crypto.decrypt(prefs[Keys.alphaVantageKey] ?: ""),
                onboardingDone = prefs[Keys.onboardingDone] ?: false,
                notificationsEnabled = prefs[Keys.notificationsEnabled] ?: false,
                avDailyCount = prefs[Keys.avDailyCount] ?: 0,
                avCountDate = prefs[Keys.avCountDate] ?: "",
                usdCzkRate = prefs[Keys.usdCzkRate] ?: 23.0,
                usdCzkRateLastUpdated = prefs[Keys.usdCzkRateLastUpdated] ?: 0L,
            )
        }

    suspend fun setTheme(theme: String) = update(Keys.theme, theme)
    suspend fun setAccent(accent: String) = update(Keys.accent, accent)
    suspend fun setCurrency(currency: String) = update(Keys.currency, currency.lowercase())
    suspend fun setDefaultChart(defaultChart: String) = update(Keys.defaultChart, defaultChart)
    suspend fun setDensity(density: String) = update(Keys.density, density)
    suspend fun setShowFngOnWidget(show: Boolean) = update(Keys.showFngOnWidget, show)
    suspend fun setCoinGeckoKey(key: String) = update(Keys.coingeckoKey, crypto.encrypt(key.trim()))
    suspend fun setAlphaVantageKey(key: String) = update(Keys.alphaVantageKey, crypto.encrypt(key.trim()))
    suspend fun setOnboardingDone(done: Boolean) = update(Keys.onboardingDone, done)
    suspend fun setNotificationsEnabled(enabled: Boolean) = update(Keys.notificationsEnabled, enabled)
    suspend fun setAlphaVantageDailyCount(count: Int, date: String) {
        dataStore.edit { prefs ->
            prefs[Keys.avDailyCount] = count
            prefs[Keys.avCountDate] = date
        }
    }

    suspend fun setUsdCzkRate(rate: Double, lastUpdated: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.usdCzkRate] = rate
            prefs[Keys.usdCzkRateLastUpdated] = lastUpdated
        }
    }

    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        update(Keys.refreshIntervalMinutes, if (minutes <= 0) 0 else minutes.coerceAtLeast(15))
    }

    private suspend fun update(key: Preferences.Key<String>, value: String) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    private suspend fun update(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    private suspend fun update(key: Preferences.Key<Int>, value: Int) {
        dataStore.edit { prefs -> prefs[key] = value }
    }

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val accent = stringPreferencesKey("accent")
        val currency = stringPreferencesKey("currency")
        val refreshIntervalMinutes = intPreferencesKey("refresh_interval_minutes")
        val defaultChart = stringPreferencesKey("default_chart")
        val density = stringPreferencesKey("density")
        val showFngOnWidget = booleanPreferencesKey("show_fng_on_widget")
        val coingeckoKey = stringPreferencesKey("coingecko_key")
        val alphaVantageKey = stringPreferencesKey("alpha_vantage_key")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val avDailyCount = intPreferencesKey("av_daily_count")
        val avCountDate = stringPreferencesKey("av_count_date")
        val usdCzkRate = doublePreferencesKey("usd_czk_rate")
        val usdCzkRateLastUpdated = longPreferencesKey("usd_czk_rate_last_updated")
    }
}
