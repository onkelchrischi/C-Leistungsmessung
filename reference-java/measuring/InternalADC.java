package measuring;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import program.Communication;
import program.Main;
import program.ProgStates;
import tools.MyFile;

public class InternalADC extends Device{

	public InternalADC(Main main) {
		super(main);
	}

	public void startMe(){
        boolean startTimeSet = false;
        long time_startMessung = 0;
        LinkedList<Short> messungList = new LinkedList<Short>();

    	anzMesswerte = -20;		//Verwerfe die ersten 20 Messwerte
    	anzStateChanges = 0;
    	Messzeit = (new Date()).getTime();
    	
    	BufferedWriter debugOut = null;
    	try {
			debugOut = new BufferedWriter(new FileWriter(new File("debug.csv")));
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
    	
        while (ProgStates.MEASSURE_RUNS || !messungList.isEmpty()) {
            try {                                      
            	short val = _main.com.readShort(ProgStates.ADC_INTERNAL);
            	synchronized (ProgStates.syncAlive) {
            		ProgStates.LASTALIVE = System.currentTimeMillis();
            	}
                if (val == Communication.STOP_MESSUNG){			//Messung beenden	(short)
                	stopMeasure();
                	System.out.println("Befehl Messung stoppen erhalten: " + val);
                	continue;
                }else if(val == Communication.START_MESSUNG){	//Signal für Messung starten, ab hier beginnt die Messung	(short)
                    continue;
                }else if (val == Communication.STATE_START){		//Flag für anfang der Statechanges	(short)
                    while (true){						//lese solange Statechanges ein, bis das End-Flag empfangen wird
                    	long state = _main.com.readLong(ProgStates.ADC_INTERNAL);
                        byte caseIRQ = (byte) ((state & TimestampTypeMask) >> 56);	//Grund des Statechange die ersten 8 Bit des state-Packets
                        
                        if (state == Communication.STATE_STOP){	//Statechange-End-Flag, verarbeite die zwischen gespeicherten Messwerte
                            break;
                        }
                        if (!startTimeSet){             
                            time_startMessung = state;
                            startTimeSet = true;
                            stateList.add(new State(State.S_START,0L));
                        }else{
                        	long ts = state & 0xFFFFFFFFFFFFFFL;
                            long t_state = (ts - time_startMessung) / 1000; //Zeit in Mikrosekungen
                            t_state *= Messdaten.TIMECORRECTION;			//Zeitkorrektur für Preon
                            stateList.add(new State(caseIRQ,t_state));
                            
                            //System.out.println("timestamp: " + t_state);
                            debugOut.write(String.valueOf(t_state));
                            debugOut.newLine();
                            
                        	anzStateChanges++;
                        }
                    }
                    int[] values = new int[2];
                    
                    if (messungList.size() < ProgStates.OUTPUT_BUFFER){
                    	System.out.println("Zu wenig Messwerte: " + messungList.size() + ", erwartet: " + ProgStates.OUTPUT_BUFFER);
                    }
                    while (!messungList.isEmpty()){
                    	anzMesswerte++;
                        if (anzMesswerte <= 0){
                        	continue;		//verwerfe die ersten 20 Messwert
                        }
                        for (int n=0; n < 2; n++){
                        	if (!messungList.isEmpty()){
                        		values[n] = (int) messungList.removeFirst();
                        	}else{
                        		values[n] = 0;
                        		System.out.println("Messwerte verloren!!!");
                        	}
	                        _main.data.addValueToMessdaten(n, values[n],0,false);                        		
                        }
                    }
                }else{
                    messungList.add(val);
                }
            } catch (Exception ex) {
            	_main.stopMeasure();
            	_main.gui.myMenue.updateStatus("Error: " + ex.getMessage());
            	if (ProgStates.printError) ex.printStackTrace();
            	//Messung abbrechen
            	
                break;
            } 
        } 
        
        try {
			debugOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        if (!ProgStates.SAVE_DATA) return;
                
        MeasureFinished(true);
	}

	@Override
	public boolean stopMeasure() {  	
		return stopMeasure(true);
	}

	@Override
	public boolean stopMeasure(boolean forceStop) {
		try {
			_main.com.writeString(_main.com.ACD_MS0);
			if (ProgStates.ADC_MODE == ProgStates.ADC_HARTING) _main.com.setHartingMode(false);
			ProgStates.MEASSURE_RUNS = false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}
}
