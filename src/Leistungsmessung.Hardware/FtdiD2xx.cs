using System.Runtime.InteropServices;
using System.Text;

namespace Leistungsmessung.Hardware;

/// <summary>
/// Native FTDI D2XX discovery and connection only. No measurement commands are transmitted.
/// The official 64-bit ftd2xx.dll must be installed and discoverable by Windows.
/// </summary>
public sealed class FtdiD2xx : IDisposable
{
    private IntPtr _handle;
    public bool IsOpen => _handle != IntPtr.Zero;

    public IReadOnlyList<FtdiDevice> Enumerate()
    {
        EnsureClosed();
        Check(Native.FT_CreateDeviceInfoList(out var count), "FT_CreateDeviceInfoList");
        var devices = new List<FtdiDevice>();
        for (uint i = 0; i < count; i++)
        {
            var serial = new StringBuilder(16);
            var description = new StringBuilder(64);
            Check(Native.FT_GetDeviceInfoDetail(i, out var flags, out var type,
                out var id, out var location, serial, description, out _),
                $"FT_GetDeviceInfoDetail({i})");
            devices.Add(new FtdiDevice(i, serial.ToString(), description.ToString(),
                id, location, type, flags));
        }
        return devices;
    }

    public void Open(FtdiDevice device)
    {
        EnsureClosed();
        // Enumerations are snapshots: caller must refresh before choosing a device.
        Check(Native.FT_Open(device.Index, out var handle), "FT_Open");
        if (handle == IntPtr.Zero)
            throw new FtdiException("FT_Open hat keinen gültigen Handle geliefert.");
        _handle = handle;
        try
        {
            Check(Native.FT_SetDataCharacteristics(_handle, 8, 0, 0),
                "FT_SetDataCharacteristics (8N1)");
            Check(Native.FT_SetTimeouts(_handle, 1000, 1000), "FT_SetTimeouts");
        }
        catch
        {
            Close();
            throw;
        }
    }

    /// <summary>Bytes already buffered by the D2XX driver; non-blocking query.</summary>
    public uint BytesAvailable()
    {
        RequireOpen();
        Check(Native.FT_GetQueueStatus(_handle, out var count), "FT_GetQueueStatus");
        return count;
    }

    /// <summary>Read at most maxBytes currently queued. Never sends device commands.</summary>
    public byte[] ReadAvailable(int maxBytes = 4096)
    {
        if (maxBytes <= 0 || maxBytes > 1_048_576)
            throw new ArgumentOutOfRangeException(nameof(maxBytes));
        var pending = BytesAvailable();
        var length = (int)Math.Min((uint)maxBytes, pending);
        if (length == 0) return [];
        var buffer = new byte[length];
        Check(Native.FT_Read(_handle, buffer, (uint)length, out var received), "FT_Read");
        if (received > buffer.Length)
            throw new FtdiException("Ungültige Byteanzahl vom D2XX-Treiber.");
        if (received != length) Array.Resize(ref buffer, (int)received);
        return buffer;
    }

    private void RequireOpen()
    {
        if (!IsOpen) throw new InvalidOperationException("FTDI ist nicht verbunden.");
    }

    public void Close()
    {
        if (_handle == IntPtr.Zero) return;
        var handle = _handle;
        _handle = IntPtr.Zero;
        Check(Native.FT_Close(handle), "FT_Close");
    }

    private void EnsureClosed()
    {
        if (IsOpen) throw new InvalidOperationException(
            "Verbindung ist geöffnet. Erst trennen, dann Geräte suchen.");
    }

    private static void Check(uint status, string operation)
    {
        if (status != 0) throw new FtdiException(
            $"{operation} fehlgeschlagen (FT_STATUS={status}: {StatusName(status)}).");
    }

    private static string StatusName(uint status) => status switch
    {
        1 => "INVALID_HANDLE", 2 => "DEVICE_NOT_FOUND", 3 => "DEVICE_NOT_OPENED",
        4 => "IO_ERROR", 5 => "INSUFFICIENT_RESOURCES", 6 => "INVALID_PARAMETER",
        7 => "INVALID_BAUD_RATE", 8 => "DEVICE_NOT_OPENED_FOR_ERASE",
        9 => "DEVICE_NOT_OPENED_FOR_WRITE", 10 => "FAILED_TO_WRITE_DEVICE",
        11 => "EEPROM_READ_FAILED", 12 => "EEPROM_WRITE_FAILED",
        13 => "EEPROM_ERASE_FAILED", 16 => "INVALID_ARGS", 17 => "NOT_SUPPORTED",
        18 => "OTHER_ERROR", 19 => "DEVICE_LIST_NOT_READY", _ => "UNBEKANNT"
    };

    public void Dispose() => Close();

    private static class Native
    {
        private const string Library = "ftd2xx.dll";
        [DllImport(Library, CallingConvention = CallingConvention.StdCall)]
        internal static extern uint FT_CreateDeviceInfoList(out uint numberOfDevices);

        // D2XX expects 16-byte serial and 64-byte description output buffers.
        // StringBuilders use LPStr/ANSI, matching the native Windows D2XX API.
        [DllImport(Library, CallingConvention = CallingConvention.StdCall, CharSet = CharSet.Ansi)]
        internal static extern uint FT_GetDeviceInfoDetail(
            uint index, out uint flags, out uint type, out uint id,
            out uint locationId,
            [Out, MarshalAs(UnmanagedType.LPStr)] StringBuilder serialNumber,
            [Out, MarshalAs(UnmanagedType.LPStr)] StringBuilder description,
            out IntPtr ftHandle);

        [DllImport(Library, CallingConvention = CallingConvention.StdCall)]
        internal static extern uint FT_Open(uint deviceNumber, out IntPtr handle);

        [DllImport(Library, CallingConvention = CallingConvention.StdCall)]
        internal static extern uint FT_SetDataCharacteristics(
            IntPtr handle, byte wordLength, byte stopBits, byte parity);

        [DllImport(Library, CallingConvention = CallingConvention.StdCall)]
        internal static extern uint FT_SetTimeouts(IntPtr handle, uint readTimeout, uint writeTimeout);

        [DllImport(Library, CallingConvention = CallingConvention.StdCall)]
        internal static extern uint FT_GetQueueStatus(IntPtr handle, out uint rxBytes);

        [DllImport(Library, CallingConvention = CallingConvention.StdCall)]
        internal static extern uint FT_Read(IntPtr handle, [Out] byte[] buffer,
            uint bytesToRead, out uint bytesReturned);

        [DllImport(Library, CallingConvention = CallingConvention.StdCall)]
        internal static extern uint FT_Close(IntPtr handle);
    }
}

public sealed record FtdiDevice(
    uint Index, string SerialNumber, string Description,
    uint DeviceId, uint LocationId, uint Type, uint Flags)
{
    public override string ToString() => $"{Description} · SN {SerialNumber} · Index {Index}";
}

public sealed class FtdiException(string message) : Exception(message);
