# PROGRESS — snapshot

Datum: 2026-06-06

## Hotovo
- Krok 1: Kostra projektu.
  - Gradle: version catalog (`gradle/libs.versions.toml`), root + app build
    skripty, settings, gradle.properties, wrapper properties, `gradlew` skripty
    a `gradle-wrapper.jar`.
  - AndroidManifest, `ObchodnikApp` (Application), `MainActivity` se spustitelnou
    úvodní obrazovkou (branding + tlačítko Nastavení vpravo nahoře).
  - Téma: `Color.kt` (3 témata + akcenty + up/down), `Theme.kt` (tokeny,
    radii, CompositionLocal, `ObchodnikTheme`), `Type.kt` (typografie).
  - Resources: strings (cs), colors, themes.xml, adaptivní launcher ikona.
  - Dokumentace: README, AGENTS, ROADMAP, PROGRESS.
  - Projektová hygiena: `.gitignore`, `gradle-wrapper.jar`, opravené wrapper
    skripty, lokální `local.properties` s cestou k Android SDK.
  - Design handoff `/Users/jenda/Downloads/CryptoWidget.zip` prostudován a
    zařazen jako vizuální reference; technická pravidla dál řídí
    `IMPLEMENTATION_PLAN.md`.
- Krok 2: Datová vrstva CoinGecko.
  - Ověřen veřejný CoinGecko base URL `https://api.coingecko.com/api/v3` a
    endpointy `coins/markets`, `coins/{id}/market_chart`, `coins/{id}/ohlc`
    a `search` proti oficiální dokumentaci i reálným keyless dotazům.
  - Přidány doménové modely `Asset`, `Quote`, `PricePoint`, `Candle`,
    `ChartRange`, `AssetType`, `DataProvider` a obecný `core.Result`.
  - Přidán `CoinGeckoApi` Retrofit kontrakt, DTO přes kotlinx.serialization a
    mappery DTO → doména.
  - Přidány JSON fixtures a unit testy mapperů bez síťového volání.
- Krok 3: Repozitář + Room + DataStore.
  - Přidána Room databáze `ObchodnikDatabase` s entitami pro assets, quotes,
    history, holdings a alerts.
  - Přidány DAO/local store kontrakty pro watchlist, quote cache, historii,
    portfolio a alerty.
  - Přidán `SettingsRepository` nad DataStore pro téma, akcent, měnu, interval,
    klíče, onboarding a notifikace.
  - Přidána repository vrstva: `MarketRepository`, `WatchlistRepository`,
    `PortfolioRepository`, `AlertRepository`.
  - Přidán `MarketDataSource` a `CryptoDataSource` nad CoinGecko API.
  - Přidán ruční DI container `AppContainer` a napojen do `ObchodnikApp`.
  - Přidány unit testy pro TTL cache chování s fake data source/fake clock a
    základní watchlist logiku.
- Krok 4: Obrazovka Trh.
  - `MainActivity` přepnuta na Navigation Compose s výchozí routou `markets`
    a placeholder routou `settings`.
  - Přidán `MarketsViewModel`, který založí defaultní watchlist, čte watchlist
    a quote cache z repository vrstvy a refreshuje ceny přes CoinGecko.
  - Přidány UI komponenty `AssetIcon`, `Change`, `ObchodnikCard`, `Sparkline`,
    `SkeletonBlock` a formátování cen/procent pro české UI.
  - Přidána obrazovka Trh: top bar s hledáním/alerty/refreshem/nastavením,
    F&G placeholder karta, category tabs, watchlist řádky se sparkline,
    prázdný stav, chybový strip a přidávací CTA.
  - Přidán jednoduchý pull-to-refresh gesture na začátku seznamu; volá
    `MarketRepository.refreshQuotes`.
  - Opraveno zachování watchlist stavu při refreshi quote cache.
- Krok 5: Vyhledávání a přidávání libovolných mincí.
  - Přidána route `search` do Navigation Compose a napojení z lupy i CTA v Trhu.
  - Přidán `SearchViewModel` s debounce vyhledáváním přes CoinGecko search API.
  - Přidána `SearchScreen` se search barem, sekcí CoinGecko výsledků, katalogem
    doporučených aktiv a star toggle pro přidání/odebrání z watchlistu.
  - Přidán `StaticAssetCatalog` pro populární krypto, tokenizované kovy,
    komodity a index ETF zástupce dle handoffu.
  - Přidání CoinGecko aktiva do watchlistu rovnou refreshne jeho quote cache.
  - Komodity a indexy lze přidat do watchlistu už teď; ceny dostanou až v kroku
    9 po napojení Alpha Vantage.
- Krok 6: Detail aktiva s grafem.
  - Přidána route `detail/{assetId}` a napojení otevření detailu z obrazovek Trh
    i Vyhledávání.
  - Přidán `DetailViewModel`, `DetailScreen` a grafické komponenty
    `LinePriceChart` / `CandlePriceChart`.
  - Detail zobrazuje velkou cenu, denní změnu, přepínač Křivka/Svíčky, rozsahy
    1D/1T/1M/1R/VŠE, stats grid a akce Alert/Sledovat.
  - `MarketRepository` nově umí načíst a cachovat historii i OHLC svíčky přes
    Room `HistoryEntity`; cache rozlišuje `range`, `currency` a `kind`.
  - Databáze byla posunuta na verzi 2 a v této vývojové fázi používá
    destructive migration.
  - Detail používá uložené kandidáty z vyhledávání i statický katalog, takže jde
    otevřít i aktivum, které ještě není ve watchlistu.
  - Ne-CoinGecko aktiva v detailu zatím vrací českou chybovou hlášku, že data pro
    tento zdroj nejsou dostupná; reálná data přijdou v kroku 9.
- Krok 7: Nastavení.
  - Vytvořena plnohodnotná obrazovka `SettingsScreen` (témata Terminal/Aurora/Mono, 4 akcentní barvy, měna USD/CZK, interval aktualizace na pozadí 15/30/60/180/360 min/Jen při otevření, výchozí typ grafu, hustota zobrazení, přepínač Fear & Greed pro widget, API klíče a přepínač notifikací).
  - Vytvořen `SettingsViewModel` a `SettingsUiState` pro řízení a reakci na změny nastavení.
  - Upraven `SettingsRepository` pro podporu hodnoty `0` (vypnutí aktualizace na pozadí).
  - `MainActivity` nově dynamicky odebírá nastavení a aplikuje zvolené téma a akcent globálně na celou aplikaci pomocí `ObchodnikTheme(theme, accent)`.
- Krok 8: Widget v Jetpack Glance.
  - Vytvořen initial layout `obchodnik_widget_initial.xml` pro spolehlivou inicializaci RemoteViews.
  - Vytvořen metadata soubor `obchodnik_widget_info.xml` s definicí konfigurace, min/výchozích rozměrů a responzivního chování.
  - Zaregistrován `ObchodnikWidgetReceiver` a `WidgetConfigActivity` v `AndroidManifest.xml`.
  - Vytvořen samotný widget `ObchodnikWidget.kt` podporující 3 responzivní velikosti (Small 2x2, Medium 4x2, Large 4x4).
  - Vykreslování sparkline grafů a F&G ukazatele (gauge) vyřešeno dynamickým kreslením na `Bitmap` přes standardní Android `Canvas` (obejití RemoteViews limitů).
  - Vytvořena konfigurace `WidgetConfigActivity.kt` (Compose), umožňující vybrat až 5 sledovaných aktiv a přepínač F&G indexu.
  - Implementován `RefreshWorker.kt` na pozadí, který stahuje data z CoinGecko, ukládá do Room a vynucuje překreslení widgetu.
  - Napojeno periodické stahování přes `WorkScheduler.kt` s respektováním intervalu nastavení (včetně vypnutí na 0 min a minima 15 min).
  - Upraven `SettingsViewModel` a testy pro spolehlivé přeplánování na pozadí bez pádů v čistých JVM unit testech.
- Krok 9: Druhý zdroj dat (Alpha Vantage).
  - Rozšířeno rozhraní `CoinGeckoApi.kt` o metodu `simplePrice` pro získání live kurzu USD/CZK.
  - Vytvořeny DTO třídy `AlphaVantageDtos.kt` pro indexy a komodity včetně zachycování rate-limit odpovědí.
  - Implementován mapper `AlphaVantageMapper.kt` mapující na Quote, PricePoint a Candle modely a filtrující historická data podle zadaného range.
  - Vytvořen `CurrencyRateProvider.kt` stahující a cachující kurz USD/CZK na 24 hodin do DataStore s automatickým fallbackem.
  - Vytvořen `CommodityDataSource.kt` stahující data z Alpha Vantage, podporující fallback na commodity candles, a hlídající denní limit 25 požadavků.
  - Vytvořen asynchronní `DataSourceRouter.kt` paralelně odesílající dotazy do CoinGecko a Alpha Vantage.
  - Propojena nová vrstva v `AppContainer.kt` a injektován `DataSourceRouter` jako jednotné tržní API do `MarketRepository`.
  - Vytvořeny rozsáhlé unit testy pro mapper, provider a data source.
- Krok 10: Portfolio, Alerty, Fear & Greed detail a Onboarding.
  - Vytvořen 4-krokový uvítací průvodce `OnboardingScreen` a `OnboardingViewModel` (uvítání, výběr watchlistu, žádost o systémové povolení notifikací, dokončení flow a uložení stavu).
  - Vytvořena obrazovka `PortfolioScreen` a `PortfolioViewModel` (souhrnná karta s celkovou hodnotou portfolia v aktivní měně, absolutní a procentuální zisk/ztráta, segmentový allocation bar podílů aktiv, bottom sheet pro zadávání nákupních pozic a smazání pozic).
  - Vytvořena obrazovka `AlertsScreen` a `AlertsViewModel` pro správu cenových upozornění (Nad / Pod cílovou cenu, přepínač zapnutí/vypnutí, smazání alertu).
  - Implementováno vyhodnocování cenových alertů na pozadí uvnitř `RefreshWorker.kt` (automatické načítání cen z CoinGecko/Alpha Vantage pro sledovaná aktiva i aktiva s aktivním alertem, spuštění upozornění, automatická deaktivace a odeslání lokální push notifikace přes vysokoprioritní kanál `"cenove_alerty"`).
  - Vytvořena obrazovka `FngScreen` a `FngViewModel` (půlkruhový gauge ukazatel s ručičkou na Compose Canvas, 30-denní spojnicový area graf vývoje indexu a přehledová tabulka historických hodnot).
  - Upravena `MainActivity.kt` pro integraci spodního navigačního panelu `BottomNavigationBar` (4 záložky: Trh, Portfolio, F&G, Alerty) na top-level obrazovkách, a ochranu navigačního stromu před dokončením onboardingu.
  - Doplněny unit testy pro výpočetní logiku portfolia (`PortfolioViewModelTest`) a správu alertů (`AlertsViewModelTest`).

## Stav
- Verze: AGP 8.7.0, Kotlin 2.0.21, Compose BOM 2024.10.01, Glance 1.1.0,
  Gradle 8.9. Min SDK 26 / target 35.
- Lokální Gradle wrapper je funkční.
- Android SDK Platform 35 byla při prvním buildu doinstalována do lokálního SDK.
- `./gradlew :app:assembleDebug --no-daemon` prošlo.
- `./gradlew :app:testDebugUnitTest --no-daemon` prošlo.
- Kombinované ověření
  `./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon` prošlo
  2026-06-06 po dokončení Kroků 10 a 11.
- Room DB je verze 2 s exportem schémat (`app/schemas`) a verzovanými
  migracemi (`ObchodnikDatabase.MIGRATIONS`); destruktivní migrace jen pro
  downgrade.

## Předávka pro Gemini
- Projekt je plně implementován, otestován a zkompilován. Všechny požadavky zadání byly splněny.

## Další krok
- Hotovo! Projekt předán uživateli k finálnímu testování v Android Studiu / emulátoru.

## Otevřené otázky
- Žádné, projekt je dokončen.
