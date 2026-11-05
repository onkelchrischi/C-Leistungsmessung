# Phase 18 – Build-Fix CS0103

In `src/Leistungsmessung.Desktop/MainWindow.xaml.cs` wurde die veraltete Zeile `_sensorDisplay.Clear();` in `SensorSelected` entfernt. Der separate Sensor-Anzeigepuffer existiert nach der Zusammenlegung der Messansichten nicht mehr. Die gemeinsame Kurve wird bereits über `RenderHistory()` beziehungsweise `RenderChart()` und `UpdateOverview()` aktualisiert.

Die Warnungen CS0169 und CS0414 sind nicht build-blockierend und wurden nicht inhaltlich geändert.

**Validierung:** statische Prüfung des Quelltexts; Windows/.NET 10 Build hier nicht ausführbar.
