using System.Buffers.Binary;
using System.Globalization;
using System.Text;

namespace Leistungsmessung.Core;

public sealed record Sample(int Index, double TimeSeconds, double Current, double Voltage)
{
    public double Power => Current * Voltage;
}
public sealed record RdfHeader(double Version, byte Marker, long Timestamp, int TimeUnitIndex,
    double Interval, int CurrentRangeIndex, byte AdcMode, int EstimatedSamples);
public sealed record Measurement(RdfHeader Header, IReadOnlyList<Sample> Samples,
    int StateChangeCount, string SourceFile);

public static class RdfReader
{
    // Java DataInputStream stores multi-byte primitives in BIG ENDIAN order.
    public static Measurement Read(string path, int maxSamples = 5_000_000)
    {
        using var stream = File.OpenRead(path);
        using var reader = new BinaryReader(stream);
        double version = ReadDouble(reader);
        if (version < 0.6 || version > 1.0) throw new NotSupportedException(
            $"RDF version {version} isn't validated for this importer (supported 0.6 through 1.0).");
        byte marker = reader.ReadByte();
        long timestamp = ReadInt64(reader);
        int timeUnit = ReadInt32(reader);
        double dt = ReadDouble(reader);
        int range = ReadInt32(reader);
        byte adc = reader.ReadByte();
        int estimated = ReadInt32(reader);
        if (estimated < 0) throw new InvalidDataException("Negative estimated sample count.");
        var header = new RdfHeader(version,marker,timestamp,timeUnit,dt,range,adc,estimated);
        var samples = new List<Sample>(Math.Min(estimated, 100_000));
        int changes = 0;
        // For RDF >= 0.9 the payload is an int index + 2 IEEE754 floats.
        // Negative indices represent device state changes and have a different record structure.
        while (stream.Position + 4 <= stream.Length)
        {
            int index = ReadInt32(reader);
            if (index >= 0)
            {
                if (samples.Count >= maxSamples) throw new InvalidDataException("Sample safety limit exceeded.");
                if (version >= 0.9)
                {
                    if (stream.Length-stream.Position < 8) throw new InvalidDataException("Truncated RDF sample.");
                    float current = ReadFloat(reader), voltage = ReadFloat(reader);
                    samples.Add(new Sample(index,index*dt / Math.Pow(1000,timeUnit),current,voltage));
                }
                else if (version == 0.8)
                {
                    if (stream.Length-stream.Position < 16) throw new InvalidDataException("Truncated RDF sample.");
                    samples.Add(new Sample(index,index*dt / Math.Pow(1000,timeUnit),ReadDouble(reader),ReadDouble(reader)));
                }
                else
                {
                    if (stream.Length-stream.Position < 8) throw new InvalidDataException("Truncated RDF sample.");
                    samples.Add(new Sample(index,index*dt / Math.Pow(1000,timeUnit),ReadInt32(reader),ReadInt32(reader)));
                }
            }
            else
            {
                // Java: BOARD -> int index + byte type; other ADCs -> long time + byte type.
                int extra = adc == 3 ? 5 : 9; // BOARD mode must be confirmed with original constants.
                if (stream.Length-stream.Position < extra)
                    throw new InvalidDataException("Truncated RDF state-change entry.");
                reader.ReadBytes(extra);
                changes++;
            }
        }
        return new Measurement(header,samples,changes,path);
    }
    private static int ReadInt32(BinaryReader r) => BinaryPrimitives.ReadInt32BigEndian(r.ReadBytes(4));
    private static long ReadInt64(BinaryReader r) => BinaryPrimitives.ReadInt64BigEndian(r.ReadBytes(8));
    private static float ReadFloat(BinaryReader r) => BitConverter.Int32BitsToSingle(ReadInt32(r));
    private static double ReadDouble(BinaryReader r) => BitConverter.Int64BitsToDouble(ReadInt64(r));
}
public sealed record CsvStatistic(int Index, double TimeSeconds, double CurrentMilliAmpere,
    double VoltageMilliVolt, double PowerMilliWatt, double EnergyMilliWattSeconds, double ChargeMilliAmpSeconds);
public static class CsvStatisticsReader
{
    public static IReadOnlyList<CsvStatistic> Read(string path)
    {
        var lines = File.ReadLines(path,Encoding.UTF8).ToArray();
        if (lines.Length == 0) throw new InvalidDataException("CSV is empty.");
        string[] columns = lines[0].Trim('\uFEFF').Split(';');
        int Col(string name) => Array.FindIndex(columns,x=>x.Trim().Equals(name,StringComparison.OrdinalIgnoreCase));
        string[] required = ["index","gesamt Zeit in Sekunden","Mittelwert Strom [mA]",
            "Mittelwert Spannung [mV]","Mittelwert Leistung [mW]","Energie [mWs]","Ladung [mAs]"];
        int[] indices = required.Select(Col).ToArray();
        if (indices.Any(x=>x<0)) throw new InvalidDataException("CSV statistics header not recognized.");
        var culture = CultureInfo.GetCultureInfo("de-DE");
        double Num(string[] a,int column) => double.Parse(a[column],NumberStyles.Float,culture);
        var rows = new List<CsvStatistic>();
        foreach(var line in lines.Skip(1))
        {
            if (string.IsNullOrWhiteSpace(line)) continue;
            var fields=line.Split(';');
            if (fields.Length <= indices.Max()) throw new InvalidDataException("Incomplete CSV row.");
            rows.Add(new CsvStatistic((int)Num(fields,indices[0]),Num(fields,indices[1]),
                Num(fields,indices[2]),Num(fields,indices[3]),Num(fields,indices[4]),
                Num(fields,indices[5]),Num(fields,indices[6])));
        }
        return rows;
    }
}
