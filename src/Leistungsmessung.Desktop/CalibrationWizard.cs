using System.Globalization;
using System.IO;
using Microsoft.Win32;
using System.Windows;
using System.Windows.Controls;
using Leistungsmessung.Core;

namespace Leistungsmessung.Desktop;

/// <summary>Geführte Kalibrierung mit 1000 gültigen AD7765-Samples je Referenzpunkt.</summary>
public sealed class CalibrationWizard : Window
{
    private readonly string _serial;
    private readonly Func<int, Task<(double Current, double Voltage)>> _capture;
    private Ad7765BoardCalibration? _draft;
    private readonly CheckBox _voltage = new() { Content = "Spannung", IsChecked = true, Margin = new Thickness(8) };
    private readonly CheckBox _current1 = new() { Content = "Strom 1 (10–2500 mA)", Margin = new Thickness(8) };
    private readonly CheckBox _current2 = new() { Content = "Strom 2 (0–10 mA)", Margin = new Thickness(8) };
    private readonly TextBox _reference = new() { Width = 115, Text = "0", Margin = new Thickness(8) };
    private readonly ComboBox _unit = new() { Width = 115, Margin = new Thickness(8) };
    private readonly Button _next = new() { Content = "Hardware prüfen", MinWidth = 185, Padding = new Thickness(12, 7, 12, 7) };
    private readonly TextBlock _status = new() { TextWrapping = TextWrapping.Wrap, Margin = new Thickness(4, 12, 4, 12), FontSize = 14 };
    private readonly TextBlock _step = new() { FontSize = 17, FontWeight = FontWeights.SemiBold };
    private readonly StackPanel _selection = new();
    private readonly StackPanel _measurement = new();
    private readonly List<(string Name, int Range)> _areas = new();
    private int _areaIndex, _pointIndex, _phase;
    private (double Current, double Voltage) _first;
    private double _firstReference;
    public Ad7765BoardCalibration? CreatedProfile { get; private set; }

    public CalibrationWizard(string serial, Func<int, Task<(double Current, double Voltage)>> capture,
        Ad7765BoardCalibration? original)
    {
        _serial = serial;
        _capture = capture;
        _draft = original;
        Title = "Kalibrierassistent · " + serial;
        Width = 620; Height = 440; MinWidth = 550; MinHeight = 380;
        WindowStartupLocation = WindowStartupLocation.CenterOwner;
        Background = System.Windows.Media.Brushes.White;
        Foreground = System.Windows.Media.Brushes.Black;
        var panel = new StackPanel { Margin = new Thickness(25) };
        Content = new ScrollViewer { Content = panel };
        panel.Children.Add(new TextBlock { Text = "Kalibrierung für " + serial, FontSize = 23, FontWeight = FontWeights.Bold });
        panel.Children.Add(_step);
        panel.Children.Add(_status);
        _selection.Children.Add(new TextBlock { Text = "Zu kalibrierende Bereiche (auch alle drei möglich):" });
        _selection.Children.Add(_voltage);
        _selection.Children.Add(_current1);
        _selection.Children.Add(_current2);
        panel.Children.Add(_selection);
        _measurement.Children.Add(new TextBlock { Text = "Referenzwert in V bzw. A:" });
        _measurement.Children.Add(_reference);
        _measurement.Children.Add(_unit);
        _unit.Items.Add("V"); _unit.Items.Add("mV");
        _unit.Items.Add("A"); _unit.Items.Add("mA"); _unit.Items.Add("µA");
        _unit.SelectedIndex = 0;
        panel.Children.Add(_measurement);
        panel.Children.Add(_next);
        _next.Click += NextClick;
        _measurement.Visibility = Visibility.Collapsed;
        _step.Text = "Schritt 1 · Automatische Hardwareprüfung";
        _status.Text = "Verbindung und Datenempfang werden automatisch im Hintergrund geprüft.";
        Loaded += async (_, _) =>
        {
            if (_phase == 0) await RunHardwareCheckAsync();
        };
    }

    private async void NextClick(object sender, RoutedEventArgs e)
    {
        _next.IsEnabled = false;
        try
        {
            if (_phase == 0)
            {
                await RunHardwareCheckAsync();
            }
            else if (_phase == 1)
            {
                _areas.Clear();
                if (_voltage.IsChecked == true) _areas.Add(("Spannung", 0));
                if (_current1.IsChecked == true) _areas.Add(("Strom 1", 1));
                if (_current2.IsChecked == true) _areas.Add(("Strom 2", 4));
                if (_areas.Count == 0) throw new InvalidDataException("Mindestens einen Bereich auswählen.");
                if (_draft is null || !_draft.DeviceName.Equals(_serial, StringComparison.OrdinalIgnoreCase))
                    throw new InvalidDataException("Für die Zweipunktkalibrierung zunächst ein vorhandenes Profil dieses Geräts laden. Fehlende Bereiche dürfen nicht überschrieben werden.");
                _phase = 2;
                _selection.Visibility = Visibility.Collapsed;
                _measurement.Visibility = Visibility.Visible;
                _next.Content = "OK · 1.000 Werte aufnehmen";
                ShowStep();
            }
            else if (_phase == 2)
            {
                double reference = double.Parse(_reference.Text, NumberStyles.Float, CultureInfo.CurrentCulture);
                if (!double.IsFinite(reference)) throw new FormatException("Ungültiger Referenzwert.");
                int range = _areas[_areaIndex].Range;
                double factor = _unit.SelectedItem?.ToString() switch
                { "µA" => 1e-6, "mA" => 1e-3, "mV" => 1e-3, _ => 1 };
                reference *= factor;
                _status.Text = "1.000 ADC-Werte werden aufgenommen (maximal 12 Sekunden) …";
                var sample = await _capture(range);
                if (_pointIndex == 0)
                {
                    _first = sample; _firstReference = reference; _pointIndex = 1;
                    _reference.Text = "1";
                    ShowStep();
                }
                else
                {
                    _draft = range == 0
                        ? _draft!.WithVoltageTwoPointCalibration(_first.Voltage, _firstReference, sample.Voltage, reference)
                        : _draft!.WithCurrentTwoPointCalibration(range, _first.Current, _firstReference, sample.Current, reference);
                    _areaIndex++; _pointIndex = 0;
                    if (_areaIndex == _areas.Count)
                    {
                        _phase = 3;
                        _measurement.Visibility = Visibility.Collapsed;
                        _step.Text = "Schritt 6 · Kalibrierung übernehmen?";
                        _status.Text = "Alle ausgewählten Messbereiche wurden berechnet. Profil wird ausschließlich nach deiner Bestätigung für " + _serial + " gespeichert.";
                        _next.Content = "Ja · Für dieses Gerät speichern";
                    }
                    else { _reference.Text = "0"; ShowStep(); }
                }
            }
            else if (_phase == 3)
            {
                var choice = MessageBox.Show(this,
                    "Ja: für dieses Gerät speichern und beim nächsten Start automatisch laden.\n" +
                    "Nein: unter anderem Dateinamen speichern (ohne Aktivierung).\n" +
                    "Abbrechen: Kalibrierung verwerfen.",
                    "Kalibrierung abschließen",
                    MessageBoxButton.YesNoCancel, MessageBoxImage.Question);
                if (choice == MessageBoxResult.Cancel)
                {
                    DialogResult = false;
                    return;
                }
                string safe = string.Concat(_serial.Where(ch => char.IsLetterOrDigit(ch) || ch is '-' or '_'));
                string dir = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                    "Leistungsmessung", "calibrations");
                string path;
                bool activate = choice == MessageBoxResult.Yes;
                if (activate)
                {
                    Directory.CreateDirectory(dir);
                    path = Path.Combine(dir, "calib_" + safe + ".properties");
                }
                else
                {
                    var picker = new SaveFileDialog { Filter = "Kalibrierprofil (*.properties)|*.properties",
                        FileName = "calib_" + safe + "_export.properties" };
                    if (picker.ShowDialog(this) != true) return;
                    path = picker.FileName;
                }
                string temp = path + ".tmp";
                _draft!.Save(temp);
                var verified = Ad7765BoardCalibration.Load(temp);
                if (!verified.DeviceName.Equals(_serial, StringComparison.OrdinalIgnoreCase))
                    throw new InvalidDataException("Profil-Seriennummer ungültig.");
                if (File.Exists(path)) File.Copy(path, path + ".backup", true);
                File.Move(temp, path, true);
                if (activate) CreatedProfile = verified;
                DialogResult = true;
            }
        }
        catch (Exception ex)
        {
            _status.Text = "Fehler: " + ex.Message;
        }
        finally { _next.IsEnabled = true; }
    }

    private async Task RunHardwareCheckAsync()
    {
        _next.IsEnabled = false;
        try
        {
            _step.Text = "Hardwareprüfung läuft …";
            await _capture(0);
            _phase = 1;
            _step.Text = "Schritt 2 · Bereiche auswählen";
            _status.Text = "Messgerät erreichbar; 1.000 gültige ADC-Werte empfangen.";
            _next.Content = "Auswahl bestätigen";
        }
        catch (Exception ex)
        {
            _status.Text = "Hardwareprüfung fehlgeschlagen: " + ex.Message;
            _next.Content = "Erneut prüfen";
            _phase = 0;
        }
        finally { _next.IsEnabled = true; }
    }

    private void ShowStep()
    {
        var area = _areas[_areaIndex];
        _unit.SelectedItem = area.Range == 0 ? "V" : "mA";
        _step.Text = $"Schritt {_areaIndex + 3} · {area.Name} · Referenzpunkt {_pointIndex + 1}/2";
        _status.Text = $"Definierten {(area.Range == 0 ? "Spannungswert" : "Stromwert")} einstellen, stabilisieren lassen und OK drücken. Danach werden genau 1.000 gültige Messwerte gemittelt.";
    }
}
