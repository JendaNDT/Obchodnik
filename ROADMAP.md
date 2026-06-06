# ROADMAP — Obchodník

Verze 1.0 (kroky 1–11) hotová. Pokračování v Codexu — viz „Vylepšení po 1.0" níže.

1. ✅ Kostra projektu (Gradle, témata/barvy, MainActivity, ikona, docs)
2. ✅ Datová vrstva CoinGecko (modely, Retrofit, search/ceny/historie, testy)
3. ✅ Repozitář + Room + DataStore (watchlist, cache, nastavení)
4. ✅ Obrazovka Trh (watchlist, F&G karta, kategorie, sparkline řádky)
5. ✅ Vyhledávání a přidávání libovolných mincí
6. ✅ Detail aktiva s grafem (čára + svíčky, časové rozsahy, stats)
7. ✅ Nastavení (tlačítko vpravo nahoře): téma, akcent, měna, interval, zdroj dat
8. ✅ Widget v Jetpack Glance (3 velikosti, konfigurace, WorkManager refresh)
9. ✅ Druhý zdroj dat: Alpha Vantage (komodity + index ETF) do DataSource
10. ✅ Portfolio, Alerty, Fear & Greed detail, Onboarding, prázdné/chybové stavy
11. ✅ Závěrečná kontrola a předání (build pokyny, finální průchod)

Legenda: ✅ hotovo · ⏳ čeká · ✖ přeskočeno

## Vylepšení po 1.0 (pokračování v Codexu)

12. ✅ Room: export schémat + verzované migrace (konec destruktivní migrace)
13. ✅ Git + GitHub repo (JendaNDT/Obchodnik)
14. ✅ Záloha dat: export/import do JSON (watchlist, portfolio, alerty, nastavení)
15. ✅ Bezpečnost: šifrování API klíčů přes Android Keystore
16. ✅ Detail + Trh: stáří dat, zdroj, „≈ ETF" poznámky/badge
17. ✅ Přístupnost: contentDescription pro grafy/gauge/sparkline
18. ✅ Widget: tap na řádek → detail, tlačítko ručního refreshe
19. ✅ Fonty: Hanken Grotesk + JetBrains Mono přes Downloadable Fonts

Nápady do budoucna: instrumentované Room migrační testy; unit test importu zálohy;
Sentry crash reporting (účet + DSN); vyřadit klíče z exportu zálohy.
