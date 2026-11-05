using Leistungsmessung.Core;
var ring = new LiveDisplayBuffer(50, 5);
for (var i = 0; i < 3000; i++) ring.Add(i / 500.0, i, 2 * i);
ring.FlushPending();
var visible = ring.Snapshot();
if (visible.Length != 250 || visible[0].Seconds < 1 || visible[^1].Seconds > 6)
    throw new Exception("5-second 50-Hz window failed");
if (visible.Any(v => v.Count <= 0 || Math.Abs(v.Voltage - 2 * v.Current) > 0.001))
    throw new Exception("mean aggregation failed");
Console.WriteLine("LiveDisplayBuffer smoke tests successful");
