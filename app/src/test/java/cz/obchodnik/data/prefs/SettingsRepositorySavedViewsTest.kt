package cz.obchodnik.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.obchodnik.ui.markets.MarketCategory
import cz.obchodnik.ui.markets.MarketSortMode
import cz.obchodnik.ui.markets.SavedMarketView
import cz.obchodnik.ui.markets.SavedMarketViewSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class SettingsRepositorySavedViewsTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val file = File(tmpFolder.newFolder(), "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file },
        )
        settingsRepository = SettingsRepository(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `saved views default to empty`() = runTest(testDispatcher) {
        assertEquals("", settingsRepository.settings.first().savedMarketViewsJson)
        assertTrue(SavedMarketViewSerializer.decode("").isEmpty())
    }

    @Test
    fun `saved views persist and round trip through settings flow`() = runTest(testDispatcher) {
        val views = listOf(
            SavedMarketView("1", "Krypto růst", MarketCategory.CRYPTO, MarketSortMode.GAINERS),
            SavedMarketView("2", "BTC", MarketCategory.ALL, MarketSortMode.NAME, query = "btc"),
        )

        settingsRepository.setSavedMarketViewsJson(SavedMarketViewSerializer.encode(views))

        val storedJson = settingsRepository.settings.first().savedMarketViewsJson
        assertEquals(views, SavedMarketViewSerializer.decode(storedJson))
    }

    @Test
    fun `removing a view persists the smaller list`() = runTest(testDispatcher) {
        val views = listOf(
            SavedMarketView("1", "A", MarketCategory.CRYPTO, MarketSortMode.GAINERS),
            SavedMarketView("2", "B", MarketCategory.INDICES, MarketSortMode.MANUAL),
        )
        settingsRepository.setSavedMarketViewsJson(SavedMarketViewSerializer.encode(views))

        val remaining = SavedMarketViewSerializer
            .decode(settingsRepository.settings.first().savedMarketViewsJson)
            .filterNot { it.id == "1" }
        settingsRepository.setSavedMarketViewsJson(SavedMarketViewSerializer.encode(remaining))

        val result = SavedMarketViewSerializer.decode(
            settingsRepository.settings.first().savedMarketViewsJson,
        )
        assertEquals(1, result.size)
        assertEquals("2", result.first().id)
    }
}
