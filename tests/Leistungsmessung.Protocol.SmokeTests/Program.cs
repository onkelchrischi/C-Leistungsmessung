using Leistungsmessung.Hardware;
static void Assert(bool ok, string detail) { if (!ok) throw new Exception(detail); }
Assert(MeasurementProtocol.StartMeasurement == 0x7F00, "start code");
Assert(MeasurementProtocol.Ad7765StartByte == 0, "Java low-byte start");
var decoder = new Ad7765EvalDecoder();
Assert(decoder.Feed(new byte[]{0x7f}).Length == 0, "first half");
var a = decoder.Feed(new byte[]{0xff,0x80,0x00,0xff,0xff});
Assert(a.SequenceEqual(new short[]{32767,-32768,-1}), "signed big-endian split-buffer decode");
Assert(decoder.SampleCount == 3, "count");
decoder.Reset();
Assert(decoder.SampleCount == 0, "reset");
Console.WriteLine("Protocol smoke tests passed.");


// AD7765 BOARD 54-byte frames, split FT_Read boundaries, CRC and counter.
var board = new Ad7765BoardDecoder();
static byte[] BoardFrame(ushort counter)
{
    var frame = new byte[54];
    frame[0] = 0xE5;
    frame[1] = 0x30;
    frame[2] = 0xFF; frame[3] = 0xFF; frame[4] = 0xF0; // U = -16
    frame[5] = 0; frame[6] = 0; frame[7] = 0x11; // I = 0x11
    frame[50] = (byte)(counter >> 8); frame[51] = (byte)counter;
    var crc = Ad7765BoardDecoder.ComputeCrc(frame.AsSpan(0, 52));
    frame[52] = (byte)(crc >> 8); frame[53] = (byte)crc;
    return frame;
}
var frameA = BoardFrame(65535);
Assert(board.Feed(frameA.AsSpan(0, 10)).Count == 0, "partial board frame");
var decodedA = board.Feed(frameA.AsSpan(10));
Assert(decodedA.Count == 1 && decodedA[0].Samples.Count == 8, "board frame size");
Assert(decodedA[0].Samples[0].VoltageRaw == -16, "signed board voltage");
Assert(decodedA[0].Samples[0].CurrentRaw == 0x11, "board current");
var frameB = BoardFrame(0);
Assert(board.Feed(frameB).Count == 1 && board.CounterDiscontinuities == 0, "counter wrap");
var corrupt = BoardFrame(1);
corrupt[7] ^= 0x20;
Assert(board.Feed(corrupt).Count == 0 && board.BadCrcFrames > 0, "CRC rejection");
Console.WriteLine("AD7765 BOARD decoder tests passed.");
