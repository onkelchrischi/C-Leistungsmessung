# Phase 7 – Build-Korrektur 1

- `src/Leistungsmessung.Desktop/MainWindow.xaml.cs`: `using System.IO;` ergänzt, damit `InvalidDataException` und `IOException` aufgelöst werden.
- Fünf versehentlich als Quelltext eingefügte `\n`-Sequenzen durch echte Zeilenumbrüche ersetzt.
- Die `\n`-Escapes in `MeasurementProtocol.cs` sind Bestandteil der seriellen Protokollbefehle und wurden bewusst nicht verändert.

Hinweis: Ein Windows/.NET-WPF-Build war in der Erstellungsumgebung nicht möglich. Bitte die Solution unter Windows neu erstellen.
