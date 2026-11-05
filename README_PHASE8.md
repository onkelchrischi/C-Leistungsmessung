# Phase 8 – FTDI-Performance und Live-Darstellung

## Geändert
- FTDI-Empfang und AD7765-Board-Dekodierung laufen in einem Hintergrundtask; WPF-UI ist unabhängig.
- FTDI-Queue wird ohne den bisherigen pauschalen 100-ms-Wartezyklus ausgelesen, in maximal 64-KiB-Blöcken; bei Leerlauf 2 ms Pause.
- Anzeige wird unabhängig durch `DispatcherTimer` alle 50 ms aktualisiert (Ziel 20 FPS). Empfangene Messwerte werden vollständig für Statistik berechnet, aber Anzeige verwendet nur das aktuelle gleitende Fenster.
- Live-Datenpuffer begrenzt, Batch-Trimming anstatt RemoveRange nach jedem Frame.
- Status zeigt Durchsatz KiB/s, FTDI-Warteschlange, Renderdauer und Samplezähler.
- Verbindungsabbau wartet asynchron auf Empfangstask, um gleichzeitiges FT_Read/FT_Close zu vermeiden.
- Der Import von RDF/CSV, die passive Messdatenanzeige und die bestehenden Kalibrieranzeige-Funktionen bleiben erhalten.

## Sicherheit / Grenzen
- Es werden weiterhin **keine** Hardwaremessbefehle gesendet.
- Änderungen am Kalibriersystem sind ausdrücklich auf später verschoben.
- Kein vollständiger Rohdatenrekorder: der Debug-Mitschnitt ist nach wie vor auf 32 MiB begrenzt.
- Der empfangsseitige FTDI-Treiberpuffer kann trotz schneller Software überlaufen, wenn das System überlastet ist. In dem Fall Diagnose prüfen.
- Diagrammwerte sind weiterhin **ADC-Rohwerte**.
- **Kein Windows-Build hier möglich** (dotnet SDK nicht installiert); bitte in Visual Studio kompilieren und am Board testen.

## Testvorschlag
1. Java beenden und Visual-Studio-Solution starten.
2. UM232H PMPP_DEV2 verbinden, Board-Modus aktiv lassen und Beobachtung starten.
3. Spannung am Netzteil sprunghaft variieren, Reaktionsverhalten mit Java vergleichen.
4. Im Diagnose-Text FTDI-Puffer, Empfangsrate, Darstellung und Frames prüfen.
5. Stoppen, erneut starten sowie Trennen beim Empfang testen.

## Relevante Datei
`src/Leistungsmessung.Desktop/MainWindow.xaml.cs`
