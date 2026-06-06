# Obchodník

Nativní Android aplikace pro sledování cen kryptoměn, drahých kovů a komodit,
s home-screen widgetem postaveným na **Jetpack Glance**. Aktuální cena, graf
historie, vyhledávání libovolných mincí, uživatelsky nastavitelný interval
aktualizace, moderní tmavý design.

- Jazyk UI: čeština
- Platforma: Android (Jetpack Compose + Glance)
- Data: CoinGecko (krypto + tokenizované kovy), Alpha Vantage (komodity, indexy)
- Stav: všech 11 kroků hotových, zkompilováno a otestováno (2026-06-06)

## Jak projekt otevřít a spustit

1. Nainstaluj **Android Studio** (Ladybug 2024.2.1 nebo novější). Obsahuje
   vestavěné JDK 17, které tento projekt vyžaduje.
2. **File → Open** a vyber složku `Obchodník`.
3. Android Studio spustí Gradle Sync. Stáhne si Gradle 8.9 dle
   `gradle/wrapper/gradle-wrapper.properties`.
   - Gradle wrapper je součástí projektu včetně `gradle-wrapper.jar`.
4. Vyber emulátor nebo připojený telefon a klikni **Run ▶**.

## Ověření z terminálu

Z kořene projektu:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon
```

Poslední ověření: 2026-06-06, `BUILD SUCCESSFUL`.

### API klíče (zdarma, doplníš později)

Aplikace na startu poběží i bez klíčů (krypto přes veřejný tier CoinGecko má
ale nízký limit). Doporučené free klíče:

- **CoinGecko Demo** – 100 req/min, 10k req/měsíc
- **Alpha Vantage** – 25 req/den (na komodity/indexy, tvrdě cachujeme)

Klíče se zadávají v Nastavení aplikace (ozubené kolo vpravo nahoře).
`SettingsRepository` je ukládá do DataStore (`coingeckoKey`, `alphaVantageKey`)
a klienti je používají v hlavičkách/queries.

## Stav projektu

Viz:

- `GEMINI_HANDOFF.md` — stručná předávka pro další AI.
- `PROGRESS.md` — aktuální snapshot hotové práce.
- `ROADMAP.md` — číslované kroky.
- `IMPLEMENTATION_PLAN.md` — detailní architektura a akceptační kritéria.

Hotovo (všech 11 kroků):

- Kostra Android projektu, téma, DI a navigace.
- CoinGecko datová vrstva, mappery a unit testy.
- Room/DataStore repository vrstva.
- Obrazovka Trh.
- Vyhledávání a přidávání aktiv.
- Detail aktiva s line/candle grafem a cache historie.
- Nastavení (téma, akcent, měna, interval, zdroj dat, API klíče, notifikace).
- Widget v Jetpack Glance (3 velikosti, konfigurace, WorkManager refresh).
- Druhý zdroj dat Alpha Vantage (komodity + indexy přes ETF).
- Portfolio, Alerty, Fear & Greed detail, Onboarding, prázdné/chybové stavy.

Otevřené technické dluhy: Room používá `fallbackToDestructiveMigration()`
(před produkcí nahradit migracemi); fonty jsou zatím systémové (viz níže).

## Design handoff

Vizuální reference je v archivu `/Users/jenda/Downloads/CryptoWidget.zip`.
Archiv obsahuje high-fidelity HTML/React prototyp, který slouží jako předloha
pro vzhled obrazovek, widgetů, grafů, prázdných stavů a interakcí.

Prototyp není produkční kód a nepřebírá se přímo. Při rozporu platí:

1. `IMPLEMENTATION_PLAN.md` má přednost pro Android architekturu, API zdroje,
   perzistenci, WorkManager, Jetpack Glance a technická omezení platformy.
2. `CryptoWidget.zip` má přednost pro vizuální styl, rozložení obrazovek,
   spacing, barvy aktiv, grafické komponenty a UX detaily.
3. Zobrazovaný název aplikace v projektu je „Obchodník", i když handoff používá
   název „CryptoWidget".
4. Nastavení je podle aktuálního projektu dostupné ozubeným kolem vpravo nahoře,
   ne jako položka spodní navigace.
5. Periodický refresh widgetu respektuje Android WorkManager minimum 15 minut,
   i když prototyp mluví o kratším intervalu.

## Poznámka k fontům

Design používá Hanken Grotesk + JetBrains Mono. Aplikace zatím běží na vestavěných
fontech (sans/monospace), aby projekt šel sestavit bez přibalených binárek.
Skutečné fonty se přidají jedním klikem přes Android Studio → `res/font` →
Add font → Downloadable Font (vygeneruje i `font_certs.xml`). Detail v
`app/src/main/java/cz/obchodnik/ui/theme/Type.kt`.
