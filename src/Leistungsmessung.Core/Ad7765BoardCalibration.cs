using System.Globalization;

namespace Leistungsmessung.Core;

/// <summary>
/// Reproduces the polynomial selection used by Java tools.Power.getI/getU.
/// The outputs are in the legacy calibration units; display units must be verified
/// against the original application's Range and Messdaten configuration.
/// </summary>
public sealed class Ad7765BoardCalibration
{
    public readonly record struct Polynomial(double K0, double K1, double K2)
    {
        public double Apply(double value) => (K2 * value + K1) * value + K0;
    }

    public string DeviceName { get; }
    public Polynomial Voltage { get; }
    public IReadOnlyDictionary<int, Polynomial> CurrentRanges { get; }

    private Ad7765BoardCalibration(string name, Polynomial voltage, Dictionary<int, Polynomial> ranges)
    {
        DeviceName = name;
        Voltage = voltage;
        CurrentRanges = ranges;
    }

    public static Ad7765BoardCalibration Load(string path)
    {
        var properties = new Dictionary<string, string>(StringComparer.Ordinal);
        foreach (string source in File.ReadLines(path))
        {
            var line = source.Trim();
            if (line.Length == 0 || line.StartsWith("#") || line.StartsWith("!")) continue;
            int sep = line.IndexOf('=');
            if (sep < 0) continue;
            properties[line[..sep].Trim()] = line[(sep + 1)..].Trim();
        }
        double Required(string key)
        {
            if (!properties.TryGetValue(key, out string? value) ||
                !double.TryParse(value, NumberStyles.Float, CultureInfo.InvariantCulture, out double number) ||
                !double.IsFinite(number))
                throw new InvalidDataException($"Ungültiger oder fehlender Kalibrierparameter: {key}");
            return number;
        }
        Polynomial Poly(string prefix) => new(
            Required(prefix + ".k0"), Required(prefix + ".k1"), Required(prefix + ".k2"));
        string name = properties.GetValueOrDefault("name", "");
        if (string.IsNullOrWhiteSpace(name))
            throw new InvalidDataException("Die Kalibrierdatei enthält keinen Gerätenamen.");
        return new Ad7765BoardCalibration(name, Poly("AD7765_U"),
            new Dictionary<int, Polynomial>
            {
                // Java Power.getI maps range bits 1 => B2, 2 => B1, 4 => B0.
                [1] = Poly("AD7765_B2"),
                [2] = Poly("AD7765_B1"),
                [4] = Poly("AD7765_B0"),
            });
    }

    public Ad7765BoardCalibration WithTwoPointCalibration(
        int range, double rawCurrentA, double currentReferenceA, double rawCurrentB, double currentReferenceB,
        double rawVoltageA, double voltageReferenceA, double rawVoltageB, double voltageReferenceB)
    {
        if (!CurrentRanges.ContainsKey(range)) throw new InvalidDataException("Unbekannter ADC-Strombereich.");
        static Polynomial Fit(double x1, double y1, double x2, double y2)
        {
            if (!double.IsFinite(x1) || !double.IsFinite(x2) || !double.IsFinite(y1) || !double.IsFinite(y2) ||
                Math.Abs(x2 - x1) < 1e-9) throw new InvalidDataException("Zwei unterschiedliche Rohwerte und gültige Referenzen erforderlich.");
            double slope = (y2 - y1) / (x2 - x1);
            return new Polynomial(y1 - slope * x1, slope, 0);
        }
        var updated = new Dictionary<int, Polynomial>(CurrentRanges);
        updated[range] = Fit(rawCurrentA, currentReferenceA, rawCurrentB, currentReferenceB);
        return new Ad7765BoardCalibration(DeviceName,
            Fit(rawVoltageA, voltageReferenceA, rawVoltageB, voltageReferenceB), updated);
    }

    private static Polynomial FitReference(double x1, double y1, double x2, double y2)
    {
        if (!double.IsFinite(x1) || !double.IsFinite(x2) || !double.IsFinite(y1) || !double.IsFinite(y2) ||
            Math.Abs(x2 - x1) < 1e-9) throw new InvalidDataException("Zwei unterschiedliche Rohwerte erforderlich.");
        var slope = (y2 - y1) / (x2 - x1);
        return new Polynomial(y1 - slope * x1, slope, 0);
    }

    public Ad7765BoardCalibration WithVoltageTwoPointCalibration(double rawA, double refA, double rawB, double refB) =>
        new(DeviceName, FitReference(rawA, refA, rawB, refB), new Dictionary<int, Polynomial>(CurrentRanges));

    public Ad7765BoardCalibration WithCurrentTwoPointCalibration(int range, double rawA, double refA, double rawB, double refB)
    {
        if (!CurrentRanges.ContainsKey(range)) throw new InvalidDataException("Ungültiger Strombereich.");
        var currents = new Dictionary<int, Polynomial>(CurrentRanges);
        currents[range] = FitReference(rawA, refA, rawB, refB);
        return new(DeviceName, Voltage, currents);
    }

    public void Save(string path)
    {
        var lines = new List<string> { "# C# two-point draft calibration (verify against original Java software)",
            "name=" + DeviceName };
        static string Num(double v) => v.ToString("R", CultureInfo.InvariantCulture);
        void Add(string key, Polynomial p)
        {
            lines.Add(key + ".k0=" + Num(p.K0));
            lines.Add(key + ".k1=" + Num(p.K1));
            lines.Add(key + ".k2=" + Num(p.K2));
        }
        Add("AD7765_U", Voltage);
        Add("AD7765_B2", CurrentRanges[1]);
        Add("AD7765_B1", CurrentRanges[2]);
        Add("AD7765_B0", CurrentRanges[4]);
        File.WriteAllLines(path, lines);
    }

    public bool TryConvert(int currentRaw, int voltageRaw, out double current, out double voltage)
    {
        // VALUE_MASK = 0xFFFFFFF0 and RANGE_MASK = 0x07 in measuring/AD7765.java.
        int range = currentRaw & 0x07;
        if (!CurrentRanges.TryGetValue(range, out var coefficients))
        {
            current = voltage = double.NaN;
            return false;
        }
        current = coefficients.Apply(currentRaw & unchecked((int)0xFFFFFFF0));
        voltage = Voltage.Apply(voltageRaw);
        return true;
    }
}
