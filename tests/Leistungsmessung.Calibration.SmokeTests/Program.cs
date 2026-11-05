using Leistungsmessung.Core;
static void Check(bool ok, string label) { if (!ok) throw new Exception(label); }
var path = Path.Combine(AppContext.BaseDirectory, "calib_PMPP_DEV2.properties");
var calib = Ad7765BoardCalibration.Load(path);
Check(calib.DeviceName == "PMPP_DEV2", "profile name");
Check(Math.Abs(calib.Voltage.Apply(0) - (-1480.9246)) < 0.001, "voltage offset");
Check(calib.TryConvert(0x14, 0, out double current, out double voltage), "range 4");
Check(Math.Abs(current - (4.618585 + 0x10 * 7.723973E-7)) < 0.0001, "range B0");
Check(Math.Abs(voltage + 1480.9246) < 0.001, "voltage");
Check(!calib.TryConvert(0, 0, out _, out _), "reject missing range");
Console.WriteLine("Calibration smoke tests passed.");
