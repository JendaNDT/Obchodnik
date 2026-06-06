# AGENTS.md — kontrakt pro práci na projektu Obchodník

## Spolupráce s uživatelem

Uživatel (Jenda) neprogramuje — má nápady, kód píše navazující AI agent
(aktuálně Gemini AI od Google). Platí:

- **Diskuze před kódem.** Nikdy nepiš/needituj soubory bez popisu plánu a
  explicitního schválení („piš" / „ok" / „pokračuj").
- **Po malých krocích.** Jeden krok = jedna ucelená věc, pak shrnutí a čekání
  na další „piš". Žádné velké dávky.
- **Ověřuj fakta.** Verze knihoven, API endpointy, limity — z webu nebo
  z kódu, ne z hlavy. Když si nejsi jistý, řekni to.
- **Bez emojis** v kódu, UI textech ani commitech.
- **Jazyk:** konverzace a UI stringy česky; kód, názvy, komentáře, commity
  anglicky.

## Stack a konvence

- Kotlin, Jetpack Compose (UI), **Jetpack Glance** (widget — pevný požadavek).
- Síť: Retrofit + OkHttp + kotlinx.serialization.
- Perzistence: Room (watchlist, holdings, alerts) + DataStore (nastavení).
- Widget refresh: WorkManager dle uživatelského intervalu.
- DI: ruční container v `ObchodnikApp` (žádný Hilt — méně pohyblivých částí).
- Package root: `cz.obchodnik`. Project/Gradle name: `Obchodnik` (ASCII).
  Zobrazované jméno appky: „Obchodník" (v `strings.xml`).
- Verze řízené přes `gradle/libs.versions.toml` (version catalog).
- Min SDK 26, target/compile SDK 35. JDK 17.
- Aktuální stav projektu: všech 11 kroků je hotových, zkompilováno a
  otestováno (2026-06-06). Detaily jsou v `PROGRESS.md` a `GEMINI_HANDOFF.md`.

## Design tokeny

Z handoff balíčku. 3 tmavá témata (Terminal výchozí, Aurora, Mono) + 4 akcenty
(modrá výchozí). Tokeny v `ui/theme/Color.kt` + `Theme.kt`, přístup přes
`Obchodnik.colors` / `Obchodnik.radii`. Sémantické barvy: up `#16C784`,
down `#EA3943`. Čísla v monospace (zabraňuje poskakování číslic).

## Data — důležité ověřené fakty

- CoinGecko = krypto plně; kovy jen tokenizované (PAXG, XAUT) + XAU/XAG
  reference. **Reálné komodity (ropa, plyn, měď, pšenice) a akciové indexy
  (SPX, NDX) v CoinGecko NEJSOU** — řeší druhé API (Alpha Vantage), indexy
  přes ETF zástupce (SPY/QQQ/DIA).
- CoinGecko Demo: 100/min, 10k/měsíc. Alpha Vantage free: 25/den, 5/min →
  komodity tvrdě cachovat.

## Gotchas (placené lekce — nepřepisovat znovu)

- Gradle wrapper je funkční včetně `gradle-wrapper.jar`.
- `local.properties` je lokální soubor s cestou k Android SDK
  (`/Users/jenda/Library/Android/sdk`) a patří do `.gitignore`.
- Poslední ověření proběhlo příkazem
  `./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon`.
- Fonty: zatím vestavěné rodiny; Google Fonts certs se nevymýšlejí ručně
  (viz `Type.kt`).
- Room databáze je ve vývojové fázi na verzi 2 a používá
  `fallbackToDestructiveMigration()`. Před produkcí nahradit skutečnými migracemi.
