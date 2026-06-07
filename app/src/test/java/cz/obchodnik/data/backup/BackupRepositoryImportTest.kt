package cz.obchodnik.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.obchodnik.data.local.AssetLocalStore
import cz.obchodnik.data.local.dao.AlertDao
import cz.obchodnik.data.local.dao.HoldingDao
import cz.obchodnik.data.local.dao.PortfolioSnapshotDao
import cz.obchodnik.data.local.entity.AlertEntity
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.HoldingEntity
import cz.obchodnik.data.local.entity.PortfolioSnapshotEntity
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.AlertRepository
import cz.obchodnik.data.repository.PortfolioRepository
import cz.obchodnik.data.repository.WatchlistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryImportTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var assetStore: FakeAssetStore
    private lateinit var holdingDao: FakeHoldingDao
    private lateinit var alertDao: FakeAlertDao
    private lateinit var repository: BackupRepository

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val file = File(tmpFolder.newFolder(), "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file },
        )
        settingsRepository = SettingsRepository(dataStore)
        assetStore = FakeAssetStore()
        holdingDao = FakeHoldingDao()
        alertDao = FakeAlertDao()
        repository = BackupRepository(
            watchlistRepository = WatchlistRepository(assetStore),
            portfolioRepository = PortfolioRepository(holdingDao, FakePortfolioSnapshotDao()),
            alertRepository = AlertRepository(alertDao),
            settingsRepository = settingsRepository,
            json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            },
            appVersion = "test",
            now = { 1_700_000_000_000L },
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun `import stores valid rows and applies settings`() = runTest(testDispatcher) {
        val backup = BackupData(
            watchlist = listOf(
                BackupAsset("cg:bitcoin", "BTC", "Bitcoin", "CRYPTO", "COINGECKO", "bitcoin"),
                BackupAsset("broken", "BAD", "Broken", "UNKNOWN", "COINGECKO", "broken"),
            ),
            holdings = listOf(BackupHolding("cg:bitcoin", qty = 0.25, avgPrice = 50_000.0)),
            alerts = listOf(
                BackupAlert(
                    assetId = "cg:bitcoin",
                    above = true,
                    target = 70_000.0,
                    enabled = false,
                    triggeredAt = 1_700_000_000_000L,
                    triggeredPrice = 71_000.0,
                    triggeredCurrency = "usd",
                ),
            ),
            settings = BackupSettings(
                theme = "aurora",
                accent = "green",
                currency = "czk",
                refreshIntervalMinutes = 5,
                defaultChart = "candle",
                density = "compact",
                showFngOnWidget = false,
                coingeckoKey = "cg_secret",
                alphaVantageKey = "av_secret",
                geminiApiKey = "gemini_secret",
                notificationsEnabled = true,
            ),
        )

        val summary = repository.import(backup)

        assertEquals(1, summary.assets)
        assertEquals(1, summary.holdings)
        assertEquals(1, summary.alerts)

        val watchlist = assetStore.watchlistAssets()
        assertEquals(listOf("cg:bitcoin"), watchlist.map { it.id })
        assertEquals(listOf(0), watchlist.map { it.sortOrder })
        assertFalse(assetStore.assets.any { it.id == "broken" })

        val holding = holdingDao.holdings.first()
        assertEquals("cg:bitcoin", holding.assetId)
        assertEquals(0.25, holding.qty, 0.0)
        assertEquals(50_000.0, holding.avgPrice, 0.0)

        val alert = alertDao.alerts.first()
        assertEquals("cg:bitcoin", alert.assetId)
        assertTrue(alert.above)
        assertEquals(70_000.0, alert.target, 0.0)
        assertFalse(alert.enabled)
        assertEquals(1_700_000_000_000L, alert.triggeredAt)
        assertEquals(71_000.0, alert.triggeredPrice ?: 0.0, 0.0)
        assertEquals("usd", alert.triggeredCurrency)

        val settings = settingsRepository.settings.first()
        assertEquals("aurora", settings.theme)
        assertEquals("green", settings.accent)
        assertEquals("czk", settings.currency)
        assertEquals(15, settings.refreshIntervalMinutes)
        assertEquals("candle", settings.defaultChart)
        assertEquals("compact", settings.density)
        assertFalse(settings.showFngOnWidget)
        assertEquals("cg_secret", settings.coingeckoKey)
        assertEquals("av_secret", settings.alphaVantageKey)
        assertEquals("gemini_secret", settings.geminiApiKey)
        assertTrue(settings.notificationsEnabled)
    }

    @Test
    fun `import without api keys keeps existing stored keys`() = runTest(testDispatcher) {
        settingsRepository.setCoinGeckoKey("existing_cg")
        settingsRepository.setAlphaVantageKey("existing_av")
        settingsRepository.setGeminiApiKey("existing_gemini")

        repository.import(
            BackupData(
                settings = BackupSettings(
                    theme = "mono",
                    coingeckoKey = null,
                    alphaVantageKey = null,
                    geminiApiKey = null,
                ),
            ),
        )

        val settings = settingsRepository.settings.first()
        assertEquals("mono", settings.theme)
        assertEquals("existing_cg", settings.coingeckoKey)
        assertEquals("existing_av", settings.alphaVantageKey)
        assertEquals("existing_gemini", settings.geminiApiKey)
    }

    private class FakeAssetStore : AssetLocalStore {
        val assets = mutableListOf<AssetEntity>()

        override suspend fun upsertAssets(assets: List<AssetEntity>) {
            assets.forEach { upsertAsset(it) }
        }

        override suspend fun upsertAsset(asset: AssetEntity) {
            assets.removeAll { it.id == asset.id }
            assets += asset
        }

        override suspend fun assetById(id: String): AssetEntity? =
            assets.firstOrNull { it.id == id }

        override fun observeWatchlistAssets(): Flow<List<AssetEntity>> =
            MutableStateFlow(sortedWatchlist())

        override suspend fun watchlistAssets(): List<AssetEntity> = sortedWatchlist()

        override suspend fun maxSortOrder(): Int? =
            assets.filter { it.inWatchlist }.maxOfOrNull { it.sortOrder }

        override suspend fun setWatchlistState(id: String, inWatchlist: Boolean, sortOrder: Int) {
            val index = assets.indexOfFirst { it.id == id }
            if (index >= 0) {
                assets[index] = assets[index].copy(inWatchlist = inWatchlist, sortOrder = sortOrder)
            }
        }

        private fun sortedWatchlist(): List<AssetEntity> =
            assets.filter { it.inWatchlist }.sortedWith(compareBy<AssetEntity> { it.sortOrder }.thenBy { it.symbol })
    }

    private class FakePortfolioSnapshotDao : PortfolioSnapshotDao {
        override fun observeSnapshots(currency: String): Flow<List<PortfolioSnapshotEntity>> =
            MutableStateFlow(emptyList())
        override suspend fun upsert(snapshot: PortfolioSnapshotEntity) {}
        override suspend fun getSnapshotForDay(dayStart: Long, currency: String): PortfolioSnapshotEntity? = null
    }

    private class FakeHoldingDao : HoldingDao {
        private val state = MutableStateFlow<List<HoldingEntity>>(emptyList())
        val holdings: List<HoldingEntity>
            get() = state.value

        override fun observeHoldings(): Flow<List<HoldingEntity>> = state

        override suspend fun upsertHolding(holding: HoldingEntity) {
            val current = state.value.toMutableList()
            current.removeAll { it.id == holding.id }
            val nextId = if (holding.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1L else holding.id
            current += holding.copy(id = nextId)
            state.value = current
        }

        override suspend fun deleteHolding(holding: HoldingEntity) {
            state.value = state.value.filter { it.id != holding.id }
        }
    }

    private class FakeAlertDao : AlertDao {
        private val state = MutableStateFlow<List<AlertEntity>>(emptyList())
        val alerts: List<AlertEntity>
            get() = state.value

        override fun observeAlerts(): Flow<List<AlertEntity>> = state

        override suspend fun enabledAlerts(): List<AlertEntity> =
            state.value.filter { it.enabled }

        override suspend fun upsertAlert(alert: AlertEntity) {
            val current = state.value.toMutableList()
            current.removeAll { it.id == alert.id }
            val nextId = if (alert.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1L else alert.id
            current += alert.copy(id = nextId)
            state.value = current
        }

        override suspend fun deleteAlert(alert: AlertEntity) {
            state.value = state.value.filter { it.id != alert.id }
        }
    }
}
