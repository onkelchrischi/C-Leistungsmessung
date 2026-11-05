# Phase 14 – gezielte Fehlerkorrekturen

## Änderungen
- Die Navigation ist oberhalb des Hauptframes (TabStripPlacement=Top).
- Der historische Kalibrierverlauf wird vor asynchronen Lesezugriffen geflusht.
- Der Mittelwert-Verlaufsstream wird zusätzlich etwa alle 64 Mittelwertpunkte synchronisiert.
- Pan- und Zoom-Grenzen verwenden die tatsächlich aufgezeichnete Zeit statt ausschließlich die PC-Stoppuhr.
- Beim Auslösen von RenderChart bleibt die letzte Kurve sichtbar, bis die nachgeladene historische Ansicht eintrifft.
- Leer-Anzeige erklärt jetzt fehlende Werte im gewählten Zeitabschnitt.
- Timeout der Stromkalibrierung meldet sowohl den erwarteten als auch den zuletzt gesehenen Bereichscode.

## Hinweise
- Die aktuelle Zuordnung Strom 1 → ADC-Code 1, Strom 2 → ADC-Code 4 wurde NICHT ungeprüft geändert.
- Bitte bei einem Kalibrierfehler den gemeldeten *zuletzt beobachteten ADC-Bereichscode* mitteilen.
- Bitte Pan/Zoom auf denselben Messdaten wie beim Screenshot prüfen und berichten, ob beim Verschieben die Kurve sichtbar bleibt.
- Quelltext wurde statisch bearbeitet und XAML als XML geparst. Windows-Build und Hardwaretest stehen noch aus.
