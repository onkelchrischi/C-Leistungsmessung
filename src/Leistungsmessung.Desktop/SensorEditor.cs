using System;
using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using Leistungsmessung.Core;

namespace Leistungsmessung.Desktop;

public sealed class SensorEditor : Window
{
    private readonly TextBox name = new() { Width = 260 };
    private readonly ComboBox input = new() { Width = 220, ItemsSource = Enum.GetValues<SensorInput>() };
    private readonly ComboBox conversion = new() { Width = 220, ItemsSource = Enum.GetValues<SensorConversion>() };
    private readonly ComboBox rtd = new() { Width = 220, ItemsSource = Enum.GetValues<RtdType>() };
    private readonly TextBox inMin = new() { Width = 130 }, inMax = new() { Width = 130 };
    private readonly TextBox outMin = new() { Width = 130 }, outMax = new() { Width = 130 };
    private readonly TextBox formula = new() { Width = 360 };
    private readonly TextBox outputUnit = new() { Width = 130 }, excitation = new() { Width = 130 };
    public SensorProfile? Result { get; private set; }

    public SensorEditor(SensorProfile? existing)
    {
        Title = "Sensor definieren"; Width = 530; Height = 650; WindowStartupLocation = WindowStartupLocation.CenterOwner;
        Background = System.Windows.Media.Brushes.White;
        Foreground = System.Windows.Media.Brushes.Black;
        var panel = new StackPanel { Margin = new Thickness(24) };
        Content = new ScrollViewer { Content = panel };
        var source = existing ?? new SensorProfile();
        void Add(string label, Control control)
        {
            panel.Children.Add(new TextBlock { Text = label, Margin = new Thickness(0, 10, 0, 3) });
            panel.Children.Add(control);
        }
        name.Text = source.Name; Add("Sensorname", name);
        input.SelectedItem = source.Input; Add("Eingangsgröße", input);
        conversion.SelectedItem = source.Conversion; Add("Messumformer linear / PT-Widerstand direkt", conversion);
        rtd.SelectedItem = source.Rtd; Add("PT-Typ (nur direkte Widerstandsmessung)", rtd);
        inMin.Text = source.InputMin.ToString("G", CultureInfo.InvariantCulture);
        inMax.Text = source.InputMax.ToString("G", CultureInfo.InvariantCulture);
        Add("Eingangsminimum (A bei Strom, V bei Spannung)", inMin);
        Add("Eingangsmaximum (A bei Strom, V bei Spannung)", inMax);
        outMin.Text = source.OutputMin.ToString("G", CultureInfo.InvariantCulture);
        outMax.Text = source.OutputMax.ToString("G", CultureInfo.InvariantCulture);
        Add("Ausgangsminimum", outMin); Add("Ausgangsmaximum", outMax);
        outputUnit.Text = source.OutputUnit; Add("Ausgabeeinheit (z. B. °C, bar)", outputUnit);
        formula.Text = source.Formula; Add("Formel (x = elektrischer Eingang in A oder V; z. B. (x-0.004)*6250)", formula);
        excitation.Text = source.ExcitationCurrentAmps.ToString("G", CultureInfo.InvariantCulture);
        Add("Erregerstrom für direkte PT-Messung (A)", excitation);
        panel.Children.Add(new TextBlock { Text = "Direkte PT-Messung benötigt eine bekannte Erregerschaltung. Bei Messumformern ist die Umrechnung linear; die PT-Kennlinie wird nicht zusätzlich angewendet.", TextWrapping = TextWrapping.Wrap, Margin = new Thickness(0, 14, 0, 5) });
        var save = new Button { Content = "Sensor speichern", Margin = new Thickness(0, 15, 0, 0), Padding = new Thickness(12) };
        panel.Children.Add(save);
        save.Click += (_, _) =>
        {
            bool Parse(TextBox tb, out double value) => double.TryParse(tb.Text.Replace(',', '.'), NumberStyles.Float, CultureInfo.InvariantCulture, out value) && double.IsFinite(value);
            if (string.IsNullOrWhiteSpace(name.Text) || !Parse(inMin, out var a) || !Parse(inMax, out var b)
                || !Parse(outMin, out var c) || !Parse(outMax, out var d) || !Parse(excitation, out var ex))
            { MessageBox.Show(this, "Bitte gültige Zahlen und einen Sensornamen eingeben."); return; }
            if ((SensorConversion)conversion.SelectedItem == SensorConversion.Linear && a >= b)
            { MessageBox.Show(this, "Das Eingangsmaximum muss größer als das Minimum sein."); return; }
            if ((SensorConversion)conversion.SelectedItem == SensorConversion.RtdDirect &&
                ((SensorInput)input.SelectedItem != SensorInput.Voltage || (RtdType)rtd.SelectedItem == RtdType.None || ex <= 0))
            { MessageBox.Show(this, "Direkte PT-Messung benötigt Spannungseingang, PT-Typ und positiven Erregerstrom."); return; }
            if ((SensorConversion)conversion.SelectedItem == SensorConversion.Formula)
            {
                try { _ = SensorFormula.Evaluate(formula.Text, a); _ = SensorFormula.Evaluate(formula.Text, b); }
                catch (FormatException err) { MessageBox.Show(this, err.Message, "Formel ungültig"); return; }
            }
            Result = new SensorProfile { Formula = formula.Text.Trim(), Name = name.Text.Trim(), Input = (SensorInput)input.SelectedItem,
                Conversion = (SensorConversion)conversion.SelectedItem, Rtd = (RtdType)rtd.SelectedItem,
                InputMin = a, InputMax = b, OutputMin = c, OutputMax = d, OutputUnit = outputUnit.Text,
                ExcitationCurrentAmps = ex };
            DialogResult = true;
        };
    }
}
