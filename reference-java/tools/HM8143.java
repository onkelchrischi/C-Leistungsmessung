package tools;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class HM8143 extends Source{

	//Netzteilsteuerung		
	private final String endline = "\r";
	private final String HM8143_STATUS = "STA?" + endline;
	private final String HM8143_SETI = "TRI:";
	private final String HM8143_SETU = "TRU:";
	private final String HM8143_GETU_IST = "MU1" + endline;
	private final String HM8143_GETI_IST = "MI1" + endline;
	private final String HM8143_ENABLE_OUT = "OP1" + endline;
	private final String HM8143_DISABLE_OUT = "OP0" + endline;
	private final String HM8143_ENABLE_RM = "RM1" + endline;
	private final String HM8143_DISABLE_RM = "RM0" + endline;
	
	private final int baud = 9600;
	
	public HM8143(String port){
		super(port);
	}
	
	public boolean connect(){
		try{
			if (connected){
				disconnect();
				return false;
			}
			if (portIdentifier == null){
				portIdentifier = CommPortIdentifier.getPortIdentifier( port );
			}
	    	con = portIdentifier.open("HM8143", 1000);
	    	if( con instanceof SerialPort ){
	    		SerialPort serialPort = ( SerialPort ) con;
	            serialPort.setSerialPortParams( baud,
	                                            SerialPort.DATABITS_8,
	                                            SerialPort.STOPBITS_1,
	                                            SerialPort.PARITY_NONE );
	            bis = new BufferedInputStream(serialPort.getInputStream());
		        bos = new BufferedOutputStream(serialPort.getOutputStream());
		        connected = true;
		        
		        bos.write(HM8143_ENABLE_RM.getBytes());
		        bos.flush();
		        
		        Thread.sleep(100);
		        
		        bos.write(HM8143_STATUS.getBytes());
		        bos.flush();
		        Thread.sleep(100);
		        
		        setOut(false);


		        bos.write(HM8143_GETI_IST.getBytes());
		        bos.flush();

		        Thread.sleep(100);

		        bos.write(HM8143_GETU_IST.getBytes());
		        bos.flush();

		        Thread.sleep(100);
	    	}
		}catch (Exception e){
			System.out.println("Verbindungsfehler: HM8143");
			return false;
		}		
        return true;
	}
	
	public boolean disconnect(){
		if (connected){
			try{
				bos.write(HM8143_DISABLE_RM.getBytes());
				bos.flush();
				
				con.close();
				connected = false;
			}catch (Exception e){
				return false;
			}
		}
		return true;
	}
	
	public double getI(){
		synchronized(syncValue){
			newValue[1] = false;
			return _i;
		}
	}
	
	public double getU(){
		synchronized(syncValue){
			newValue[0] = false;
			return _u;
		}
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
		        	//System.out.println(input);
		        	
		        	if (input.contains("I1:")){
		        		String sub = input.substring(4, 9);
		        		synchronized(syncValue){
		        			newValue[1] = true;
		        			_i = Double.parseDouble(sub);
		        		}		        		
		        	}else if (input.contains("U1:")){
		        		String sub = input.substring(3, 8);
		        		synchronized(syncValue){
		        			newValue[0] = true;
		        			_u = Double.parseDouble(sub);
		        		}		        		
		        	}
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
	
	/**
	 * send command to set Voltage
	 * send format is "#.##"
	 * @param u
	 */
	public void setU(String u){
		try{
			u = u.replace(",",".");

			int lac = u.length() - u.indexOf(".") - 1;
			for (int i = lac;i<2;i++) u += "0";
	
			String s_u = HM8143_SETU + u + endline;
			bos.write(s_u.getBytes());
			bos.flush();
			
			Thread.sleep(200);

			bos.write(HM8143_GETU_IST.getBytes());
			bos.flush();
			
			Thread.sleep(200);
			
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void setI(String i){
		try{
			i = i.replace(",",".");

			int lac = i.length() - i.indexOf(".") - 1;
			for (int k = lac;k<3;k++) i += "0";
	
			String s_i = HM8143_SETI + i + endline;
			bos.write(s_i.getBytes());
			bos.flush();
			
			Thread.sleep(200);
			
			bos.write(HM8143_GETU_IST.getBytes());
			bos.flush();
			
			Thread.sleep(200);
			
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void setI(float i){
		try{
			String s_i = String.format("%.3f", i);
			s_i = s_i.replace(",", ".");
			s_i = HM8143_SETI + s_i + endline;
			bos.write(s_i.getBytes());
			bos.flush();
			
			Thread.sleep(200);
			
			bos.write(HM8143_GETU_IST.getBytes());
			bos.flush();
			
			Thread.sleep(200);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public void setU(float u){
		try{
			String s_u = String.format("%.2f", u);
			s_u = s_u.replace(",", ".");
			s_u = HM8143_SETU + s_u + endline;
			bos.write(s_u.getBytes());
			bos.flush();
			
			Thread.sleep(200);
			
			bos.write(HM8143_GETU_IST.getBytes());
			bos.flush();
			
			Thread.sleep(200);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public void setOut(boolean setEnable){
		try{			
			if (setEnable) bos.write(HM8143_ENABLE_OUT.getBytes());
			else bos.write(HM8143_DISABLE_OUT.getBytes());
			bos.flush();
			
			Thread.sleep(200);			
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void enableRemote(boolean enable){
		try{			
			if (enable) bos.write(HM8143_ENABLE_RM.getBytes());
			else bos.write(HM8143_DISABLE_RM.getBytes());
			bos.flush();
			
			Thread.sleep(200);			
		}catch (Exception e){
			e.printStackTrace();
		}		
	}

	@Override
	protected void analyse(String in) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getMaxI() {
		// TODO Auto-generated method stub
		return 0;
	}
}
