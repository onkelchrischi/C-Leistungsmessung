package tools;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

public abstract class USART_Tools implements Runnable{

	protected final int dataBit = 8;
	protected boolean connected = false;
	protected CommPort con;
	protected BufferedInputStream bis;
	protected BufferedOutputStream bos;
	protected String port = "";
	protected double _i = 0;
	protected double _u = 0;
	protected Object syncValue = new Object();
	protected boolean[] newValue = {false, false};			//neuer Wert 0: Spannung, 1: Strom
	protected CommPortIdentifier portIdentifier;

	public abstract boolean connect();
	public abstract boolean disconnect();
	
	public boolean isConnected(){
		return connected;
	}
	
	public USART_Tools(String port){
		this.port = port;
	}

	public void setPort(String port){
		if (connected){
			disconnect();
			portIdentifier = null;
		}
		this.port = port;
	}
	
	public int newValues(){
		int res = 0;
		if (newValue[0]) res += 1; 
		if (newValue[1]) res += 2;
		return res;
	}
}
