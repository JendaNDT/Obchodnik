# ROADMAP — Obchodník

Verze 1.0 (kroky 1–11) hotová. Pokračování v Coworku — viz „Vylepšení po 1.0" níže.

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

## Vylepšení po 1.0 (pokračování v Coworku)

12. ✅ Room: export schémat + verzované migrace (konec destruktivní migrace)
13. ✅ Git + GitHub repo (JendaNDT/Obchodnik)
14. ✅ Záloha dat: export/import do JSON (watchlist, portfolio, alerty, nastavení)
15. ✅ Bezpečnost: šifrování API klíčů přes Android Keystore
16. ✅ Detail + Trh: stáří dat, zdroj, „≈ ETF" poznámky/badge
17. ✅ Přístupnost: contentDescription pro grafy/gauge/sparkline
18. ✅ Widget: tap na řádek → detail, tlačítko ručního refreshe
19. ✅ Fonty: Hanken Grotesk + JetBrains Mono přes Downloadable Fonts
20. ✅ Záloha dat: bezpečnější export bez API klíčů ve výchozím stavu, import preview
21. ✅ Alerty: historie spuštění včetně ceny a měny, možnost znovu aktivovat
22. ✅ Stáří dat: cache fallback a upozornění při limitu, offline stavu nebo chybějícím Alpha Vantage klíči
23. ✅ Portfolio: editace existujících pozic
24. ✅ Trh: ruční řazení watchlistu
25. ✅ Widget: režimy Vyvážený, Ceny a Grafy
26. ✅ Detail + Trh: klikací vysvětlení ETF/komoditních aproximací
27. ✅ Portfolio: export pozic a P/L do CSV přes systémový výběr souboru
28. ✅ Trh: filtrování watchlistu a řazení podle růstu, poklesu, názvu nebo ceny
29. ✅ Detail: SMA 7 a SMA 30 indikátory nad line grafem
30. ✅ Detail: vytvoření cenového alertu přímo z detailu aktiva
31. ✅ Alerty: rychlé šablony cílové ceny +5 %, +10 %, -5 %, -10 %
32. ✅ Trh: vestavěné rychlé pohledy Ruční, Roste, Padá, Krypto, Indexy
33. ✅ Trh: uložené vlastní pohledy watchlistu (uložení/aplikace/mazání, perzistence v DataStore)
34. ✅ Alerty: opakované alerty s hysterezí (přepínač Opakovat, re-arm po návratu ceny; DB v4)
35. ✅ Portfolio: graf vývoje hodnoty v čase (denní snapshoty, rozsahy 1T/1M/1R/VŠE; DB v5)

Nápady do budoucna: žádné otevřené — backlog je hotový.

Mimo scope (rozhodnutí 2026-06-07): Sentry crash reporting — záměrně vynecháno.
