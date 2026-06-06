package cz.obchodnik.domain.model

import cz.obchodnik.domain.AssetType

object StaticAssetCatalog {
    val assets: List<Asset> = listOf(
        Asset("cg:bitcoin", "BTC", "Bitcoin", AssetType.CRYPTO, DataProvider.COINGECKO, "bitcoin", "#f7931a", "https://coin-images.coingecko.com/coins/images/1/large/bitcoin.png"),
        Asset("cg:ethereum", "ETH", "Ethereum", AssetType.CRYPTO, DataProvider.COINGECKO, "ethereum", "#627eea", "https://coin-images.coingecko.com/coins/images/279/large/ethereum.png"),
        Asset("cg:solana", "SOL", "Solana", AssetType.CRYPTO, DataProvider.COINGECKO, "solana", "#14f195", "https://coin-images.coingecko.com/coins/images/4128/large/solana.png"),
        Asset("cg:binancecoin", "BNB", "BNB", AssetType.CRYPTO, DataProvider.COINGECKO, "binancecoin", "#f3ba2f", null),
        Asset("cg:ripple", "XRP", "XRP", AssetType.CRYPTO, DataProvider.COINGECKO, "ripple", "#23292f", null),
        Asset("cg:cardano", "ADA", "Cardano", AssetType.CRYPTO, DataProvider.COINGECKO, "cardano", "#0033ad", null),
        Asset("cg:dogecoin", "DOGE", "Dogecoin", AssetType.CRYPTO, DataProvider.COINGECKO, "dogecoin", "#c2a633", "https://coin-images.coingecko.com/coins/images/5/large/dogecoin.png"),
        Asset("cg:avalanche-2", "AVAX", "Avalanche", AssetType.CRYPTO, DataProvider.COINGECKO, "avalanche-2", "#e84142", null),
        Asset("cg:pax-gold", "PAXG", "PAX Gold", AssetType.METAL, DataProvider.COINGECKO, "pax-gold", "#d4af37", "https://coin-images.coingecko.com/coins/images/9519/large/paxgold.png"),
        Asset("cg:tether-gold", "XAUT", "Tether Gold", AssetType.METAL, DataProvider.COINGECKO, "tether-gold", "#d4af37", null),
        Asset("av:c:BRENT", "BRENT", "Ropa Brent", AssetType.COMMODITY, DataProvider.ALPHAVANTAGE, "BRENT", "#5b8c3e", null),
        Asset("av:c:WTI", "WTI", "Ropa WTI", AssetType.COMMODITY, DataProvider.ALPHAVANTAGE, "WTI", "#6b9c4e", null),
        Asset("av:c:NATURAL_GAS", "NG", "Zemní plyn", AssetType.COMMODITY, DataProvider.ALPHAVANTAGE, "NATURAL_GAS", "#4aa3df", null),
        Asset("av:c:WHEAT", "WHEAT", "Pšenice", AssetType.COMMODITY, DataProvider.ALPHAVANTAGE, "WHEAT", "#d6b85a", null),
        Asset("av:c:COPPER", "COPPER", "Měď", AssetType.COMMODITY, DataProvider.ALPHAVANTAGE, "COPPER", "#c87f4a", null),
        Asset("av:i:SPY", "SPY", "S&P 500 přes ETF", AssetType.INDEX, DataProvider.ALPHAVANTAGE, "SPY", "#3b82f6", null),
        Asset("av:i:QQQ", "QQQ", "Nasdaq 100 přes ETF", AssetType.INDEX, DataProvider.ALPHAVANTAGE, "QQQ", "#8b5cf6", null),
        Asset("av:i:DIA", "DIA", "Dow Jones přes ETF", AssetType.INDEX, DataProvider.ALPHAVANTAGE, "DIA", "#22c55e", null),
    )

    fun search(query: String): List<Asset> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return assets
        return assets.filter { asset ->
            asset.symbol.lowercase().contains(normalized) ||
                asset.name.lowercase().contains(normalized)
        }
    }
}
