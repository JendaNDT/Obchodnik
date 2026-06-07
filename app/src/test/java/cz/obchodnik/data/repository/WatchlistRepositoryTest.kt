package cz.obchodnik.data.repository

import cz.obchodnik.domain.AssetType
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.DataProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchlistRepositoryTest {
    @Test
    fun `add stores asset in watchlist with next sort order`() = runTest {
        val store = FakeAssetStore()
        val repository = WatchlistRepository(store)

        repository.add(asset("cg:bitcoin", "BTC"))
        repository.add(asset("cg:ethereum", "ETH"))

        val watchlist = store.watchlistAssets()
        assertEquals(listOf("BTC", "ETH"), watchlist.map { it.symbol })
        assertEquals(listOf(0, 1), watchlist.map { it.sortOrder })
        assertTrue(watchlist.all { it.inWatchlist })
    }

    @Test
    fun `remove keeps asset row but removes it from watchlist`() = runTest {
        val store = FakeAssetStore()
        val repository = WatchlistRepository(store)

        repository.add(asset("cg:bitcoin", "BTC"))
        repository.remove("cg:bitcoin")

        val stored = store.assetById("cg:bitcoin")
        assertEquals("BTC", stored?.symbol)
        assertFalse(stored?.inWatchlist ?: true)
    }

    @Test
    fun `move reorders watchlist and normalizes sort orders`() = runTest {
        val store = FakeAssetStore()
        val repository = WatchlistRepository(store)

        repository.add(asset("cg:bitcoin", "BTC"))
        repository.add(asset("cg:ethereum", "ETH"))
        repository.add(asset("cg:solana", "SOL"))

        repository.move("cg:solana", -1)

        var watchlist = store.watchlistAssets()
        assertEquals(listOf("BTC", "SOL", "ETH"), watchlist.map { it.symbol })
        assertEquals(listOf(0, 1, 2), watchlist.map { it.sortOrder })

        repository.move("cg:bitcoin", 1)

        watchlist = store.watchlistAssets()
        assertEquals(listOf("SOL", "BTC", "ETH"), watchlist.map { it.symbol })
        assertEquals(listOf(0, 1, 2), watchlist.map { it.sortOrder })
    }

    private fun asset(id: String, symbol: String): Asset =
        Asset(
            id = id,
            symbol = symbol,
            name = symbol,
            type = AssetType.CRYPTO,
            source = DataProvider.COINGECKO,
            sourceId = id.removePrefix("cg:"),
            colorHex = null,
            logoUrl = null,
        )
}
