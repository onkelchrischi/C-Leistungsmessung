# Phase 17 – Sensoren und Einstellungen

- Einstellungsnavigation schaltet jeweils einen von fünf Bereichen sichtbar.
- Sensor hinzufügen / bearbeiten sind getrennt; die Bearbeitung verlangt eine Auswahl.
- Sensorstart/-stopp verwendet den bestehenden FTDI-Start-/Stopppfad; keine zweite Geräteverbindung.
- Sensorformeln: `x` = Eingangsgröße in A bzw. V. Erlaubt: Zahlen mit Dezimalpunkt,
  Klammern, +, -, *, / und ^. Beispiel: `(x-0.004)*6250` für 4–20 mA -> 0–100.
  Keine Funktionsaufrufe, Variablen außer x oder beliebiger Programmcode.
- Profile werden wie zuvor global in sensors.json gespeichert.
- Wichtige Einschränkung: Nicht mit Windows/.NET kompiliert oder an Hardware getestet.

## Zu prüfen
1. Einstellungsmenü links wechseln; immer nur eine Gruppe rechts.
2. Sensor hinzufügen, Formelprofil erstellen, Sensor ändern und neu laden.
3. Sensorstart/-stopp mit PMPP_DEV2, Aufnahmezeit und Anzeige beachten.
4. Formelwerte mit bekanntem Referenzwert vergleichen.
