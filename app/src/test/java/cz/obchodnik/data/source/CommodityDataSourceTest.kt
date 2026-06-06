package cz.obchodnik.data.source

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.obchodnik.core.Result
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.remote.alphavantage.AlphaVantageApi
import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageBarDto
import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageCommodityDto
import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageMetaDto
import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageSeriesDto
import cz.obchodnik.data.remote.alphavantage.dto.CommodityDataPointDto
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CommodityDataSourceTest {

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
    fun `quotes returns error when api key is blank`() = runTest(testDispatcher) {
        settingsRepository.setAlphaVantageKey("")
        val rateProvider = CurrencyRateProvider(DummyCoinGeckoApi(), settingsRepository)
        val dataSource = CommodityDataSource(
            api = FakeAlphaVantageApi(),
            settingsRepository = settingsRepository,
            currencyRateProvider = rateProvider
        )

        val result = dataSource.quotes(listOf(wtiAsset()), "usd")

        assertTrue(result is Result.Error)
        assertEquals("Zadejte prosím API klíč pro Alpha Vantage v Nastavení.", (result as Result.Error).message)
    }

    @Test
    fun `quotes returns error when daily budget is exceeded`() = runTest(testDispatcher) {
        settingsRepository.setAlphaVantageKey("valid_key")
        settingsRepository.setAlphaVantageDailyCount(25, LocalDate.now().toString())
        val rateProvider = CurrencyRateProvider(DummyCoinGeckoApi(), settingsRepository)
        val dataSource = CommodityDataSource(
            api = FakeAlphaVantageApi(),
            settingsRepository = settingsRepository,
            currencyRateProvider = rateProvider
        )

        val result = dataSource.quotes(listOf(wtiAsset()), "usd")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("Překročen denní limit"))
    }

    @Test
    fun `quotes fetches and maps successfully and increments limit counter`() = runTest(testDispatcher) {
        settingsRepository.setAlphaVantageKey("valid_key")
        val todayStr = LocalDate.now().toString()
        settingsRepository.setAlphaVantageDailyCount(5, todayStr)

        // Cache the rate as fresh so CurrencyRateProvider doesn't hit the CoinGecko API
        settingsRepository.setUsdCzkRate(22.0, System.currentTimeMillis())

        val commodityDto = AlphaVantageCommodityDto(
            name = "Crude Oil Prices WTI",
            data = listOf(
                CommodityDataPointDto("2026-06-05", "72.34"),
                CommodityDataPointDto("2026-06-04", "71.20")
            )
        )

        val fakeApi = FakeAlphaVantageApi(commodityResponse = commodityDto)
        val rateProvider = CurrencyRateProvider(DummyCoinGeckoApi(), settingsRepository)
        val dataSource = CommodityDataSource(
            api = fakeApi,
            settingsRepository = settingsRepository,
            currencyRateProvider = rateProvider
        )

        val result = dataSource.quotes(listOf(wtiAsset()), "czk")

        assertTrue(result is Result.Success)
        val quotes = (result as Result.Success).data
        assertEquals(1, quotes.size)
        val quote = quotes[0]
        assertEquals("av:c:WTI", quote.assetId)
        assertEquals(72.34 * 22.0, quote.price, 0.0) // scaled by exchange rate
        assertEquals(1.6011, quote.change24hPct ?: 0.0, 0.0001)

        assertEquals(1, fakeApi.commodityCalls)

        // Verify budget counter incremented in DataStore
        val updatedSettings = settingsRepository.settings.first()
        assertEquals(todayStr, updatedSettings.avCountDate)
        assertEquals(6, updatedSettings.avDailyCount)
    }

    private fun wtiAsset() = Asset(
        id = "av:c:WTI",
        symbol = "WTI",
        name = "Ropa WTI",
        type = AssetType.COMMODITY,
        source = DataProvider.ALPHAVANTAGE,
        sourceId = "WTI",
        colorHex = "#6b9c4e",
        logoUrl = null
    )

    private class DummyCoinGeckoApi : cz.obchodnik.data.remote.coingecko.CoinGeckoApi {
        override suspend fun simplePrice(ids: String, vsCurrencies: String) = throw AssertionError()
        override suspend fun search(query: String) = throw AssertionError()
        override suspend fun markets(a: String, b: String, c: String, d: Int, e: Int, f: Boolean, g: String) = throw AssertionError()
        override suspend fun marketChart(a: String, b: String, c: String) = throw AssertionError()
        override suspend fun ohlc(a: String, b: String, c: String) = throw AssertionError()
    }

    private class FakeAlphaVantageApi(
        private val commodityResponse: AlphaVantageCommodityDto = AlphaVantageCommodityDto(),
        private val seriesResponse: AlphaVantageSeriesDto = AlphaVantageSeriesDto()
    ) : AlphaVantageApi {
        var commodityCalls = 0
        var seriesCalls = 0

        override suspend fun getCommodity(function: String, interval: String, apiKey: String): AlphaVantageCommodityDto {
            commodityCalls++
            return commodityResponse
        }

        override suspend fun getStockSeries(function: String, symbol: String, outputSize: String, apiKey: String): AlphaVantageSeriesDto {
            seriesCalls++
            return seriesResponse
        }
    }
}
