# Phase 15 – Korrekturen aus Phase-14-Testbericht

## Änderungen
- Während laufender Messung sind Pan, Zoom und Sprünge in der Gesamtübersicht deaktiviert; die Echtzeitdarstellung bleibt an der neuesten Messzeit.
- Nach Messende sind Pan, Zoom und Auswahl aus der Übersicht wieder verfügbar.
- Die Übersicht verwendet die tatsächlich aufgezeichnete Dauer statt der PC-Stoppuhr. Das verhindert eine verschobene Fenster-Markierung nach Unterbrechungen.
- Kalibrierassistent: Ja = für Geräte-Seriennummer übernehmen, Nein = eigenes Profil exportieren ohne Aktivierung, Abbrechen = verwerfen. Bestehende Profile werden vor Überschreiben gesichert.
- Einstellungen werden als JSON unter `%LOCALAPPDATA%\Leistungsmessung\display-settings.json` gespeichert und geladen.
- Standardachsen: I = 0 bis 100 mA, U = 0 bis 5 V. Die Einheitenskalierung wird beim Anwenden berücksichtigt.
- Die Debug-Zeile zeigt den letzten beobachteten AD7765-Strombereichscode und die Häufigkeit aller Codes 0–7.

## Noch offen / wichtig
- Der Bereichscode `1` für Strom 1 und `4` für Strom 2 sind aus der bisherigen Implementierung übernommen, **nicht durch Hardwaretests bestätigt**. Beobachte die angezeigten Code-Häufigkeiten während der jeweiligen Bereichswahl. Ein anderes Mapping darf erst nach Bestätigung portiert werden; keine falschen Koeffizienten speichern.
- Wenn beim Experiment der gleiche Code für beide Bereiche erscheint, muss zuerst geklärt werden, ob die Hardware separat umgeschaltet werden muss.
- Die automatische Skalierung kann weiter verbessert werden; Phase 15 fokussiert die berichteten Fehler.
- Windows/.NET-Build und FTDI-Hardwaretest hier nicht ausführbar. Bitte in Visual Studio neu erstellen und testen.
