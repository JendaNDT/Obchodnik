# HANDOFF — Obchodník (pro Cowork / Anthropic)

Předávka projektu Obchodník pro pokračování v nástroji Cowork od Anthropic.

Datum předávky: 2026-06-06.

## Krátce

Obchodník je plně implementovaná nativní Android aplikace v Kotlinu a Jetpack Compose pro sledování kryptoměn, tokenizovaných kovů, reálných komodit a akciových indexů. Projekt je ve stavu: **všechny kroky 1–11 jsou úspěšně dokončeny, zkompilovány a otestovány**.

## Co číst jako první

1. `AGENTS.md` — pravidla spolupráce, konvence, jazyk.
2. `ROADMAP.md` — přehled dokončených kroků.
3. `PROGRESS.md` — detailní historie a popis implementace všech kroků.

## Ověřený stav a sestavení

Všechny unit testy a kompilace debug sestavení byly plně ověřeny příkazem:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

Výsledek: `BUILD SUCCESSFUL` dne 2026-06-06.

- Android SDK je nastaveno v lokálním souboru `local.properties`.
- Aplikace byla úspěšně nasazena a spuštěna na emulátoru `medium_phone` (`emulator-5554`).
- Snímky obrazovky jednotlivých kroků onboardingu, trhu, Fear & Greed detailu a portfolia jsou k dispozici v adresáři konverzace.

## Přehled implementace (Kroky 7–10)

- **Nastavení (Krok 7)**: Kompletní obrazovka nastavení (témata Terminal/Aurora/Mono, 4 akcenty, USD/CZK, interval na pozadí, výchozí typ grafu, API klíče pro CoinGecko a Alpha Vantage, notifikace).
- **Glance Widget (Krok 8)**: Widget pro domovskou obrazovku podporující 3 responzivní velikosti (Small 2x2, Medium 4x2, Large 4x4) s dynamic canvas Bitmap renderováním pro F&G gauge a sparkline grafy. Aktualizován přes WorkManager periodický `RefreshWorker`.
- **Alpha Vantage (Krok 9)**: Integrace druhého zdroje dat pro komodity (BRENT, WTI, NG, atd.) a indexy (SPY, QQQ, DIA) přes asynchronní `DataSourceRouter` a automatický přepočet cen live kurzem USD/CZK.
- **Portfolio & Alerty & Onboarding (Krok 10)**: 
  - Stepper Onboarding (4 kroky) pro první spuštění.
  - Portfolio se statistikami celkového P/L a barevným segmentovým bar allocation.
  - Vyhodnocování cenových alertů v `RefreshWorker` na pozadí s odesíláním push notifikací.
  - Detailní Fear & Greed obrazovka s velkým Compose Canvas gauge ukazatelem a 30-denním spojnicovým area grafem.

## Důležité nově přidané soubory

- `app/src/main/java/cz/obchodnik/ui/onboarding/OnboardingScreen.kt` & `OnboardingViewModel.kt`
- `app/src/main/java/cz/obchodnik/ui/portfolio/PortfolioScreen.kt` & `PortfolioViewModel.kt`
- `app/src/main/java/cz/obchodnik/ui/alerts/AlertsScreen.kt` & `AlertsViewModel.kt`
- `app/src/main/java/cz/obchodnik/ui/fng/FngScreen.kt` & `FngViewModel.kt`
- `app/src/test/java/cz/obchodnik/ui/portfolio/PortfolioViewModelTest.kt`
- `app/src/test/java/cz/obchodnik/ui/alerts/AlertsViewModelTest.kt`

## Pravidla a gotchas k zapamatování

- **Žádné emojis** v kódu, UI textech ani commitech.
- **Jazyk**: konverzace a UI stringy česky; kód, názvy, komentáře anglicky.
- **Čísla**: Vždy monospace písmo (`JetBrainsMono`) pro ceny a procenta, aby se předešlo layout jitteru.
- **DI**: Projekt nepoužívá Hilt ani Koin, nýbrž ruční dependency injection container `AppContainer` napojený přes Application třídu.
- **Room**: Room DB je verze 2 s exportem schémat (`app/schemas`) a verzovanými migracemi (`ObchodnikDatabase.MIGRATIONS`). Destruktivní migrace je povolená jen pro downgrade. Každá změna entity = zvýšit verzi + přidat Migration + commitnout nový schemas/<verze>.json.
