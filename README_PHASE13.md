# Phase 13 – UI- und Echtzeitdiagramm-Optimierung

Änderungen:
- Durchschnittlich ein Zeichenpunkt pro horizontalem Bildpixel im sichtbaren Fenster
- Historisches Laden mittels Dateistream im Hintergrund mit Pixel-Buckets; kein vollständiges Laden der Messhistorie in den RAM
- Maus-Fadenkreuz mit Binärsuche im bereits aufbereiteten Chartcache (ohne Datei- oder Messpufferzugriff)
- Zoom mit Mausrad um Mauszeit, Pan mit linker Maustaste, Handcursor beim Ziehen
- Schmale, klickbare Gesamtübersicht mit Ausschnitt-Markierung und gesampelter Kurve statt Scrollbar
- Farben der Navigation überarbeitet, Einstellungen für Hell/Dunkel hinzugefügt
- Geführte Kalibrierung startet Datenempfang bei Bedarf automatisch, prüft Hardware im Dialog, mit Zeitlimit für 1000 Samples
- Korrektur Strom-2-Bereichskennung von 2 auf 4 entsprechend vorhandener AD7765-Calibrierungsmap
- Einheiten im Assistenten: A, mA, µA bzw. V, mV

WARNUNGEN:
- Kalibrierung schaltet die physischen Strombereiche nicht automatisch. Kommen innerhalb 12 s nicht 1000 gültige Frames der angeforderten Kennung, wird eine Fehlermeldung angezeigt.
- Die Flächenfarben im Hellmodus sind vorläufig; vollständige Theme-Konsistenz folgt.
- Die Übersicht zeigt ein gesampeltes Signal und keine vollständige signalgetreue Min/Max-Hüllkurve.
- Ein Windows-Build und Hardwaretest wurden in dieser Umgebung NICHT ausgeführt. Bitte mit Visual Studio builden und testen.
