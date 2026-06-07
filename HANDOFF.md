# HANDOFF — Obchodník

Předávka projektu pro pokračování ve vývoji. Další úpravy pokračují v nástroji
**Antigravity od Google**. (Dříve: Cowork od Anthropic, Gemini a Codex od OpenAI.)

Datum poslední aktualizace: 2026-06-07.

## Krátce

Obchodník je plně funkční nativní Android aplikace (Kotlin, Jetpack Compose +
Jetpack Glance widget) pro sledování cen kryptoměn, tokenizovaných kovů, komodit
a akciových indexů. Verze 1.0 (kroky 1–11) i navazující vylepšení po 1.0 jsou
hotové, zkompilované, otestované a na GitHubu.

- GitHub: https://github.com/JendaNDT/Obchodnik (větev `main`)
- Jazyk UI: čeština. Kód/komentáře/commity: angličtina.

## Co číst jako první

1. `AGENTS.md` — pravidla spolupráce, konvence, jazyk (DŮLEŽITÉ).
2. `ROADMAP.md` — kroky 1–11 + vylepšení po 1.0.
3. `PROGRESS.md` — detailní historie implementace.
4. `IMPLEMENTATION_PLAN.md` — architektura, API kontrakty, datové zdroje.

## Aktuální pracovní strom

Veškeré vylepšení včetně uložených vlastních pohledů watchlistu je commitnuté
a pushnuté na GitHub (větev `main`); pracovní strom je čistý.

## Pravidla spolupráce (shrnutí AGENTS.md)

- Uživatel (Jenda) neprogramuje — má nápady, kód píše AI agent.
- Diskuze před kódem: nic needituj bez popisu plánu a explicitního „piš".
- Po malých krocích; po každém shrnutí a build + unit testy.
- Ověřuj fakta (verze, API) z webu/kódu, ne z hlavy.
- Žádné emoji v kódu, UI textech ani commitech.
- UI stringy česky, kód anglicky.

## Build a ověření

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

Android SDK je v `local.properties` (mimo git). Verze: AGP 8.7.0, Kotlin 2.0.21,
Compose BOM 2024.10.01, Glance 1.1.0, Gradle 8.9, minSdk 26 / target 35, JDK 17.

## Stav implementace

Verze 1.0 (kroky 1–11): kostra, CoinGecko datová vrstva, Room + DataStore,
obrazovka Trh, vyhledávání, detail s grafem, Nastavení, Glance widget,
Alpha Vantage (komodity/indexy přes ETF), Portfolio/Alerty/F&G/Onboarding.

Vylepšení po 1.0 (každé samostatný commit na GitHubu):

- Room: export schémat (`app/schemas`) + verzované migrace
  (`ObchodnikDatabase.MIGRATIONS`); destruktivní migrace jen pro downgrade.
- Záloha dat: `data/backup/` (export/import do JSON) + UI „Záloha dat" v
  Nastavení (Storage Access Framework).
- Bezpečnost: `core/crypto/KeystoreCrypto.kt` (AES-256-GCM, Android Keystore);
  `SettingsRepository` šifruje API klíče.
- Detail/Trh: `DataInfoFooter` (stáří dat, zdroj, ETF/komoditní poznámka),
  badge „≈ ETF" u indexů; formátovače `MarketFormatters.time/sourceLabel`.
- Přístupnost: `contentDescription` u grafů/gauge/sparkline (TalkBack).
- Widget: tap na řádek → detail aktiva (`MainActivity.EXTRA_ASSET_ID`,
  `singleTop` + `onNewIntent`), tlačítko „↻ Obnovit" (`RefreshWidgetAction`).
- Fonty: Hanken Grotesk + JetBrains Mono přes Downloadable Fonts (`Type.kt`,
  `res/values/font_certs.xml`).
- Záloha dat: export API klíčů je výchoze vypnutý, import má preview a
  `BackupRepository.import` má unit test orchestrace.
- Alerty: historie spuštění ukládá cenu a měnu; spuštěné alerty lze z UI znovu
  aktivovat.
- Alerty: `AddAlertSheet` je sdílený; Detail aktiva ho otevírá předvyplněný
  aktuálním aktivem a cenou.
- Alerty: `AddAlertSheet` nabízí rychlé šablony `+5 %`, `+10 %`, `-5 %`,
  `-10 %`; výpočet kryje `AlertTargetPresetsTest`.
- Stáří dat: Trh a Detail zobrazují notice při cache fallbacku, limitu Alpha
  Vantage, chybějícím klíči nebo částečně neaktualizovaných datech.
- Detail: line graf umí `SMA 7` a `SMA 30` overlay indikátory; výpočet kryje
  `MovingAverageCalculatorTest`.
- Portfolio: existující pozice lze editovat.
- Portfolio: export pozic a P/L do CSV přes systémový výběr souboru; formát
  kryje `PortfolioCsvExporterTest`.
- Trh: watchlist lze ručně řadit v kategorii „Vše"; navíc má lokální filtr a
  řazení podle růstu, poklesu, názvu nebo ceny. Transformaci kryje
  `MarketListTransformerTest`.
- Trh: vestavěné rychlé pohledy `Ruční`, `Roste`, `Padá`, `Krypto`, `Indexy`
  nastavují existující kategorii/řazení/dotaz; model kryje `MarketQuickViewTest`.
- Trh: uložené vlastní pohledy watchlistu – uložení/aplikace/mazání názvem
  pojmenované kombinace filtr+řazení (`SavedMarketView`), perzistence jako JSON
  v DataStore; kryjí `SavedMarketViewSerializerTest` a `SettingsRepositorySavedViewsTest`.
- Portfolio: graf vývoje hodnoty v čase – denní snapshoty (`portfolio_snapshots`,
  DB v5), zápis z `RefreshWorker` i `PortfolioViewModel` (upsert na den+měnu);
  karta s grafem a rozsahy 1T/1M/1R/VŠE; `PortfolioHistory` kryje `PortfolioHistoryTest`.
  Hodnota v aktuální měně + kód, graf filtruje na aktuální měnu; historie ode dneška.
- Alerty: opakované alerty – přepínač Opakovat v `AddAlertSheet`, chip v seznamu.
  Hystereze přes `armed`: opakovaný alert se po splnění odzbrojí a znovu se ozve
  až po návratu ceny za cíl. Logika v čisté `AlertEvaluator` (`AlertEvaluatorTest`),
  volá ji `RefreshWorker`. DB v4 (`repeating`, `armed`), `MIGRATION_3_4`, `schemas/4.json`.
- Widget: konfigurace má režimy `Vyvážený`, `Ceny` a `Grafy`.
- Detail/Trh: klikací dialog vysvětluje ETF zástupce, denní komoditní data a
  měnový dopočet.

## Klíčové konvence a gotchas

- DI: ruční `AppContainer` (žádný Hilt/Koin), napojený přes `ObchodnikApp`.
- Čísla v monospace (`JetBrainsMono`) kvůli layout jitteru.
- Room: aktuální verze databáze je 5. Změna entity = zvýšit `version` v
  `@Database`, přidat `Migration` do `MIGRATIONS`, commitnout nový
  `app/schemas/<verze>.json`.
- Fonty vyžadují na zařízení Google Play Services; jinak fallback na systémový
  font (žádný pád).
- Záloha: import přidává řádky (ne přepis); export API klíčů je možný jen po
  explicitním zapnutí volby v UI a pak jsou v JSON souboru čitelné.
- Data: CoinGecko = krypto + tokenizované kovy; reálné komodity a indexy přes
  Alpha Vantage (free 25/den → tvrdě cachovat), indexy přes ETF (SPY/QQQ/DIA).

## Nápady do budoucna (nezačato)

Backlog je hotový, žádné otevřené nápady.

## Mimo scope

- Sentry crash reporting — záměrně vynecháno (rozhodnutí 2026-06-07).
