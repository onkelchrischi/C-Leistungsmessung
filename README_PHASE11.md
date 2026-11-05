# Leistungsmessung – Phase 11 (Oberfläche und Verlauf)

## Neu
- Drei Tabs: **Messung**, **Kalibrierung**, **Programmeinstellung**.
- Zwei getrennte Kalibrierpunkte je ausgewähltem Kanal (Strom 1, Strom 2, Spannung); andere Kanäle bleiben unverändert.
- Empfang/Start/Stopp, Geräteerkennung, FTDI-Status und ursprünglicher Rohdatenmitschnitt aus Phase 10 übernommen.
- Rohwerte werden weiter gespeichert und für Kalibrierung verwendet, aber ohne Kalibrierprofil nicht als Live-Messwerte angezeigt.
- Aktuelle Werte mit Strom, Spannung, Leistung (I × U) und Arbeit (Integral P dt). Java `Power.getI()` liefert A und `Power.getU()` V.
- Strom µA/mA, Spannung mV/V, Achsen mit drei Dezimalstellen; feste Achsen werden in der gewählten Einheit eingegeben.
- Einstellbare Mittelwertfrequenz 10–1000 Hz (Zeitfenster default 5s). **Dies ist die Mittelwert-/Datenrate, nicht eine zugesicherte WPF-Bildwiederholrate.**
- Horizontale Scrollbar zum Anzeigen bereits erfasster Daten (auch nach Messende), Mausziehen zum Verschieben, Mausrad bzw. +/− zum Zoomen.
- Zweite temporäre BIN-Datei speichert die *kalibrierten, gemittelten* Verlaufspunkte als Folge von `double timestamp, double currentA, double voltageV` (je 24 Byte, little-endian .NET). Zeitbasierte Binärsuche: RAM hält nur die gewählte Ansicht.
- Datenrate/Frames/CRC unten in Statuszeile, Detaildaten im Programmeinstellungs-Tab.
- Kleinere Schriftgrößen im Vollbild.
- wörtliche `\\n` in `MainWindow.xaml.cs` entfernt / nicht vorhanden.

## Offene Punkte und Grenzen
- Die tatsächliche Windows-Kompilierung und die Hardwarevalidierung konnten in der bereitgestellten Umgebung nicht ausgeführt werden.
- Strom 1 ist gegenwärtig an das ADC-Bereichsbit 1, Strom 2 an Bereichsbit 2 gebunden (Java-Decoder: Bit 1 -> B2 und Bit 2 -> B1). Diese Benennung muss anhand der tatsächlichen beiden Strombereiche der Anlage bestätigt werden; Bereichsbit 4 bleibt aus dem bestehenden Profil übernommen.
- Im Kalibrierungsdialog müssen Stromreferenzwerte in **A**, Spannungsreferenzen in **V** eingegeben werden.
- Die Referenzwertumrechnung und die Leistungs-/Arbeitsberechnung müssen mit der ursprünglichen Java-Anwendung abgeglichen werden, insbesondere bei Bereichswechseln.
- Die 10–1000 Hz sind ein Mittelungsraster auf Basis der PC-Empfangszeit. Es ersetzt keine ADC-Zeitstempel und garantiert keine WPF-Renderingrate von 1000 FPS.
- Änderungen an Frequenz oder Zeitfenster während laufender Messung setzen den aktuellen Live-Ringpuffer zurück. Bereits auf Datenträger gespeicherte historische Punkte behalten ihre alte Rasterung.
- Die History-Datei wird noch nicht als dauerhafter Messdatei-Export bereitgestellt. Der existierende Button „Rohdaten speichern“ kopiert die ursprünglichen FTDI-Bytes.
- Bei hoher Mittelungsrate/History-Zoom muss das Datei-/Renderverhalten unter Last getestet werden.
- Zoom bei Dateiansichten (RDF/CSV) wird noch nicht vollständig an den Verlaufsscroller gekoppelt.
- Kalibrierung als Zweipunktverfahren, nicht die vollständige Java-Mehpunktprozedur.

## Testablauf
1. `Leistungsmessung.sln` in VS öffnen; Solution **neu erstellen**.
2. Gerät automatisch erkennen lassen. Kalibrierdatei laden.
3. Messung starten und Spannung ändern: Werte, Kurven, Achsen prüfen.
4. Messung stoppen und Verlauf mit Scrollbar und Mausrad prüfen.
5. In Programmeinstellung Frequenz 10, 50 und 1000 Hz ausprobieren; feste Y-Achsen und µA/mA/mV/V prüfen.
6. Kalibrierung einzeln für Strom 1, Strom 2 und Spannung testen; ungewählte Koeffizienten dürfen sich nicht ändern.
7. In Programmeinstellung Frame-Zähler und CRC-Fehler beobachten.
