package tools;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

import program.Main;
import program.ProgStates;
import program.Property;

public class Preon32 extends USART_Tools{
	
	private int baud = 921600;
	private SerialPort serialPort;
	private boolean isEnabled = false;
	private final String endline = "\n";
	private Main _main;
    private DataInputStream dis;

	public Preon32(String port, Main m) {
		super(port);
		baud = Property.getPropertiesInt(Property.COM_BAUTRATE);
		_main = m;
	}


	@Override
	public void run() {
		while(connected){
			try{
				if (!isEnabled){
					Thread.sleep(200);
					continue;
				}
				if (bis.available() > 0){
					//readline
					String input = "";
					do{
						byte[] b = {(byte) bis.read()};
						input += new String(b);
					}while (!input.contains(endline));
					
					if (input.contains("START")){
						//Messung starten
						_main.measure.start();
					}
		        }
				Thread.sleep(200);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean connect() {
		try{
			if (connected && serialPort != null){
				return true;
			}
	    	CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier( port );
	    	con = portIdentifier.open("Preon32", 1000);
	    	if( con instanceof SerialPort ){
	    		serialPort = (SerialPort) con;
	            serialPort.setSerialPortParams( baud,
	                                            SerialPort.DATABITS_8,
	                                            SerialPort.STOPBITS_1,
	                                            SerialPort.PARITY_NONE );
	            bis = new BufferedInputStream(serialPort.getInputStream());
		        bos = new BufferedOutputStream(serialPort.getOutputStream());
		        dis = new DataInputStream(bis);											
		        enableDevice(true);
		        connected = true;		        
	    	}
		}catch (Exception e){
			System.out.println("Verbindungsfehler: Preon32");
			return false;
		}		
        return true;
	}

	@Override
	public boolean disconnect() {
		if (connected){
			try{
				con.close();
				connected = false;
			}catch (Exception e){
				return false;
			}
		}
		return true;
	}
	
	public void enableDevice(boolean enable){
		if (serialPort == null){
			isEnabled = false;
			return;
		}
        serialPort.setRTS(enable);
		serialPort.setDTR(enable);
        isEnabled = enable;
	}
	
    /**
     * Schicke String zum Preon
     * @param s	String zum senden
     * @throws IOException
     */
    public void writeString(String s) throws IOException{
    	if (!connected || !isEnabled) return;
    	if (!s.contains("\n")) s += "\n";
    	System.out.print(s);
    	bos.write(s.getBytes());
    	bos.flush();
    }
    
    /**
     * schreibt ein Byte auf den USART
     * @param b	Byte zum schreiben
     * @param mode	< 2 -> Comand USART, >= 2 -> DatenUSART
     * @throws IOException
     */
    public void writeByte(byte b) throws IOException{
    	byte[] out = {b};
        if (!connected || !isEnabled) return;       	
        bos.write(out);
        bos.flush();
    }
	
    /**
     * lese short vom InputBuffer
     * @return gelesene Daten
     * @throws IOException
     */
    public short readShort() throws IOException{
    	return dis.readShort();
    }
    
    /**
     * lese long vom InputBuffer
     * @return gelesene Daten
     * @throws IOException
     */
    public long readLong() throws IOException{
    	return dis.readLong();
    }
    
    /**
     * liest eine Anzahl an Bytes und gibt sie zurück
     * @param mode	< 2 -> Comand USART, >= 2 -> DatenUSART
     * @param len Anzahl der geforderten Bytes
     * @return bytes[] der Länge len und den Daten aus dem USART. null, wenn timeout überschritten und Anzahl der Daten im Puffer < len
     * @throws IOException
     */
    public byte[] readBytes(int len) throws IOException{
    	byte[] in = new byte[len];
    	dis.read(in,0,len);
    	return in;
    }
    
    /**
     * gibt die Anzahl der bytes im Comandline USARTPuffer zurück
     * INFO Wird nur beim AD7765 EVAL Board genutzt
     * @return Bytes im Puffer
     */
    public int available(){
    	if (!connected || isEnabled) return 0;
		try {
	    	return bis.available();
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
    	return -1;
    }

    /**
     * Wartet so lange, bis de gewünschte Anzahl an Bytes in USART Puffer vorhanden ist und gibt sie zurück
     * @param mode Mode < 2 Comand USART, Mode >=2 Daten USART
     * @param len	Anzahl der geforderten Bytes
     * @param timeout	maximale Wartezeit, um auf Daten zu warten
     * @return bytes[] der Länge len und den Daten aus dem USART. null, wenn timeout überschritten und Anzahl der Daten im Puffer < len
     */
    public byte[] waitAvailable(int len, long timeout){
    	int available = 0;
    	long lastContact = System.currentTimeMillis();
    	
    	while (available < len && System.currentTimeMillis() - lastContact < timeout){
    		try {
 				available = bis.available();
    			Thread.sleep(10);
			} catch (Exception e) {
				if (ProgStates.printError) e.printStackTrace();
				MyFile.saveError(e);
			}
    	}
    	if (available < len){	
    		return null;
    	}
    	
		try {
	    	return readBytes(len);
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
		}
		return null;
    }

}
