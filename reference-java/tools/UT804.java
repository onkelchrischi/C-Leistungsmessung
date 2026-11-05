package tools;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.HashMap;

import program.ProgStates;

public class UT804 extends Multimeter {
	
	private final String endline = "\r\n";
	private final int rangePos = 5;
	private final int modePos = 6;
	private final int infoPos = 8;
	private final int signPos = 0x4;
	private final float maxI = 10;
	
	private final static HashMap<Integer,String> hm_mode = new HashMap<Integer, String>();
	private final static HashMap<String,Double[]> hm_range = new HashMap<String, Double[]>();

	private final int baud = 2400;

	static {
		hm_mode.put(1, "V");
		hm_mode.put(2, "V");
		hm_mode.put(3, "mV");
		hm_mode.put(4, "Ohm");
		hm_mode.put(5, "F");
		hm_mode.put(6, "°C");
		hm_mode.put(7, "µA");
		hm_mode.put(8, "mA");
		hm_mode.put(9, "A");
		hm_mode.put(0x3A, "Pieps");
		hm_mode.put(0x3B, "Diode");
		hm_mode.put(0x3C, "Hz");
		hm_mode.put(0x3D, "°F");
		hm_mode.put(0x3F, "%");
		
		//Strom wird in Milliampere angegeben/umgerechnet, Spannung wird in Millivolt angezeigt/umgerechnet
		
		hm_range.put("V", 		new Double[]{0.,1000.,10000.,100000.,1000000.});
		hm_range.put("mV", 	new Double[]{100.});
		hm_range.put("Ohm", 	new Double[]{0.,100.,1000.,10000.,100000.,1000000.,10000000.});
		hm_range.put("F", 		new Double[]{});
		hm_range.put("Hz", 	new Double[]{});
		hm_range.put("µA", 	new Double[]{0.1,1.});
		hm_range.put("mA", 	new Double[]{10.,100.});
		hm_range.put("A", 		new Double[]{0.,10000.});
		hm_range.put("%", 		new Double[]{});
		hm_range.put("°C", 	new Double[]{});
		hm_range.put("°F", 	new Double[]{});
	}
	
	public UT804(String port){
		super(port);
		connected = false;
	}
	
	@Override
	public boolean disconnect(){
		if (connected){
			try{
				con.close();
				connected = false;
			}catch (Exception e){
				
			}
		}
		return true;
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
	    	con = portIdentifier.open("UT804", 1000);
	    	if( con instanceof SerialPort ){
	    		SerialPort serialPort = ( SerialPort ) con;
	            serialPort.setSerialPortParams( baud,
	                                            SerialPort.DATABITS_7,
	                                            SerialPort.STOPBITS_1,
	                                            SerialPort.PARITY_ODD );
	     
	            bis = new BufferedInputStream(serialPort.getInputStream());
		        bos = new BufferedOutputStream(serialPort.getOutputStream());
		        ((SerialPort) con).setDTR(true);
		        connected = true;
		        
		        Thread.sleep(100);
	    	}
		}catch (Exception e){
			if (ProgStates.DEBUG) e.printStackTrace();
			return false;
		}		
	    return true;
	 }

	@Override
	public void run() {
		while(connected){
			try{
				if (bis.available() > 0){
					//readline
					String input = "";
					do{
						byte[] b = {(byte) bis.read()};
						input += new String(b);
					}while (!input.contains(endline));
		        	analyse(input.substring(0,9));
		        }
				Thread.sleep(200);
			}catch (InterruptedException e){
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void analyse(String msg){
		//Ziffern auslesen
		try{
			String number = msg.substring(0,1) + "." + msg.substring(1,5);
			double num = Double.parseDouble(number);
			//Mode
			String mode = hm_mode.get(Integer.parseInt(msg.substring(modePos,modePos+1)));
			
			//range 
			double r_factor = hm_range.get(mode)[Integer.parseInt(msg.substring(rangePos,rangePos+1))];
			double val = num * r_factor;
			
			//vorzeichen
			int sign = Integer.parseInt(msg.substring(infoPos,infoPos+1)) & signPos;
			
			if (sign > 0) val *= -1;
			
			synchronized (syncValue) {
				newValue[0] = newValue[1] = false;
				if (mode.equals("V") || mode.equals("mV")){
					newValue[0] = true;
					_u = val / 1000;
					_i = 0;
				}
				else if (mode.equals("µA") || mode.equals("mA") || mode.equals("A")){
					newValue[1] = true;
					_i = val / 1000;
					_u = 0;
				}
			}
		}catch (Exception e){}
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

	@Override
	public float getMaxI() {
		return maxI;
	}
}
