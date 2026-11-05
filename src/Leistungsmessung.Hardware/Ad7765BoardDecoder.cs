using System.Buffers.Binary;

namespace Leistungsmessung.Hardware;

/// <summary>
/// Passive AD7765 BOARD frame parser, transcribed from Java measuring/AD7765.java.
/// Never sends hardware commands. Raw signed ADC counts, NOT calibrated physical units.
/// </summary>
public sealed class Ad7765BoardDecoder
{
    public const int FrameLength = 54;
    private readonly List<byte> _pending = new();
    public long ValidFrames { get; private set; }
    public long BadCrcFrames { get; private set; }
    public long DiscardedBytes { get; private set; }
    public long CounterDiscontinuities { get; private set; }
    private ushort? _lastCounter;
    public byte LastStateFlags { get; private set; }

    public IReadOnlyList<Ad7765BoardFrame> Feed(ReadOnlySpan<byte> input)
    {
        foreach (byte b in input) _pending.Add(b);
        var output = new List<Ad7765BoardFrame>();
        while (_pending.Count >= FrameLength)
        {
            if (_pending[0] != 0xE5)
            {
                _pending.RemoveAt(0);
                DiscardedBytes++;
                continue;
            }
            byte[] bytes = _pending.GetRange(0, FrameLength).ToArray();
            ushort actual = BinaryPrimitives.ReadUInt16BigEndian(bytes.AsSpan(52, 2));
            ushort expected = ComputeCrc(bytes.AsSpan(0, 52));
            if (expected != actual)
            {
                BadCrcFrames++;
                // On CRC failure try the next byte as a possible frame boundary.
                _pending.RemoveAt(0);
                DiscardedBytes++;
                continue;
            }
            ushort counter = BinaryPrimitives.ReadUInt16BigEndian(bytes.AsSpan(50, 2));
            if (_lastCounter is ushort last && counter != unchecked((ushort)(last + 1)))
                CounterDiscontinuities++;
            _lastCounter = counter;
            LastStateFlags = (byte)(bytes[1] >> 4);
            var values = new List<Ad7765BoardSample>(8);
            for (int i = 0; i < 8; i++)
            {
                int voltageOffset = 2 + i * 6;
                int currentOffset = voltageOffset + 3;
                int voltage = SignExtend24((bytes[voltageOffset] << 16)
                    | (bytes[voltageOffset + 1] << 8)
                    | (bytes[voltageOffset + 2] & 0xF0));
                int current = SignExtend24((bytes[currentOffset] << 16)
                    | (bytes[currentOffset + 1] << 8)
                    | (bytes[currentOffset + 2] & 0xF7));
                // Java skips initial invalid sample pairs before the first valid current sample.
                values.Add(new Ad7765BoardSample(current, voltage,
                    (bytes[currentOffset + 2] & 0x0F) != 0));
            }
            output.Add(new Ad7765BoardFrame(counter, LastStateFlags, values));
            ValidFrames++;
            _pending.RemoveRange(0, FrameLength);
        }
        // If no preamble exists, keep bounded memory (up to one partial frame).
        if (_pending.Count > FrameLength * 2)
        {
            int remove = _pending.Count - (FrameLength - 1);
            _pending.RemoveRange(0, remove);
            DiscardedBytes += remove;
        }
        return output;
    }

    private static int SignExtend24(int number) => (number << 8) >> 8;

    /// <summary>CRC16 algorithm copied from Java tools/MyMath.java crc16_add, seed zero.</summary>
    public static ushort ComputeCrc(ReadOnlySpan<byte> bytes)
    {
        int acc = 0;
        foreach (byte b in bytes)
        {
            acc ^= b;
            acc = ((acc & 0xFFFF) >> 8) | (((acc & 0xFFFF) << 8) & 0xFFFF);
            acc ^= ((acc & 0xFF00) << 4) & 0xFFFF;
            acc ^= (((acc & 0xFFFF) >> 8) >> 4) & 0xFFFF;
            acc ^= ((acc & 0xFF00) >> 5) & 0xFFFF;
        }
        return (ushort)(acc & 0xFFFF);
    }

    public void Reset()
    {
        _pending.Clear();
        ValidFrames = BadCrcFrames = DiscardedBytes = CounterDiscontinuities = 0;
        _lastCounter = null;
        LastStateFlags = 0;
    }
}

public sealed record Ad7765BoardSample(int CurrentRaw, int VoltageRaw, bool CurrentIndicatorValid);
public sealed record Ad7765BoardFrame(ushort Counter, byte StateFlags, IReadOnlyList<Ad7765BoardSample> Samples);
