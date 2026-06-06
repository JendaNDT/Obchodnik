package cz.obchodnik.data.source

import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.remote.coingecko.CoinGeckoApi
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CurrencyRateProvider(
    private val coinGeckoApi: CoinGeckoApi,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun getUsdCzkRate(): Double {
        val settings = settingsRepository.settings.first()
        val now = System.currentTimeMillis()
        val cacheAge = now - settings.usdCzkRateLastUpdated
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        if (cacheAge >= oneDayMillis || settings.usdCzkRateLastUpdated == 0L) {
            runCatching {
                val response = coinGeckoApi.simplePrice(ids = "tether", vsCurrencies = "czk")
                response["tether"]?.get("czk")
            }.fold(
                onSuccess = { rate ->
                    if (rate != null && rate > 0.0) {
                        settingsRepository.setUsdCzkRate(rate, now)
                        return rate
                    }
                },
                onFailure = {
                    // Silent fallback to cached rate
                }
            )
        }

        return settings.usdCzkRate
    }
}
