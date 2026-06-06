package cz.obchodnik.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import cz.obchodnik.core.CoroutineDispatchers
import cz.obchodnik.data.local.ObchodnikDatabase
import cz.obchodnik.data.prefs.SettingsRepository
import cz.obchodnik.data.remote.coingecko.CoinGeckoApi
import cz.obchodnik.data.remote.alphavantage.AlphaVantageApi
import cz.obchodnik.data.remote.fng.FngApi
import cz.obchodnik.data.repository.AlertRepository
import cz.obchodnik.data.repository.FngRepository
import cz.obchodnik.data.repository.MarketRepository
import cz.obchodnik.data.repository.PortfolioRepository
import cz.obchodnik.data.repository.WatchlistRepository
import cz.obchodnik.data.source.CommodityDataSource
import cz.obchodnik.data.source.CryptoDataSource
import cz.obchodnik.data.source.CurrencyRateProvider
import cz.obchodnik.data.source.DataSourceRouter
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainer(
    context: Context,
) {
    val dispatchers = CoroutineDispatchers()

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    val settingsRepository = SettingsRepository(context)

    val database: ObchodnikDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            ObchodnikDatabase::class.java,
            "obchodnik.db",
        )
            .addMigrations(*ObchodnikDatabase.MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    private val okHttpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .build()

    private val coinGeckoRetrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val coinGeckoApi: CoinGeckoApi =
        coinGeckoRetrofit.create(CoinGeckoApi::class.java)

    val cryptoDataSource = CryptoDataSource(coinGeckoApi)

    private val alphaVantageRetrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.alphavantage.co/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val alphaVantageApi: AlphaVantageApi =
        alphaVantageRetrofit.create(AlphaVantageApi::class.java)

    val currencyRateProvider = CurrencyRateProvider(coinGeckoApi, settingsRepository)

    val commodityDataSource = CommodityDataSource(alphaVantageApi, settingsRepository, currencyRateProvider)

    val dataSourceRouter = DataSourceRouter(
        cryptoDataSource = cryptoDataSource,
        commodityDataSource = commodityDataSource
    )

    val watchlistRepository = WatchlistRepository(database.assetDao())
    val marketRepository = MarketRepository(
        assetStore = database.assetDao(),
        quoteStore = database.quoteDao(),
        historyDao = database.historyDao(),
        marketDataSource = dataSourceRouter,
        settingsStore = settingsRepository,
        json = json,
    )
    val portfolioRepository = PortfolioRepository(database.holdingDao())
    val alertRepository = AlertRepository(database.alertDao())

    private val fngRetrofit: Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.alternative.me/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    val fngApi: FngApi =
        fngRetrofit.create(FngApi::class.java)

    val fngRepository = FngRepository(fngApi)
}
