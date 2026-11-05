using System.IO;
using System.Text.Json;
using System.Globalization;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Diagnostics;
using System.Windows.Threading;
using System.Windows;
using System.Windows.Media;
using Microsoft.Win32;
using Leistungsmessung.Core;
using Leistungsmessung.Hardware;
namespace Leistungsmessung.Desktop;
public partial class MainWindow : Window
{

    private BinaryWriter? _sensorWriter;
    private readonly System.Collections.ObjectModel.ObservableCollection<SensorProfile> _sensors = new();
    private SensorProfile? _activeSensor;
    private bool _overviewDragging;
    private double _overviewDragStartX, _overviewDragStartHistory;

    private void SettingsSectionClick(object sender, RoutedEventArgs e)
    {
        if (sender is not Button button || !int.TryParse(button.Tag?.ToString(), out int index)) return;
        FrameworkElement[] panels = { CalibrationSettingsPanel, DeviceSettingsPanel, AppearanceSettingsPanel, AxesSettingsPanel, DebugSettingsPanel };
        for (int i = 0; i < panels.Length; i++) panels[i].Visibility = i == index ? Visibility.Visible : Visibility.Collapsed;
    }

    private void LoadSensors()
    {
        foreach (var sensor in SensorRepository.Load()) _sensors.Add(sensor);
        SensorPicker.ItemsSource = _sensors;
        if (_sensors.Count > 0) SensorPicker.SelectedIndex = 0;
    }

    private void SensorSelected(object sender, SelectionChangedEventArgs e)
    {
        _activeSensor = SensorPicker.SelectedItem as SensorProfile;
        /* Gemeinsamer Messgraph */
        SensorInfoText.Text = _activeSensor is null ? "Kein Sensor ausgewählt" :
            $"{_activeSensor.Input} · {_activeSensor.Conversion} · {_activeSensor.OutputUnit}";
        if (SensorMode)
        {
            _overviewLastRefreshTick = 0;
            if (_historyMode) RenderHistory(); else RenderChart();
            UpdateOverview();
        }
    }

    private void AddSensorClick(object sender, RoutedEventArgs e) => OpenSensorEditor(null);

    private void EditSensorClick(object sender, RoutedEventArgs e)
    {
        if (SensorPicker.SelectedItem is SensorProfile sensor) OpenSensorEditor(sensor);
    }

    private void OpenSensorEditor(SensorProfile? selected)
    {
        var editor = new SensorEditor(selected) { Owner = this };
        if (editor.ShowDialog() != true || editor.Result is null) return;
        int index = selected is null ? -1 : _sensors.IndexOf(selected);
        if (index >= 0) _sensors[index] = editor.Result;
        else { _sensors.Add(editor.Result); index = _sensors.Count - 1; }
        try { SensorRepository.Save(_sensors); }
        catch (Exception ex) { MessageBox.Show(this, "Sensor konnte nicht gespeichert werden: " + ex.Message); }
        SensorPicker.SelectedIndex = index;
    }

    private void RemoveSensorClick(object sender, RoutedEventArgs e)
    {
        if (SensorPicker.SelectedItem is not SensorProfile sensor) return;
        if (MessageBox.Show(this, $"Sensor '{sensor.Name}' löschen?", "Sensor löschen", MessageBoxButton.YesNo) != MessageBoxResult.Yes) return;
        _sensors.Remove(sensor);
        try { SensorRepository.Save(_sensors); }
        catch (Exception ex) { MessageBox.Show(this, "Sensor konnte nicht gelöscht werden: " + ex.Message); }
    }

    private bool SensorMode => MeasurementModePicker?.SelectedIndex == 1;

    private void MeasurementModeChanged(object sender, SelectionChangedEventArgs e)
    {
        if (ElectricalToolbar is null || SensorToolbar is null) return;
        bool sensor = SensorMode;
        ElectricalToolbar.Visibility = sensor ? Visibility.Collapsed : Visibility.Visible;
        SensorToolbar.Visibility = sensor ? Visibility.Visible : Visibility.Collapsed;
        ElectricalValuePanel.Visibility = sensor ? Visibility.Collapsed : Visibility.Visible;
        SensorValuePanel.Visibility = sensor ? Visibility.Visible : Visibility.Collapsed;
        CurrentAxisCaption.Text = sensor ? (_activeSensor?.Name ?? "Sensor") : "Strom ←";
        PlotTitle.Text = sensor ? "Sensormessung" : "AD7765 BOARD · Strom / Spannung";
        CurrentAxisCaption.Foreground = sensor ? Brushes.LightGreen : Brushes.LightSkyBlue;
        VoltageAxisCaption.Visibility = sensor ? Visibility.Collapsed : Visibility.Visible;
        RightTopAxis.Visibility = sensor ? Visibility.Collapsed : Visibility.Visible;
        RightBottomAxis.Visibility = sensor ? Visibility.Collapsed : Visibility.Visible;
        _cursorPoints = Array.Empty<(double X, double I, double U)>();
        _overviewLastRefreshTick = 0;
        if (_historyMode) RenderHistory(); else RenderChart();
        UpdateOverview();
    }

    private void UpdateSensorDisplay(LiveMean[] means)
    {
        if (_activeSensor is null || _calibration is null || means.Length == 0)
        {
            SensorValueText.Text = "Sensorprofil und Kalibrierung erforderlich";
            return;
        }
        var latest = means[^1];
        double value = _activeSensor.ConvertCalibrated(latest.Current, latest.Voltage);
        SensorValueText.Text = double.IsFinite(value)
            ? $"{value:N3} {_activeSensor.OutputUnit}" : "Ungültiger Sensorwert";
    }

    private void OverviewMouseMove(object sender, System.Windows.Input.MouseEventArgs e)
    {
        if (!_overviewDragging || _monitorCancellation is not null || e.LeftButton != System.Windows.Input.MouseButtonState.Pressed) return;
        double duration = Math.Max(_windowSeconds, _lastRecordedHistorySeconds);
        double width = Math.Max(1, OverviewCanvas.ActualWidth);
        double movedSeconds = (e.GetPosition(OverviewCanvas).X - _overviewDragStartX) * duration / width;
        _historyStart = Math.Clamp(_overviewDragStartHistory + movedSeconds, 0, Math.Max(0, duration - _windowSeconds));
        _historyMode = true;
        RenderHistory();
    }
    private void OverviewMouseUp(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        _overviewDragging = false;
        OverviewCanvas.ReleaseMouseCapture();
        OverviewCanvas.Cursor = System.Windows.Input.Cursors.Arrow;
    }

    private List<(double X,double Y)> _points = [];
    private readonly FtdiD2xx _ftdi = new();
    private IReadOnlyList<FtdiDevice> _enumeratedDevices = [];
    private CancellationTokenSource? _monitorCancellation;
    private FileStream? _capture;
    private FileStream? _historyWrite;
    private BinaryWriter? _historyWriter;
    private string? _historyPath;
    private long _historyPoints;
    private double _lastRecordedHistorySeconds;
    private int _lastObservedAdcRange = -1;
    private readonly long[] _observedRangeCounts = new long[8];
    private long _lastRangeDiagnosticsTick;
    private bool _updatingScroll;
    private bool _historyMode;
    private double _historyStart;
    private int _displayRate = 50;
    private double _currentUnitScale = 1000; // Java Power.getI is in A -> mA.
    private double _voltageUnitScale = 1; // Java Power.getU is in V.
    private string _currentUnitName = "mA", _voltageUnitName = "V";
    private double _energyJ;
    private double _lastEnergyTime = -1;
    private string? _capturePath;
    private LiveDisplayBuffer _displayBuffer = new(50, 5);
    private readonly Stopwatch _acquisitionClock = new();
    private double _windowSeconds = 5;
    private bool _fixedAxes;
    private double _iMin = 0, _iMax = 0.1, _uMin = 0, _uMax = 5;
    
    private long _receivedBytes;
    private readonly Ad7765EvalDecoder _evalDecoder = new();
    private readonly Ad7765BoardDecoder _boardDecoder = new();
    private bool _captureLimitReached;
    private long _liveSampleCount;
    private double _minCurrent = double.PositiveInfinity, _maxCurrent = double.NegativeInfinity;
    private double _sumCurrent, _minVoltage = double.PositiveInfinity, _maxVoltage = double.NegativeInfinity, _sumVoltage;
    
    private readonly object _receiveGate = new();
    private readonly DispatcherTimer _displayTimer = new() { Interval = TimeSpan.FromMilliseconds(20) };
    private Task? _receiveTask;
    private long _renderedSampleCount;
    private long _lastDisplayBytes;
    private long _lastDisplayTimestamp;
    private double _receiveRate;
    private uint _pendingBytes;
    private int _displayMs;
    private int _lastCurrentRaw, _lastVoltageRaw;
    private bool _showLive;
    private Ad7765BoardCalibration? _calibration;
    private readonly DispatcherTimer _deviceScanTimer = new() { Interval = TimeSpan.FromSeconds(5) };
    private bool _deviceScanInProgress;
    private bool _measurementTransition;
    private readonly string _preferencesPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Leistungsmessung", "last-device.txt");
    private double _calI, _calU;
    private long _calCount;
    private TaskCompletionSource<(double Current, double Voltage)>? _sampleRequest;
    private int _sampleRange, _sampleRemaining;
    private double _sampleSumI, _sampleSumU;
    private double _allMinI = double.PositiveInfinity, _allMaxI = double.NegativeInfinity;
    private double _allMinU = double.PositiveInfinity, _allMaxU = double.NegativeInfinity;

    private Task<(double Current, double Voltage)> CaptureReferenceAsync(int range)
    {
        lock (_receiveGate)
        {
            if (_monitorCancellation is null) throw new InvalidOperationException("Messdatenempfang ist nicht aktiv.");
            if (_sampleRequest is not null) throw new InvalidOperationException("Es läuft bereits eine Referenzaufnahme.");
            _sampleRange = range;
            _sampleRemaining = 1000;
            _sampleSumI = _sampleSumU = 0;
            _sampleRequest = new(TaskCreationOptions.RunContinuationsAsynchronously);
            return WaitForReferenceAsync(_sampleRequest.Task);
        }
    }

    private async Task<(double Current, double Voltage)> WaitForReferenceAsync(Task<(double Current, double Voltage)> task)
    {
        try { return await task.WaitAsync(TimeSpan.FromSeconds(12)); }
        catch (TimeoutException)
        {
            lock (_receiveGate)
                if (_sampleRequest?.Task == task) _sampleRequest = null;
            throw new TimeoutException($"Innerhalb von 12 Sekunden kamen keine 1.000 gültigen Werte für Bereichscode {_sampleRange}. Zuletzt gemeldeter AD7765-Bereichscode: {_lastObservedAdcRange}. Codes während der Aufnahme: {string.Join(", ", _observedRangeCounts.Select((n, code) => $"{code}={n}"))}. Bitte Strombereich am Messgerät prüfen.");
        }
    }

    private void UpdateCalibrationSample(int current, int voltage)
    {
        int range = current & 7;
        _lastObservedAdcRange = range;
        _observedRangeCounts[range]++;
        if (_sampleRequest is null) return; // Called under _receiveGate.
        if (_sampleRange != 0 && range != _sampleRange) return;
        _sampleSumI += current & unchecked((int)0xFFFFFFF0);
        _sampleSumU += voltage;
        if (--_sampleRemaining != 0) return;
        var complete = _sampleRequest;
        _sampleRequest = null;
        complete.TrySetResult((_sampleSumI / 1000, _sampleSumU / 1000));
    }

    private void ResetGlobalScale()
    {
        _allMinI = _allMinU = double.PositiveInfinity;
        _allMaxI = _allMaxU = double.NegativeInfinity;
    }


    private LiveDisplayBuffer NewDisplayBuffer(int rate, double seconds)
    {
        var buffer = new LiveDisplayBuffer(rate, seconds);
        buffer.MeanCompleted = mean =>
        {
            if (_historyWriter is null) return;
            _historyWriter.Write(mean.Seconds);
            _historyWriter.Write(mean.Current);
            _historyWriter.Write(mean.Voltage);
            if (_sensorWriter is not null && _activeSensor is not null && _calibration is not null)
            {
                double sensorValue = _activeSensor.ConvertCalibrated(mean.Current, mean.Voltage);
                _sensorWriter.Write(mean.Seconds);
                _sensorWriter.Write(mean.Current);
                _sensorWriter.Write(mean.Voltage);
                _sensorWriter.Write(sensorValue);
            }
            _historyPoints++;
            _lastRecordedHistorySeconds = mean.Seconds;
            if ((_historyPoints & 63) == 0) _historyWriter.Flush();
            if (_calibration is not null)
            {
                if (_lastEnergyTime >= 0)
                    _energyJ += mean.Current * mean.Voltage * Math.Clamp(mean.Seconds - _lastEnergyTime, 0, 1);
                _lastEnergyTime = mean.Seconds;
            }
        };
        return buffer;
    }


    private System.Windows.Point? _panOrigin;
    private double _panStartValue;
    private (double X, double I, double U)[] _cursorPoints = Array.Empty<(double, double, double)>();
    private bool _dragging;
    private double _visibleFrom, _visibleTo;
    private long _lastCursorTicks;
    private void ChartPanStart(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        if (!_showLive || _monitorCancellation is not null || e.ChangedButton != System.Windows.Input.MouseButton.Left) return;
        _dragging = true;
        _panOrigin = e.GetPosition(Chart);
        _panStartValue = _historyMode ? _historyStart : Math.Max(0, _acquisitionClock.Elapsed.TotalSeconds - _windowSeconds);
        Chart.Cursor = System.Windows.Input.Cursors.Hand;
        Chart.CaptureMouse();
        e.Handled = true;
    }
    private void ChartPanMove(object sender, System.Windows.Input.MouseEventArgs e)
    {
        var position = e.GetPosition(Chart);
        UpdateCursor(position);
        if (!_dragging || _panOrigin is not { } origin || Chart.ActualWidth < 100) return;
        double delta = (position.X - origin.X) * _windowSeconds / Math.Max(1, Chart.ActualWidth - 80);
        double max = Math.Max(0, Math.Max(_lastRecordedHistorySeconds, _acquisitionClock.Elapsed.TotalSeconds) - _windowSeconds);
        _historyStart = Math.Clamp(_panStartValue - delta, 0, max);
        _historyMode = true;
        long now = Environment.TickCount64;
        if (now - _lastCursorTicks >= 25) { _lastCursorTicks = now; RenderHistory(); }
    }
    private void UpdateCursor(Point p)
    {
        if (!_showLive || _calibration is null || Chart.ActualWidth < 100) return;
        // Cursor-only WPF updates: no snapshot, IO or curve rebuilding on pointer motion.
        double fraction = Math.Clamp((p.X - 40) / Math.Max(1, Chart.ActualWidth - 80), 0, 1);
        double moment = _visibleFrom + fraction * Math.Max(.001, _visibleTo - _visibleFrom);
        var points = _cursorPoints;
        if (points.Length > 0)
        {
            int lo = 0, hi = points.Length - 1;
            while (lo < hi) { int mid = lo + (hi - lo) / 2; if (points[mid].X < moment) lo = mid + 1; else hi = mid; }
            int index = lo > 0 && Math.Abs(points[lo - 1].X - moment) < Math.Abs(points[lo].X - moment) ? lo - 1 : lo;
            var nearest = points[index];
            CursorReadout.Text = SensorMode && _activeSensor is not null
                ? $"t={nearest.X:F3}s   {_activeSensor.Name}: {_activeSensor.ConvertCalibrated(nearest.I, nearest.U):F3} {_activeSensor.OutputUnit}"
                : $"t={nearest.X:F3}s   I={nearest.I * _currentUnitScale:F3} {_currentUnitName}   U={nearest.U * _voltageUnitScale:F3} {_voltageUnitName}";
        }
        else CursorReadout.Text = $"t={moment:F3}s";
        CursorLine.X1 = CursorLine.X2 = p.X;
        CursorLine.Y1 = 20;
        CursorLine.Y2 = Math.Max(20, Chart.ActualHeight - 20);
        System.Windows.Controls.Canvas.SetLeft(CursorReadout, Math.Clamp(p.X + 12, 0, Math.Max(0, Chart.ActualWidth - 330)));
        System.Windows.Controls.Canvas.SetTop(CursorReadout, 36);
        CursorLine.Visibility = CursorReadout.Visibility = Visibility.Visible;
    }
    private void ChartPanEnd(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        _dragging = false;
        _panOrigin = null;
        Chart.Cursor = System.Windows.Input.Cursors.Cross;
        Chart.ReleaseMouseCapture();
    }
    private void ChartZoomWheel(object sender, System.Windows.Input.MouseWheelEventArgs e)
    {
        if (!_showLive || _monitorCancellation is not null) return;
        var p = e.GetPosition(Chart);
        double fraction = Math.Clamp((p.X - 40) / Math.Max(1, Chart.ActualWidth - 80), 0, 1);
        double anchor = _visibleFrom + fraction * _windowSeconds;
        _windowSeconds = Math.Clamp(_windowSeconds * (e.Delta > 0 ? .8 : 1.25), .1, 120);
        WindowSeconds.Text = _windowSeconds.ToString("0.###", CultureInfo.CurrentCulture);
        double max = Math.Max(0, Math.Max(_lastRecordedHistorySeconds, _acquisitionClock.Elapsed.TotalSeconds) - _windowSeconds);
        _historyStart = Math.Clamp(anchor - fraction * _windowSeconds, 0, max);
        _historyMode = true;
        RenderHistory();
        e.Handled = true;
    }

    private void ZoomInClick(object sender, RoutedEventArgs e) => ZoomWindow(.5);
    private void ZoomOutClick(object sender, RoutedEventArgs e) => ZoomWindow(2);
    private void ZoomWindow(double multiplier)
    {
        _windowSeconds = Math.Clamp(_windowSeconds * multiplier, .1, 120);
        WindowSeconds.Text = _windowSeconds.ToString("0.###", CultureInfo.CurrentCulture);
        UpdateHistoryRange();
        if (_historyMode) RenderHistory(); else RenderChart();
    }

    private void UpdateHistoryRange() => UpdateOverview();
    private void OverviewResized(object sender, SizeChangedEventArgs e) => UpdateOverview();

    private void ThemeChanged(object sender, SelectionChangedEventArgs e)
    {
        if (!IsLoaded || ThemeChoice.SelectedItem is not ComboBoxItem chosen) return;
        bool light = string.Equals(chosen.Content?.ToString(), "Hell", StringComparison.Ordinal);
        // WPF kann XAML-Ressourcen einfrieren (Freeze). Daher vorhandene Brushes
        // nicht verändern, sondern pro Farbwechsel neue Instanzen einsetzen.
        Application.Current.Resources["BackgroundBrush"] = new SolidColorBrush(
            light ? Color.FromRgb(232, 240, 248) : Color.FromRgb(16, 24, 39));
        Application.Current.Resources["CardBrush"] = new SolidColorBrush(
            light ? Color.FromRgb(38, 58, 81) : Color.FromRgb(28, 41, 59));
        Foreground = light ? Brushes.Black : Brushes.White;
        SaveDisplayPreferences();
    }

    private int _historyRequestVersion;
    private void RenderHistory()
    {
        if (!_showLive || _historyPath is null || !File.Exists(_historyPath)) return;
        lock (_receiveGate) _historyWriter?.Flush();
        var path = _historyPath;
        var from = Math.Clamp(_historyStart, 0, Math.Max(0, _lastRecordedHistorySeconds - _windowSeconds));
        _historyStart = from;
        var to = from + _windowSeconds;
        int requestId = Interlocked.Increment(ref _historyRequestVersion);
        // Disk access is never allowed to stall the WPF pointer or paint loop.
        _ = Task.Run(() =>
        {
            int columns = Math.Max(2, (int)(Chart.ActualWidth - 80));
            var sumsI = new double[columns];
            var sumsU = new double[columns];
            var sumsT = new double[columns];
            var counts = new int[columns];
            using var file = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            using var reader = new BinaryReader(file);
            long total = file.Length / 24;
            long low = 0, high = total;
            while (low < high)
            {
                long mid = low + (high - low) / 2;
                file.Position = mid * 24;
                if (reader.ReadDouble() < from) low = mid + 1; else high = mid;
            }
            file.Position = Math.Min(total, Math.Max(0, low - 1)) * 24;
            while (file.Position + 24 <= file.Length)
            {
                double time = reader.ReadDouble(), i = reader.ReadDouble(), u = reader.ReadDouble();
                if (time > to) break;
                if (time < from) continue;
                int bucket = (int)Math.Clamp((time - from) / Math.Max(1e-9, to - from) * (columns - 1), 0, columns - 1);
                sumsI[bucket] += i;
                sumsU[bucket] += u;
                sumsT[bucket] += time;
                counts[bucket]++;
            }
            var current = new List<(double X, double Y)>(columns);
            var voltage = new List<(double X, double Y)>(columns);
            for (int b = 0; b < columns; b++)
            {
                if (counts[b] == 0) continue;
                double time = sumsT[b] / counts[b];
                current.Add((time, sumsI[b] / counts[b]));
                voltage.Add((time, sumsU[b] / counts[b]));
            }
            return (current, voltage);
        }).ContinueWith(task =>
        {
            if (requestId != Volatile.Read(ref _historyRequestVersion)) return;
            if (task.IsFaulted)
            {
                Status.Text = "Verlauf: " + task.Exception?.GetBaseException().Message;
                return;
            }
            RenderVisiblePoints(task.Result.current, task.Result.voltage, from, to);
            UpdateOverview();
        }, CancellationToken.None, TaskContinuationOptions.None, TaskScheduler.FromCurrentSynchronizationContext());
    }

    private void ResetLiveDisplayForCalibration()
    {
        lock (_receiveGate)
        {
            _displayBuffer.Clear();
            ResetGlobalScale();
            _historyMode = false;
            _calI = _calU = 0;
            _calCount = 0;
            _renderedSampleCount = -1;
        }
        CurrentAxisCaption.Text = "Strom ←";
        VoltageAxisCaption.Text = "→ Spannung";
        if (_showLive)
        {
            Curve.Data = Geometry.Empty;
            VoltageCurve.Data = Geometry.Empty;
            PlotTitle.Text = _calibration is null
                ? "AD7765 BOARD · Kalibrierung erforderlich"
                : "AD7765 BOARD · Strom / Spannung";
            PlotDetail.Text = _calibration is null
                ? "Kalibrierprofil laden, um Messwerte darzustellen"
                : $"{_displayRate} Hz · kalibrierte Werte";
        }
    }

    private void LoadCalibrationClick(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog { Filter = "Java-Kalibrierdatei (*.properties)|*.properties" };
        if (dialog.ShowDialog() != true) return;
        try
        {
            var calibration = Ad7765BoardCalibration.Load(dialog.FileName);
            if (FtdiDevices.SelectedItem is FtdiDevice connected &&
                !string.Equals(calibration.DeviceName, connected.SerialNumber, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException($"Kalibrierdatei für {calibration.DeviceName}, ausgewähltes Gerät: {connected.SerialNumber}.");
            lock (_receiveGate) _calibration = calibration;
            ResetLiveDisplayForCalibration();
            CalibrationStatus.Text = $"Profil geladen: {calibration.DeviceName}";
        }
        catch (Exception ex) when (ex is IOException or UnauthorizedAccessException or InvalidDataException)
        {
            lock (_receiveGate) _calibration = null;
            ResetLiveDisplayForCalibration();
            CalibrationStatus.Text = "Kalibrierfehler: " + ex.Message;
        }
    }

    private async void FtdiScanClick(object sender, RoutedEventArgs e) => await ScanDevicesAsync();

    private string CalibrationPath(string serial)
    {
        var safe = string.Concat(serial.Where(c => char.IsLetterOrDigit(c) || c is '-' or '_'));
        return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "Leistungsmessung", "calibrations", "calib_" + safe + ".properties");
    }

    private void TryAutoLoadCalibration(string serial)
    {
        if (_calibration is not null && _calibration.DeviceName.Equals(serial, StringComparison.OrdinalIgnoreCase)) return;
        var path = CalibrationPath(serial);
        if (!File.Exists(path)) return;
        try
        {
            var profile = Ad7765BoardCalibration.Load(path);
            if (!profile.DeviceName.Equals(serial, StringComparison.OrdinalIgnoreCase))
                throw new InvalidDataException("Die Seriennummer im Profil stimmt nicht überein.");
            lock (_receiveGate) { _calibration = profile; ResetGlobalScale(); }
            CalibrationStatus.Text = "Automatisch geladen: " + serial;
        }
        catch (Exception ex) { CalibrationStatus.Text = "Kalibrierung konnte nicht geladen werden: " + ex.Message; }
    }

    private async Task ScanDevicesAsync()
    {
        if (_deviceScanInProgress || _ftdi.IsOpen || _monitorCancellation is not null || _measurementTransition) return;
        _deviceScanInProgress = true;
        try
        {
            // All native FTDI calls are serialized: no scan while a handle is in use.
            var devices = await Task.Run(() => _ftdi.Enumerate());
            string? selected = (FtdiDevices.SelectedItem as FtdiDevice)?.SerialNumber;
            string? preferred = selected;
            if (string.IsNullOrWhiteSpace(preferred) && File.Exists(_preferencesPath))
                preferred = File.ReadAllText(_preferencesPath).Trim();
            _enumeratedDevices = devices;
            FtdiDevices.ItemsSource = devices;
            FtdiDevices.SelectedItem = devices.FirstOrDefault(d =>
                string.Equals(d.SerialNumber, preferred, StringComparison.OrdinalIgnoreCase))
                ?? devices.FirstOrDefault(d => string.Equals(d.SerialNumber, "PMPP_DEV2", StringComparison.OrdinalIgnoreCase))
                ?? devices.FirstOrDefault();
            if (FtdiDevices.SelectedItem is FtdiDevice detected && _monitorCancellation is null)
                TryAutoLoadCalibration(detected.SerialNumber);
            FtdiStatus.Text = devices.Count == 0 ? "Kein FTDI-Gerät gefunden; erneute Suche läuft."
                : $"{devices.Count} FTDI-Gerät(e) erkannt · Auswahl: {(FtdiDevices.SelectedItem as FtdiDevice)?.SerialNumber}";
        }
        catch (Exception ex)
        {
            FtdiStatus.Text = "FTDI-Suche: " + ex.Message;
        }
        finally { _deviceScanInProgress = false; }
    }

    private async void StartMeasurementClick(object sender, RoutedEventArgs e)
    {
        if (_measurementTransition || _monitorCancellation is not null) return;
        _measurementTransition = true;
        StartMeasurementButton.IsEnabled = false;
        SensorStartButton.IsEnabled = false;
        try
        {
            if (!_ftdi.IsOpen)
            {
                if (FtdiDevices.SelectedItem is not FtdiDevice) throw new InvalidOperationException("Noch kein FTDI-Gerät erkannt. Bitte automatische Suche abwarten.");
                if (FtdiDevices.SelectedItem is not FtdiDevice device)
                    throw new InvalidOperationException("Kein Messgerät gefunden.");
                _ftdi.Open(device);
                Directory.CreateDirectory(Path.GetDirectoryName(_preferencesPath)!);
                File.WriteAllText(_preferencesPath, device.SerialNumber);
                FtdiStatus.Text = $"Verbunden: {device.Description} / {device.SerialNumber}";
            }
            MonitorStartClick(sender, e);
            if (_monitorCancellation is null) throw new InvalidOperationException("Empfang konnte nicht gestartet werden.");
            StopMeasurementButton.IsEnabled = true;
        SensorStopButton.IsEnabled = true;
            FtdiDevices.IsEnabled = false;
        }
        catch (Exception ex)
        {
            FtdiStatus.Text = "Messstart fehlgeschlagen: " + ex.Message;
            if (_ftdi.IsOpen && _monitorCancellation is null) _ftdi.Close();
            StartMeasurementButton.IsEnabled = true;
        SensorStartButton.IsEnabled = true;
        }
        finally { _measurementTransition = false; }
    }

    private async void StopMeasurementClick(object sender, RoutedEventArgs e)
    {
        if (_measurementTransition) return;
        _measurementTransition = true;
        StopMeasurementButton.IsEnabled = false;
        SensorStopButton.IsEnabled = false;
        try
        {
            _monitorCancellation?.Cancel();
            if (_receiveTask is not null) await _receiveTask;
            if (_ftdi.IsOpen) _ftdi.Close();
            FtdiStatus.Text = "Messung gestoppt · FTDI-Verbindung getrennt.";
            UpdateOverview();
        }
        catch (Exception ex) { FtdiStatus.Text = "Stop-Fehler: " + ex.Message; }
        finally
        {
            _measurementTransition = false;
            StartMeasurementButton.IsEnabled = true;
        SensorStartButton.IsEnabled = true;
            FtdiDevices.IsEnabled = true;
        }
    }

    private async void CalibrationWizardClick(object sender, RoutedEventArgs e)
    {
        if (_measurementTransition) return;
        bool startedForCalibration = _monitorCancellation is null;
        try
        {
            if (startedForCalibration)
            {
                StartMeasurementClick(sender, e);
                if (_monitorCancellation is null)
                    throw new InvalidOperationException("Hardware nicht erreichbar oder D2XX-Verbindung fehlgeschlagen.");
            }
            var serial = (FtdiDevices.SelectedItem as FtdiDevice)?.SerialNumber
                         ?? throw new InvalidOperationException("Kein Messgerät ausgewählt.");
            var wizard = new CalibrationWizard(serial, CaptureReferenceAsync, _calibration) { Owner = this };
            if (wizard.ShowDialog() == true && wizard.CreatedProfile is not null)
            {
                lock (_receiveGate) _calibration = wizard.CreatedProfile;
                ResetLiveDisplayForCalibration();
                CalibrationStatus.Text = $"Kalibrierung gespeichert: {serial}";
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show(this, ex.Message, "Hardwareprüfung / Kalibrierung", MessageBoxButton.OK, MessageBoxImage.Error);
        }
        finally
        {
            if (startedForCalibration && _monitorCancellation is not null)
            {
                _monitorCancellation.Cancel();
                try { if (_receiveTask is not null) await _receiveTask; }
                catch (OperationCanceledException) { }
                if (_ftdi.IsOpen) _ftdi.Close();
                StopMeasurementButton.IsEnabled = false;
        SensorStopButton.IsEnabled = false;
                StartMeasurementButton.IsEnabled = true;
        SensorStartButton.IsEnabled = true;
                FtdiDevices.IsEnabled = true;
            }
        }
    }

    private void FtdiConnectClick(object sender, RoutedEventArgs e)
    {
        if (FtdiDevices.SelectedItem is not FtdiDevice device)
        {
            FtdiStatus.Text = "Zuerst ein Gerät suchen und auswählen.";
            return;
        }
        try
        {
            if (_ftdi.IsOpen) { FtdiStatus.Text = "Bereits verbunden – zuerst trennen."; return; }
            _ftdi.Open(device);
            FtdiStatus.Text = $"D2XX geöffnet: {device.Description}, SN {device.SerialNumber}. Keine Messbefehle gesendet.";
        }
        catch (Exception ex) when (ex is DllNotFoundException
                                       or BadImageFormatException
                                       or EntryPointNotFoundException
                                       or FtdiException
                                       or InvalidOperationException)
        {
            FtdiStatus.Text = "Verbindungsfehler: " + ex.Message;
        }
    }
    private void MonitorStartClick(object sender, RoutedEventArgs e)
    {
        if (!_ftdi.IsOpen) { MonitorStatus.Text = "Bitte zuerst FTDI verbinden."; return; }
        if (_monitorCancellation is not null) return;
        lock (_receiveGate)
        {
            _capture?.Dispose();
            _sensorWriter?.Dispose();
            _sensorWriter = null;
            if (_activeSensor is not null)
            {
                string sensorPath = Path.Combine(Path.GetTempPath(), "Leistungsmessung_Sensor_" + Guid.NewGuid().ToString("N") + ".bin");
                _sensorWriter = new BinaryWriter(new FileStream(sensorPath, FileMode.CreateNew, FileAccess.Write, FileShare.Read, 65536));
            }
            _historyWriter?.Dispose(); _historyWrite?.Dispose();
            _historyPath = Path.Combine(Path.GetTempPath(), "Leistungsmessung_History_" + Guid.NewGuid().ToString("N") + ".bin");
            _historyWrite = new FileStream(_historyPath, FileMode.CreateNew, FileAccess.Write, FileShare.Read, 65536, FileOptions.SequentialScan);
            _historyWriter = new BinaryWriter(_historyWrite);
            _historyPoints = 0; _lastRecordedHistorySeconds = 0; ResetGlobalScale(); _energyJ = 0; _lastEnergyTime = -1; _historyMode = false;
            _capturePath = Path.Combine(Path.GetTempPath(), "Leistungsmessung_" + Guid.NewGuid().ToString("N") + ".bin");
            _capture = new FileStream(_capturePath, FileMode.CreateNew, FileAccess.Write, FileShare.Read, 262144, FileOptions.SequentialScan);
            _displayBuffer = NewDisplayBuffer(_displayRate, _windowSeconds);
            _acquisitionClock.Restart();
            _receivedBytes = 0;
            _evalDecoder.Reset();
            _boardDecoder.Reset();
            _liveSampleCount = 0;
            _displayBuffer.Clear();
            ResetGlobalScale();
            _minCurrent = _minVoltage = double.PositiveInfinity;
            _maxCurrent = _maxVoltage = double.NegativeInfinity;
            _sumCurrent = _sumVoltage = 0;
            _calI = _calU = 0; _calCount = 0;
            _captureLimitReached = false;
            _renderedSampleCount = 0;
            _lastDisplayBytes = 0;
            _lastDisplayTimestamp = Stopwatch.GetTimestamp();
            _receiveRate = 0;
            _pendingBytes = 0;
        }
        _showLive = DecodeBoardCheck.IsChecked == true;
        CurrentAxisCaption.Text = "Strom ←";
        VoltageAxisCaption.Text = "→ Spannung";
        bool boardMode = DecodeBoardCheck.IsChecked == true;
        bool evalMode = DecodeEvalCheck.IsChecked == true && !boardMode;
        bool calibrationEnabled = _calibration is not null;
        if (_showLive)
        {
            PlotTitle.Text = calibrationEnabled ? "AD7765 BOARD · Strom / Spannung" : "AD7765 BOARD · Kalibrierprofil erforderlich";
            PlotDetail.Text = calibrationEnabled
                ? "50 Hz Mittelwerte · 5 s gleitend · Strom links / Spannung rechts · Kalibrierwerte (Einheiten mit Java vergleichen)"
                : "50 Hz Mittelwerte · 5 s gleitend · Strom links / Spannung rechts (Rohwerte)";
            _points.Clear();
            RenderChart();
        }
        DecodeEvalCheck.IsEnabled = false;
        DecodeBoardCheck.IsEnabled = false;
        MonitorStartButton.IsEnabled = false;
        MonitorStatus.Text = "Passiver Empfang auf Hintergrundthread; keine Messbefehle.";
        _monitorCancellation = new CancellationTokenSource();
        _displayTimer.Start();
        var cancellationToken = _monitorCancellation.Token;
        _receiveTask = Task.Run(() => ReceiveLoop(cancellationToken, boardMode, evalMode));
    }

    private void ReceiveLoop(CancellationToken token, bool boardMode, bool evalMode)
    {
        try
        {
            while (!token.IsCancellationRequested)
            {
                // Drain the driver's queue in bounded 64 KiB batches without UI-thread stalls.
                // D2XX handle is exclusively owned by this worker until it terminates.
                int drained = 0;
                while (drained < 16 && !token.IsCancellationRequested)
                {
                    var pending = _ftdi.BytesAvailable();
                    lock (_receiveGate) _pendingBytes = pending;
                    if (pending == 0) break;
                    var bytes = _ftdi.ReadAvailable(65536);
                    if (bytes.Length == 0) break;
                    drained++;
                    lock (_receiveGate)
                    {
                        _receivedBytes += bytes.Length;
                        if (boardMode)
                        {
                            var frames = _boardDecoder.Feed(bytes);
                            foreach (var frame in frames)
                            foreach (var entry in frame.Samples)
                            {
                                if (!entry.CurrentIndicatorValid) continue;
                                var x = (double)_liveSampleCount++;
                                _lastCurrentRaw = entry.CurrentRaw;
                                _lastVoltageRaw = entry.VoltageRaw;
                                UpdateCalibrationSample(entry.CurrentRaw, entry.VoltageRaw);
                                _minCurrent = Math.Min(_minCurrent, entry.CurrentRaw);
                                _maxCurrent = Math.Max(_maxCurrent, entry.CurrentRaw);
                                _sumCurrent += entry.CurrentRaw;
                                _minVoltage = Math.Min(_minVoltage, entry.VoltageRaw);
                                _maxVoltage = Math.Max(_maxVoltage, entry.VoltageRaw);
                                _sumVoltage += entry.VoltageRaw;
                                // Calibration is applied before averaging, not to the already averaged raw ADC words.
                                // Range bits in CurrentRaw must remain intact during conversion.
                                var activeProfile = _calibration;
                                if (activeProfile is not null)
                                {
                                    if (activeProfile.TryConvert(entry.CurrentRaw, entry.VoltageRaw,
                                        out double currentCal, out double voltageCal) &&
                                        double.IsFinite(currentCal) && double.IsFinite(voltageCal))
                                    {
                                        _displayBuffer.Add(_acquisitionClock.Elapsed.TotalSeconds, currentCal, voltageCal);
                                         _allMinI = Math.Min(_allMinI, currentCal);
                                         _allMaxI = Math.Max(_allMaxI, currentCal);
                                         _allMinU = Math.Min(_allMinU, voltageCal);
                                         _allMaxU = Math.Max(_allMaxU, voltageCal);
                                        _calI += currentCal;
                                        _calU += voltageCal;
                                        _calCount++;
                                    }
                                }
                                // Without calibration, receive/capture continues but no raw values enter the live chart.
                            }

                        }
                        else if (evalMode) _evalDecoder.Feed(bytes);
                        _capture?.Write(bytes, 0, bytes.Length);
                    }
                }
                if (drained == 0) Thread.Sleep(2);
                else if (drained == 16) Thread.Yield();
            }
        }
        catch (Exception ex) when (ex is FtdiException or InvalidOperationException
                                   or DllNotFoundException or BadImageFormatException)
        {
            Dispatcher.BeginInvoke(() => MonitorStatus.Text = "Empfangsfehler: " + ex.Message);
        }
        finally
        {
            lock (_receiveGate) {
                _sampleRequest?.TrySetException(new OperationCanceledException("Die Messung wurde beendet."));
                _sampleRequest = null;
                _displayBuffer.FlushPending();
                _historyWriter?.Flush(); _historyWriter?.Dispose(); _historyWriter = null;
                _sensorWriter?.Flush(); _sensorWriter?.Dispose(); _sensorWriter = null;
                _historyWrite = null;
                _capture?.Flush(); _capture?.Dispose(); _capture = null;
            }
            Dispatcher.BeginInvoke(() =>
            {
                _displayTimer.Stop();
                _monitorCancellation?.Dispose();
                _monitorCancellation = null;
                _receiveTask = null;
                MonitorStartButton.IsEnabled = true;
                if (!_measurementTransition && _monitorCancellation is null)
                {
                    StartMeasurementButton.IsEnabled = true;
        SensorStartButton.IsEnabled = true;
                    StopMeasurementButton.IsEnabled = false;
        SensorStopButton.IsEnabled = false;
                    FtdiDevices.IsEnabled = true;
                }
                DecodeEvalCheck.IsEnabled = true;
                DecodeBoardCheck.IsEnabled = true;
            });
        }
    }

    private void DisplayTick(object? sender, EventArgs e)
    {
        if (_monitorCancellation is null) return;
        _historyMode = false;
        UpdateHistoryRange();
        List<(double X, double Y)> current, voltage;
        long count, bytes, calCount, validFrames, badFrames, discontinuities, evalCount;
        double minI, maxI, sumI, minU, maxU, sumU, calI, calU;
        int lastI, lastU;
        long captureLength;
        bool captureFull;
        uint pending;
        lock (_receiveGate)
        {
            // Snapshot only display data. Never hold the lock while drawing WPF geometry.
            var means = _showLive ? _displayBuffer.Snapshot() : Array.Empty<LiveMean>();
            current = means.Select(v => (v.Seconds, v.Current)).ToList();
            voltage = means.Select(v => (v.Seconds, v.Voltage)).ToList();
            count = _liveSampleCount; bytes = _receivedBytes;
            minI = _minCurrent; maxI = _maxCurrent; sumI = _sumCurrent;
            minU = _minVoltage; maxU = _maxVoltage; sumU = _sumVoltage;
            calI = _calI; calU = _calU; calCount = _calCount;
            validFrames = _boardDecoder.ValidFrames;
            badFrames = _boardDecoder.BadCrcFrames;
            discontinuities = _boardDecoder.CounterDiscontinuities;
            evalCount = _evalDecoder.SampleCount;
            lastI = _lastCurrentRaw; lastU = _lastVoltageRaw;
            captureLength = _capture?.Length ?? (_capturePath is not null && File.Exists(_capturePath) ? new FileInfo(_capturePath).Length : 0);
            captureFull = false; pending = _pendingBytes;
        }
        var now = Stopwatch.GetTimestamp();
        var elapsed = Stopwatch.GetElapsedTime(_lastDisplayTimestamp, now).TotalSeconds;
        if (elapsed > 0)
            _receiveRate = (bytes - _lastDisplayBytes) / elapsed;
        _lastDisplayTimestamp = now;
        _lastDisplayBytes = bytes;
        var sw = Stopwatch.StartNew();
        if (_showLive && (count != _renderedSampleCount || _renderedSampleCount < 0))
        {
            if (SensorMode && _activeSensor is not null && _calibration is not null && current.Count > 0 && voltage.Count > 0)
            {
                double sv = _activeSensor.ConvertCalibrated(current[^1].Y, voltage[^1].Y);
                SensorValueText.Text = double.IsFinite(sv) ? $"{sv:N3} {_activeSensor.OutputUnit}" : "Ungültiger Sensorwert";
            }
            if (!_historyMode) RenderLiveChart(current, voltage);
            _renderedSampleCount = count;
        }
        sw.Stop(); _displayMs = (int)sw.ElapsedMilliseconds;
        if (_showLive && count > 0)
        {
            if (_calibration is not null && current.Count > 0 && voltage.Count > 0)
            {
                double i = current[^1].Y, u = voltage[^1].Y;
                CurrentValue.Text = $"{i * _currentUnitScale:N3} {_currentUnitName}";
                VoltageValue.Text = $"{u * _voltageUnitScale:N3} {_voltageUnitName}";
                PowerValue.Text = $"{i * u:N3} W";
                EnergyValue.Text = $"{_energyJ:N3} J";
            }
            else
                CurrentValue.Text = VoltageValue.Text = PowerValue.Text = EnergyValue.Text = "Kalibrierung erforderlich";
            if (_calibration is not null)
                LiveStats.Text = calCount > 0
                    ? $"Kalibriert: I Ø {calI / calCount:N5} · U Ø {calU / calCount:N5} · Samples {calCount:N0} · Werte gegen Java prüfen"
                    : "Kalibrierprofil aktiv – warte auf gültige Samples und Messbereiche.";
            else
                LiveStats.Text = "Kein gültiges Kalibrierprofil – Rohdaten werden aufgezeichnet, aber nicht dargestellt.";
        }
        if (_showLive)
            DecodeStatus.Text = $"AD7765 BOARD: {validFrames:N0} Frames · CRC {badFrames:N0} · Zählerlücken {discontinuities:N0} · Bereich {_lastObservedAdcRange} · Codes {string.Join(",", _observedRangeCounts.Select((count, index) => $"{index}:{count}"))} · I={lastI}, U={lastU} (roh)";
        else if (evalCount > 0)
            DecodeStatus.Text = $"AD7765 EVAL: {evalCount:N0} Samples";
        Status.Text = $"Datenrate {_receiveRate / 1024:N1} KiB/s · {validFrames:N0} Frames · CRC {badFrames:N0} · FTDI {pending:N0} B";
        MonitorStatus.Text = $"Empfang {_receiveRate / 1024:N1} KiB/s · FTDI-Puffer {pending:N0} B · Anzeige {_displayMs} ms · " +
            $"Samples angezeigt bis {_renderedSampleCount:N0}/{count:N0} · Mitschnitt {captureLength:N0} B" +
            (captureFull ? "" : "") + " · keine Messbefehle";
    }

    private void RenderVisiblePoints(List<(double X, double Y)> current, List<(double X, double Y)> voltage, double from, double to)
    {
        if (Chart.ActualWidth <= 100 || Chart.ActualHeight <= 50) return;
        int columns = Math.Max(2, (int)(Chart.ActualWidth - 80));
        // Only one mean per physical pixel. This also bounds geometry allocation.
        var bins = new (double si, double su, int n, double time)[columns];
        int n = Math.Min(current.Count, voltage.Count);
        for (int j = 0; j < n; j++)
        {
            double x = current[j].X;
            if (x < from || x > to) continue;
            int b = (int)Math.Clamp((x - from) / Math.Max(1e-9, to - from) * (columns - 1), 0, columns - 1);
            bins[b].si += current[j].Y;
            bins[b].su += voltage[j].Y;
            bins[b].time += x;
            bins[b].n++;
        }
        var iPoints = new List<(double X, double Y)>(columns);
        var uPoints = new List<(double X, double Y)>(columns);
        var cursor = new List<(double X, double I, double U)>(columns);
        for (int b = 0; b < bins.Length; b++)
        {
            var value = bins[b];
            if (value.n == 0) continue;
            double x = value.time / value.n, i = value.si / value.n, u = value.su / value.n;
            iPoints.Add((x, i)); uPoints.Add((x, u)); cursor.Add((x, i, u));
        }
        if (SensorMode)
        {
            if (_activeSensor is null || _calibration is null)
            {
                Curve.Data = Geometry.Empty;
                VoltageCurve.Data = Geometry.Empty;
                EmptyMessage.Text = "Sensor und Kalibrierprofil auswählen";
                EmptyMessage.Visibility = Visibility.Visible;
                return;
            }
            var sensorPoints = cursor.Select(p => (p.X, Y: _activeSensor.ConvertCalibrated(p.I, p.U)))
                .Where(p => double.IsFinite(p.Y)).ToList();
            double low = sensorPoints.Count > 0 ? sensorPoints.Min(p => p.Y) : 0;
            double high = sensorPoints.Count > 0 ? sensorPoints.Max(p => p.Y) : 1;
            if (high <= low) { low -= 0.5; high += 0.5; }
            var geometry = new StreamGeometry();
            using (var context = geometry.Open())
            {
                bool first = true;
                foreach (var p in sensorPoints)
                {
                    var point = new Point(40 + (p.X - from) / Math.Max(1e-9, to - from) * Math.Max(1, Chart.ActualWidth - 80),
                        20 + (1 - (p.Y - low) / (high - low)) * Math.Max(1, Chart.ActualHeight - 42));
                    if (first) { context.BeginFigure(point, false, false); first = false; }
                    else context.LineTo(point, true, false);
                }
            }
            geometry.Freeze();
            Curve.Data = geometry;
            Curve.Stroke = Brushes.LightGreen;
            VoltageCurve.Data = Geometry.Empty;
            _cursorPoints = cursor.ToArray();
            _visibleFrom = from; _visibleTo = to;
            CurrentAxisCaption.Text = $"{_activeSensor.Name} ({_activeSensor.OutputUnit})";
            LeftTopAxis.Text = $"{high:F3}";
            LeftBottomAxis.Text = $"{low:F3}";
            RightTopAxis.Visibility = RightBottomAxis.Visibility = Visibility.Collapsed;
            ChartGrid.Children.Clear();
            EmptyMessage.Visibility = sensorPoints.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
            EmptyMessage.Text = "Keine gültigen Sensorwerte im Zeitabschnitt";
            return;
        }
        Curve.Stroke = Brushes.LightSkyBlue;
        RightTopAxis.Visibility = RightBottomAxis.Visibility = Visibility.Visible;
        CurrentAxisCaption.Text = "Strom ←";
        _visibleFrom = from; _visibleTo = to;
        _cursorPoints = cursor.ToArray();
        Curve.Data = LiveGeometry(iPoints, from, to, true);
        VoltageCurve.Data = LiveGeometry(uPoints, from, to, false);
        EmptyMessage.Text = _showLive ? "Im gewählten Zeitabschnitt keine aufgezeichneten Messwerte" : "Datei öffnen, um die Messkurve anzuzeigen";
        EmptyMessage.Visibility = cursor.Count == 0 ? Visibility.Visible : Visibility.Collapsed;
    }
    private void RenderLiveChart(List<(double X, double Y)> current, List<(double X, double Y)> voltage)
    {
        double end = Math.Max(_windowSeconds, _acquisitionClock.Elapsed.TotalSeconds);
        RenderVisiblePoints(current, voltage, end - _windowSeconds, end);
        UpdateOverview();
    }
    private long _overviewLastRefreshTick;
    private bool _overviewReading;
    private void RefreshOverviewTrace(double totalSeconds)
    {
        if (_overviewReading || _historyPath is null || !File.Exists(_historyPath)) return;
        if (Environment.TickCount64 - _overviewLastRefreshTick < 1200) return;
        _overviewLastRefreshTick = Environment.TickCount64;
        string path = _historyPath;
        int columns = (int)Math.Max(2, OverviewCanvas.ActualWidth);
        bool wasSensorMode = SensorMode;
        var activeSensor = wasSensorMode ? _activeSensor : null;
        bool missingSensor = SensorMode && (activeSensor is null || _calibration is null);
        if (missingSensor) { OverviewTrace.Data = Geometry.Empty; return; }
        _overviewReading = true;
        _ = Task.Run(() =>
        {
            var raw = new List<(double X, double Y)>(columns);
            using var file = new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            using var reader = new BinaryReader(file);
            long count = file.Length / 24;
            for (int j = 0; j < columns && count > 0; j++)
            {
                long index = Math.Min(count - 1, (long)((double)j / Math.Max(1, columns - 1) * (count - 1)));
                file.Position = index * 24;
                double t = reader.ReadDouble();
                double i = reader.ReadDouble();
                double u = reader.ReadDouble();
                double value = activeSensor is null ? i : activeSensor.ConvertCalibrated(i, u);
                if (double.IsFinite(value)) raw.Add((t, value));
            }
            return raw;
        }).ContinueWith(task =>
        {
            _overviewReading = false;
            if (wasSensorMode != SensorMode || (wasSensorMode && !ReferenceEquals(activeSensor, _activeSensor))) return;
            if (task.IsFaulted || task.Result.Count < 2) return;
            var samples = task.Result;
            double low = samples.Min(v => v.Y), high = samples.Max(v => v.Y);
            if (high <= low) high = low + 1;
            double height = Math.Max(1, OverviewCanvas.ActualHeight - 4);
            var geometry = new StreamGeometry();
            using (var context = geometry.Open())
                for (int j = 0; j < samples.Count; j++)
                {
                    var pt = new Point(j * (double)(columns - 1) / (samples.Count - 1),
                        2 + height * (1 - (samples[j].Y - low) / (high - low)));
                    if (j == 0) context.BeginFigure(pt, false, false);
                    else context.LineTo(pt, true, false);
                }
            geometry.Freeze();
            OverviewTrace.Data = geometry;
        }, CancellationToken.None, TaskContinuationOptions.None, TaskScheduler.FromCurrentSynchronizationContext());
    }

    private void UpdateOverview()
    {
        if (OverviewCanvas.ActualWidth <= 1) return;
        double total = Math.Max(_windowSeconds, _lastRecordedHistorySeconds);
        double width = OverviewCanvas.ActualWidth;
        double left = _historyMode ? _historyStart / total * width : Math.Max(0, total - _windowSeconds) / total * width;
        double selectionWidth = Math.Clamp(_windowSeconds / total * width, 4, width);
        OverviewSelection.Width = selectionWidth;
        System.Windows.Controls.Canvas.SetLeft(OverviewSelection, Math.Clamp(left, 0, Math.Max(0, width - selectionWidth)));
        OverviewDescription.Text = $"Gesamt: {total:F1} s   ·   Ansicht: {_windowSeconds:F2} s";
        RefreshOverviewTrace(total);
    }
    private void OverviewMouseDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        if (!_showLive || _monitorCancellation is not null) return;
        double duration = Math.Max(_windowSeconds, _lastRecordedHistorySeconds);
        double fraction = e.GetPosition(OverviewCanvas).X / Math.Max(1, OverviewCanvas.ActualWidth);
        _historyStart = Math.Clamp(fraction * duration - _windowSeconds / 2, 0, Math.Max(0, duration - _windowSeconds));
        _historyMode = true;
        _overviewDragging = true;
        _overviewDragStartX = e.GetPosition(OverviewCanvas).X;
        _overviewDragStartHistory = _historyStart;
        OverviewCanvas.CaptureMouse();
        OverviewCanvas.Cursor = System.Windows.Input.Cursors.Hand;
        RenderHistory();
    }

    private void MonitorStopClick(object sender, RoutedEventArgs e)
    {
        _monitorCancellation?.Cancel();
    }

    private void SaveCaptureClick(object sender, RoutedEventArgs e)
    {
        if (_receivedBytes == 0)
        {
            MonitorStatus.Text = "Keine Bytes empfangen. Es gibt noch keinen Mitschnitt.";
            return;
        }
        var picker = new SaveFileDialog
        {
            Filter = "Binäre Rohdaten (*.bin)|*.bin",
            FileName = "ftdi_capture.bin"
        };
        if (picker.ShowDialog() != true) return;
        try
        {
            lock (_receiveGate) _capture?.Flush();
            if (_capturePath is null) throw new IOException("Keine temporäre Datei vorhanden.");
            File.Copy(_capturePath, picker.FileName, overwrite: true);
            MonitorStatus.Text = $"Mitschnitt gespeichert: {new FileInfo(picker.FileName).Length:N0} Bytes.";
        }
        catch (IOException ex) { MonitorStatus.Text = "Speicherfehler: " + ex.Message; }
    }

    private async void FtdiDisconnectClick(object sender, RoutedEventArgs e)
    {
        _monitorCancellation?.Cancel();
        if (_receiveTask is not null)
            try { await _receiveTask; } catch (OperationCanceledException) { }
        try
        {
            _ftdi.Close();
            FtdiStatus.Text = "D2XX-Verbindung getrennt.";
        }
        catch (FtdiException ex) { FtdiStatus.Text = ex.Message; }
    }
    protected override void OnClosed(EventArgs e)
    {
        _deviceScanTimer.Stop();
        _monitorCancellation?.Cancel();
        try
        {
            _receiveTask?.Wait(TimeSpan.FromSeconds(2));
            if (_receiveTask is null || _receiveTask.IsCompleted) _ftdi.Dispose();
        }
        catch (Exception) { /* Process exit releases device handle. */ }
        base.OnClosed(e);
    }

    private sealed record DisplayPreferences(
        double Seconds, int Frequency, bool FixedAxes,
        string CurrentUnit, string VoltageUnit,
        double CurrentMin, double CurrentMax, double VoltageMin, double VoltageMax,
        string Theme);
    private readonly string _displayPreferencesPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "Leistungsmessung", "display-settings.json");

    private void SaveDisplayPreferences()
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(_displayPreferencesPath)!);
            var prefs = new DisplayPreferences(_windowSeconds, _displayRate, _fixedAxes,
                _currentUnitName, _voltageUnitName,
                double.Parse(CurrentMin.Text, CultureInfo.CurrentCulture),
                double.Parse(CurrentMax.Text, CultureInfo.CurrentCulture),
                double.Parse(VoltageMin.Text, CultureInfo.CurrentCulture),
                double.Parse(VoltageMax.Text, CultureInfo.CurrentCulture),
                (ThemeChoice.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "Dunkel");
            File.WriteAllText(_displayPreferencesPath, JsonSerializer.Serialize(prefs));
        }
        catch (Exception ex) { Status.Text = "Einstellungen nicht gespeichert: " + ex.Message; }
    }
    private void LoadDisplayPreferences()
    {
        CurrentMin.Text = "0"; CurrentMax.Text = "100";
        VoltageMin.Text = "0"; VoltageMax.Text = "5";
        if (!File.Exists(_displayPreferencesPath)) return;
        try
        {
            var p = JsonSerializer.Deserialize<DisplayPreferences>(File.ReadAllText(_displayPreferencesPath));
            if (p is null) return;
            WindowSeconds.Text = p.Seconds.ToString(CultureInfo.CurrentCulture);
            DisplayRate.Text = p.Frequency.ToString(CultureInfo.CurrentCulture);
            FixedAxes.IsChecked = p.FixedAxes;
            CurrentMin.Text = p.CurrentMin.ToString(CultureInfo.CurrentCulture);
            CurrentMax.Text = p.CurrentMax.ToString(CultureInfo.CurrentCulture);
            VoltageMin.Text = p.VoltageMin.ToString(CultureInfo.CurrentCulture);
            VoltageMax.Text = p.VoltageMax.ToString(CultureInfo.CurrentCulture);
            foreach (ComboBox control in new[] { CurrentUnit, VoltageUnit, ThemeChoice })
            {
                string value = ReferenceEquals(control, CurrentUnit) ? p.CurrentUnit :
                    ReferenceEquals(control, VoltageUnit) ? p.VoltageUnit : p.Theme;
                foreach (var item in control.Items.OfType<ComboBoxItem>())
                    if (string.Equals(item.Content?.ToString(), value, StringComparison.Ordinal))
                    { control.SelectedItem = item; break; }
            }
            // Apply through the same validation path as the settings button.
            ApplyDisplaySettingsClick(this, new RoutedEventArgs());
        }
        catch (Exception ex) { Status.Text = "Einstellungen nicht geladen: " + ex.Message; }
    }

    public MainWindow()
    {
        InitializeComponent();
        LoadDisplayPreferences();
        LoadSensors();
        _displayTimer.Tick += DisplayTick;
        _deviceScanTimer.Tick += async (_, _) => await ScanDevicesAsync();
        _deviceScanTimer.Start();
        Loaded += async (_, _) =>
        {
            ThemeChanged(this, null!);
            await ScanDevicesAsync();
        };
        FtdiDevices.SelectionChanged += (_, _) =>
        {
            if (FtdiDevices.SelectedItem is FtdiDevice selected && _monitorCancellation is null)
                TryAutoLoadCalibration(selected.SerialNumber);
        };
    }
    private void OpenClick(object sender,RoutedEventArgs e)
    {
        var picker = new OpenFileDialog { Filter="Messdateien (*.rdf;*.csv)|*.rdf;*.csv|Alle Dateien (*.*)|*.*" };
        if (picker.ShowDialog()!=true) return;
        try
        {
            if (System.IO.Path.GetExtension(picker.FileName).Equals(".rdf",StringComparison.OrdinalIgnoreCase))
            {
                var m=RdfReader.Read(picker.FileName);
                _points=m.Samples.Select(s=>(s.TimeSeconds,s.Current)).ToList();
                PlotTitle.Text="RDF · Stromverlauf";
                PlotDetail.Text=$"Version {m.Header.Version:F1} · {m.Samples.Count:N0} Messpunkte · {m.StateChangeCount} Statuswechsel · ADC-Modus {m.Header.AdcMode}";
            }
            else
            {
                var stats=CsvStatisticsReader.Read(picker.FileName);
                _points=stats.Select(s=>(s.TimeSeconds,s.CurrentMilliAmpere)).ToList();
                PlotTitle.Text="CSV · mittlerer Strom [mA]";
                PlotDetail.Text=$"{stats.Count:N0} Statistikintervalle · Zeitachse [s]";
            }
            _showLive = false;
            LiveStats.Text = "Dateiansicht · Live-Statistik nur beim aktiven Empfang";
            Status.Text=picker.FileName;
            RenderChart();
        }
        catch(Exception ex)
        {
            MessageBox.Show(this,ex.Message,"Importfehler",MessageBoxButton.OK,MessageBoxImage.Error);
            Status.Text="Import nicht erfolgreich: "+ex.Message;
        }
    }
    private void ClearLiveClick(object sender, RoutedEventArgs e)
    {
        lock (_receiveGate)
        {
            _liveSampleCount = 0;
            _displayBuffer.Clear();
            ResetGlobalScale();
            _minCurrent = _minVoltage = double.PositiveInfinity;
            _maxCurrent = _maxVoltage = double.NegativeInfinity;
            _sumCurrent = _sumVoltage = 0;
            _calI = _calU = 0; _calCount = 0;
            _renderedSampleCount = 0;
        }
        LiveStats.Text = "Live-Statistik zurückgesetzt";
        Curve.Data = Geometry.Empty;
        VoltageCurve.Data = Geometry.Empty;
    }
    private void ResetClick(object sender,RoutedEventArgs e) => RenderChart();
    private void ChartResized(object sender,SizeChangedEventArgs e) { RenderChart(); DrawGrid(); }
    private Geometry MakeGeometry(List<(double X, double Y)> data)
    {
        double w = Math.Max(1, Chart.ActualWidth - 40);
        double h = Math.Max(1, Chart.ActualHeight - 40);
        double minX = data[0].X, maxX = data[^1].X;
        if (maxX <= minX) maxX = minX + 1;
        double minY = data.Min(v => v.Y), maxY = data.Max(v => v.Y);
        if (maxY <= minY) maxY = minY + 1;
        var geometry = new StreamGeometry();
        using (var ctx = geometry.Open())
        {
            // Pixel-bucket extrema; O(n) with only two segments per pixel.
            int bucket = -1;
            (double X, double Y) low = default, high = default;
            bool started = false;
            void Add((double X, double Y) v)
            {
                var p = new Point(20 + (v.X - minX) / (maxX - minX) * w,
                                  20 + h - (v.Y - minY) / (maxY - minY) * h);
                if (!started) { ctx.BeginFigure(p, false, false); started = true; }
                else ctx.LineTo(p, true, false);
            }
            foreach (var v in data)
            {
                int b = (int)Math.Clamp((v.X - minX) / (maxX - minX) * w, 0, w);
                if (b != bucket)
                {
                    if (bucket >= 0) { Add(low); Add(high); }
                    bucket = b; low = high = v;
                }
                else
                {
                    if (v.Y < low.Y) low = v;
                    if (v.Y > high.Y) high = v;
                }
            }
            if (bucket >= 0) { Add(low); Add(high); }
        }
        geometry.Freeze();
        return geometry;
    }
    private static (double Min, double Max) NiceBounds(double min, double max)
    {
        var range = Math.Max(max - min, 1e-12);
        var step = Math.Pow(10, Math.Floor(Math.Log10(range / 5)));
        foreach (var factor in new[] { 1.0, 2.0, 5.0, 10.0 })
        {
            if (range / (step * factor) <= 6) { step *= factor; break; }
        }
        return (Math.Floor(min / step) * step, Math.Ceiling(max / step) * step);
    }

    private void DrawGrid()
    {
        ChartGrid.Children.Clear();
        if (Chart.ActualWidth < 100 || Chart.ActualHeight < 100) return;
        double height = Chart.ActualHeight - 42;
        // Aligned horizontal divisions for both physical axes.
        for (int line = 0; line <= 5; line++)
        {
            var y = 20 + height * line / 5;
            var gridLine = new System.Windows.Shapes.Line {
                X1 = 40, X2 = Chart.ActualWidth - 40, Y1 = y, Y2 = y,
                Stroke = new SolidColorBrush(Color.FromRgb(60, 79, 103)),
                StrokeThickness = 0.75, IsHitTestVisible = false
            };
            ChartGrid.Children.Add(gridLine);
            if (_calibration is null) continue;
            var (iLo, iHi) = _fixedAxes ? (_iMin, _iMax) :
                (double.IsFinite(_allMinI) && double.IsFinite(_allMaxI) ? NiceBounds(_allMinI, _allMaxI) : (0.0, 1.0));
            var (uLo, uHi) = _fixedAxes ? (_uMin, _uMax) :
                (double.IsFinite(_allMinU) && double.IsFinite(_allMaxU) ? NiceBounds(_allMinU, _allMaxU) : (0.0, 1.0));
            double ci = iHi - (iHi - iLo) * line / 5;
            double vu = uHi - (uHi - uLo) * line / 5;
            // Preserve matching grid heights, one value for each physical axis.
            if (line > 0 && line < 5 && double.IsFinite(ci) && double.IsFinite(vu))
            {
                var left = new TextBlock { Text = (ci * _currentUnitScale).ToString("F3", CultureInfo.CurrentCulture),
                    Foreground = Brushes.LightSkyBlue, FontSize = 10, IsHitTestVisible = false };
                Canvas.SetLeft(left, 2); Canvas.SetTop(left, y - 7);
                ChartGrid.Children.Add(left);
                var right = new TextBlock { Text = (vu * _voltageUnitScale).ToString("F3", CultureInfo.CurrentCulture),
                    Foreground = Brushes.Orange, FontSize = 10, IsHitTestVisible = false };
                Canvas.SetRight(right, 1); Canvas.SetTop(right, y - 7);
                ChartGrid.Children.Add(right);
            }
        }
    }

    private Geometry LiveGeometry(List<(double X, double Y)> data, double from, double to, bool current)
    {
        double width = Math.Max(1, Chart.ActualWidth - 80);
        double height = Math.Max(1, Chart.ActualHeight - 42);
        double lo = current ? _iMin : _uMin, hi = current ? _iMax : _uMax;
        if (!_fixedAxes)
        {
            lo = current ? _allMinI : _allMinU;
            hi = current ? _allMaxI : _allMaxU;
            if (!double.IsFinite(lo) || !double.IsFinite(hi)) return Geometry.Empty;
            if (hi <= lo) { hi += 0.5; lo -= 0.5; }
            (lo, hi) = NiceBounds(lo, hi);
        }
        if (current) DrawGrid();
        if (current)
        {
            LeftTopAxis.Text = $"{hi * (_calibration is null ? 1 : _currentUnitScale):N3} {(_calibration is null ? "raw" : _currentUnitName)}";
            LeftBottomAxis.Text = $"{lo * (_calibration is null ? 1 : _currentUnitScale):N3}";
        }
        else
        {
            RightTopAxis.Text = $"{hi * (_calibration is null ? 1 : _voltageUnitScale):N3} {(_calibration is null ? "raw" : _voltageUnitName)}";
            RightBottomAxis.Text = $"{lo * (_calibration is null ? 1 : _voltageUnitScale):N3}";
        }
        var geometry = new StreamGeometry();
        using (var ctx = geometry.Open())
        {
            bool first = true;
            foreach (var v in data)
            {
                if (v.X < from || v.X > to) continue;
                var point = new Point(40 + (v.X - from) / (to - from) * width,
                    20 + height * (1 - (v.Y - lo) / (hi - lo)));
                if (first) { ctx.BeginFigure(point, false, false); first = false; }
                else ctx.LineTo(point, true, false);
            }
        }
        geometry.Freeze();
        return geometry;
    }
    private void ApplyDisplaySettingsClick(object sender, RoutedEventArgs e)
    {
        if (!double.TryParse(WindowSeconds.Text, out double seconds) || seconds < .1 || seconds > 120 ||
            !int.TryParse(DisplayRate.Text, out int rate) || rate < 10 || rate > 1000 ||
            !double.TryParse(CurrentMin.Text, out double imin) ||
            !double.TryParse(CurrentMax.Text, out double imax) ||
            !double.TryParse(VoltageMin.Text, out double umin) ||
            !double.TryParse(VoltageMax.Text, out double umax) ||
            imin >= imax || umin >= umax)
        {
            MessageBox.Show("Bitte gültige Skalengrenzen und eine Zeit zwischen 1 und 120 Sekunden eingeben.");
            return;
        }
        lock (_receiveGate)
        {
            _windowSeconds = seconds;
            _displayRate = rate;
            _currentUnitName = (CurrentUnit.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "mA";
            _voltageUnitName = (VoltageUnit.SelectedItem as ComboBoxItem)?.Content?.ToString() ?? "V";
            _currentUnitScale = _currentUnitName == "µA" ? 1_000_000 : 1000;
            _voltageUnitScale = _voltageUnitName == "mV" ? 1000 : 1;
            _fixedAxes = FixedAxes.IsChecked == true;
            _iMin = imin / (_calibration is null ? 1 : _currentUnitScale);
            _iMax = imax / (_calibration is null ? 1 : _currentUnitScale);
            _uMin = umin / (_calibration is null ? 1 : _voltageUnitScale);
            _uMax = umax / (_calibration is null ? 1 : _voltageUnitScale);
            // Changing averaging frequency resets the live viewport, not the raw data file.
            // Historical points recorded at a former rate remain on disk.
            _displayBuffer = NewDisplayBuffer(rate, seconds);
            _historyMode = false;
        }
        _displayTimer.Interval = TimeSpan.FromMilliseconds(Math.Max(20, 1000.0 / _displayRate));
        UpdateHistoryRange();
        PlotDetail.Text = $"{_displayRate} Hz · Fenster {_windowSeconds:0.###} s · Anzeigewerte gemittelt";
        RenderChart();
        SaveDisplayPreferences();
    }
    private void RenderChart()
    {
        if (!_showLive) { Curve.Data=Geometry.Empty; VoltageCurve.Data=Geometry.Empty; }
        if (SensorMode && !_showLive)
        {
            Curve.Data = Geometry.Empty;
            VoltageCurve.Data = Geometry.Empty;
            EmptyMessage.Text = "Sensormessung starten, um Sensorwerte anzuzeigen";
            EmptyMessage.Visibility = Visibility.Visible;
            return;
        }
        if (_showLive)
        {
            LiveMean[] means;
            lock (_receiveGate) means = _displayBuffer.Snapshot();
            UpdateSensorDisplay(means);
            if (_historyMode) { RenderHistory(); return; }
            if (means.Length > 1)
                RenderLiveChart(means.Select(v => (v.Seconds, v.Current)).ToList(),
                                means.Select(v => (v.Seconds, v.Voltage)).ToList());
            return;
        }
        EmptyMessage.Visibility=_points.Count==0?Visibility.Visible:Visibility.Collapsed;
        if (_points.Count<2 || Chart.ActualWidth<2 || Chart.ActualHeight<2) return;
        // Bounded rendering: at most two extrema per pixel column.
        double w=Chart.ActualWidth-40,h=Chart.ActualHeight-40;
        var values=_points.Where(p=>double.IsFinite(p.X)&&double.IsFinite(p.Y)).ToList();
        if(values.Count<2)return;
        double minX=values.Min(p=>p.X),maxX=values.Max(p=>p.X);
        double minY=values.Min(p=>p.Y),maxY=values.Max(p=>p.Y);
        if (maxX<=minX)maxX=minX+1;
        if (maxY<=minY)maxY=minY+1;
        var groups=values.GroupBy(p=>(int)Math.Clamp((p.X-minX)/(maxX-minX)*w,0,w));
        var geometry=new StreamGeometry();
        using(var context=geometry.Open())
        {
            bool first=true;
            foreach(var group in groups.OrderBy(g=>g.Key))
            {
                foreach(var p in new[]{group.MinBy(v=>v.Y),group.MaxBy(v=>v.Y)})
                {
                    var point=new Point(20+(p.X-minX)/(maxX-minX)*w,20+h-(p.Y-minY)/(maxY-minY)*h);
                    if(first){context.BeginFigure(point,false,false);first=false;}
                    else context.LineTo(point,true,false);
                }
            }
        }
        geometry.Freeze();
        Curve.Data=geometry;
    }
}
