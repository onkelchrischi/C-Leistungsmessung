# Phase 9 – vereinfachte Bedienung und Kalibrierungsentwurf

## Änderungen
- `Messung starten`: öffnet das gewählte FTDI-Gerät und startet **den bisherigen passiven AD7765-Board-Empfang**.
- `Messung stoppen`: beendet den Empfang, wartet auf den Empfangsthread und schließt D2XX.
- Hintergrund-Gerätesuche beim Öffnen und alle fünf Sekunden, solange kein FTDI-Handle offen ist.
- Seriennummer zuletzt genutzten Geräts im Benutzerprofil: `%LOCALAPPDATA%\\Leistungsmessung\\last-device.txt`; erneute Erkennung statt Annahme einer unveränderlichen Gerätenummer.
- Kalibrierungsfenster mit zwei Referenzmesspunkten; erstellt ein *neues* `calib_<serial>_neu.properties`.
- Neue Kalibrierung ist zunächst ein **linearer Entwurf (k2=0)** nur für den aktuell gewählten Strombereich und die Spannung. Andere Strombereiche werden aus dem zuvor geladenen Originalprofil übernommen. Die vollwertige Java-Kalibrierprozedur mit genauer Referenz-/Einheitenprüfung ist **noch nicht portiert**.
- Alle außerhalb von Strings eingeschleusten wörtlichen `\\n` in `MainWindow.xaml.cs` bereinigt.

## Wichtig: Sicherheit
Es werden noch **keine** AD7765-Start-/Stop-Kommandos an die Hardware gesendet: Die Schaltflächen orchestrieren die erprobte D2XX-Verbindung und den passiven Datenempfang. Das Gerät muss wie in Phase 8.1 bereits Daten liefern. Kein automatischer Austausch bestehender Kalibrierdateien.

## Kalibrierung testen
1. Programm öffnen, automatische Gerätesuche abwarten.
2. Bisheriges Profil für `PMPP_DEV2` mit `Kalibrierprofil laden` öffnen (`calibration-reference/calib_PMPP_DEV2.properties`).
3. Messung starten, stabile Referenzbedingungen herstellen und `Kalibrieren…` wählen.
4. Für Messpunkt A Strom- und Spannungsreferenz (in den *Legacy-Einheiten* des Profils) eingeben und `Übernehmen` klicken.
5. Messgerät auf Messpunkt B (im **gleichen Strommessbereich**) einstellen, neue Referenzen eingeben, `Übernehmen` klicken.
6. Kalibrierdatei an einem **neuen Pfad** speichern. Vor produktiver Nutzung mit den Referenzwerten der Java-Anwendung vergleichen.

## Noch offen
- Verifizierte Kalibrierung der physikalischen Einheiten und Übernahme der vollständigen Java-Prozedur.
- Richtiger Hardware-Messstart/-stopp (derzeit nur passiver Empfang).
- Dauerlauf, Build und Hardwarevalidierung unter Windows (in dieser Umgebung kein dotnet SDK).
- Langfristige Aufzeichnung: BIN-Stream bleibt im temporären Ordner, bis die Datei explizit exportiert wird.
