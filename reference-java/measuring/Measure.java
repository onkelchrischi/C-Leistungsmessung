/**
 * TODO Spannungsabfall durch Messwiderstand hinzufügen 	Property.getPropertiesDouble(Property.R_MEAS_ALL) * i
 */

package measuring;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.SwingWorker;

import program.Communication;
import program.Main;
import program.ProgStates;

public class Measure implements Runnable{
	 
	protected final Main _main;
	protected int anzMesswerte;
	protected long Messzeit;		//in Sekunden
	protected int anzStateChanges;
	protected final long TimestampTypeMask = 0xFF00000000000000L;
	Device device = null;
	
    protected ArrayList<State> stateList;

    public Measure(Main main){
    	_main = main;
    }
    
	/**
	 * startet die Messung
	 * @throws IOException
	 */
	public void start() throws IOException{
		if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
			if (!ProgStates.ISCONNECTED_FAST) return;
		}else{
			if (!ProgStates.ISCONNECTED || ProgStates.MEASSURE_RUNS) return;	
		}

		ProgStates.MEASSURE_RUNS = true;

        if (ProgStates.ADC_MODE == ProgStates.ADC_INTERNAL || ProgStates.ADC_MODE == ProgStates.ADC_HARTING){
        	_main.com.writeString(_main.com.ACD_MS1);
        }else if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL){
        	byte b = (byte) (Communication.START_MESSUNG & 0xFF); 
        	_main.com.writeByte(b, Communication.COMMANDLINE);
        }else if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
        	byte b = (byte) (Communication.START_MESSUNG & 0xFF);         	
        	_main.com.startWriteThread();
        	//_main.com.startDataBuffer();
        	_main.com.writeByte(b, Communication.COMMANDLINE);
        }else if (ProgStates.ADC_MODE == ProgStates.ADC_DUMMY){
        	_main.com.startWriteThread();
        }
	}
	
	/**
	 * stoppt die Messung
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void stop() throws IOException, InterruptedException, NullPointerException{
		if ((!ProgStates.ISCONNECTED && !ProgStates.useFT232HOnly) || !ProgStates.MEASSURE_RUNS || device == null) return;

		device.stopMeasure(true);	
		_main.com.t_write.join();
	}
		    
    /**
     * gibt die Anzahl der Messwerte in EINER Messreihe zurück
     * @return Anzahl der Messwerte
     */
    public long getAnzMesswerte(){
    	return anzMesswerte;
    }
    
    /**
     * gibt die Anzahl der Statechanges der Messung zurück
     * @return Anzahl der Statechanges
     */
    public int getAnzStateChanges(){
    	return anzStateChanges;
    }
    
    /**
     * gibt die Gesamtmesszeit zückt
     * @return gesamte Messzeit
     */
    public long getMesszeit(){
    	return ((new Date()).getTime() - Messzeit) / 1000;
    }
	
    /**
     * läd gespeicherte Messung
     * @param path Pfad für die Messung
     * @throws IOException
     * @throws InterruptedException 
     */
    public void loadMessung(final File f) throws Exception{
		_main.gui.p_chart.setLOCK(true);
		
		SwingWorker<Boolean,Void> myWorker = new SwingWorker<Boolean,Void>(){
			@Override
			protected Boolean doInBackground() throws Exception {
				_main.data.readRaw(f,this);
		        //_main.gui.p_chart.resetZoom();
				return true;
			}
			
        	@Override
            public void done() {
        		_main.gui.loadDone();
        		for (int i = 0; i < _main.data.getAnz(); i++)
        			_main.gui.p_chart.setFixesMinMax(i, _main.data.getDaten(i).getMin(),  _main.data.getDaten(i).getMax());
            }
		};

		myWorker.addPropertyChangeListener(new PropertyChangeListener() {			
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				if(arg0.getPropertyName().equals("VALUES")){
					int val = (int) arg0.getNewValue();
					int max = (int) arg0.getOldValue();
					int pc = (int) (((long)val * 100) / max);
					_main.gui.myMenue.setProgressbarValue(pc);
					_main.gui.myMenue.setProgressBarText("lade Messwerte (" + f.getName() + "): " + String.valueOf(pc) + "%");
				}if (arg0.getPropertyName().equals("JOB")){
					_main.gui.myMenue.setProgressbarValue((Integer) arg0.getOldValue());
					_main.gui.myMenue.setProgressBarText((String) arg0.getNewValue());
				}
			}
		});
		_main.gui.myMenue.setProgressBar(true, false);
		myWorker.execute();
    }

	/**
	 * Runnable für die Messung. Hier werden die Daten eingehenden Daten vom Preon ausgewertet und verarbeitet
	 * Jeder Messblock endet mit einen Block für Statechanges. Dieser kann jedoch leer sein
	 * Die ersten 20 Messwerte werden verworfen
	 */
	@Override
	public void run() {
		switch(ProgStates.ADC_MODE){
			case ProgStates.ADC_INTERNAL:
			case ProgStates.ADC_HARTING:
				device = new InternalADC(_main);
				break;
			case ProgStates.ADC_AD7765_EVAL:
				device = new AD7765Eval(_main);
				break;
			case ProgStates.ADC_AD7765_BOARD:
				device = new AD7765(_main);				
				break;
			case ProgStates.ADC_DUMMY:
				device = new DummyHW(_main);
				break;
		}
		if (device != null) device.startMe();
	}

	protected void stop2PortMeasure(){
		ProgStates.MEASSURE_RUNS = false;
		if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD) _main.com.disconnect_FAST();
		
		//FTDI Verbindung nicht trennen
		//else if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD) Communication.disconnectFTDI();
		_main.com.disconnect();
        if (!ProgStates.SAVE_DATA) return;

		ProgStates.LASTALIVE = System.currentTimeMillis();		
	}

	protected void MeasureFinished(boolean sucess){
		if (sucess){
	        for (State s: stateList){ 
	            _main.data.writeRAW(-1);
	            if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD) _main.data.writeDataRDFInt(s.getIndex());
	            else _main.data.writeDataRDFLong(s.getTimeStamp());
	            _main.data.writeDataRDFByte(s.getType());
	        }
	        _main.data.setStateChanges(stateList);					//Speichere Statechanges in Messdaten    
		}
        _main.data.closeStream();
		
    	_main.gui.clearActualValue();
    	_main.gui.clearLables();
    	if (!_main.gui.chk_fixYAxis.isSelected()) _main.gui.chk_fixYAxis.doClick();
    	
		if (ProgStates.IS_PERIODIC_MEASURE && !ProgStates.SAVE_DATA){
			_main.gui.changeTab(0);
			_main.data.clearAll();
			_main.gui.p_chart.clearAll();
		}else{
			if (!sucess || ProgStates.ISCALIB) return;
	        Messzeit = ((new Date()).getTime() - Messzeit);
	    	if (ProgStates.ADC_MODE == ProgStates.ADC_INTERNAL || ProgStates.ADC_MODE == ProgStates.ADC_HARTING) _main.data.calcIndexOfStateChange();					//Berechne Indizes der Statechanges
	    	_main.gui.updateMessergebnis(anzMesswerte, anzStateChanges, Messzeit / 1000);   	
	    	_main.gui.updateStatistics();
	    	_main.data.setSelectedBounds(0,_main.data.getSize()-1);
	        _main.gui.p_chart.setAnzGraphen(_main.data.getAnz(), _main.data.getNamesOfData());
		}
	}

	protected boolean plausibleCheck(byte[] in){
		int count = 0;
		float threshold = 0.95F;
		//Wenn 95% des LOWBytes gleich sind sind daten nicht plausibel
		for (int i=0;i<in.length - 2;i=i+2){
			if (in[i+1] == in[i+3]){
				count++;
			}
		}
		float res = (float)count / (float) in.length;
		if (res > threshold) return false;
		else return true;
	}
	
}
