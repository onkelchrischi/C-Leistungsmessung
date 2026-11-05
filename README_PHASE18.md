# Phase 18 – Gemeinsame Messansicht

## Änderungen
- Der separate Haupt-Tab „Sensoren“ wurde entfernt.
- Der Tab „Strom-/Spannungsmessung“ enthält die Messart-Auswahl `Strom/Spannung messen` oder `Sensoren`.
- Die beiden Messarten verwenden denselben Hauptgraphen, dieselbe Gesamtübersicht und denselben FTDI-Aufzeichnungsweg.
- Je nach Messart ist nur die entsprechende Werkzeugleiste sichtbar.
- Im Sensormodus zeigt der Graph ausschließlich den berechneten Sensorwert. Die Spannungskurve und rechte Y-Achse sind ausgeblendet.
- Die Anzeige des aktuellen Sensorwerts ersetzt im Sensormodus die vier elektrischen Messwertfelder.
- Im Sensorverlauf werden die historisch gespeicherten kalibrierten Strom-/Spannungswerte gemäß Sensorprofil umgerechnet; die Rohdatenaufzeichnung bleibt unverändert.
- Die Übersichtsgraphik wird ebenfalls nach dem Sensorprofil erstellt.
- Sensor hinzufügen, bearbeiten, löschen und Messung starten/stoppen bleiben erhalten.

## Testplan
1. Projekt in Visual Studio unter Windows 11 (x64) neu erstellen.
2. `Strom/Spannung messen` auswählen und die gewohnte Anzeige prüfen.
3. `Sensoren` wählen, Sensorprofil auswählen und Messung starten.
4. Verifizieren, dass **nur** Sensorwert und Sensorgraph erscheinen; keine Strom-/Spannungskurven.
5. Messung stoppen, in der Übersicht pannen und den historischen Sensorverlauf überprüfen.
6. Zu `Strom/Spannung messen` zurückwechseln und beide elektrischen Kurven prüfen.

## Bekannte Grenzen
- Windows-Build und Prüfung an der AD7765-Hardware müssen auf dem Zielrechner erfolgen; das .NET-SDK ist in dieser Umgebung nicht installiert.
- Für Sensorwerte ist weiterhin eine gültige Geräte-Kalibrierung erforderlich.
- Der Messdatei-Importer der ursprünglichen elektrischen Darstellung wurde nicht für einen separaten Sensor-Dateiimport erweitert.
