# Obchodník — Implementační plán

Samostatný, podrobný plán pro dokončení aplikace. Napsáno tak, aby podle něj
mohl pokračovat jiný nástroj (aktuálně Codex od OpenAI) bez další
kontextové znalosti.
Zdroj pravdy pro architekturu, API kontrakty a kroky. Doplňuje `AGENTS.md`
(konvence) a `ROADMAP.md` (číslované kroky).

Datum: 2026-06-06. Stav: verze 1.0 (kroky 1–11) hotová + vylepšení po 1.0 (viz `ROADMAP.md` a `PROGRESS.md`). Zkompilováno a otestováno.

---

## 0. Jak tento plán používat (pokyny pro navazující AI)

- Postupuj po krocích z kapitoly 18 (odpovídají `ROADMAP.md`). Každý krok má
  cíl, seznam souborů a akceptační kritéria.
- Design handoff je v `/Users/jenda/Downloads/CryptoWidget.zip`. Používej ho jako
  vizuální referenci pro obrazovky, widgety, grafy a interakce, ale ne jako
  produkční zdrojový kód.
- Při rozporu má tento plán přednost pro Android architekturu, API zdroje,
  Jetpack Glance, WorkManager, perzistenci a platformní omezení.
- Odchylky od handoffu, které už platí: zobrazovaný název je „Obchodník";
  nastavení je vpravo nahoře, ne ve spodní navigaci; periodický refresh widgetu
  respektuje WorkManager minimum 15 minut.
- Po každém kroku spusť `./gradlew :app:assembleDebug` a `./gradlew :app:testDebugUnitTest`.
  Krok je hotový, až build i unit testy projdou.
- Konvence v `AGENTS.md` platí: balíček `cz.obchodnik`, UI česky, kód/komentáře
  anglicky, žádné emoji, čísla v monospace.
- Verze knihoven měň jen vědomě přes `gradle/libs.versions.toml`. Aktuální
  kombinace je ověřená a konzistentní (kap. 2).
- API endpointy v kap. 5 jsou ověřené k 2026-06. Tvar JSON ber jako kontrakt;
  pokud se reálná odpověď liší, uprav DTO, ne doménový model.
- Před pokračováním si přečti `HANDOFF.md`; je to krátká provozní
  předávka aktuálního stavu.

### 0.1 Aktuální implementační snapshot

Všech 11 kroků je hotových (zkompilováno a otestováno 2026-06-06). Tato kapitola
už neslouží jako TODO, ale jako přehled výsledné architektury.

Hotové části:

- `MainActivity` hostuje Compose Navigation pro `markets`, `portfolio`, `fng`,
  `alerts` (spodní navigace), `search`, `detail/{assetId}`, `settings`
  a `onboarding` (push).
- `AppContainer` vytváří Room DB, DataStore settings, CoinGecko i Alpha Vantage
  Retrofit, `CryptoDataSource`, `CommodityDataSource`, `DataSourceRouter`,
  `CurrencyRateProvider` a repository vrstvu.
- `MarketRepository` umí sledovat quote cache, refresh quotes, vrátit cached quote
  a cachovat line historii i OHLC svíčky napříč oběma zdroji přes router.
- `WatchlistRepository` umí default watchlist, add/remove/toggle, uložit
  kandidáta z vyhledávání a načíst asset podle id.
- `SettingsRepository` má plný DataStore model: téma, akcent, měna, interval,
  default graf, hustota, widget F&G, API klíče, onboarding, oznámení a Alpha
  Vantage denní počítadlo. UI Nastavení i propagace klíčů do klientů je hotová.
- Hotové UI složky: `ui/markets`, `ui/search`, `ui/detail`, `ui/settings`,
  `ui/portfolio`, `ui/alerts`, `ui/fng`, `ui/onboarding`, `ui/components`,
  `ui/theme`.
- Hotový widget (`widget/*`) v Jetpack Glance ve 3 velikostech s konfigurací,
  WorkManager refresh (`work/*`) a vyhodnocením alertů s notifikacemi.

Otevřené technické dluhy (ne blokery):

- Room má export schémat (`exportSchema = true`, `room.schemaLocation`) a
  verzované migrace přes `ObchodnikDatabase.MIGRATIONS`; destruktivní migrace
  jen pro downgrade. Baseline schéma je v `app/schemas`.
- Fonty: Hanken Grotesk + JetBrains Mono přes Downloadable Fonts (Google Fonts
  provider) — `ui/theme/Type.kt` + `res/values/font_certs.xml`.

---

## 1. Účel a rozsah

Nativní Android aplikace „Obchodník" pro sledování cen:

- kryptoměn (vyhledání a přidání libovolné mince),
- drahých kovů (tokenizované: PAX Gold, Tether Gold; + XAU/XAG reference),
- komodit (ropa WTI/Brent, zemní plyn, měď, pšenice…),
- akciových indexů (přes ETF zástupce SPY/QQQ/DIA).

Funkce: watchlist, detail s grafem (čára + svíčky, časové rozsahy), Fear & Greed
index, portfolio s P/L, cenové alerty, home-screen **widget (Jetpack Glance)**
ve 3 velikostech, uživatelsky nastavitelný interval aktualizace, 3 tmavá témata,
měna USD/CZK. Tlačítko nastavení v pravém horním rohu.

---

## 2. Tech stack a verze (pinned)

Vše v `gradle/libs.versions.toml`.

| Komponenta | Verze | Pozn. |
|---|---|---|
| Android Gradle Plugin | 8.7.0 | vyžaduje JDK 17, Gradle 8.9 |
| Kotlin | 2.0.21 | + `kotlin.plugin.compose` (stejná verze) |
| Compose BOM | 2024.10.01 | řídí verze všech compose knihoven |
| Glance | 1.1.0 | `glance-appwidget`, `glance-material3` |
| Navigation Compose | 2.8.4 | |
| Room | 2.6.1 | přes KSP `2.0.21-1.0.25` |
| DataStore Preferences | 1.1.1 | nastavení |
| WorkManager | 2.9.1 | refresh widgetu |
| Retrofit | 2.11.0 | + JakeWharton kotlinx-serialization converter 1.0.0 |
| OkHttp | 4.12.0 | + logging-interceptor |
| kotlinx.serialization | 1.7.3 | JSON |
| Coil | 2.7.0 | loga aktiv |

`minSdk = 26`, `targetSdk = 35`, `compileSdk = 35`. Java/Kotlin target 17.
DI: ruční container (žádný Hilt).

---

## 3. Architektura

Jednosměrný tok dat, MVVM, čisté vrstvy:

```
UI (Compose screens) ── stav ──> ViewModel ──> Repository ──> DataSource(s) ──> API
        ^                              |             |
        └────────── StateFlow ────────┘             └─> Room (cache) + DataStore (nastavení)
```

- **UI** je bezstavové composable + `collectAsStateWithLifecycle()`.
- **ViewModel** drží `StateFlow<UiState>`, volá repository, mapuje na UI model.
- **Repository** sloučí zdroje, řeší cache, rate-limit budget, jednotky a měnu.
- **DataSource** = rozhraní per třída aktiv; router vybírá zdroj dle `AssetType`.
- **Room** je offline cache (poslední ceny, historie) → widget i app fungují bez sítě.
- **DataStore** = uživatelská nastavení (téma, akcent, měna, interval, klíče).

Asynchronní: Kotlin Coroutines + Flow. Žádné blokující IO na main threadu.

---

## 4. Struktura balíčků

```
cz.obchodnik
├── ObchodnikApp.kt              # Application + AppContainer (DI)
├── MainActivity.kt              # host pro Compose navigaci
├── di/
│   └── AppContainer.kt          # ruční DI: OkHttp, Retrofit, DAO, repos, prefs
├── core/
│   ├── Result.kt                # sealed výsledek (Loading/Success/Error)
│   ├── Dispatchers.kt           # injektovatelné dispatchery (test)
│   └── format/
│       ├── PriceFormatter.kt    # cs-CZ formát ceny dle velikosti
│       └── PercentFormatter.kt
├── data/
│   ├── remote/
│   │   ├── coingecko/           # CoinGeckoApi + DTO
│   │   ├── alphavantage/        # AlphaVantageApi + DTO
│   │   └── fng/                 # FngApi + DTO (alternative.me)
│   ├── local/
│   │   ├── ObchodnikDatabase.kt
│   │   ├── dao/                 # AssetDao, PriceDao, HistoryDao, HoldingDao, AlertDao
│   │   └── entity/              # *Entity
│   ├── prefs/
│   │   └── SettingsRepository.kt # DataStore
│   ├── source/
│   │   ├── MarketDataSource.kt   # rozhraní
│   │   ├── CryptoDataSource.kt   # CoinGecko impl
│   │   ├── CommodityDataSource.kt# Alpha Vantage impl
│   │   └── DataSourceRouter.kt
│   └── repository/
│       ├── MarketRepository.kt
│       ├── WatchlistRepository.kt
│       ├── PortfolioRepository.kt
│       ├── AlertRepository.kt
│       └── FngRepository.kt
├── domain/
│   ├── model/                   # Asset, Quote, PricePoint, Candle, Holding, Alert, Fng
│   └── AssetType.kt
├── ui/
│   ├── theme/                   # Color, Theme, Type (hotovo v kroku 1)
│   ├── components/              # AssetIcon, Change, Card, Sparkline, PriceChart, Gauge…
│   ├── navigation/              # plánované vyčlenění rout; aktuálně v MainActivity
│   ├── markets/                 # MarketsScreen + MarketsViewModel
│   ├── detail/                  # DetailScreen + DetailViewModel
│   ├── search/                  # SearchScreen + ViewModel
│   ├── portfolio/               # PortfolioScreen + ViewModel
│   ├── alerts/                  # AlertsScreen + ViewModel
│   ├── fng/                     # FngScreen + ViewModel
│   ├── settings/                # čeká na krok 7
│   └── onboarding/              # OnboardingScreen
├── widget/                      # čeká na krok 8
│   ├── ObchodnikWidget.kt       # GlanceAppWidget
│   ├── ObchodnikWidgetReceiver.kt
│   ├── WidgetState.kt           # serializace stavu pro Glance
│   └── WidgetConfigActivity.kt
└── work/                        # čeká na krok 8
    └── RefreshWorker.kt         # periodický refresh dle intervalu
```

---

## 5. Datové zdroje a API kontrakty (ověřeno 2026-06)

### 5.1 CoinGecko (krypto + tokenizované kovy)

- Base URL: `https://api.coingecko.com/api/v3/`
- Autentizace (Demo): hlavička `x-cg-demo-api-key: <KEY>` (preferováno) nebo
  query `x_cg_demo_api_key`. Bez klíče funguje veřejný tier (5–15/min) — appka
  musí běžet i bez klíče, jen s nižším limitem.
- Limity Demo: 100 req/min, 10 000 req/měsíc.
- `vs_currency` podporuje `usd` i `czk` → měnu řešíme přímo dotazem.

Endpointy:

1. `GET /ping` — health check.
2. `GET /search?query={q}` →
   ```json
   { "coins": [ { "id":"bitcoin","name":"Bitcoin","api_symbol":"bitcoin",
     "symbol":"BTC","market_cap_rank":1,"thumb":"...","large":"..." } ] }
   ```
3. `GET /coins/markets?vs_currency=usd&ids=bitcoin,ethereum&order=market_cap_desc&sparkline=true&price_change_percentage=24h,7d,30d&per_page=50&page=1`
   → pole:
   ```json
   [ { "id":"bitcoin","symbol":"btc","name":"Bitcoin","image":"...",
     "current_price":68000,"market_cap":1.2e12,"market_cap_rank":1,
     "total_volume":3.1e10,"high_24h":69000,"low_24h":67000,
     "price_change_percentage_24h":1.5,
     "price_change_percentage_24h_in_currency":1.5,
     "price_change_percentage_7d_in_currency":-2.1,
     "price_change_percentage_30d_in_currency":8.0,
     "sparkline_in_7d": { "price":[...] } } ]
   ```
   Toto je hlavní endpoint pro watchlist (ceny + % změny + sparkline naráz).
4. `GET /coins/{id}/market_chart?vs_currency=usd&days={1|7|30|365|max}` →
   ```json
   { "prices":[[1700000000000,68000.1], ...],
     "market_caps":[[...]], "total_volumes":[[...]] }
   ```
   `prices` = [timestamp_ms, price] → pro režim „čára".
5. `GET /coins/{id}/ohlc?vs_currency=usd&days={1|7|14|30|90|180|365}` →
   ```json
   [ [1700000000000, open, high, low, close], ... ]
   ```
   Pro režim „svíčky". (Granularita je dána CoinGeckem dle `days`.)

Tokenizované kovy: jsou to běžné coiny → `pax-gold` (PAXG), `tether-gold`
(XAUT). Sledují se stejnými endpointy jako krypto, jen je zařadíme do
kategorie „Kovy".

### 5.2 Alpha Vantage (komodity + indexy přes ETF)

- Base URL: `https://www.alphavantage.co/query`
- Autentizace: query `apikey=<KEY>`. Bez klíče nelze (klíč je zdarma).
- Limity free: 25 req/den, 5 req/min, `outputsize=compact` (100 bodů).
  → **tvrdě cachovat** (komodity refresh max párkrát denně), viz kap. 10.

Endpointy:

1. Komodity: `?function={WTI|BRENT|NATURAL_GAS|COPPER|ALUMINUM|WHEAT|CORN|COTTON|SUGAR|COFFEE|ALL_COMMODITIES}&interval={daily|weekly|monthly}&apikey=KEY`
   →
   ```json
   { "name":"Crude Oil Prices WTI","interval":"daily","unit":"dollars per barrel",
     "data":[ {"date":"2026-06-05","value":"72.34"}, ... ] }
   ```
   Poslední `data[0].value` = aktuální cena; pole = historie pro graf.
2. Index ETF (zástupci indexů): `?function=TIME_SERIES_DAILY&symbol={SPY|QQQ|DIA}&outputsize=compact&apikey=KEY`
   →
   ```json
   { "Meta Data": {...},
     "Time Series (Daily)": {
       "2026-06-05": {"1. open":"...","2. high":"...","3. low":"...",
                      "4. close":"529.12","5. volume":"..."}, ... } }
   ```
   `TIME_SERIES_DAILY` je ve free (na rozdíl od `..._ADJUSTED`). Mapování:
   SPY→S&P 500, QQQ→Nasdaq 100, DIA→Dow Jones. V UI to označ jako „přibližně
   přes ETF" (poznámka u indexů).
3. Rychlá cena: `?function=GLOBAL_QUOTE&symbol=SPY&apikey=KEY` →
   ```json
   { "Global Quote": { "05. price":"529.12","09. change":"1.2",
     "10. change percent":"0.23%" } }
   ```
   Volitelně pro aktuální cenu bez celé historie (šetří kredity).

Pozn.: Alpha Vantage při překročení limitu vrací JSON s klíčem `Note` nebo
`Information` místo dat — DTO i parser to musí detekovat a vrátit `Result.Error`
(rate limit), nikoli spadnout.

### 5.3 Fear & Greed index (alternative.me)

- `GET https://api.alternative.me/fng/?limit={n}&format=json` →
  ```json
  { "data":[ {"value":"54","value_classification":"Neutral",
    "timestamp":"1700000000","time_until_update":"3600"} ] }
  ```
  `limit=1` aktuální, větší `limit` = historie pro graf na F&G obrazovce.
  Bez klíče, štědrý limit. (Toto je krypto F&G, dle handoff.)

### 5.4 Měna a kurz USD/CZK

- Krypto: dotazuj přímo s `vs_currency` = aktuální měna (usd/czk) — žádný
  ruční přepočet.
- Alpha Vantage (komodity, ETF): hodnoty jsou v USD. Pro CZK přepočítej kurzem.
  Kurz získej z CoinGecko: `GET /simple/price?ids=tether&vs_currencies=czk`
  (USDT≈USD) nebo z dedikovaného kurzového API. Kurz cachuj 1×/den.
  Plán: jednoduchý `CurrencyRateProvider` s denní cache; default fallback kurz
  ulož jako konstantu a označ „přibližný", dokud se nenačte živý.

---

## 6. Doménový model

```kotlin
enum class AssetType { CRYPTO, METAL, COMMODITY, INDEX }

data class Asset(
    val id: String,            // stabilní klíč napříč zdroji, viz kap. 8
    val symbol: String,        // BTC, WTI, SPY…
    val name: String,          // Bitcoin, Ropa WTI…
    val type: AssetType,
    val source: DataProvider,  // COINGECKO | ALPHAVANTAGE
    val sourceId: String,      // id v rámci zdroje (coingecko id / av symbol/function)
    val colorHex: String?,     // z handoff tabulky, jinak odvodit z tickeru
    val logoUrl: String?,      // jen krypto (CoinGecko image)
)

data class Quote(
    val assetId: String,
    val price: Double,
    val change24hPct: Double?,
    val change7dPct: Double?,
    val change30dPct: Double?,
    val high24h: Double?,
    val low24h: Double?,
    val marketCap: Double?,
    val volume24h: Double?,
    val sparkline7d: List<Double>,
    val currency: String,      // "usd" | "czk"
    val updatedAt: Long,
)

data class PricePoint(val timestamp: Long, val price: Double)
data class Candle(val timestamp: Long, val open: Double, val high: Double,
                  val low: Double, val close: Double)

enum class ChartRange { D1, W1, M1, Y1, ALL }   // 1D/1T/1M/1R/VŠE

data class Holding(val id: Long, val assetId: String, val qty: Double, val avgPrice: Double)
data class PriceAlert(val id: Long, val assetId: String, val above: Boolean,
                      val target: Double, val enabled: Boolean, val triggeredAt: Long?)
data class Fng(val value: Int, val classification: String, val timestamp: Long)
```

`DataProvider` enum: `COINGECKO`, `ALPHAVANTAGE`.

---

## 7. DTO a mapování

- Každý endpoint má vlastní `@Serializable` DTO v `data/remote/.../dto`.
- Mapování DTO → doména v samostatných `*Mapper.kt` (čistě testovatelné funkce).
- Alpha Vantage „Time Series (Daily)" má dynamické klíče (datumy) → parsuj jako
  `Map<String, DailyBarDto>` a seřaď podle data sestupně.
- Detekce rate-limit odpovědí Alpha Vantage: DTO s volitelnými poli
  `Note`/`Information` → pokud přítomna, mapper vrátí chybu.

---

## 8. Katalog aktiv a sjednocení zdrojů

Problém: krypto má dynamická id (CoinGecko), komodity/indexy mají pevné funkce
(Alpha Vantage). Řešení — jednotné `Asset.id`:

- Krypto/kovy: `cg:{coingeckoId}` (např. `cg:bitcoin`, `cg:pax-gold`).
- Komodity: `av:c:{FUNCTION}` (např. `av:c:WTI`).
- Indexy: `av:i:{SYMBOL}` (např. `av:i:SPY`).

`DataSourceRouter` podle prefixu (`cg:` / `av:`) vybere `MarketDataSource`.

Statický seznam ne-krypto aktiv (komodity, indexy, kovové tokeny) ulož jako
`assets_catalog.json` v `res/raw` nebo Kotlin konstantu — s ticker, name,
barvou (z handoff tabulky barev), kategorií. Krypto se přidává dynamicky přes
vyhledávání. Barvy a kategorie viz `README` handoff (sekce „Barvy aktiv").

---

## 9. DataSource abstrakce

```kotlin
interface MarketDataSource {
    suspend fun quotes(assets: List<Asset>, currency: String): Result<List<Quote>>
    suspend fun history(asset: Asset, range: ChartRange, currency: String): Result<List<PricePoint>>
    suspend fun candles(asset: Asset, range: ChartRange, currency: String): Result<List<Candle>>
    suspend fun search(query: String): Result<List<Asset>>   // jen krypto zdroj plní
}
```

- `CryptoDataSource` (CoinGecko): plní vše včetně `search`.
- `CommodityDataSource` (Alpha Vantage): `quotes`/`history`; `candles` = z denních
  barů poskládat OHLC, `search` vrací prázdné (komodity/indexy jsou ze statického
  katalogu).
- `DataSourceRouter` rozdělí seznam aktiv dle providera, zavolá paralelně
  (`coroutineScope { async {} }`), sloučí výsledky.

---

## 10. Repository, cache a rate-limit budget

`MarketRepository`:

- Vrací `Flow` z Room (single source of truth pro UI); síť plní/aktualizuje Room.
- `refresh(assets, currency)` — zavolá router, uloží do Room, ošetří chyby.
- Politika čerstvosti (TTL) per provider:
  - CoinGecko quotes: TTL = uživatelský interval (min 1 min), jinak servíruj cache.
  - CoinGecko historie: TTL dle rozsahu (1D: 5 min, 1T: 30 min, delší: 6 h).
  - Alpha Vantage komodity/indexy: TTL = 6–12 h (limit 25/den!). Nikdy nevolej
    při každém otevření; jen pokud cache starší než TTL nebo na manuální refresh.
- Rate-limit guard: jednoduchý token-bucket / throttling per provider v
  `AppContainer` (CoinGecko ~ max ~50/min konzervativně, Alpha Vantage ~5/min a
  denní počítadlo v DataStore). Při překročení vrať cache + tichý log.
- Batchování: pro watchlist krypto použij jediný `coins/markets?ids=` dotaz
  (ne N dotazů). Komodity nelze batchovat → každá je 1 dotaz, proto silná cache.

---

## 11. Perzistence

Room (`ObchodnikDatabase`):

- `AssetEntity(id PK, symbol, name, type, provider, sourceId, colorHex, logoUrl, inWatchlist, sortOrder)`
- `QuoteEntity(assetId PK, currency PK, price, change24h, change7d, change30d, high24h, low24h, marketCap, volume24h, sparklineJson, updatedAt)`
- `HistoryEntity(assetId PK, range PK, currency PK, kind PK, pointsJson, updatedAt)` —
  cache grafu; `kind` rozlišuje `line` a `candle`
- `HoldingEntity(id PK auto, assetId, qty, avgPrice)`
- `AlertEntity(id PK auto, assetId, above, target, enabled, triggeredAt)`

DAO: Flow dotazy pro watchlist (`getWatchlistWithQuotes()`), CRUD pro holdings/alerts.
Sparkline a body grafu se ukládají jako JSON string (kotlinx.serialization) —
jednoduché a stačí.

DataStore (`SettingsRepository`), klíče:
`theme`, `accent`, `currency`, `refreshIntervalMinutes`, `defaultChart`
(`line|candle`), `density`, `showFngOnWidget`, `coingeckoKey`, `alphaVantageKey`,
`onboardingDone`, `notificationsEnabled`, `avDailyCount`, `avCountDate`.

---

## 12. DI container

`AppContainer` (vytvořen v `ObchodnikApp.onCreate`, předáván ViewModelům přes
`viewModelFactory` nebo `AndroidViewModel` + `application`):

- `OkHttpClient` (logging v debug, hlavičkový interceptor pro CoinGecko klíč,
  timeouts 15 s).
- `Retrofit` instance pro CoinGecko a Alpha Vantage (různé base URL) + FNG.
- `Json { ignoreUnknownKeys = true; coerceInputValues = true }`.
- `ObchodnikDatabase` (Room.databaseBuilder).
- Repozitáře, datové zdroje, router, `SettingsRepository`.
- Sdílený `CoroutineDispatchers`.

ViewModely berou závislosti z containeru (žádný Hilt). `MainActivity` a widget
sahají na `(*application as ObchodnikApp).container`.

Aktuálně je implementovaná CoinGecko větev bez API key interceptoru. V kroku 7
doplnit čtení `coingeckoKey` z `SettingsRepository`; v kroku 9 přidat Alpha
Vantage Retrofit a router.

---

## 13. UI základ

### 13.1 Navigace
`Navigation Compose`. Top-level destinace (spodní navigace): `markets`,
`portfolio`, `fng`, `alerts`. Push destinace: `settings`, `search`,
`detail/{assetId}`, `onboarding`.
Spodní nav má 4 položky (Trh, Portfolio, Index, Alerty); **Nastavení je v pravém
horním rohu** TopBaru (ikona ozubeného kola), ne ve spodní navigaci — odchylka
od handoff dle požadavku uživatele.

### 13.2 Theming mapping (handoff → kód)
Hotovo v kroku 1: `Obchodnik.colors` (bg, surface, surface2, border,
borderStrong, text, text2, text3, accent, onAccent, up, down) + `Obchodnik.radii`.
Témata Terminal (default) / Aurora / Mono; akcenty modrá (default)/zelená/
oranžová/fialová. Mono téma přepíná UI font na monospace.

### 13.3 Společné komponenty (`ui/components`)
- `AssetIcon` — kruh `color+22` výplň, `color+55` border, monogram nebo Coil logo.
- `Change` — % změna se šipkou, barva up/down, monospace.
- `Card`, `SectionLabel`, `Chip`, `SegmentedControl`, `BottomSheetHandle`.
- `Sparkline` (Canvas) — line + gradient area, up/down barva.
- `PriceChart` (Canvas) — line (area+gradient, dot na konci) i candle (OHLC),
  gridlines, popisky os; přepínání režimu.
- `Gauge` (Canvas) — F&G půlkruh, 5 segmentů, ručička dle hodnoty.
- `Skeleton` — shimmer placeholder. `ErrorState`, `EmptyState`.

Grafy kresli přes `Canvas {}` (drawLine/drawPath/drawArc). Animace překreslení
při změně rozsahu. Žádná externí grafová knihovna (méně závislostí).

---

## 14. Obrazovky (detail dle handoff README)

Pro každou: composable + ViewModel se `StateFlow<UiState>`; akce jako lambdy.

1. **Onboarding** (4 kroky: uvítání, výběr aktiv, oznámení, hotovo). Při dokončení
   `onboardingDone=true`, založí výchozí watchlist.
2. **Trh / Watchlist** — sticky TopBar (název „Trh" + lupa + ozubené kolo vpravo),
   F&G karta, kategorie taby (Vše/Krypto/Kovy/Komodity/Indexy), seznam aktiv se
   sparkline, CTA přidat, prázdný stav. Pull-to-refresh.
3. **Detail aktiva** — velká cena + % chip, toggle Křivka/Svíčky, `PriceChart`,
   range taby (1D/1T/1M/1R/VŠE), stats grid (24h max/min, změny, kap., objem),
   tlačítka Alert / Sledovat.
4. **Vyhledávání** — search bar (debounce ~300 ms → CoinGecko `/search`),
   výsledky se star toggle (přidá `cg:{id}` do watchlistu). Komodity/indexy ze
   statického katalogu zobraz jako filtrovatelné sekce.
5. **Portfolio** — summary (hodnota, dnes, P/L, allocation bar), pozice řádky,
   bottom sheet „Přidat pozici" (výběr aktiva, množství, nákupní cena, live P/L).
6. **Alerty** — seznam s podmínkou a switchem, bottom sheet „Nový alert" (aktivum,
   nad/pod, cílová cena). Vyhodnocení v `RefreshWorker` → notifikace.
7. **Fear & Greed** — gauge, historické hodnoty (4), historický area graf,
   složení indexu (řádky s progress barem). Data z FNG API (`limit` pro historii).
8. **Nastavení** (push z ozubeného kola) — viz kap. 17.
9. **Chybové/prázdné/skeleton stavy** dle handoff.

---

## 15. Widget — Jetpack Glance

- `ObchodnikWidget : GlanceAppWidget` + `ObchodnikWidgetReceiver : GlanceAppWidgetReceiver`.
- Tři velikosti přes `SizeMode.Responsive` se sadou breakpointů:
  - Malý (~2×2): 1 aktivum (ticker, název, velká cena, % změna, sparkline).
  - Střední (~4×2): 3 řádky + volitelný F&G mini gauge.
  - Velký (~4×4): 5 řádků + header + footer „Aktualizováno HH:mm" / „Otevřít →".
- Stav: widget čte z Room cache (NE přímo ze sítě). `WidgetState` se serializuje
  do Glance `stateDefinition` (PreferencesGlanceStateDefinition) nebo widget čte
  DAO přes container ve `provideGlance`. Doporučeno: čti z Room v `provideGlance`,
  drobné konfigurace (vybraná aktiva, showFng, velikost) v Glance preferences.
- Glance má omezenou sadu komponent → sparkline ve widgetu vykresli jako
  `Image` z `Bitmap` (Canvas do bitmapy) nebo zjednoduš na text+šipku, pokud
  bitmapa dělá potíže. (Rozhodni při implementaci; preferuj bitmapu pro věrnost.)
- Tap na widget → `actionStartActivity<MainActivity>()`. Long-press konfigurace →
  `WidgetConfigActivity` (Compose) pro výběr aktiv a velikosti.
- Pozadí: poloprůhledný gradient (Glance `background` + `cornerRadius`), na
  Android 12+ lze přidat zaoblení; blur neřeš (Glance nepodporuje spolehlivě).

---

## 16. WorkManager — refresh dle intervalu

- `RefreshWorker : CoroutineWorker` — načte watchlist, refreshne quotes
  (respektuj TTL a rate-limit), uloží do Room, zavolá
  `ObchodnikWidget().updateAll(context)`, vyhodnotí alerty.
- Plánování: `PeriodicWorkRequest` s intervalem = `refreshIntervalMinutes`
  (min WorkManager = 15 min). Pro kratší „pocit" aktuálnosti refreshuj při
  otevření appky a při interakci s widgetem; periodicky pak min 15 min.
  → V UI nabídni intervaly: 15 / 30 / 60 / 180 / 360 min + „jen při otevření".
  (Pozn.: hodnoty < 15 min WorkManager neumí periodicky — v UI to zohledni nebo
  doplň expedited one-time chain; default 30 min.)
- Constraints: `NetworkType.CONNECTED`. Při změně intervalu v Nastavení přeplánuj
  (`enqueueUniquePeriodicWork(REPLACE)`).

---

## 17. Nastavení (detail)

Obrazovka přístupná ozubeným kolem vpravo nahoře. Sekce:

- **Zobrazení**: téma (Terminal/Aurora/Mono), akcent (4 barvy), hustota seznamu,
  výchozí typ grafu (čára/svíčky).
- **Měna & jednotky**: USD / CZK.
- **Aktualizace**: interval (15/30/60/180/360 min / jen při otevření) — zapisuje
  `refreshIntervalMinutes` a přeplánuje WorkManager.
- **Zdroj dat / klíče**: pole pro CoinGecko Demo klíč a Alpha Vantage klíč
  (uložené v DataStore; appka běží i bez nich s omezením). Odkaz, kde je získat.
- **Oznámení**: zapnout/vypnout, runtime permission `POST_NOTIFICATIONS` (API 33+).
- **Aplikace**: verze, odkaz na zdroje dat, reset onboardingu.

---

## 18. Krokový plán (kroky 2–11)

Každý krok: cíl → soubory → akceptační kritéria. Po každém build + testy.

### Krok 2 — Datová vrstva CoinGecko
- Soubory: `core/Result.kt`, `domain/model/*`, `domain/AssetType.kt`,
  `data/remote/coingecko/CoinGeckoApi.kt` + DTO + mappery, jednotky:
  `CoinGeckoMapperTest`.
- Akceptace: Retrofit služba kompiluje; mappery pokryté testy (markets, market_chart,
  ohlc, search) z uložených JSON fixture; žádné síťové volání v testech.

### Krok 3 — Repository, Room, DataStore
- Soubory: `data/local/*` (DB, entity, DAO), `data/prefs/SettingsRepository.kt`,
  `data/repository/MarketRepository.kt`, `WatchlistRepository.kt`,
  `di/AppContainer.kt`, napojení v `ObchodnikApp`.
- Akceptace: DB se vytvoří, watchlist Flow funguje (instrumented nebo Robolectric
  test volitelně), TTL logika otestovaná unit testem (fake DataSource + fake clock).

### Krok 4 — Obrazovka Trh
- Soubory: `ui/components/*` (AssetIcon, Change, Card, Sparkline, Skeleton),
  `ui/markets/*`, `ui/navigation/*`, úprava `MainActivity` na NavHost.
- Akceptace: seznam watchlistu se zobrazí z cache, pull-to-refresh volá repo,
  kategorie taby filtrují, prázdný stav. Ozubené kolo v TopBaru (zatím naviguje
  na prázdné Nastavení).

### Krok 5 — Vyhledávání a přidávání mincí
- Soubory: `ui/search/*`, rozšíření `WatchlistRepository` (add/remove/reorder),
  statický katalog ne-krypto aktiv (`assets_catalog`).
- Akceptace: debounced search proti CoinGecko, star toggle přidá/odebere aktivum,
  projeví se v Trhu.

### Krok 6 — Detail aktiva s grafem
- Soubory: `ui/components/PriceChart.kt`, `ui/detail/*`, historie/candles v repo.
- Akceptace: graf čára i svíčky, přepínání rozsahů překreslí, stats grid,
  Sledovat/Alert tlačítka funkční (alert otevře sheet z kroku 10 nebo placeholder).
- Stav: hotovo. Alert akce je zatím vizuální/placeholder podle plánu pro krok 10.

### Krok 7 — Nastavení (ozubené kolo)
- Soubory: `ui/settings/*`, napojení DataStore na téma/akcent/měnu/interval/klíče,
  aplikace tématu globálně přes `ObchodnikTheme(theme, accent)` z nastavení.
- Akceptace: změna tématu/akcentu/měny se okamžitě projeví; interval se uloží;
  klíče se uloží a použijí v hlavičkách/queries.
- Stav: hotovo.

### Krok 8 — Widget v Jetpack Glance
- Soubory: `widget/*`, `work/RefreshWorker.kt`, registrace receiveru v manifestu,
  `xml/obchodnik_widget_info.xml`.
- Akceptace: widget jde přidat na plochu ve 3 velikostech, čte z Room cache,
  tap otevře app, konfigurace vybere aktiva, RefreshWorker aktualizuje dle intervalu.

### Krok 9 — Druhý zdroj dat (Alpha Vantage)
- Soubory: `data/remote/alphavantage/*`, `data/source/CommodityDataSource.kt`,
  `DataSourceRouter.kt`, `CurrencyRateProvider`, rate-limit počítadlo v DataStore.
- Akceptace: komodity (WTI/Brent/…) a indexy (SPY/QQQ/DIA) se zobrazí v Trhu i
  detailu; respektován denní limit (cache 6–12 h); rate-limit odpověď nezpůsobí pád.

### Krok 10 — Portfolio, Alerty, F&G, Onboarding
- Soubory: `ui/portfolio/*`, `ui/alerts/*`, `ui/fng/*`, `ui/onboarding/*`,
  `data/repository/PortfolioRepository.kt`, `AlertRepository.kt`, `FngRepository.kt`,
  notifikace v `RefreshWorker`, `ui/components/Gauge.kt`.
- Akceptace: portfolio P/L počítá správně (unit test), alerty se vyhodnotí a
  pošlou notifikaci, F&G gauge + historie, onboarding flow při prvním spuštění.

### Krok 11 — Závěrečná kontrola a předání
- Soubory: doladění, `release` proguard ověření, aktualizace README/PROGRESS.
- Akceptace: `./gradlew assembleRelease` projde; manuální průchod všech obrazovek;
  lint bez chyb (warnings ok); aktualizovaný PROGRESS.

---

## 19. Testovací strategie

- **Unit (JUnit)**: mappery (DTO→doména), formátování ceny/procent, TTL a
  rate-limit logika (fake clock), výpočet portfolia P/L, vyhodnocení alertů.
- JSON fixtures ulož do `app/src/test/resources/`.
- **Instrumented (volitelně)**: Room DAO, jeden smoke Compose test obrazovky Trh.
- Síť se v testech nikdy nevolá (fake `MarketDataSource`).

---

## 20. Klíčová rozhodnutí a rizika

- **CoinGecko nepokrývá reálné komodity/indexy** → druhý zdroj Alpha Vantage,
  indexy jen přes ETF zástupce (označit v UI). Kovy primárně tokenizované.
- **Alpha Vantage 25 req/den** je tvrdý limit → agresivní cache, žádné volání při
  každém otevření, denní počítadlo. Bez toho appka „oslepne".
- **WorkManager min 15 min** → kratší interval nelze periodicky; UI to musí
  komunikovat, doplnit refresh při otevření a interakci s widgetem.
- **Glance omezené UI** → sparkline ve widgetu řešit přes bitmapu; mít textový
  fallback.
- **Fonty**: Hanken Grotesk + JetBrains Mono přes Downloadable Fonts; certs v
  `res/values/font_certs.xml` (oficiální Google sample). Vyžadují Play Services,
  jinak fallback na systémový font.
- **API klíče v DataStore**: pro osobní appku OK; nejde o sdílené tajemství.

---

## 21. Reference (ověřeno 2026-06)

- CoinGecko autentizace: https://docs.coingecko.com/v3.0.1/reference/authentication
- CoinGecko ceník/limity: https://www.coingecko.com/en/api/pricing
- CoinGecko market_chart: https://docs.coingecko.com/reference/coins-id-market-chart
- CoinGecko tokenizované komodity: https://www.coingecko.com/learn/track-weekend-gold-price-tokenized-commodities
- Alpha Vantage dokumentace: https://www.alphavantage.co/documentation/
- Alpha Vantage limity: https://www.macroption.com/alpha-vantage-api-limits/
- Fear & Greed (alternative.me): https://api.alternative.me/fng/
- Jetpack Glance: https://developer.android.com/jetpack/androidx/releases/glance
- Compose ↔ Kotlin kompatibilita: https://developer.android.com/jetpack/androidx/releases/compose-kotlin
