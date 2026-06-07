package cz.obchodnik

import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import cz.obchodnik.ui.detail.DetailScreen
import cz.obchodnik.ui.detail.DetailViewModel
import cz.obchodnik.ui.markets.MarketsScreen
import cz.obchodnik.ui.markets.MarketsViewModel
import cz.obchodnik.ui.onboarding.OnboardingScreen
import cz.obchodnik.ui.onboarding.OnboardingViewModel
import cz.obchodnik.ui.search.SearchScreen
import cz.obchodnik.ui.search.SearchViewModel
import cz.obchodnik.ui.settings.SettingsScreen
import cz.obchodnik.ui.settings.SettingsViewModel
import cz.obchodnik.ui.theme.AccentChoice
import cz.obchodnik.ui.theme.Obchodnik
import cz.obchodnik.ui.theme.ObchodnikTheme
import cz.obchodnik.ui.theme.ThemeChoice

class MainActivity : ComponentActivity() {

    private val pendingAssetId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingAssetId.value = intent?.getStringExtra(EXTRA_ASSET_ID)
        val container = (application as ObchodnikApp).container
        val settingsRepository = container.settingsRepository

        setContent {
            val settingsState by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            val state = settingsState
            if (state == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF07090C))
                )
                return@setContent
            }

            val theme = try {
                ThemeChoice.valueOf(state.theme.uppercase())
            } catch (e: Exception) {
                ThemeChoice.TERMINAL
            }
            val accent = try {
                AccentChoice.valueOf(state.accent.uppercase())
            } catch (e: Exception) {
                AccentChoice.BLUE
            }

            ObchodnikTheme(theme = theme, accent = accent) {
                if (!state.onboardingDone) {
                    val onboardingViewModel: OnboardingViewModel = viewModel(
                        factory = OnboardingViewModel.factory(application as ObchodnikApp)
                    )
                    val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
                    OnboardingScreen(
                        state = onboardingState,
                        onToggleAsset = onboardingViewModel::toggleAsset,
                        onNextStep = onboardingViewModel::nextStep,
                        onPreviousStep = onboardingViewModel::previousStep,
                        onRequestNotificationsPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestPermissions(
                                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                                    101
                                )
                            }
                        },
                        onComplete = {
                            onboardingViewModel.completeOnboarding(state.currency)
                        }
                    )
                } else {
                    ObchodnikAppContent(
                        app = application as ObchodnikApp,
                        pendingAssetId = pendingAssetId.value,
                        onAssetConsumed = { pendingAssetId.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAssetId.value = intent.getStringExtra(EXTRA_ASSET_ID)
    }

    companion object {
        const val EXTRA_ASSET_ID = "cz.obchodnik.extra.ASSET_ID"
    }
}

@Composable
private fun ObchodnikAppContent(
    app: ObchodnikApp,
    pendingAssetId: String?,
    onAssetConsumed: () -> Unit,
) {
    val navController = rememberNavController()
    LaunchedEffect(pendingAssetId) {
        if (pendingAssetId != null) {
            navController.navigate("detail/${Uri.encode(pendingAssetId)}")
            onAssetConsumed()
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = setOf("markets", "portfolio", "fng", "alerts")
    val showBottomBar = currentRoute in topLevelRoutes

    val c = Obchodnik.colors

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = c.surface,
                    tonalElevation = 0.dp
                ) {
                    val items = listOf(
                        Triple("markets", "Trh", Icons.AutoMirrored.Rounded.TrendingUp),
                        Triple("portfolio", "Portfolio", Icons.Rounded.FolderOpen),
                        Triple("fng", "F&G", Icons.Rounded.Speed),
                        Triple("alerts", "Alerty", Icons.Rounded.Notifications),
                    )
                    items.forEach { (route, label, icon) ->
                        val selected = currentRoute == route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("markets") {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) c.accent else c.text3
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    color = if (selected) c.accent else c.text3,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = c.accent.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "markets",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("markets") {
                val viewModel: MarketsViewModel = viewModel(factory = MarketsViewModel.factory(app))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                MarketsScreen(
                    state = state,
                    onCategorySelected = viewModel::selectCategory,
                    onQueryChanged = viewModel::updateQuery,
                    onSortModeSelected = viewModel::selectSortMode,
                    onQuickViewSelected = viewModel::applyQuickView,
                    onApplySavedView = viewModel::applySavedView,
                    onSaveCurrentView = viewModel::saveCurrentView,
                    onDeleteSavedView = viewModel::deleteSavedView,
                    onRefresh = { viewModel.refresh(force = true) },
                    onSearch = { navController.navigate("search") },
                    onOpenAsset = { assetId -> navController.navigate("detail/${Uri.encode(assetId)}") },
                    onMoveAsset = viewModel::moveAsset,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenAlerts = { navController.navigate("alerts") },
                )
            }
            composable("portfolio") {
                val viewModel: cz.obchodnik.ui.portfolio.PortfolioViewModel = viewModel(
                    factory = cz.obchodnik.ui.portfolio.PortfolioViewModel.factory(app)
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                cz.obchodnik.ui.portfolio.PortfolioScreen(
                    state = state,
                    onAddPosition = viewModel::addPosition,
                    onUpdatePosition = viewModel::updatePosition,
                    onDeletePosition = viewModel::deletePosition,
                    onExportCsv = { uri -> viewModel.exportCsv(uri, state) },
                )
            }
            composable("fng") {
                val viewModel: cz.obchodnik.ui.fng.FngViewModel = viewModel(
                    factory = cz.obchodnik.ui.fng.FngViewModel.factory(app)
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                cz.obchodnik.ui.fng.FngScreen(
                    state = state
                )
            }
            composable("alerts") {
                val viewModel: cz.obchodnik.ui.alerts.AlertsViewModel = viewModel(
                    factory = cz.obchodnik.ui.alerts.AlertsViewModel.factory(app)
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                cz.obchodnik.ui.alerts.AlertsScreen(
                    state = state,
                    onAddAlert = viewModel::addAlert,
                    onToggleAlert = viewModel::toggleAlertEnabled,
                    onReactivateAlert = viewModel::reactivateAlert,
                    onDeleteAlert = viewModel::deleteAlert,
                )
            }
            composable("search") {
                val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.factory(app))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SearchScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onToggleWatch = viewModel::toggleWatch,
                    onOpenAsset = { asset ->
                        viewModel.openAsset(asset) { assetId ->
                            navController.navigate("detail/${Uri.encode(assetId)}")
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "detail/{assetId}",
                arguments = listOf(navArgument("assetId") { type = NavType.StringType }),
            ) { entry ->
                val assetId = Uri.decode(entry.arguments?.getString("assetId").orEmpty())
                val viewModel: DetailViewModel = viewModel(factory = DetailViewModel.factory(app, assetId))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onToggleWatch = viewModel::toggleWatch,
                    onChartMode = viewModel::setChartMode,
                    onRange = viewModel::setRange,
                    onToggleSma7 = viewModel::toggleSma7,
                    onToggleSma30 = viewModel::toggleSma30,
                    onAddAlert = viewModel::addAlert,
                )
            }
            composable("settings") {
                val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onThemeChanged = viewModel::setTheme,
                    onAccentChanged = viewModel::setAccent,
                    onCurrencyChanged = viewModel::setCurrency,
                    onRefreshIntervalChanged = viewModel::setRefreshIntervalMinutes,
                    onDefaultChartChanged = viewModel::setDefaultChart,
                    onDensityChanged = viewModel::setDensity,
                    onShowFngOnWidgetChanged = viewModel::setShowFngOnWidget,
                    onCoinGeckoKeyChanged = viewModel::setCoinGeckoKey,
                    onAlphaVantageKeyChanged = viewModel::setAlphaVantageKey,
                    onNotificationsEnabledChanged = viewModel::setNotificationsEnabled,
                    onResetOnboarding = viewModel::resetOnboarding,
                    onExportData = viewModel::exportData,
                    onPreviewImportData = viewModel::previewImportData,
                    onConfirmImportData = viewModel::confirmImportData,
                    onDismissImportPreview = viewModel::dismissImportPreview,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
