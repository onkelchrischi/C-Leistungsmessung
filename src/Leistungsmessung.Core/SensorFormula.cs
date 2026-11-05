using System;
using System.Globalization;

namespace Leistungsmessung.Core;

/// <summary>Safe arithmetic-only expression evaluator; x is the input in A or V.</summary>
public static class SensorFormula
{
    public static double Evaluate(string expression, double x)
    {
        if (string.IsNullOrWhiteSpace(expression) || expression.Length > 256)
            throw new FormatException("Formel fehlt oder ist zu lang.");
        var reader = new Parser(expression, x);
        double result = reader.Expression();
        reader.Space();
        if (!reader.End || !double.IsFinite(result)) throw new FormatException("Ungültige Formel oder Ergebnis.");
        return result;
    }
    private sealed class Parser
    {
        private readonly string _s;
        private readonly double _x;
        private int _p;
        public Parser(string s, double x) { _s = s; _x = x; }
        public bool End => _p == _s.Length;
        public void Space() { while (_p < _s.Length && char.IsWhiteSpace(_s[_p])) _p++; }
        private bool Take(char c) { Space(); if (_p < _s.Length && _s[_p] == c) { _p++; return true; } return false; }
        public double Expression()
        {
            double a = Term();
            while (true) { if (Take('+')) a += Term(); else if (Take('-')) a -= Term(); else return a; }
        }
        private double Term()
        {
            double a = Signed();
            while (true) { if (Take('*')) a *= Signed(); else if (Take('/')) a /= Signed(); else return a; }
        }
        private double Signed() { if (Take('+')) return Signed(); if (Take('-')) return -Signed(); return Power(); }
        private double Power() { double a = Atom(); return Take('^') ? Math.Pow(a, Signed()) : a; }
        private double Atom()
        {
            if (Take('(')) { double a = Expression(); if (!Take(')')) throw new FormatException("Klammer fehlt."); return a; }
            Space();
            if (_p < _s.Length && (_s[_p] == 'x' || _s[_p] == 'X')) { _p++; return _x; }
            int start = _p;
            while (_p < _s.Length && (char.IsDigit(_s[_p]) || _s[_p] == '.')) _p++;
            if (_p < _s.Length && (_s[_p] == 'e' || _s[_p] == 'E'))
            {
                _p++; if (_p < _s.Length && (_s[_p] == '+' || _s[_p] == '-')) _p++;
                while (_p < _s.Length && char.IsDigit(_s[_p])) _p++;
            }
            if (start == _p || !double.TryParse(_s[start.._p], NumberStyles.Float, CultureInfo.InvariantCulture, out double value))
                throw new FormatException("Unbekanntes Zeichen in der Formel.");
            return value;
        }
    }
}
