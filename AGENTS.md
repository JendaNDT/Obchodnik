# AGENTS.md — kontrakt pro práci na projektu Obchodník

## Spolupráce s uživatelem

Uživatel (Jenda) neprogramuje — má nápady, kód píše navazující AI agent.
Aktuální pokračování má probíhat v nástroji Antigravity od Google
(dříve Cowork od Anthropic, Gemini a Codex od OpenAI). Platí:

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
- Aktuální stav projektu: verze 1.0 (kroky 1–11) hotová + rozsáhlá sada
  vylepšení po 1.0 (Room migrace, záloha dat, šifrování klíčů, fonty,
  chytrý widget, přístupnost, stale-data notices, alert history, editace
  portfolia, CSV export, portfolio insights, filtrování/řazení watchlistu,
  režimy widgetu, SMA indikátory, alert z detailu, rychlé alert šablony,
  uložené pohledy watchlistu, opakované alerty s hysterezí a graf vývoje
  hodnoty portfolia).
  Build i unit testy OK. Detaily v `PROGRESS.md` a `HANDOFF.md`.

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
- Poslední ověření proběhlo příkazy
  `./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon` a
  `./gradlew :app:assembleDebugAndroidTest --no-daemon`.
- Fonty: Hanken Grotesk + JetBrains Mono přes Downloadable Fonts (`Type.kt`),
  certs v `res/values/font_certs.xml` (oficiální Google sample, nevymýšlet ručně).
  Vyžadují Google Play Services; jinak fallback na systémové písmo.
- Room má zapnutý export schémat (`room.schemaLocation` -> `app/schemas`,
  `exportSchema = true`) a verzované migrace; aktuální DB verze je 5.
  Destruktivní migrace je povolená jen pro *downgrade*. **Workflow při změně
  entity:** zvyš `version` v `@Database`, přidej `Migration` do
  `ObchodnikDatabase.MIGRATIONS`, commitni nový `app/schemas/<verze>.json`.
