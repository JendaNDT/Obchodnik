package cz.obchodnik.data.remote.alphavantage

import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageCommodityDto
import cz.obchodnik.data.remote.alphavantage.dto.AlphaVantageSeriesDto
import cz.obchodnik.domain.model.Candle
import cz.obchodnik.domain.model.ChartRange
import cz.obchodnik.domain.model.PricePoint
import cz.obchodnik.domain.model.Quote
import java.time.LocalDate
import java.time.ZoneOffset

object AlphaVantageMapper {

    private fun parseDateToTimestamp(dateStr: String): Long? =
        runCatching {
            LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()

    fun mapCommodityToQuote(
        dto: AlphaVantageCommodityDto,
        assetId: String,
        currency: String,
        usdToCzkRate: Double,
        nowMillis: Long
    ): Quote {
        // Detect Alpha Vantage API Note/Information
        if (!dto.note.isNullOrBlank()) {
            throw IllegalStateException("Alpha Vantage API Note: ${dto.note}")
        }
        if (!dto.information.isNullOrBlank()) {
            throw IllegalStateException("Alpha Vantage API Information: ${dto.information}")
        }

        val dataPoints = dto.data.orEmpty()
            .mapNotNull { point ->
                val valDouble = point.value.toDoubleOrNull()
                val time = parseDateToTimestamp(point.date)
                if (valDouble != null && time != null) {
                    PricePoint(time, valDouble)
                } else null
            }

        if (dataPoints.isEmpty()) {
            throw IllegalArgumentException("Commodity data points are empty")
        }

        val rawPrice = dataPoints[0].price
        val rate = if (currency.lowercase() == "czk") usdToCzkRate else 1.0
        val price = rawPrice * rate

        val change24hPct = if (dataPoints.size >= 2) {
            val prevPrice = dataPoints[1].price
            ((rawPrice - prevPrice) / prevPrice) * 100.0
        } else null

        // Sparkline is chronological order (oldest to newest)
        val sparkline = dataPoints.take(7)
            .map { it.price * rate }
            .reversed()

        return Quote(
            assetId = assetId,
            price = price,
            change24hPct = change24hPct,
            change7dPct = null,
            change30dPct = null,
            high24h = price,
            low24h = price,
            marketCap = null,
            volume24h = null,
            sparkline7d = sparkline,
            currency = currency,
            updatedAt = nowMillis
        )
    }

    fun mapStockSeriesToQuote(
        dto: AlphaVantageSeriesDto,
        assetId: String,
        currency: String,
        usdToCzkRate: Double,
        nowMillis: Long
    ): Quote {
        if (!dto.note.isNullOrBlank()) {
            throw IllegalStateException("Alpha Vantage API Note: ${dto.note}")
        }
        if (!dto.information.isNullOrBlank()) {
            throw IllegalStateException("Alpha Vantage API Information: ${dto.information}")
        }

        val bars = dto.timeSeries.orEmpty()
            .mapNotNull { (dateStr, barDto) ->
                val close = barDto.close.toDoubleOrNull()
                val high = barDto.high.toDoubleOrNull()
                val low = barDto.low.toDoubleOrNull()
                val volume = barDto.volume.toDoubleOrNull()
                val time = parseDateToTimestamp(dateStr)
                if (close != null && time != null) {
                    TimeBar(time, close, high, low, volume)
                } else null
            }
            .sortedByDescending { it.timestamp }

        if (bars.isEmpty()) {
            throw IllegalArgumentException("Stock series bars are empty")
        }

        val rawPrice = bars[0].close
        val rate = if (currency.lowercase() == "czk") usdToCzkRate else 1.0
        val price = rawPrice * rate

        val change24hPct = if (bars.size >= 2) {
            val prevPrice = bars[1].close
            ((rawPrice - prevPrice) / prevPrice) * 100.0
        } else null

        val high24h = bars[0].high?.let { it * rate } ?: price
        val low24h = bars[0].low?.let { it * rate } ?: price
        val volume = bars[0].volume

        val sparkline = bars.take(7)
            .map { it.close * rate }
            .reversed()

        return Quote(
            assetId = assetId,
            price = price,
            change24hPct = change24hPct,
            change7dPct = null,
            change30dPct = null,
            high24h = high24h,
            low24h = low24h,
            marketCap = null,
            volume24h = volume,
            sparkline7d = sparkline,
            currency = currency,
            updatedAt = nowMillis
        )
    }

    fun mapCommodityToHistory(
        dto: AlphaVantageCommodityDto,
        range: ChartRange,
        currency: String,
        usdToCzkRate: Double
    ): List<PricePoint> {
        val rate = if (currency.lowercase() == "czk") usdToCzkRate else 1.0
        val allPoints = dto.data.orEmpty()
            .mapNotNull { point ->
                val valDouble = point.value.toDoubleOrNull()
                val time = parseDateToTimestamp(point.date)
                if (valDouble != null && time != null) {
                    PricePoint(time, valDouble * rate)
                } else null
            }
            .sortedByDescending { it.timestamp }

        if (allPoints.isEmpty()) return emptyList()
        val latestTime = allPoints.first().timestamp
        val cutoffTime = when (range) {
            ChartRange.D1 -> latestTime - 2 * 24 * 60 * 60_000L
            ChartRange.W1 -> latestTime - 7 * 24 * 60 * 60_000L
            ChartRange.M1 -> latestTime - 30 * 24 * 60 * 60_000L
            ChartRange.Y1, ChartRange.ALL -> Long.MIN_VALUE
        }
        return allPoints.filter { it.timestamp >= cutoffTime }.reversed()
    }

    fun mapStockSeriesToHistory(
        dto: AlphaVantageSeriesDto,
        range: ChartRange,
        currency: String,
        usdToCzkRate: Double
    ): List<PricePoint> {
        val rate = if (currency.lowercase() == "czk") usdToCzkRate else 1.0
        val allPoints = dto.timeSeries.orEmpty()
            .mapNotNull { (dateStr, barDto) ->
                val close = barDto.close.toDoubleOrNull()
                val time = parseDateToTimestamp(dateStr)
                if (close != null && time != null) {
                    PricePoint(time, close * rate)
                } else null
            }
            .sortedByDescending { it.timestamp }

        if (allPoints.isEmpty()) return emptyList()
        val latestTime = allPoints.first().timestamp
        val cutoffTime = when (range) {
            ChartRange.D1 -> latestTime - 2 * 24 * 60 * 60_000L
            ChartRange.W1 -> latestTime - 7 * 24 * 60 * 60_000L
            ChartRange.M1 -> latestTime - 30 * 24 * 60 * 60_000L
            ChartRange.Y1, ChartRange.ALL -> Long.MIN_VALUE
        }
        return allPoints.filter { it.timestamp >= cutoffTime }.reversed()
    }

    fun mapStockSeriesToCandles(
        dto: AlphaVantageSeriesDto,
        range: ChartRange,
        currency: String,
        usdToCzkRate: Double
    ): List<Candle> {
        val rate = if (currency.lowercase() == "czk") usdToCzkRate else 1.0
        val allCandles = dto.timeSeries.orEmpty()
            .mapNotNull { (dateStr, barDto) ->
                val open = barDto.open.toDoubleOrNull()
                val high = barDto.high.toDoubleOrNull()
                val low = barDto.low.toDoubleOrNull()
                val close = barDto.close.toDoubleOrNull()
                val time = parseDateToTimestamp(dateStr)
                if (open != null && high != null && low != null && close != null && time != null) {
                    Candle(
                        timestamp = time,
                        open = open * rate,
                        high = high * rate,
                        low = low * rate,
                        close = close * rate
                    )
                } else null
            }
            .sortedByDescending { it.timestamp }

        if (allCandles.isEmpty()) return emptyList()
        val latestTime = allCandles.first().timestamp
        val cutoffTime = when (range) {
            ChartRange.D1 -> latestTime - 2 * 24 * 60 * 60_000L
            ChartRange.W1 -> latestTime - 7 * 24 * 60 * 60_000L
            ChartRange.M1 -> latestTime - 30 * 24 * 60 * 60_000L
            ChartRange.Y1, ChartRange.ALL -> Long.MIN_VALUE
        }
        return allCandles.filter { it.timestamp >= cutoffTime }.reversed()
    }

    private data class TimeBar(
        val timestamp: Long,
        val close: Double,
        val high: Double?,
        val low: Double?,
        val volume: Double?
    )
}
