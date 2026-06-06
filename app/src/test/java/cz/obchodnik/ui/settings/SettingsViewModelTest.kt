package cz.obchodnik.ui.settings

import cz.obchodnik.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var dataStoreScope: CoroutineScope

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SettingsViewModel

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
        viewModel = SettingsViewModel(null, settingsRepository)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects defaults`() = testScope.runTest {
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.uiState.collect {}
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("terminal", state.settings.theme)
        assertEquals("blue", state.settings.accent)
        assertEquals("usd", state.settings.currency)
        assertEquals(30, state.settings.refreshIntervalMinutes)

        collectJob.cancel()
    }

    @Test
    fun `setTheme updates state and repository`() = testScope.runTest {
        viewModel.setTheme("aurora")
        
        val saved = settingsRepository.settings.first()
        assertEquals("aurora", saved.theme)
    }

    @Test
    fun `setAccent updates state and repository`() = testScope.runTest {
        viewModel.setAccent("green")
        
        val saved = settingsRepository.settings.first()
        assertEquals("green", saved.accent)
    }

    @Test
    fun `setCurrency updates state and repository`() = testScope.runTest {
        viewModel.setCurrency("czk")
        
        val saved = settingsRepository.settings.first()
        assertEquals("czk", saved.currency)
    }

    @Test
    fun `setRefreshIntervalMinutes coerces positive values and keeps 0`() = testScope.runTest {
        // Test normal positive coercion
        viewModel.setRefreshIntervalMinutes(5)
        var saved = settingsRepository.settings.first()
        assertEquals(15, saved.refreshIntervalMinutes)

        // Test 0 retention
        viewModel.setRefreshIntervalMinutes(0)
        saved = settingsRepository.settings.first()
        assertEquals(0, saved.refreshIntervalMinutes)

        // Test negative coercion
        viewModel.setRefreshIntervalMinutes(-5)
        saved = settingsRepository.settings.first()
        assertEquals(0, saved.refreshIntervalMinutes)
    }
}
