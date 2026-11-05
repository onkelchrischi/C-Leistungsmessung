using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;

namespace Leistungsmessung.Core;

public enum SensorInput { Current, Voltage }
public enum SensorConversion { Linear, RtdDirect, Formula }
public enum RtdType { None, Pt100, Pt500, Pt1000 }

public sealed class SensorProfile
{
    public string Name { get; set; } = "Neuer Sensor";
    public SensorInput Input { get; set; } = SensorInput.Current;
    public SensorConversion Conversion { get; set; } = SensorConversion.Linear;
    public RtdType Rtd { get; set; } = RtdType.None;
    public double InputMin { get; set; } = 0.004;  // A bzw. V
    public double InputMax { get; set; } = 0.020;
    public double OutputMin { get; set; } = 0;
    public double OutputMax { get; set; } = 100;
    public string OutputUnit { get; set; } = "°C";
    public double ExcitationCurrentAmps { get; set; } = 0.001;
    public string Formula { get; set; } = "x";

    /// <summary>
    /// Converts ONLY calibrated electrical values: current in amperes, voltage in volts.
    /// Never pass ADC words, scaled display values (mA/µA/mV), or range codes here.
    /// </summary>
    public double ConvertCalibrated(double currentAmps, double voltageVolts)
    {
        if (!double.IsFinite(currentAmps) || !double.IsFinite(voltageVolts))
            return double.NaN;
        double input = Input == SensorInput.Current ? currentAmps : voltageVolts;
        if (Conversion == SensorConversion.Formula)
        {
            try { return SensorFormula.Evaluate(Formula, input); }
            catch (FormatException) { return double.NaN; }
        }
        if (Conversion == SensorConversion.Linear)
        {
            if (InputMax <= InputMin) return double.NaN;
            return OutputMin + (input - InputMin) * (OutputMax - OutputMin) / (InputMax - InputMin);
        }
        if (ExcitationCurrentAmps <= 0 || Input != SensorInput.Voltage || Rtd == RtdType.None)
            return double.NaN;
        double nominal = Rtd switch { RtdType.Pt100 => 100, RtdType.Pt500 => 500, _ => 1000 };
        return TemperatureFromResistance(input / ExcitationCurrentAmps, nominal);
    }

    [Obsolete("Use ConvertCalibrated with current in A and voltage in V.")]
    public double Convert(double currentAmps, double voltageVolts)
        => ConvertCalibrated(currentAmps, voltageVolts);

    // IEC 60751 (alpha = 0.00385); numerical bisection covers -200...850 °C.
    public static double TemperatureFromResistance(double ohms, double nominal)
    {
        if (!double.IsFinite(ohms) || ohms <= 0 || nominal <= 0) return double.NaN;
        double low = -200, high = 850;
        for (int i = 0; i < 60; i++)
        {
            double mid = (low + high) / 2;
            double resistance = nominal * (1 + 3.9083e-3 * mid - 5.775e-7 * mid * mid
                + (mid < 0 ? -4.183e-12 * (mid - 100) * mid * mid * mid : 0));
            if (resistance < ohms) low = mid; else high = mid;
        }
        double result = (low + high) / 2;
        double maxR = nominal * (1 + 3.9083e-3 * 850 - 5.775e-7 * 850 * 850);
        double minR = nominal * (1 + 3.9083e-3 * -200 - 5.775e-7 * 40000 + -4.183e-12 * (-300) * (-8000000));
        return ohms < minR || ohms > maxR ? double.NaN : result;
    }
}

public static class SensorRepository
{
    public static string FilePath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Leistungsmessung", "sensors.json");

    public static List<SensorProfile> Load()
    {
        try
        {
            if (File.Exists(FilePath))
                return JsonSerializer.Deserialize<List<SensorProfile>>(File.ReadAllText(FilePath)) ?? new();
        }
        catch (Exception) { /* beschädigte Datei nicht überschreiben, bis Benutzer speichert */ }
        return new()
        {
            new() { Name = "PT100 · 4–20 mA", Rtd = RtdType.Pt100 },
            new() { Name = "PT500 · 4–20 mA", Rtd = RtdType.Pt500 },
            new() { Name = "PT1000 · 4–20 mA", Rtd = RtdType.Pt1000 }
        };
    }

    public static void Save(IEnumerable<SensorProfile> sensors)
    {
        Directory.CreateDirectory(Path.GetDirectoryName(FilePath)!);
        string temp = FilePath + ".tmp";
        File.WriteAllText(temp, JsonSerializer.Serialize(sensors, new JsonSerializerOptions { WriteIndented = true }));
        File.Move(temp, FilePath, true);
    }
}
