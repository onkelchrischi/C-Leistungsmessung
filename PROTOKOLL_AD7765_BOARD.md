# AD7765 Board – aus Java abgeleitet

Quelle: `reference-java/measuring/AD7765.java`, `reference-java/measuring/Measure.java`, `reference-java/tools/MyMath.java`.

* 54 Bytes: `E5`, Statusbyte, 8 × (U 3 Byte + I 3 Byte), Counter Big Endian 2 Byte, CRC Big Endian 2 Byte.
* CRC über Bytes 0–51, Seed 0, Algorithmus `MyMath.crc16_add`.
* Statusflags in oberem Nibble von Byte 1.
* I: jeweils 3 Bytes mit `byte2 & 0xF7`, 24-bit signed; U: `byte2 & 0xF0`, 24-bit signed.
* Board-Start laut `Measure.start()` über `writeByte(lowByte(START_MESSUNG), COMMANDLINE)`; `START_MESSUNG=0x7F00`, also 0x00. Der Weg kann Preon/serielle Leitung oder USB sein. **Noch nicht senden**.
* Für valide Messwerte müssen später die Kalibrierung, zeitliche Skalierung, Software-Oversampling und das Überspringen anfänglich ungültiger Samples originalgetreu übernommen werden.
* Decoder nimmt keine Schreiboperation vor. CRC-Prüfung allein bestätigt noch keine funktionierende Messung; Diagnosewerte mit Originalanwendung abgleichen.
