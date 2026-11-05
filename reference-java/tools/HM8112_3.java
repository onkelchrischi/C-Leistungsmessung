package tools;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class HM8112_3 extends Multimeter{

	//Multimetersteuerung		
	private final String endline = "\n";
	private final String NULL = "0171" + endline;				//setzt Wert auf Null
	private final String START_MESSURE = "0160" + endline;			//startet automatische Messwertübertragung
	private final String STOP_MESSURE = "0161" + endline;			//stoppt automatische Messwertübertragung
	private final String AUTORANGE_ON = "0101" + endline;			//aktiviert den Autorange
	private final String AUTORANGE_OFF = "0100" + endline;			//deaktiviert den Autorange
	private final String T_10	= "0111" + endline;					//Messzeit 10	ms
	private final String T_50	= "0112" + endline;					//Messzeit 50	ms
	private final String T_100	= "0113" + endline;					//Messzeit 100	ms
	private final String T_500	= "0114" + endline;					//Messzeit 500	ms
	private final String T_1000	= "0115" + endline;					//Messzeit 1000	ms
	private final String AUTOSTATUS_ON	= "02C4" + endline;			//aktiviert den automatischen Statusbericht
	private final String AUTOSTATUS_OFF	= "02C3" + endline;		//deaktiviert den automatischen Statusbericht
	private final String STATUS = "02C2" + endline;				//fragt Gerätestatus ab
	private final String RANGE_VOLTAGE = "0009" + endline;		//wählt den Messbereich Spannung aus
	private final String RANGE_CURRENT = "0029" + endline;		//wählt den Messbereich Strom aus
	
	private final int baud = 9600;
	private final int maxI = 1;
	
	private String mode = "";
	private String actualRange = "";
	
	private final static HashMap<String,String> hm_mode = new HashMap<String, String>();
	
	static {
		hm_mode.put("000", "mV");
		hm_mode.put("001", "V");
		hm_mode.put("002", "V");
		hm_mode.put("003", "V");
		hm_mode.put("004", "V");
		hm_mode.put("020", "uA");
		hm_mode.put("021", "mA");
		hm_mode.put("022", "mA");
		hm_mode.put("023", "mA");
		hm_mode.put("024", "A");
	}
	
	public HM8112_3(String port) {
		super(port);
	}

	@Override
	public void run() {
		if (connected){
			getStatus();
		}
		while(connected){
			try{
				if (bis.available() > 0){
					//readline
					String input = "";
					do{
						byte[] b = {(byte) bis.read()};
						input += new String(b);
					}while (!input.contains(endline));
		        	analyse(input);
		        }
				Thread.sleep(10);
			}catch (InterruptedException e){
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean connect() {
		try{
			if (connected){
				disconnect();
				return false;
			}
			if (portIdentifier == null){
				portIdentifier = CommPortIdentifier.getPortIdentifier( port );
			}
	    	con = portIdentifier.open("HM8112", 1000);
	    	if( con instanceof SerialPort ){
	    		SerialPort serialPort = ( SerialPort ) con;
	            serialPort.setSerialPortParams( baud,
	                                            SerialPort.DATABITS_8,
	                                            SerialPort.STOPBITS_1,
	                                            SerialPort.PARITY_NONE );
	            bis = new BufferedInputStream(serialPort.getInputStream());
		        bos = new BufferedOutputStream(serialPort.getOutputStream());
		        connected = true;
		        
		        setMeassureTime(500);
		        setAutotrange(true);  
		        setAutoStatus(true);
				startMeassure();					//startet die kontinuierliche Messung
	    	}
		}catch (Exception e){
			System.out.println("Verbindungsfehler: HM8112-3");
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
				
			}
		}
		return true;
	}
	
	protected void analyse(String in){
		//Ziffern auslesen
		try{
			if (in != null && in.contains("\n")) in = in.replace("\n", "");
			if (in.length() > 3){
				double num = Double.parseDouble(in);
	
				synchronized (syncValue) {
					newValue[0] = newValue[1] = false;
					if (mode.contains("V")){
						_i = 0;
						newValue[0] = true;
						if (mode.equals("V")) _u = num;
						else if (mode.equals("mV")) _u = num / 1000;
					}else if (mode.contains("A")){
						_u = 0;
						newValue[1] = true;
						if (mode.equals("uA")) _i = num / 1000000;
						else if (mode.equals("mA")) _i = num / 1000;
						else if (mode.equals("A")) _i = num;
					}else{
						getStatus();
					}
				}
			}else if (in.length() == 3){
				if (hm_mode.containsKey(in)){
					mode = hm_mode.get(in);
					if (in.startsWith("00")) actualRange = "U";
					else if (in.startsWith("02")) actualRange = "I";
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}		
	}

	public double getI() {
		synchronized (syncValue) {
			newValue[1] = false;
			return _i;
		}
	}

	public double getU() {
		synchronized (syncValue) {
			newValue[0] = false;
			return _u;
		}
	}

	public void startMeassure(){
        try {
			bos.write(START_MESSURE.getBytes());
	        bos.flush();
	        Thread.sleep(50);	        
		} catch (Exception e) {}	
	}
	
	public void stopMeassure(){
        try {
			bos.write(STOP_MESSURE.getBytes());
	        bos.flush();
	        Thread.sleep(50);	        
		} catch (Exception e) {}			
	}
	
	public void setNull(){
        try {
			bos.write(NULL.getBytes());
	        bos.flush();
	        Thread.sleep(50);	        
		} catch (Exception e) {}			
	}
	
	public void setAutotrange (boolean activate){		
	    try {
	    	if (activate) bos.write(AUTORANGE_ON.getBytes());
	    	else bos.write(AUTORANGE_OFF.getBytes());
		    bos.flush();
		    Thread.sleep(50);	        
	    } catch (Exception e) {}			
	}
	
	public void setMeassureTime(int time){
	    try {
	    	switch (time){
		    	case 10:
		    		bos.write(T_10.getBytes());
		    		break;
		    	case 50:
		    		bos.write(T_50.getBytes());
		    		break;
		    	case 100:
		    		bos.write(T_100.getBytes());
		    		break;
		    	case 500:
		    		bos.write(T_500.getBytes());
		    		break;
		    	case 1000:
		    		bos.write(T_1000.getBytes());
		    		break;
		    	default:
		    		bos.write(T_500.getBytes());
	    	}
		    bos.flush();
		    Thread.sleep(50);	        
	    } catch (Exception e) {}			
	}
	
	public void setAutoStatus (boolean activate){
	    try {
	    	if (activate) bos.write(AUTOSTATUS_ON.getBytes());
	    	else bos.write(AUTOSTATUS_OFF.getBytes());
		    bos.flush();
		    Thread.sleep(50);	        
	    } catch (Exception e) {}			
	}
	
	public void getStatus (){
        try {
			bos.write(STATUS.getBytes());
	        bos.flush();
	        Thread.sleep(50);	        
		} catch (Exception e) {}			
	}
	
	public void selectVoltage(){
		if (actualRange.equals("U")) return;
        try {
			bos.write(RANGE_VOLTAGE.getBytes());
	        bos.flush();
	        actualRange = "U";
	        Thread.sleep(50);	        
			setAutotrange(true);	        
		} catch (Exception e) {}				
	}
	
	public void selectCurrent(){
		if (actualRange.equals("I")) return;
        try {
			bos.write(RANGE_CURRENT.getBytes());
	        bos.flush();
	        actualRange = "I";
	        Thread.sleep(50);	        
			setAutotrange(true);	        
		} catch (Exception e) {}				
	}

	@Override
	public float getMaxI() {
		return maxI;
	}
	
}
