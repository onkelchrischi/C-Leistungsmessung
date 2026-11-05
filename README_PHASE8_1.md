# Phase 8.1 – Live-Anzeige
- 50 Hz Anzeige-Takt (WPF DispatcherTimer 20 ms; unter Last nicht echtzeitgarantiert).
- 20-ms-Mittelwerte aus beim Empfang dekodierten Messwerten.
- Gleitendes 5-s-Fenster (250 Punkte); Fensterdauer über Eingabefeld änderbar.
- Separat auto- oder manuell skalierbare Strom-/Spannungs-Y-Achsen (Rohwerte, keine kalibrierten Einheiten).
- Rohdaten in eine temporäre `.bin`-Datei im Windows-Temp-Verzeichnis; kein 32-MB-Capture-Limit mehr.
- „Mitschnitt speichern“ kopiert die temporäre Datei an den Zielort.
- Kein aktiver Messstart; FTDI wird wie zuvor passiv gelesen.

## Einschränkungen
Zeitstempel für die Mittelwertbildung werden aus der PC-Empfangsuhr bestimmt. USB-Daten kommen häufig paketweise an; zur präzisen zeitlichen Abbildung ist die ADC-Samplerate aus der Board-Konfiguration zu übernehmen. Die 50 Hz sind Anzeigefrequenz und Mittelwertfenster, nicht ADC-Samplerate.
Die binäre Capture-Datei enthält den FTDI-Rohbyte-Stream, keine RDF-Datei. Temporäre Dateien bleiben im Temp-Verzeichnis erhalten, bis ein Aufräumkonzept implementiert ist. Für robuste Langzeitaufzeichnung sind Dateifehlerbehandlung und Backpressure noch weiterzuentwickeln.
Build/Hardwaretest unter Windows konnte hier nicht ausgeführt werden.
