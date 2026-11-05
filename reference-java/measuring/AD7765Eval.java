package measuring;

import java.io.IOException;
import java.util.Date;

import javax.swing.JOptionPane;

import program.Communication;
import program.Main;
import program.ProgStates;
import tools.MyFile;

public class AD7765Eval extends Device implements StartMeasure{
	private long lastMessageIn = 0;
	private int[] values = new int[2];
	private boolean waitForPreamble = false;
	private int bufferSize = 1024;
	private byte[] buffer = new byte[bufferSize];
	private boolean plaubible = true;

	public AD7765Eval(Main main) {
		super(main);
	}
	
	@Override
	public void startMe(){
    	Messzeit = (new Date()).getTime();
    	anzMesswerte = 0;
        
		while (ProgStates.MEASSURE_RUNS){		
			//Kommandos auswerten
			if (stopMeasure()) break;
			try{
				int timeoutTreashold = 5000;					//warte 5 Sekunden auf Startsequenz
				long timeStart = System.currentTimeMillis();
				int index = 0;
				boolean start = true;
				int k = 0;
				while (waitForPreamble){
					if(start){
						System.out.println("warte auf Startsequenz");
						start = false;
					}else{
						Thread.sleep(10);
					}
					if (_main.com.available_Data() > 0){
						//warte auf Startsequenz
						byte in = _main.com.readBytes(ProgStates.ADC_MODE, 1)[0];
						System.out.println("byte " + k  + "-> in: " + in + ", expected: " + Communication.PREAMBLE_AD7765[index]);
						if (Communication.PREAMBLE_AD7765[index] == in)	index++;
						else index = 0;
						k++;	
						if (index == 4){
							waitForPreamble = false;
							System.out.println("Startsequenz erhalten.. Messung startet.");						
							break;
						}
					}					
					if (System.currentTimeMillis() - timeStart > timeoutTreashold) {
						throw new Exception("no preamble");
					}						
				}
				
				buffer = _main.com.waitAvailable(ProgStates.ADC_MODE, bufferSize, 1000);
				if (buffer == null) continue;	//Wenn nicht genug Daten im Puffer sind weiter warten
				
				if (!plaubible){
					if ((plaubible = plausibleCheck(buffer)) == false){
						//kein Rauschen auf den LOW Bytes	--> Vermutlich Fehler
						throw new Exception("not plausible");
					}
				}
				for (int i=0; i < bufferSize;i=i+2){
					//short s_in = _main.com.readShort(ProgStates.ADC_MODE);
					short s_in = (short)(((buffer[i] & 0xFF) << 8) | buffer[i+1] & 0xFF);
					int in = s_in;
					values[0] = in;					//Strom
					values[1] = 0;					//Spannung
					lastMessageIn = System.currentTimeMillis();
					ProgStates.LASTALIVE = lastMessageIn;
					_main.data.addValueToMessdaten(0,values[0],0,false);
					_main.data.addValueToMessdaten(1,values[1],0,false);
					anzMesswerte++;
				}
				//Überprüfe den letzten Dateneingang
			}catch (Exception e){
				if (e.getLocalizedMessage().equalsIgnoreCase("closed by user")){
					stop2PortMeasure();
					MeasureFinished(false);
					_main.gui.messungStoppen();
					JOptionPane.showMessageDialog(null, "COM Port wurde unerwartet geschlossen. Programm wird beendet. Bitte neustarten","COM-Port Fehler",JOptionPane.ERROR_MESSAGE);
					_main.exit();
				}else if (e.getMessage().equals("not plausible")){
					stop2PortMeasure();
					MeasureFinished(false);
					_main.gui.messungStoppen();
					JOptionPane.showMessageDialog(null, "Daten sind nicht Plausibel","ADC Fehler",JOptionPane.ERROR_MESSAGE);					
				}else if (e.getMessage().equals("no preamble")){
					stop2PortMeasure();
					MeasureFinished(false);
					_main.gui.messungStoppen();
					JOptionPane.showMessageDialog(null, "Startsequenz nicht erhalten. Messung wurde abgebrochen","USART Fehler",JOptionPane.ERROR_MESSAGE);					
				}
			}
		}
	}

	@Override
	public boolean stopMeasure() {
		if (_main.com.available() > 1){
			try {
				if (_main.com.readShort(Communication.COMMANDLINE) == Communication.STOP_MESSUNG){
					stopMeasure(true);
					return true;
				}
			} catch (IOException e){
				if (ProgStates.printError) e.printStackTrace();
				MyFile.saveError(e);
			}
		}
		return false;
	}

	@Override
	public boolean stopMeasure(boolean forceStop) {
		_main.data.addValueToMessdaten(-1,0,0,false);	//Bei periodischer Messung, schreibe die zuletzt gespeicherten Werte in die CSV Datei 
		stop2PortMeasure();	
		MeasureFinished(true);
		return false;
	}
	

}
