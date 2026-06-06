package cz.obchodnik.data.source

import cz.obchodnik.core.Result
import cz.obchodnik.domain.model.Asset
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote

interface MarketDataSource {
    suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>>
    suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>>
    suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>>
    suspend fun search(query: String): Result<List<Asset>>
}
