# Phase 18 Fix 2 – Sensoren verwenden ausschließlich kalibrierte Werte

SensorProfile.ConvertCalibrated(currentAmps, voltageVolts) ist der explizite API-Einstieg:
* Current-Eingang: kalibrierter Strom in Ampere (unabhängig vom automatisch erkannten ADC-Strommessbereich).
* Voltage-Eingang: kalibrierte Spannung in Volt.
* Die Sensorformel `x` verwendet jeweils diese SI-Einheit, z. B. x=0.004 für 4 mA.
* Das Sensorprofil wählt die Quelle mit `SensorInput.Current` oder `SensorInput.Voltage`.
* Die Anwendung konvertiert jeden gültigen AD7765-Rohsample über das gerätespezifische Kalibrierprofil (`TryConvert`) **vor** dem Live-Mittelwert.
* Nur diese kalibrierten Werte gelangen in LiveDisplayBuffer, History-Datei, Sensorverlauf, Graph, Fadenkreuz und Sensor-Berechnung.
* Ohne gültiges Kalibrierprofil wird keine Rohwert-Fallback-Sensoranzeige aktiviert.
* Rohdaten werden weiterhin separat in der temporären Binärdatei archiviert; diese sind nicht Quelle der Sensorformel.
* Kalibrierkoeffizienten müssen für die korrekten Strombereiche überprüft werden.

Hinweis: Formeln verwenden Ampere beziehungsweise Volt, nicht µA/mA/mV. Display-Einheiten beeinflussen die Berechnung nicht.
Kein vollständiger Windows-Build oder Hardwaretest in der Entwicklungsumgebung möglich.
