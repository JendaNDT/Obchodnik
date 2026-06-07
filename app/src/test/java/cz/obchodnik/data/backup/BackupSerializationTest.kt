package cz.obchodnik.data.backup

import cz.obchodnik.domain.AssetType
import cz.obchodnik.data.prefs.AppSettings
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun `backup data round-trips through json`() {
        val original = BackupData(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = 1_700_000_000_000L,
            appVersion = "0.1.0",
            watchlist = listOf(
                BackupAsset("cg:bitcoin", "BTC", "Bitcoin", "CRYPTO", "COINGECKO", "bitcoin", "#f7931a", null),
                BackupAsset("av:c:WTI", "WTI", "Ropa WTI", "COMMODITY", "ALPHAVANTAGE", "WTI", null, null),
            ),
            holdings = listOf(BackupHolding("cg:bitcoin", 0.5, 50_000.0)),
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
            settings = BackupSettings(theme = "aurora", accent = "green", currency = "czk"),
        )

        val encoded = json.encodeToString(BackupData.serializer(), original)
        val decoded = json.decodeFromString(BackupData.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `settings backup omits api keys by default`() {
        val settings = AppSettings(
            coingeckoKey = "cg_secret",
            alphaVantageKey = "av_secret",
        )

        val encoded = json.encodeToString(BackupSettings.serializer(), settings.toBackup())

        assertNull(settings.toBackup().coingeckoKey)
        assertNull(settings.toBackup().alphaVantageKey)
        assertTrue(!encoded.contains("cg_secret"))
        assertTrue(!encoded.contains("av_secret"))
    }

    @Test
    fun `settings backup can include api keys explicitly`() {
        val settings = AppSettings(
            coingeckoKey = "cg_secret",
            alphaVantageKey = "av_secret",
        )

        val backup = settings.toBackup(includeApiKeys = true)

        assertEquals("cg_secret", backup.coingeckoKey)
        assertEquals("av_secret", backup.alphaVantageKey)
    }

    @Test
    fun `import preview counts importable assets and api keys`() {
        val data = BackupData(
            schemaVersion = BACKUP_SCHEMA_VERSION,
            exportedAt = 1_700_000_000_000L,
            appVersion = "1.0.0",
            watchlist = listOf(
                BackupAsset("cg:bitcoin", "BTC", "Bitcoin", "CRYPTO", "COINGECKO", "bitcoin"),
                BackupAsset("broken", "BAD", "Broken", "UNKNOWN", "COINGECKO", "broken"),
            ),
            holdings = listOf(BackupHolding("cg:bitcoin", 1.0, 10_000.0)),
            alerts = listOf(BackupAlert("cg:bitcoin", false, 9_000.0, true)),
            settings = BackupSettings(
                coingeckoKey = null,
                alphaVantageKey = "av_secret",
            ),
        )

        val preview = data.toImportPreview()

        assertEquals(2, preview.assets)
        assertEquals(1, preview.importableAssets)
        assertEquals(1, preview.holdings)
        assertEquals(1, preview.alerts)
        assertEquals(false, preview.includesCoinGeckoKey)
        assertTrue(preview.includesAlphaVantageKey)
        assertTrue(preview.includesApiKeys)
    }

    @Test
    fun `asset maps to backup and back without loss`() {
        val asset = Asset(
            id = "cg:ethereum",
            symbol = "ETH",
            name = "Ethereum",
            type = AssetType.CRYPTO,
            source = DataProvider.COINGECKO,
            sourceId = "ethereum",
            colorHex = "#627eea",
            logoUrl = "https://example.com/eth.png",
        )

        assertEquals(asset, asset.toBackup().toAssetOrNull())
    }

    @Test
    fun `unknown enum names produce null asset`() {
        val broken = BackupAsset("x", "X", "X", "NOT_A_TYPE", "NOT_A_SOURCE", "x", null, null)
        assertNull(broken.toAssetOrNull())
    }

    @Test
    fun `unknown json fields are ignored on decode`() {
        val withExtra = """{"schemaVersion":1,"exportedAt":0,"futureField":"x","watchlist":[]}"""
        val decoded = json.decodeFromString(BackupData.serializer(), withExtra)
        assertTrue(decoded.watchlist.isEmpty())
    }
}
