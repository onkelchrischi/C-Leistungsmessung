using System;
using System.Collections.Generic;

namespace Leistungsmessung.Core;

// The window contains 50 mean values per second. Raw samples are never retained here.
public readonly record struct LiveMean(double Seconds, double Current, double Voltage, long Count);
public sealed class LiveDisplayBuffer
{
    private readonly LiveMean[] _ring;
    private readonly int _rate;
    public Action<LiveMean>? MeanCompleted { get; set; }
    private long _bucket = -1, _count;
    private double _sumI, _sumU;
    private int _next, _size;
    public LiveDisplayBuffer(int rate = 50, double seconds = 5)
    {
        if (rate < 1 || seconds <= 0) throw new ArgumentOutOfRangeException(nameof(rate));
        _rate = rate;
        _ring = new LiveMean[Math.Max(2, (int)Math.Ceiling(rate * seconds))];
    }
    public void Add(double elapsedSeconds, double current, double voltage)
    {
        long bucket = Math.Max(0, (long)Math.Floor(elapsedSeconds * _rate));
        if (_bucket != -1 && bucket != _bucket) Flush();
        _bucket = bucket;
        _sumI += current; _sumU += voltage; _count++;
    }
    public void FlushPending() { if (_count > 0) Flush(); }
    private void Flush()
    {
        if (_count == 0) return;
        _ring[_next] = new LiveMean((_bucket + 0.5) / _rate, _sumI / _count, _sumU / _count, _count);
        MeanCompleted?.Invoke(_ring[_next]);
        _next = (_next + 1) % _ring.Length;
        if (_size < _ring.Length) _size++;
        _sumI = _sumU = 0; _count = 0;
    }
    public LiveMean[] Snapshot()
    {
        var data = new LiveMean[_size];
        for (int i = 0; i < _size; i++) data[i] = _ring[(_next - _size + i + _ring.Length) % _ring.Length];
        return data;
    }
    public void Clear() { _next = _size = 0; _bucket = -1; _count = 0; _sumI = _sumU = 0; }
}
