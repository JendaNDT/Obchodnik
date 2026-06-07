package cz.obchodnik.ui.portfolio

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.obchodnik.core.Result
import cz.obchodnik.data.local.AssetLocalStore
import cz.obchodnik.data.local.QuoteLocalStore
import cz.obchodnik.data.local.dao.HistoryDao
import cz.obchodnik.data.local.dao.HoldingDao
import cz.obchodnik.data.local.dao.PortfolioSnapshotDao
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.local.entity.HistoryEntity
import cz.obchodnik.data.local.entity.HoldingEntity
import cz.obchodnik.data.local.entity.PortfolioSnapshotEntity
import cz.obchodnik.data.local.entity.QuoteEntity
import cz.obchodnik.data.local.toEntity
import cz.obchodnik.data.prefs.AppSettings
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.MarketRepository
import cz.obchodnik.data.repository.PortfolioRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.data.source.MarketDataSource
import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.DataProvider
import cz.obchodnik.domain.model.Holding
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStoreScope: CoroutineScope

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var portfolioRepository: PortfolioRepository
    private lateinit var marketRepository: MarketRepository
    
    private lateinit var fakeHoldingDao: FakeHoldingDao
    private lateinit var fakeSnapshotDao: FakePortfolioSnapshotDao
    private lateinit var fakeAssetStore: FakeAssetStore
    private lateinit var fakeQuoteStore: FakeQuoteStore

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStoreScope = CoroutineScope(testDispatcher + Job())
        val file = File(tmpFolder.newFolder(), "test_settings.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { file }
        )
        settingsRepository = SettingsRepository(dataStore)

        fakeHoldingDao = FakeHoldingDao()
        fakeSnapshotDao = FakePortfolioSnapshotDao()
        fakeAssetStore = FakeAssetStore()
        fakeQuoteStore = FakeQuoteStore()

        portfolioRepository = PortfolioRepository(fakeHoldingDao, fakeSnapshotDao)
        watchlistRepository = WatchlistRepository(fakeAssetStore)
        marketRepository = MarketRepository(
            assetStore = fakeAssetStore,
            quoteStore = fakeQuoteStore,
            historyDao = FakeHistoryDao(),
            marketDataSource = FakeMarketDataSource(),
            settingsStore = settingsRepository,
            json = json
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects empty portfolio`() = testScope.runTest {
        val viewModel = PortfolioViewModel(
            portfolioRepository = portfolioRepository,
            watchlistRepository = watchlistRepository,
            marketRepository = marketRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertEquals(0.0, state.totalValue, 0.0)
        assertEquals(0.0, state.totalInvested, 0.0)
        assertEquals(0.0, state.totalPL, 0.0)
        assertTrue(state.items.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun `calculations are correct when position is added`() = testScope.runTest {
        // Prepare watchlist asset
        val asset = Asset(
            id = "cg:bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            type = AssetType.CRYPTO,
            source = DataProvider.COINGECKO,
            sourceId = "bitcoin",
            colorHex = "#f7931a",
            logoUrl = null
        )
        fakeAssetStore.upsertAsset(asset.toEntity(inWatchlist = true, sortOrder = 0))

        // Set quote for BTC (current price = 60,000 USD)
        val quote = Quote(
            assetId = "cg:bitcoin",
            price = 60000.0,
            change24hPct = 2.5,
            change7dPct = null,
            change30dPct = null,
            high24h = null,
            low24h = null,
            marketCap = null,
            volume24h = null,
            sparkline7d = emptyList(),
            updatedAt = System.currentTimeMillis(),
            currency = "usd"
        )
        fakeQuoteStore.upsertQuotes(listOf(quote.toEntity(json)))

        val viewModel = PortfolioViewModel(
            portfolioRepository = portfolioRepository,
            watchlistRepository = watchlistRepository,
            marketRepository = marketRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        // Add holding: 2.0 BTC @ average price 50,000 USD
        viewModel.addPosition("cg:bitcoin", qty = 2.0, avgPrice = 50000.0)

        // Force emit
        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(1, state.items.size)

        val item = state.items.first()
        assertEquals(2.0, item.holding.qty, 0.0)
        assertEquals(50000.0, item.holding.avgPrice, 0.0)
        assertEquals(120000.0, item.value, 0.0) // 2.0 * 60,000
        assertEquals(100000.0, item.invested, 0.0) // 2.0 * 50,000
        assertEquals(20000.0, item.plValue, 0.0) // 120,000 - 100,000
        assertEquals(20.0, item.plPct ?: 0.0, 0.01) // 20,000 / 100,000 * 100

        // Overall stats
        assertEquals(120000.0, state.totalValue, 0.0)
        assertEquals(100000.0, state.totalInvested, 0.0)
        assertEquals(20000.0, state.totalPL, 0.0)
        assertEquals(20.0, state.totalPLPct ?: 0.0, 0.01)

        collectJob.cancel()
    }

    @Test
    fun `editing position updates existing row and recalculates stats`() = testScope.runTest {
        val asset = Asset(
            id = "cg:bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            type = AssetType.CRYPTO,
            source = DataProvider.COINGECKO,
            sourceId = "bitcoin",
            colorHex = "#f7931a",
            logoUrl = null
        )
        fakeAssetStore.upsertAsset(asset.toEntity(inWatchlist = true, sortOrder = 0))

        val quote = Quote(
            assetId = "cg:bitcoin",
            price = 60_000.0,
            change24hPct = 2.5,
            change7dPct = null,
            change30dPct = null,
            high24h = null,
            low24h = null,
            marketCap = null,
            volume24h = null,
            sparkline7d = emptyList(),
            updatedAt = System.currentTimeMillis(),
            currency = "usd"
        )
        fakeQuoteStore.upsertQuotes(listOf(quote.toEntity(json)))

        val viewModel = PortfolioViewModel(
            portfolioRepository = portfolioRepository,
            watchlistRepository = watchlistRepository,
            marketRepository = marketRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.addPosition("cg:bitcoin", qty = 1.0, avgPrice = 50_000.0)
        val holding = viewModel.uiState.value.items.first().holding

        viewModel.updatePosition(holding, qty = 2.0, avgPrice = 55_000.0)

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        val item = state.items.first()
        assertEquals(holding.id, item.holding.id)
        assertEquals(2.0, item.holding.qty, 0.0)
        assertEquals(55_000.0, item.holding.avgPrice, 0.0)
        assertEquals(120_000.0, item.value, 0.0)
        assertEquals(110_000.0, item.invested, 0.0)
        assertEquals(10_000.0, item.plValue, 0.0)
        assertEquals(9.09, item.plPct ?: 0.0, 0.01)
        assertEquals(120_000.0, state.totalValue, 0.0)
        assertEquals(110_000.0, state.totalInvested, 0.0)
        assertEquals(10_000.0, state.totalPL, 0.0)

        collectJob.cancel()
    }

    @Test
    fun `deleting position updates stats`() = testScope.runTest {
        // Add position
        val asset = Asset(
            id = "cg:bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            type = AssetType.CRYPTO,
            source = DataProvider.COINGECKO,
            sourceId = "bitcoin",
            colorHex = "#f7931a",
            logoUrl = null
        )
        fakeAssetStore.upsertAsset(asset.toEntity(inWatchlist = true, sortOrder = 0))

        val viewModel = PortfolioViewModel(
            portfolioRepository = portfolioRepository,
            watchlistRepository = watchlistRepository,
            marketRepository = marketRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.addPosition("cg:bitcoin", qty = 1.5, avgPrice = 40000.0)
        assertEquals(1, viewModel.uiState.value.items.size)

        val holding = viewModel.uiState.value.items.first().holding
        viewModel.deletePosition(holding)

        assertEquals(0, viewModel.uiState.value.items.size)
        assertEquals(0.0, viewModel.uiState.value.totalValue, 0.0)

        collectJob.cancel()
    }

    private class FakeHoldingDao : HoldingDao {
        private val _holdings = MutableStateFlow<List<HoldingEntity>>(emptyList())
        override fun observeHoldings(): Flow<List<HoldingEntity>> = _holdings

        override suspend fun upsertHolding(holding: HoldingEntity) {
            val current = _holdings.value.toMutableList()
            current.removeAll { it.id == holding.id }
            val nextId = if (holding.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1L else holding.id
            current.add(holding.copy(id = nextId))
            _holdings.value = current
        }

        override suspend fun deleteHolding(holding: HoldingEntity) {
            _holdings.value = _holdings.value.filter { it.id != holding.id }
        }
    }

    private class FakePortfolioSnapshotDao : PortfolioSnapshotDao {
        private val rows = MutableStateFlow<List<PortfolioSnapshotEntity>>(emptyList())
        override fun observeSnapshots(currency: String): Flow<List<PortfolioSnapshotEntity>> =
            rows.map { list -> list.filter { it.currency == currency } }
        override suspend fun upsert(snapshot: PortfolioSnapshotEntity) {
            val current = rows.value.toMutableList()
            current.removeAll { it.dayStartMillis == snapshot.dayStartMillis && it.currency == snapshot.currency }
            current.add(snapshot)
            rows.value = current
        }
    }

    private class FakeAssetStore : AssetLocalStore {
        private val assets = mutableListOf<AssetEntity>()
        override suspend fun upsertAssets(assets: List<AssetEntity>) {
            this.assets.addAll(assets)
        }
        override suspend fun upsertAsset(asset: AssetEntity) {
            assets.removeAll { it.id == asset.id }
            assets.add(asset)
        }
        override suspend fun assetById(id: String): AssetEntity? = assets.find { it.id == id }
        override fun observeWatchlistAssets(): Flow<List<AssetEntity>> = flowOf(assets.filter { it.inWatchlist })
        override suspend fun watchlistAssets(): List<AssetEntity> = assets.filter { it.inWatchlist }
        override suspend fun maxSortOrder(): Int? = assets.maxOfOrNull { it.sortOrder }
        override suspend fun setWatchlistState(id: String, inWatchlist: Boolean, sortOrder: Int) {
            val idx = assets.indexOfFirst { it.id == id }
            if (idx >= 0) {
                assets[idx] = assets[idx].copy(inWatchlist = inWatchlist, sortOrder = sortOrder)
            }
        }
    }

    private class FakeQuoteStore : QuoteLocalStore {
        private val savedQuotes = MutableStateFlow<List<QuoteEntity>>(emptyList())
        override suspend fun upsertQuotes(quotes: List<QuoteEntity>) {
            val current = savedQuotes.value.toMutableList()
            quotes.forEach { q ->
                current.removeAll { it.assetId == q.assetId && it.currency == q.currency }
                current.add(q)
            }
            savedQuotes.value = current
        }
        override fun observeQuotes(currency: String): Flow<List<QuoteEntity>> =
            savedQuotes.map { list -> list.filter { it.currency.lowercase() == currency.lowercase() } }

        override suspend fun quotesForAssets(assetIds: List<String>, currency: String): List<QuoteEntity> =
            savedQuotes.value.filter { it.currency.lowercase() == currency.lowercase() && it.assetId in assetIds }
    }

    private class FakeHistoryDao : HistoryDao {
        override suspend fun upsertHistory(history: HistoryEntity) {}
        override suspend fun history(assetId: String, range: String, currency: String, kind: String): HistoryEntity? = null
    }

    private class FakeMarketDataSource : MarketDataSource {
        override suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>> = Result.Success(emptyList())
        override suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>> = Result.Success(emptyList())
        override suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>> = Result.Success(emptyList())
        override suspend fun search(query: String): Result<List<Asset>> = Result.Success(emptyList())
    }
}
