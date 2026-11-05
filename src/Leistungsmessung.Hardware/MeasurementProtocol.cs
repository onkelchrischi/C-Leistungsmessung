using System.Buffers.Binary;

namespace Leistungsmessung.Hardware;

/// <summary>Values and wire encoding transcribed from Java Communication.java and AD7765Eval.java.</summary>
public static class MeasurementProtocol
{
    public const ushort StartMeasurement = 0x7F00;
    public const ushort StopMeasurement = 0x7F01;
    public const ushort Acknowledge = 0x7FFF;
    public const byte Ad7765StopByte = 0xF1;
    public static ReadOnlySpan<byte> Ad7765Preamble => new byte[] { 0xDA, 0x53, 0xAA, 0xB4 };
    public static ReadOnlySpan<byte> PreonStart => "control.measure=1\\n"u8;
    public static ReadOnlySpan<byte> PreonStop => "control.measure=0\\n"u8;
    // Java Measure.start() sends the LOW byte of START_MESSUNG for AD7765 modes.
    public static byte Ad7765StartByte => (byte)(StartMeasurement & 0xff);
}

/// <summary>
/// Streaming decoder of AD7765 EVAL input (Java AD7765Eval.java, lines 64-82):
/// signed 16-bit big-endian CURRENT samples; the Java path sets voltage to zero.
/// Allows odd byte boundaries between FT_Read calls without losing alignment.
/// Does not automatically detect ADC mode; use only when mode has been confirmed.
/// </summary>
public sealed class Ad7765EvalDecoder
{
    private byte? _firstByte;
    public long SampleCount { get; private set; }

    public short[] Feed(ReadOnlySpan<byte> bytes)
    {
        var samples = new List<short>((bytes.Length + 1) / 2);
        foreach (var value in bytes)
        {
            if (_firstByte is null) { _firstByte = value; continue; }
            samples.Add(BinaryPrimitives.ReadInt16BigEndian(new byte[] { _firstByte.Value, value }));
            _firstByte = null;
            SampleCount++;
        }
        return samples.ToArray();
    }

    public void Reset() { _firstByte = null; SampleCount = 0; }
}

/// <summary>Read-only measurement state machine; command sending must be explicitly implemented per ADC mode.</summary>
public enum MeasurementState { Disconnected, Connected, Receiving, Faulted }
public enum MeasurementAdcMode { Unknown, Internal, Harting, Ad7765Eval, Ad7765Board }

public static class MeasurementSafety
{
    public static bool CanUseEvalDecoder(MeasurementAdcMode mode) => mode == MeasurementAdcMode.Ad7765Eval;
    public static void RequireVerifiedMode(MeasurementAdcMode mode)
    {
        if (mode == MeasurementAdcMode.Unknown)
            throw new InvalidOperationException("ADC-Modus muss vor einer aktiven Messung verifiziert werden.");
    }
}
