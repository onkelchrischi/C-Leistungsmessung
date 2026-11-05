package program;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import measuring.MeasureType;
import measuring.Messdaten;
import tools.*;

public class CSVOut {
    	
	private Messdaten _data;
	private  String out = "";
	
	public CSVOut(String out, Messdaten data){
		this.out = out;
		this._data = data;
	}
	
	public void setData(Messdaten data){
		_data = data;
	}
	
	public void setOutput(String path){
		out = path;
	}
	
	public String getOutputPath(){
		return out;
	}
	
	/**
	 * ruft die Methode Export to CSV auf und exportiert einen Auszug oder die kompletten Daten nach CSV
	 * @param s Startpunkt der Daten
	 * @param e Endpunkt der Daten
	 * @return Speichern erfolgreich
	 */
    public void exportToCSV(final int s, final int e, final boolean showEndDialog){   
    	if (out.isEmpty()) out = tools.MyFile.selectFile(false,0);
    	
    	SwingWorker<Boolean, Void> myWorker = new SwingWorker<Boolean, Void>(){

			@Override
			protected Boolean doInBackground() throws Exception {
				int start = s;
				int end = e;
		    	if (start < 0) start = 0;
		    	if (end < 0) end = _data.getSize();
		    	
		    	String path = out;
		    	if (path.isEmpty()) path = tools.MyFile.selectFile(false,0);
		    	if (!path.endsWith(".csv")) path += ".csv";
		    	File f = new File(path);

		    	if (f.exists()){
		    		int m = JOptionPane.showConfirmDialog(null, "Datei existiert bereits. Soll diese überschrieben werden?", "Datei Überschreiben?", JOptionPane.YES_NO_CANCEL_OPTION);
		    		while (m == JOptionPane.NO_OPTION){
		    			path = tools.MyFile.selectFile(false,0);
		    			f = new File(path);
		    			if (f.exists()){
		    	    		m = JOptionPane.showConfirmDialog(null, "Datei existiert bereits. Soll diese überschrieben werden?", "Datei Überschreiben?", JOptionPane.YES_NO_CANCEL_OPTION);
		    			}
		    		}
		    		if (m == JOptionPane.CANCEL_OPTION) return false;
		    	}
		    	try {
					BufferedWriter bw = new BufferedWriter(new FileWriter(f));
					
			    	//write Header
					
					String line = "i;Zeit " + Range.unit(Messdaten.TIMEUNIT, true) + ";";
					for (int i=0;i<2;i++){
						if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD){
							line += _data.getNamesOfData()[i] + " [raw];" + _data.getNamesOfData()[i] + " " + 
									Range.unit(_data.getDaten(i).messbereich,true) + ";";	
						}else{
							line += _data.getNamesOfData()[i] + " " + 
									Range.unit(_data.getDaten(i).messbereich,true) + ";";	
						}
					}
					try{
						line += _data.getNamesOfData()[2] + Range.unit(_data.getDaten(2).messbereich,true) + ";";
					}catch (Exception e){}
					
					line += "State Changes";
					if (Property.getPropertiesBoolean(Property.MESSUNG_AD7765_KALIB_ENABLE) && ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
						line += ";Messbereich\n";
					}else{
						line += "\n";
					}
					bw.write(line);
					
					//write _data
					for (int i=start; i<end;i++){
						line = String.valueOf(i) + ";";
						line += String.valueOf(i * Messdaten.DT) + ";";
						
						float val_i = _data.getMessdaten(MeasureType.CURRENT.index).get(i);
						float val_u = _data.getMessdaten(MeasureType.VOLTAGE.index).get(i);
						
						float p = 0;
						p = Power.berechneLeistung(val_i, val_u);
						p = Range.convertFromTo(Range.MILLIWATT, Range.MICROWATT, p);
						line += tools.MyMath.roundString(Range.convertFromTo(Range.AMPERE, Range.MILLIAMPERE, val_i), 3) + ";";
						line += tools.MyMath.roundString(val_u, 3) + ";";
						
						line += tools.MyMath.roundString(p,3) + ";";
						for (int k=0;k<_data.getStateChanges().size();k++){
							if (_data.getStateChanges().get(k).getIndex() == i){
								line += "State Change";
								break;
							}
						}
						line = line.replace('.', ',');
						bw.write(line);
						bw.write("\n");
						
						if (i % 10 == 0){
							String pname = "speichere Messwerte " + String.valueOf(i - start) + " von " + String.valueOf(end - start);
							firePropertyChange(pname, end-start, i);
						}
					}
					bw.close();
					
					if (showEndDialog) JOptionPane.showMessageDialog(null, "Messdaten erfolgreich nach " + path + " exportiert.", "Speichern erfolgreich", JOptionPane.INFORMATION_MESSAGE);
					return true;
				} catch (IOException e1) {
					if (showEndDialog) JOptionPane.showMessageDialog(null, e1.getMessage(), "Fehler beim speichern", JOptionPane.ERROR_MESSAGE);
					if (ProgStates.printError) e1.printStackTrace();
					MyFile.saveError(e1);
					return false;
				}
			}
			
			@Override
            public void done() {
        		_data._main.gui.myMenue.setProgressBarText("");
        		if (showEndDialog) _data._main.gui.myMenue.setProgressBar(false, false);     		        		
            }
    	};
    	
    	myWorker.addPropertyChangeListener(new PropertyChangeListener() {		
			@Override
			public void propertyChange(PropertyChangeEvent arg0) {
				if (arg0.getOldValue() == SwingWorker.StateValue.PENDING || arg0.getNewValue() == SwingWorker.StateValue.DONE) return;

				int size = (Integer) arg0.getOldValue();
				int val = 0;
				try{
					_data._main.gui.myMenue.setProgressBarText(arg0.getPropertyName());
					val = (Integer) arg0.getNewValue();
					_data._main.gui.myMenue.setProgressbarValue((int) ((val * 100) / size));
				}catch (Exception e){
					if (ProgStates.printError) e.printStackTrace();
					MyFile.saveError(e);
				}
				
				//
			}
		});
    	
    	_data._main.gui.myMenue.setProgressBar(true, false);
    	myWorker.execute();
    	if (!showEndDialog){
	    	while (!myWorker.isDone() && !myWorker.isCancelled()){
	    		try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {}
	    	}
    	}
    }

    
}


