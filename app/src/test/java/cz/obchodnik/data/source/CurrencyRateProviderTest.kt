package cz.obchodnik.data.source

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.remote.coingecko.CoinGeckoApi
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketChartDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoMarketCoinDto
import cz.obchodnik.data.remote.coingecko.dto.CoinGeckoSearchResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyRateProviderTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val file = File(tmpFolder.newFolder(), "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file }
        )
        settingsRepository = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `when cache is fresh do not fetch from api`() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        settingsRepository.setUsdCzkRate(25.5, now)

        val fakeApi = FakeCoinGeckoApi(rate = 22.0)
        val provider = CurrencyRateProvider(fakeApi, settingsRepository)

        val rate = provider.getUsdCzkRate()

        assertEquals(25.5, rate, 0.0)
        assertEquals(0, fakeApi.simplePriceCalls)
    }

    @Test
    fun `when cache is stale or missing fetch from api and update cache`() = runTest(testDispatcher) {
        val fakeApi = FakeCoinGeckoApi(rate = 24.2)
        val provider = CurrencyRateProvider(fakeApi, settingsRepository)

        val rate = provider.getUsdCzkRate()

        assertEquals(24.2, rate, 0.0)
        assertEquals(1, fakeApi.simplePriceCalls)

        // Verify DataStore cache was updated
        val cached = settingsRepository.settings.first()
        assertEquals(24.2, cached.usdCzkRate, 0.0)
        assertTrue(cached.usdCzkRateLastUpdated > 0L)
    }

    @Test
    fun `when api fails return cached rate or default`() = runTest(testDispatcher) {
        // 1. Missing cache, api fails -> should return default fallback 23.0
        val failingApi = FakeCoinGeckoApi(rate = 24.2, shouldFail = true)
        val provider = CurrencyRateProvider(failingApi, settingsRepository)

        val defaultRate = provider.getUsdCzkRate()
        assertEquals(23.0, defaultRate, 0.0)

        // 2. Cached rate present, api fails -> should return cached rate
        settingsRepository.setUsdCzkRate(24.5, System.currentTimeMillis() - 10 * 24 * 60 * 60_000L) // very old cache
        val oldCachedRate = provider.getUsdCzkRate()
        assertEquals(24.5, oldCachedRate, 0.0)
    }

    private class FakeCoinGeckoApi(
        private val rate: Double,
        private val shouldFail: Boolean = false
    ) : CoinGeckoApi {
        var simplePriceCalls = 0

        override suspend fun simplePrice(ids: String, vsCurrencies: String): Map<String, Map<String, Double>> {
            simplePriceCalls++
            if (shouldFail) {
                throw RuntimeException("API error")
            }
            return mapOf("tether" to mapOf("czk" to rate))
        }

        override suspend fun search(query: String): CoinGeckoSearchResponseDto = fail()
        override suspend fun markets(
            vsCurrency: String,
            ids: String,
            order: String,
            perPage: Int,
            page: Int,
            sparkline: Boolean,
            priceChangePercentage: String
        ): List<CoinGeckoMarketCoinDto> = fail()

        override suspend fun marketChart(id: String, vsCurrency: String, days: String): CoinGeckoMarketChartDto = fail()
        override suspend fun ohlc(id: String, vsCurrency: String, days: String): List<List<Double>> = fail()

        private fun fail(): Nothing = throw AssertionError("Should not be called")
    }
}
