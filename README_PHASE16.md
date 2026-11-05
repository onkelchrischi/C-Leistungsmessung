# Phase 16 — Entwicklungsstand

## Neu
- Hauptnavigation: Strom-/Spannungsmessung, Sensoren, Einstellungen; Kalibrierung innerhalb Einstellungen.
- Sensor-Editor: global gespeicherte Sensoren unter `%LOCALAPPDATA%\Leistungsmessung\sensors.json`, linearer Strom-/Spannungs-Messumformer oder direkte PT100/PT500/PT1000-Messung bei bekannter Erregerstromversorgung.
- Vordefinierte PT-Profile sind **Messumformer-Profile 4–20mA mit exemplarischer 0–100 °C Skalierung**, keine gerätespezifische Kalibrierung.
- Separater Sensorgraph mit Anzeige eines Sensors; nur bei vorhandener Geräte-Kalibrierung.
- Separate temporäre Sensor-BIN-Datei mit Records `(double Zeit, double Strom_A, double Spannung_V, double Sensorwert)` neben bisherigen Messdaten. Der aktuelle Sensor wird bei Beginn der Aufzeichnung gewählt.
- Übersicht reagiert auf Drag mit Handzeiger.
- PMPP_DEV2 Strom 1: 10–2500mA, Strom 2: 0–10mA als Beschriftungen im Kalibrierassistenten; **ADC-Bereichscode-Zuordnung noch unverändert und zu verifizieren**.
- Einstellungen zweispaltig gruppiert.

## Bekannte Einschränkungen
- Kein Windows-Build/Hardwaretest in dieser Umgebung möglich.
- Sensorplot nutzt aktuelle gemittelte Anzeige-Werte; keine historische Wiedergabe aus Sensor-BIN eingebunden.
- Die Sensorkurve wird aus live-kalibrierten Messwerten berechnet; Sensorprofile ändern die elektrische Gerätekalibrierung nicht.
- Reine Sensor-Dateien sind bisher Roh-/Analyseformate ohne Import-UI.
- PT-Direktmessung erfordert eine geeignete bekannte Schaltung; 2-/3-/4-Leiterkompensation ist noch nicht implementiert.
- Speichern eines bearbeiteten Sensorprofils während einer aktiven Messung sollte vermieden werden; Sensor-Editor vorzugsweise vor Start öffnen.
- Kein automatisches Umschalten der ADC-Strommessbereiche.
