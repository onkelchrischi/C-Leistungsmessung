# Phase 12 – UI und geführte Kalibrierung

## Änderungen
- Kontrastreiche, kompakte Messwertfelder und linke Navigation.
- Kalibrierassistent: Hardwareprüfung anhand 1000 ADC-Samples, Bereichsauswahl Spannung/Strom 1/Strom 2 (einzeln oder kombiniert), zwei Referenzpunkte je Bereich, 1000 Samples pro Punkt, Berechnung und ausdrückliche Speicherbestätigung.
- Kalibrierprofile in %LOCALAPPDATA%/Leistungsmessung/calibrations/calib_<Seriennummer>.properties; vorherige Fassung als .backup.
- Bei Gerätesuche wird das passende gespeicherte Profil automatisch geladen.
- Automatische Achsenskalierung benutzt Min/Max der gesamten laufenden kalibrierten Messung und setzt sich bei neuer Messung zurück. Es werden horizontale Rasterlinien mit gemeinsamen Höhen für beide Achsen angezeigt.
- Mausradzoom und Linksklick-Pan; Zoom-Schaltflächen entfernt.
- Vertikales Fadenkreuz und Wertanzeige über aktueller Live-Kurve.

## Einschränkungen / offene Punkte
- Ein vorhandenes gültiges Ausgangskalibrierprofil ist weiterhin notwendig; eine vollständige Neukalibrierung ohne Basiskoeffizienten muss separat spezifiziert werden.
- Kalibrierung sendet keine Hardwarebefehle zum Umschalten von ADC-Messbereichen; der gewünschte Messbereich muss hardwareseitig aktiv sein.
- Der Maus-Cursor zeigt beim Rückblick im historischen Dateifenster gegenwärtig nur die Zeit, nicht die nachgeladenen Strom- und Spannungswerte.
- Die zugehörigen Kalibrier-Einheiten und deren Gleichheit mit Java sind weiterhin experimentell zu prüfen.
- Die automatische Skalenteilung mit 5 Unterteilungen ist eine Annäherung an 'schöne' Skalenwerte; eine gemeinsame 50 mA / 100 mV Optimierung ist noch nicht vollständig.
- Windows-/Hardware-Build und Systemtest konnten in dieser Umgebung nicht ausgeführt werden.

## Test
1. Phase12 in Visual Studio neu erstellen.
2. AD7765-Datenstrom starten, gültiges Kalibrierprofil laden.
3. Assistent unter Kalibrierung starten, nach Hardwareprüfung Bereiche wählen.
4. Bei jedem Referenzpunkt Sollwert in A bzw V eingeben und 1000 Werte aufnehmen.
5. Neue Koeffizienten vor Übernahme mit Referenzgerät und Java vergleichen.
6. Backup und erneutes automatisches Laden mit der richtigen SN prüfen.
