# Phase 10 – Kalibrierte Live-Kurven

## Änderungen
- Sobald ein Kalibrierprofil geladen oder über „Kalibrieren“ erstellt wurde, wird `TryConvert` auf **jeden gültigen AD7765-Board-Datensatz** angewandt, bevor die Werte für die 50-Hz-Mittelwertbildung in `LiveDisplayBuffer` gelangen.
- Ohne Kalibrierung bleiben Rohwertkurven aktiv.
- Das Wechseln des Profils leert nur den RAM-Anzeigepuffer, damit keine Rohwerte und kalibrierten Werte gemeinsam dargestellt werden.
- Strom- und Spannungsachsen sind unabhängig; bestehende benutzerdefinierte Y-Skalen bleiben erhalten. Achsengrenzen müssen nach Wechsel zwischen Roh- und Kalibrierwerten ggf. angepasst werden.
- Die temporäre `.bin`-Datei bleibt ein unveränderter Mitschnitt der empfangenen USB-Daten.
- Eine geladene Kalibrierdatei muss zur Seriennummer des ausgewählten Geräts passen.

## Einschränkungen
- Die zugrundeliegenden Java-Kalibriereinheiten / Messbereichssemantik müssen weiterhin mit der Originalsoftware verglichen werden. Deshalb zeigt die Oberfläche bewusst „kalibriert“, nicht unbestätigte physikalische Einheiten an.
- Der aktuelle Kalibrierdialog erstellt eine lineare Zweipunktkalibrierung nur für einen Strombereich. Die komplette Java-Kalibrierabfolge ist noch zu portieren.
- 50 Hz ist die Ziel-Aktualisierungsrate, keine Echtzeitgarantie unter WPF.
- Der Zeitbezug nutzt weiterhin die Empfangsuhr, nicht eine aus ADC-Samplezählung abgeleitete Zeitbasis.
- Nicht unter Windows kompiliert oder an Hardware ausgeführt; bitte `Leistungsmessung.sln` in Visual Studio neu erstellen.

## Test
1. Anwendung ohne Profil starten: Beschriftung „roh“, Rohwertkurven.
2. Passendes PMPP_DEV2-Profil laden: Beschriftung „kalibriert“, Kurven müssen auf die neue Skalierung wechseln.
3. Netzteilwerte verändern und die Anzeige mit der Java-Software vergleichen.
4. Aufzeichnung speichern und prüfen, dass weiterhin rohe FTDI-Bytes gespeichert werden.
5. Prüfen, dass ein Profil mit falscher Seriennummer abgelehnt wird.
