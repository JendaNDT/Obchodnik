package cz.obchodnik.ui.alerts

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import cz.obchodnik.data.local.AssetLocalStore
import cz.obchodnik.data.local.dao.AlertDao
import cz.obchodnik.data.local.entity.AlertEntity
import cz.obchodnik.data.local.entity.AssetEntity
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.repository.AlertRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.ui.onboarding.OnboardingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class AlertsViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStoreScope: CoroutineScope

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var watchlistRepository: WatchlistRepository
    private lateinit var alertRepository: AlertRepository
    
    private lateinit var fakeAlertDao: FakeAlertDao
    private lateinit var fakeAssetStore: FakeAssetStore

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

        fakeAlertDao = FakeAlertDao()
        fakeAssetStore = FakeAssetStore()

        alertRepository = AlertRepository(fakeAlertDao)
        watchlistRepository = WatchlistRepository(fakeAssetStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects empty alerts`() = testScope.runTest {
        val viewModel = AlertsViewModel(
            alertRepository = alertRepository,
            watchlistRepository = watchlistRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertTrue(state.items.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun `add alert adds it to repository`() = testScope.runTest {
        val viewModel = AlertsViewModel(
            alertRepository = alertRepository,
            watchlistRepository = watchlistRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.addAlert("cg:bitcoin", above = true, target = 65000.0)

        val state = viewModel.uiState.value
        assertEquals(1, state.items.size)
        val alert = state.items.first().alert
        assertEquals("cg:bitcoin", alert.assetId)
        assertTrue(alert.above)
        assertEquals(65000.0, alert.target, 0.0)
        assertTrue(alert.enabled)

        collectJob.cancel()
    }

    @Test
    fun `toggle alert updates state`() = testScope.runTest {
        val viewModel = AlertsViewModel(
            alertRepository = alertRepository,
            watchlistRepository = watchlistRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.addAlert("cg:bitcoin", above = true, target = 65000.0)
        assertEquals(1, viewModel.uiState.value.items.size)

        val alert = viewModel.uiState.value.items.first().alert
        viewModel.toggleAlertEnabled(alert, false)

        val updatedAlert = viewModel.uiState.value.items.first().alert
        assertFalse(updatedAlert.enabled)

        collectJob.cancel()
    }

    @Test
    fun `reactivate alert clears trigger metadata`() = testScope.runTest {
        val viewModel = AlertsViewModel(
            alertRepository = alertRepository,
            watchlistRepository = watchlistRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        alertRepository.save(
            cz.obchodnik.domain.model.PriceAlert(
                id = 0,
                assetId = "cg:bitcoin",
                above = true,
                target = 65_000.0,
                enabled = false,
                triggeredAt = 1_700_000_000_000L,
                triggeredPrice = 66_000.0,
                triggeredCurrency = "usd",
            )
        )

        val alert = viewModel.uiState.value.items.first().alert
        viewModel.reactivateAlert(alert)

        val updatedAlert = viewModel.uiState.value.items.first().alert
        assertTrue(updatedAlert.enabled)
        assertEquals(null, updatedAlert.triggeredAt)
        assertEquals(null, updatedAlert.triggeredPrice)
        assertEquals(null, updatedAlert.triggeredCurrency)

        collectJob.cancel()
    }

    @Test
    fun `delete alert removes it`() = testScope.runTest {
        val viewModel = AlertsViewModel(
            alertRepository = alertRepository,
            watchlistRepository = watchlistRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.addAlert("cg:bitcoin", above = true, target = 65000.0)
        assertEquals(1, viewModel.uiState.value.items.size)

        val alert = viewModel.uiState.value.items.first().alert
        viewModel.deleteAlert(alert)

        assertEquals(0, viewModel.uiState.value.items.size)

        collectJob.cancel()
    }

    @Test
    fun `add repeating alert stores repeating and armed`() = testScope.runTest {
        val viewModel = AlertsViewModel(
            alertRepository = alertRepository,
            watchlistRepository = watchlistRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.addAlert("cg:bitcoin", above = true, target = 65000.0, repeating = true)

        val alert = viewModel.uiState.value.items.first().alert
        assertTrue(alert.repeating)
        assertTrue(alert.armed)

        collectJob.cancel()
    }

    @Test
    fun `setRepeating toggles the repeating flag`() = testScope.runTest {
        val viewModel = AlertsViewModel(
            alertRepository = alertRepository,
            watchlistRepository = watchlistRepository,
            settingsRepository = settingsRepository
        )

        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        viewModel.addAlert("cg:bitcoin", above = true, target = 65000.0)
        val alert = viewModel.uiState.value.items.first().alert
        assertFalse(alert.repeating)

        viewModel.setRepeating(alert, true)
        assertTrue(viewModel.uiState.value.items.first().alert.repeating)

        collectJob.cancel()
    }

    private class FakeAlertDao : AlertDao {
        private val _alerts = MutableStateFlow<List<AlertEntity>>(emptyList())

        override fun observeAlerts(): Flow<List<AlertEntity>> = _alerts

        override suspend fun enabledAlerts(): List<AlertEntity> =
            _alerts.value.filter { it.enabled }

        override suspend fun upsertAlert(alert: AlertEntity) {
            val current = _alerts.value.toMutableList()
            current.removeAll { it.id == alert.id }
            val nextId = if (alert.id == 0L) (current.maxOfOrNull { it.id } ?: 0L) + 1L else alert.id
            current.add(alert.copy(id = nextId))
            _alerts.value = current
        }

        override suspend fun deleteAlert(alert: AlertEntity) {
            _alerts.value = _alerts.value.filter { it.id != alert.id }
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
}
