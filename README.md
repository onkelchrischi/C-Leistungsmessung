# Leistungsmessung – Phase 7: Gerätespezifische Java-Kalibrierung

Aufbauend auf Phase 6, die auf der Zielhardware mit beiden Live-Rohwertkurven und Statistik bestätigt wurde.

## Neu
- `Leistungsmessung.Core/Ad7765BoardCalibration.cs`: `.properties`-Parser mit InvariantCulture, Validierung aller Koeffizienten, Übertragung von `Power.getI`/`Power.getU` (quadratische Polynome, bereichsabhängig).
- Das originale **PMPP_DEV2**-Kalibrierprofil unter `calibration-reference/`; Kalibrierwerte nur für dieses Geräteprofil verwenden.
- Im WPF-Fenster: „Kalibrierdatei laden“ und Checkbox zur Anzeige zusätzlicher **kalibrierter Java-Werte** als Mittelwerte.
- Die bisherigen Rohwert-Live-Kurven bleiben erhalten. Keine Messbefehle, keine automatische Änderung des Messgeräts.
- Ein einfacher Konsolen-Smoke-Test für den Import und die Berechnungen.

## Verwendung
1. Nach dem Start UM232H verbinden, seriell PMPP_DEV2.
2. „Kalibrierdatei laden“ → `calibration-reference/calib_PMPP_DEV2.properties`.
3. Die Checkbox „Java-Kalibrierwerte anzeigen“ einschalten und Empfang beobachten.
4. Mittelwerte mit der Originalsoftware vergleichen. **Einheiten sind noch nicht verifiziert.**
5. Kalibrierungstest: `dotnet run --project tests/Leistungsmessung.Calibration.SmokeTests`.

## Hinweise zur Genauigkeit
Die Java-Anwendung berechnet die Kalibrierwerte nach Koeffizienten `k0 + k1*x + k2*x²`. Der Strombereich wird aus den unteren Bits des Rohwertes ausgewählt (`1=B2`, `2=B1`, `4=B0`); bei unbekanntem Bereich werden Werte verworfen. **Die physikalischen Einheiten und weitere Umrechnungen aus Range/Messdaten müssen noch mit Java verglichen werden.** Daher werden Volt, Ampere, Watt und Energie hier noch nicht als bestätigte Werte angezeigt.

Dieser Stand wurde in der Ausführungsumgebung mangels Windows/.NET-SDK nicht kompiliert oder an Hardware geprüft. Der Nutzer hat Phase 6 unter Windows bereits erfolgreich getestet.
