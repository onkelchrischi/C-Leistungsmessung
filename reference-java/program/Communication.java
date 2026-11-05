	package program;

//import gnu.io.CommPort;
//import gnu.io.CommPortIdentifier;
//import gnu.io.SerialPort;

import purejavacomm.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JOptionPane;

import tools.MyFile;

import com.virtenio.commander.io.DataConnection;
import com.ftdi.*;

public class Communication {
	
	private final Main _main;

    // create a preon32 node helper and run the app. The returned connection
    // can be used to communicate with the preon32
	private SerialPort con_preon;
    private BufferedInputStream _bisCom;
    private BufferedOutputStream _bosCom;
    private DataInputStream _dis;
    public Thread t_write;
    private Thread input;           //Thread fuer serielle Kommunikation
    
    //Fast USART
    private CommPort ioPort;
    private DataConnection conn_fast;
    private BufferedInputStream _bisCom_fast;
    private DataInputStream _dis_fast;
    public Thread t_write_fast;
    
    private static FTDevice um232H;
    private final static String ftdiDescription = "UM232H";
    private static BufferedInputStream _bisFTDI;
        
    public final static short START_MESSUNG      	= 0x7F00;
    public final static short STOP_MESSUNG        	= 0x7F01;
    public final static byte STOP_MESSUNG_AD7765  	= (byte) 0xF1;    
    public final static short INIT_END           	= 0x7F02;
    public final static short ACK                 	= 0x7FFF;
    public final static short READY              	= 0x7F03;
    public final static short STATE_START         	= 0x7800;
    public final static long  STATE_STOP   			= 0x7FFFFFFFFFFFFFFFL;
    public final static String  INIT_FINISHED		= "INIT FINISHED";
    public final static byte[] PREAMBLE_AD7765		= {(byte) 0xDA, (byte) 0x53, (byte) 0xAA, (byte) 0xB4};
    public final static byte PREAMBLE1Byte_AD7765	= (byte) 0xE5;
    public final static short NEW_STATECHANGE		= (short) 0xDCBD;
    public final static byte CALIB_RANGE_1			= (byte) 0xC1;
    public final static byte CALIB_RANGE_2			= (byte) 0xC2;
    public final static byte CALIB_RANGE_3			= (byte) 0xC3;
       
    public static final byte COMMANDLINE			= 0;
    public static final byte DATAIN					= 1;

    public final String ACD_MS1 = "control.measure=1\n";
    public final String ACD_MS0 = "control.measure=0\n";
    public final String TMR_INT = "config.timerinterval=";
    public final String EN_ADC_CH = "config.enableChannel=";
    public final String DIS_ADC_CH = "config.disableChannel=";
    public final String IS_READY = "status=ready\n";
    public final String SET_ADC = "config.adc="; 
    public final String DEBUG = "config.debug=\n";
    public final String TIME_CORR = "config.timecorrection=";
    public final String USE_HARTING = "config.useHarting=";
    public final String SET_OUTPUTBUFFER = "config.setOutputBuffer=";
    public final String SET_OVERSAMPLING = "config.oversamplingfactor=";
    
    public static BlockingQueue<Byte> inputBuffer = new ArrayBlockingQueue<Byte>(16777215);			//Buffer ist 260000 Byte => 40kHz Datenrate für 1 Sekunde
    /**
     * Konstruktor für Communication
     * Übergibt die Main Klasse für Zugriffe auf Gui
     * @param main Main Klasse
     */
    public Communication(Main main){
    	_main = main;
    }
    
    /**
     * Verbindet den Computer mit dem Preon
     * @return true wenn erfolgreich verbunden
     */
    public boolean connect(){
    	if (ProgStates.ISCONNECTED) return false;
        try {
	        _main.gui.myMenue.updateStatus("Verbindung wird hergestellt...");
	    	String port = Property.getProperties(Property.COM_PORT);
	    	if (con_preon == null){	//wieder verbinden
		    	int baud = Integer.parseInt(Property.getProperties(Property.COM_BAUTRATE));
		    	CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier( port );
		    	CommPort con = portIdentifier.open("preon32", 1000);
		    	if( con instanceof SerialPort ){
		    		con_preon = ( SerialPort ) con;
		    		con_preon.setSerialPortParams( baud,
		                                            SerialPort.DATABITS_8,
		                                            SerialPort.STOPBITS_1,
		                                            SerialPort.PARITY_NONE );  	
		    	}	    		
	    	}

    		con_preon.setRTS(true);
    		con_preon.setDTR(true);

			_bisCom = new BufferedInputStream(con_preon.getInputStream());
			_bosCom = new BufferedOutputStream(con_preon.getOutputStream());
	        _dis = new DataInputStream(_bisCom);											

        	System.out.println(port + " verbunden");
        	ProgStates.WAITFORCONNECTION = true;		//Wenn Verbindung zum Preon erfolgreich, 
	        input = new Thread(runInput);				// starte Thread für Input
	        input.setName("Communication");
	        input.start();
	        
	        ProgStates.ISCONNECTED = true;
	    	
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Fehler bei der Verbindung", "Connection error", JOptionPane.ERROR_MESSAGE);
			ProgStates.ISCONNECTED = false;
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
    	return ProgStates.ISCONNECTED;    	
    }
    
    /**
     * Verbindet den DatenUSART zur schnellen Datenübertragung. 
     * @return true, wenn erfolgreich verbunden. false - wenn keine Verbindung möglich ist.
     */
    public boolean connectToFastUSART(){
    	try {
    		_main.gui.myMenue.updateStatus("Verbinde zu Daten USART...");
	    	String port = Property.getProperties(Property.COM_PORT_FAST);
	    	
	    	CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier( port );
	    	ioPort = portIdentifier.open("fast USART", 10000);
	    	//ioPort.setInputBufferSize(5242880);		// 5 MB buffer
	    	if( ioPort instanceof SerialPort ){
	    		SerialPort serialPort = ( SerialPort )ioPort;
	            serialPort.setSerialPortParams( 57600,
	                                            SerialPort.DATABITS_8,
	                                            SerialPort.STOPBITS_1,
	                                            SerialPort.PARITY_NONE );
	     
	            _bisCom_fast = new BufferedInputStream(serialPort.getInputStream());
		        _dis_fast = new DataInputStream(_bisCom_fast);						
	        	System.out.println(port + " verbunden");
	        	ProgStates.ISCONNECTED_FAST = true;
	        	return true;
	    	}else{
	    		throw new Exception("com error");
	    	}
	    	
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Connection error", JOptionPane.ERROR_MESSAGE);
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
    	ProgStates.ISCONNECTED_FAST = false;
    	return false;
    }
        
    /**
     * Verbindung zum UM232H über D2XX Methoden aufbauen
     * @return 	true  - Verbindung erfolgreich
     * 			false - Verbindung nicht erfolgreich hergestellt
     */
    public static boolean connectFTDI(String SN){
    	List<FTDevice> fTDevices;
		try {
			fTDevices = FTDevice.getDevicesByDescription(ftdiDescription);
//			fTDevices = FTDevice.getDevices();
			for (FTDevice fTDevice : fTDevices) {
				System.out.println("found: " + fTDevice.getDevDescription() + ", SN: " + fTDevice.getDevSerialNumber());
				if (fTDevice.getDevDescription().equalsIgnoreCase(ftdiDescription)){
					if (!fTDevice.getDevSerialNumber().equalsIgnoreCase(SN)) continue;
					um232H = fTDevice;
					break;
				}
			}
			
			if (um232H == null) return false;
			
			um232H.open();
			um232H.setDataCharacteristics(WordLength.BITS_8,StopBits.STOP_BITS_1, Parity.PARITY_NONE);
			um232H.setTimeouts(1000, 1000);
			_bisFTDI = new BufferedInputStream(um232H.getInputStream());
			
			System.out.println(ftdiDescription + " verbunden");
			ProgStates.ISCONNECTED_FAST = true;
		} catch (FTD2XXException e) {
			System.out.println("e: " + e.getLocalizedMessage());
			e.printStackTrace();
			return false;
		}
    	return true;
    }
    
    public static void flushFTDI(){
    	try {
			int pending = um232H.getQueueStatus();
    		while (pending > 0){
    			um232H.read(pending);
    			pending = um232H.getQueueStatus();
    		}
			
    		while (_bisFTDI.available() > 0){
    			byte[] temp = new byte[_bisFTDI.available()];
    		
    			_bisFTDI.read(temp,0,_bisFTDI.available());
    		}
    		if (ProgStates.DEBUG) System.out.println("ftdi buffer empty");
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
    
    
    public static boolean disconnectFTDI(){
    	try {
			if (ProgStates.ISCONNECTED_FAST) um232H.close();
		} catch (FTD2XXException e) {
			return false;
		}
    	ProgStates.ISCONNECTED_FAST = false;
    	return true;
    }
    
    /**
     * Überprüfe, ob Verbindung besteht
     * Für die Verbindungsüberprüfung wird ein regelmäßiges "I'm alive" Signal ausgewertet
     * @return true, wenn verbunden
     */
    public boolean isConnected(){
    	if (ProgStates.WAITFORCONNECTION) return false;
    	if (ProgStates.MEASSURE_RUNS) return true;
    	return ProgStates.ISCONNECTED;
    }
    
    /**
     * trenne die Verbindung zum Preon
     * @return true, wenn erfolgreich getrennt
     * @throws Exception
     */
    public boolean disconnect(){
    	if (!ProgStates.ISCONNECTED || con_preon == null) return false;
    	ProgStates.ISCONNECTED = false;
    	input.interrupt();
    	_main.updateStatus("nicht verbunden");
    	try{
    		Thread.sleep(1000);
    		_dis.close();
    		_bosCom.close();
    		_bisCom.close();
    		con_preon.setDTR(false);
    		con_preon.setRTS(false);
    		//con_preon.close();		//Port offen halten, Nur Preon abschalten
    		System.out.println(Property.getProperties(Property.COM_PORT) + ": Datenverbindung getrennt");
    		return true;
    	}catch(Exception e){
    		if (ProgStates.printError) e.printStackTrace();
    		MyFile.saveError(e);
    	}
    	return false;
    }
    
    public boolean disconnectAll(){
    	boolean res = false;
    	if (ProgStates.ISCONNECTED || con_preon != null) disconnect();
    	if (ProgStates.ISCONNECTED_FAST){
    		if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
    			res = disconnectFTDI();
    		}else if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL){
    			res = disconnect_FAST();
    		}
    	}
    	
    	return res;
    }
    /**
     * gibt die Anzahl der bytes im Comandline USARTPuffer zurück
     * @return Bytes im Puffer
     */
    public int available(){
    	if (!ProgStates.ISCONNECTED) return -1;
		try {
	    	return _bisCom.available();
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
    	return -1;
    }
    
    /**
     * wartet auf dem Comandline UART auf eine bestimmte Anzahl an Bytes. Wenn die die Anzahl der Bytes im UART Puffer < len und der Timeout überschritten
     * wurde, wird false zurück gegeben
     * @param len	Anzahl an Bytes auf die gewartet werden soll
     * @param timeout	maximale Wartezeit. Wenn timeout = 0 möglich, es wird nur einmal die Anzahl der Bytes im UART Puffer überprüft
     * @return true, wenn die geforderte Anzahl an bytes zur verfügung steht. false - wenn timeout überschritten, oder UART nicht verbunden.	
     */
    public boolean waitAvailable(int len, long timeout){
    	if (!ProgStates.ISCONNECTED) return false;
    	long now = System.currentTimeMillis();
		try {
			if (timeout == 0) return _bisCom.available() >= len;
			while (System.currentTimeMillis() - now < timeout){
				if (_bisCom.available() >= len) return true;
				Thread.sleep(10);
			}
		} catch (Exception e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
		return false;
    }
    
    /**
     * gibt die Anzahl der Bytes im Daten USART zurück
     * @return Anzahl der Bytes im Daten USART
     */
    public int available_Data(){
    	if (!ProgStates.ISCONNECTED_FAST) return -1;
		try {
	    	return _bisCom_fast.available();
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
    public byte[] waitAvailable(int mode, int len, long timeout){
    	int available = 0;
    	long lastContact = System.currentTimeMillis();
    	
    	while (available < len && System.currentTimeMillis() - lastContact < timeout){
    		try {
    			if (mode < 2){
					available = _bisCom.available();
		    	}else{
		    		available = _bisCom_fast.available();
		    	}
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
	    	return readBytes(mode, len);
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
		}
		return null;
    }
        
    /**
     * trennt den DATEN USART
     * @return	true, erfolgreich getrennt, false, Fehler beim trennen
     */
    public boolean disconnect_FAST(){
    	if (!ProgStates.ISCONNECTED_FAST) return false;
    	ProgStates.ISCONNECTED_FAST = false;
    	try{
    		ioPort.close();
     		System.out.println(Property.getProperties(Property.COM_PORT_FAST) + ": Datenverbindung getrennt");
    		return true;
    	}catch(Exception e){
    		if (ProgStates.printError) e.printStackTrace();
    		MyFile.saveError(e);
    	}
    	return false;
    }
    
    /**
     * Schicke String zum Preon
     * @param s	String zum senden
     * @throws IOException
     */
    public void writeString(String s) throws IOException{
    	if (!ProgStates.ISCONNECTED) return;
    	if (!s.contains("\n")) s += "\n";
    	_bosCom.write(s.getBytes());
    	_bosCom.flush();
    }
    
    /**
     * schreibt ein Byte auf den USART
     * @param b	Byte zum schreiben
     * @param mode	< 2 -> Comand USART, >= 2 -> DatenUSART
     * @throws IOException
     */
    public void writeByte(byte b, int mode) throws IOException{
    	byte[] out = {b};
    	//System.out.println(new String(out));
    	if (mode < 2){
        	if (!ProgStates.ISCONNECTED && !ProgStates.useFT232HOnly && ProgStates.ISCONNECTED_FAST) return;
        	if (ProgStates.useFT232HOnly){
        		writeToUM232H(b);
        	}else{
	        	_bosCom.write(out);
	        	_bosCom.flush();
        	}
    	}else{
    		if (!ProgStates.ISCONNECTED_FAST) return;
    		conn_fast.write(out);
    		conn_fast.flush();
    	}
    }
    
    /**
     * lese short vom InputBuffer
     * @return gelesene Daten
     * @throws IOException
     */
    public short readShort(int mode) throws IOException{
    	switch (mode){
    	case 0:
    	case 1:
    		return _dis.readShort();
    	case 2:
    		return _dis_fast.readShort();
    	}
    	return 0;
    }
    
    /**
     * lese long vom InputBuffer
     * @return gelesene Daten
     * @throws IOException
     */
    public long readLong(int mode) throws IOException{
    	switch (mode){
    	case 0:
    	case 1:
    		return _dis.readLong();
    	case 2:
    		return _dis_fast.readLong();
    	}
    	return 0;
    }
    
    /**
     * liest eine Anzahl an Bytes und gibt sie zurück
     * @param mode	< 2 -> Comand USART, >= 2 -> DatenUSART
     * @param len Anzahl der geforderten Bytes
     * @return bytes[] der Länge len und den Daten aus dem USART. null, wenn timeout überschritten und Anzahl der Daten im Puffer < len
     * @throws IOException
     */
    public byte[] readBytes(int mode,int len) throws IOException{
    	byte[] in = new byte[len];
    	switch (mode){
    	case 0:
    	case 1:
    		_dis.read(in,0,len);
    		break;
    	case 2:
    	case 3:
    		_dis_fast.read(in,0,len);
    		break;
    	}
    	return in;
    }

    /**
     * Runnable zum lesen des InputBuffers
     * Wertet verschiedene Befehle aus und startet ggf. die Messung
     */
    private Runnable runInput = new Runnable() {
        @Override
        public void run() {
            try {
                short s_in;
                //Warte auf OK vom Preon
                String in = "";
                
                if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD){
	                while (ProgStates.WAITFORCONNECTION){                		
	                	s_in = _dis.readShort();
	                	
	//                    System.out.println(s_in);
	                 	byte[] c = {(byte) ((s_in & 0xFF00) >> 8),(byte) (s_in & 0x00FF)};
	                	in += new String(c);
	                 	if (in.contains(INIT_FINISHED) || s_in == READY){
	                		ProgStates.WAITFORCONNECTION = false;
	                		
	                		ProgStates.ISCONNECTED = true;
	
	                		synchronized (ProgStates.syncAlive) {
	                    		ProgStates.LASTALIVE = System.currentTimeMillis();
	                    	}
	                	}
	                }
                }else{
                	ProgStates.WAITFORCONNECTION = false;
                }
                while (ProgStates.ISCONNECTED) {
                    if (_dis.available() > 0 && !ProgStates.WAIT_FOR_VF_INPUT){
                    	byte b_in = _dis.readByte();
                    	
                    	if (b_in == 0x7F){
                    		s_in = (short) (_dis.readByte() + (0x7F << 8));
                    	}else{
                    		continue;
                    	}
//                        s_in = _dis.readShort();
                    	synchronized (ProgStates.syncAlive) {
                    		ProgStates.LASTALIVE = System.currentTimeMillis();
                    	}
                        switch (s_in){
	                        case START_MESSUNG:
	                        	//startWriteThread();
	                        	_main.updateStatus("Messung läuft");     
//	                            if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD) _main.gui.messungStarten();
	                        	try{
	                        		t_write.join();
	                        	}catch (Exception e){}
	                            _main.updateStatus("Messung beendet");
	                            Thread.sleep(1000);
	                            break;
	                        case ACK:
	                            _main.updateStatus("ACK");
	                            break;
	                        case INIT_END:
	                        case READY:
	                        	//_main.updateStatus("READY");
	                            break;
	                        case 0x30F:
	                        	_dis.readByte();
	                        	break;
                        }
                    }else if (ProgStates.WAIT_FOR_VF_INPUT){
                    	

                    }else{
                        //Thread.yield();
                    	Thread.sleep(10);
                        continue;
                    }
                }      
                System.out.println("nicht verbunden");
            } catch (InterruptedException ex) {
            	if (ProgStates.printError) MyFile.saveError(ex);
            } catch (IOException e) {
            	if (ProgStates.printError) e.printStackTrace();
            	MyFile.saveError(e);
            	disconnect();
			}              
        }
    };
    
    /**
     * Startet den Thread für die Messung
     */
    public void startWriteThread(){
    	t_write = new Thread(_main.measure);
    	t_write.setName("Messung");
    	t_write.start();
    }
    
    public void startDataBuffer(){
    	Thread dataGrabber = new Thread(new Runnable() {		
			@Override
			public void run() {
				inputBuffer.clear();
		    	while (ProgStates.MEASSURE_RUNS){
	    			try {
			    		if (available_Data() > 0){
			    			if (inputBuffer.remainingCapacity() == 0) System.out.println("buffer full!");
			    			else inputBuffer.add(readBytes(3, 1)[0]);
			    		}else {
							Thread.sleep(1);
			    		}
					} catch (InterruptedException e) {
					} catch (IOException e) {
						System.out.println(e.getMessage());
					}
		    	}
			}
		});
    	dataGrabber.start();
    }
    
    /**
     * aktiviert beim Testaufbau mit dem Harting Current den Verbraucher
     * @param b true -> aktivieren, false -> deaktivieren 
     */
    public void setHartingMode(boolean b){
		try {
	    	if (b){	
	    		writeString(_main.com.USE_HARTING + "true");
	    	}else{
	    		writeString(_main.com.USE_HARTING + "false");
	    	}
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
    }
    
    public static byte[] getBytesFromBuffer(int timeout, int len){
    	long t = System.currentTimeMillis();
    	byte[] in = new byte[len];
    	int c = 0;
    	while (c != len && (System.currentTimeMillis() - t) < timeout){
			try {
	    		if (inputBuffer.size() > 0){
						in[c++] = inputBuffer.take();
	    		}else{
	    			Thread.sleep(1);
	    		}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    	if (c != len) return null;			//bei Zeitüberschreitung null zurückgeben
    	return in;
    }
    
    public static byte[] um232H_getValidFrame(int len, byte startByte, long timeout){
    	if (!ProgStates.ISCONNECTED_FAST) return null;
    	
		byte[] in = new byte[len];
		long t = System.currentTimeMillis();
		
		boolean getFrame = true;
		while (getFrame && (System.currentTimeMillis() - t) < timeout){
			try {
				um232H.read(in,0,1);
				if (in[0] != startByte) continue;				
				um232H.read(in, 1, len -1);
				getFrame = false;
			} catch (FTD2XXException e) {
				if (e.getMessage().contains("D2XX error") && ProgStates.printError){
					System.out.println("FTDI not connected");
					e.printStackTrace();
				}
				else if (ProgStates.printError) e.printStackTrace();
				return null;
			}
		}
		if (getFrame) return null;
  	
    	return in;
    }
    
    public boolean writeToUM232H(byte out){
    	byte[] output = {out};
    	try {
			um232H.write(output);
		} catch (FTD2XXException e) {
			MyFile.saveError(e);
			return false;
		}
    	return true;
    }
}

