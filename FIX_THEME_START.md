# Phase 15 – Fix: Farbwechsel und maximierter Start

- `ThemeChanged` erzeugt neue `SolidColorBrush`-Instanzen, statt eingefrorene WPF-Ressourcen zu modifizieren.
- Der Fensterhintergrund bindet `BackgroundBrush` über `DynamicResource`, um den neuen Brush zu übernehmen.
- `WindowState="Maximized"` startet die Anwendung maximiert (Windows-Titelleiste bleibt erhalten).
- Quellstand beruht auf Phase 15; vollständiger Windows-Build wurde in dieser Umgebung nicht ausgeführt.

Zur Installation: ZIP in **neuen Ordner** entpacken und `Leistungsmessung.sln` öffnen; anschließend Lösung neu erstellen.
