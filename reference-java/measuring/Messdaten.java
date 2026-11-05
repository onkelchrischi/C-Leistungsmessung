package measuring;

import java.awt.Cursor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingWorker;

import program.CSVOut;
import program.Main;
import program.ProgStates;
import program.Property;
import tools.MyFile;
import tools.MyMath;
import tools.Power;
import tools.Range;

/**
 * TODO JavaDoc
 * @author Christoph
 *
 */
public class Messdaten{

	public Main _main;
	
	public final static Object DATASYNC = new Object();
	public static Range R_CURRENT = Range.MILLIAMPERE;
	public static Range R_VOLTAGE = Range.MILLIVOLT;
    public static float DT = 0;							// Interval
    public static Range TIMEUNIT = Range.MICROSECOND;	// Legt den Wertebereich bei der Zeit fest
    public static long MEASURETIME = 0;	
	public static int DOWNSAMPLINGSIZE = 1000;
	public static String FILENAME = "";
    public static float TIMECORRECTION = 1;
    private static final float addPcToValue = 0.05F;
    
    public enum ID{
    	CURRENT,
    	VOLTAGE,
    	POWER
    }
        
    private String version = Main.VERSION;
    private float d_version = Main.D_VERSION;
    private Daten[] messdaten = new Daten[3];
    private int size = 0;
    private int downsampledSize = 0;
    private ArrayList<State> stateChanges = new ArrayList<State>();
    private int[] markBounds = {0,0};
    private int[] selectedBounds = {0,0};
    private int[] lastSelectedBounds = {0,0};
    private int[] presampledBounds = {0,0};
    public boolean selectedBoundsUpdated = true;
    private float globalMax = Integer.MIN_VALUE;
    private float globalMin = Integer.MAX_VALUE;
    private  long timeStamp = 0;
    private ArrayList<Short> ADC_channel = new ArrayList<Short>();
    private int actualMouseIndex = 0;
    private int[] mmValues;				//Werte für Multimetermessung

    private ObjectOutputStream aOutputStream;
    private DataOutputStream dout;
    private byte fileFormat = 1;
    private boolean osIsOpen = false;
    
    private byte kennung = 0x00;					//0: Normale Messwerte
    												//1: Mehrere Messbereiche
    
    //periodische Berechnungen
    public static int VPP = 0;						//Values per periode, wenn periodische Messung aktiviert
    public static int VPPSeconds = 0;
    private float[] plastMW = {0,0,0};
    private float[] plastSA = {0,0,0};
    private float[] pMW = {0,0,0};
    private float[] pSA = {0,0,0};
    private float pEnergy = 0;
    private float pCharge = 0;
    private int[] pValueSample = {0,0};
    private float[] pValueDouble = {0,0};
	private File f = null;
	private String baseName = "Messung" + File.separator + "periode";
	private String fileName = baseName + ".csv";
	private Object pSycn = new Object();
	private int pSize = 0;
	private int pSizeAll = 0;
	private int pCounter = 0;
	private int estimatedSize = 0;
	    
    /**
     * Konstruktor, benötigt Mainklasse für Zugriff auf GUI-Elemente
     * @param main
     */
    public Messdaten(Main main){
    	_main = main;
    }
    
	/**
	 * fügt einen ADC Kanal hinzu
	 * @param channel
	 */
    public void addChannel(short channel){
        if (!ADC_channel.contains(channel)) ADC_channel.add(channel);		
    }

    /**
     * gibt eine Liste mit ADC Kanälen zurück
     * @return Liste der ADC Kanäle
     */
    public ArrayList<Short> getADCChannel(){
    	return ADC_channel;
    }

    /**
     * löscht einen ADC Kanal
     * @param channel ADC Nummer
     */
    public void removeChannel(short channel){
    	for (int i=0;i<ADC_channel.size();i++){
    		if (ADC_channel.get(i) == channel) ADC_channel.remove(i);
    	}
    }
    
    public void init(){
    	synchronized (Messdaten.DATASYNC) {
    		messdaten = new Daten[3];
    		size = 0;
    		globalMax = 0;
    		globalMin = 0;
    		messdaten[0] = new Daten(MeasureType.CURRENT,this);
    		messdaten[1] = new Daten(MeasureType.VOLTAGE,this);
    		pCounter = 0;
    	}
    }
		
	/**
	 * setzt die Messwerte für die Multimetermessung
	 * @param val
	 */
	public void setMMValues(int[] val){
		mmValues = val;
	}
	
	/**
	 * gibt die Messwerte für die Multimetermessung zurück
	 * @return Messwerte[ADC0,ADC1,...ADCn];
	 */
	public int[] getMMValues(){
		return mmValues;
	}
	
		
	/**
	 * Überprüft, ob Messreihen leer sind
	 * @return true, wenn leer
	 */
	public boolean isEmpty(){
    	return size == 0;
   }
	
	/**
	 * setzt die Markierungsgrenzen (Markierung der Gaphik)
	 * @param start	
	 * @param stop
	 */
	public void setMarkBounds(int start, int stop){
		if (start == 0 && stop == 0) return;
		int maxGraphSize = Messdaten.DOWNSAMPLINGSIZE;
		int diff = stop - start;
		//zu zeigende Messwerte vielfaches der Darstellbaren Graphik
		float temp = (float)diff / (float)maxGraphSize;
		int f = (int) temp;
		if (temp % 1 != 0 && f > 0) f++; 
		
		if (f > 0 ) stop = start + (maxGraphSize * f);
		
		markBounds[0] = start;
		markBounds[1] = stop;		
	}
	
	/**
	 * setzt die Auswahlgrenzen (Ausgehend von der Graphik)
	 * @param start
	 * @param stop
	 */
	public void setSelectedBounds(int start,int stop){
		if (start < 0) start = 0;
		if (stop > size || stop == -1) stop = size;
		if (start > stop){
			int t = stop;
			stop = start;
			start = t;
		}

		if (selectedBounds[0] == start && selectedBounds[1] == stop){
			selectedBoundsUpdated = false;
			return;
		}
		
		lastSelectedBounds[0] = selectedBounds[0];
		lastSelectedBounds[1] = selectedBounds[1];
		
		selectedBounds[0] = start;
		selectedBounds[1] = stop;
		selectedBoundsUpdated = true;
		
    	for (int id=0;id<messdaten.length;id++){
    		if (messdaten[id] != null) messdaten[id].setSelectionBounds(selectedBounds[0], selectedBounds[1]);
		}
    	
    }
	
	public void zoomIn(int pc){
		int size = getSize();
		int newSize = (100 - pc) * size / 100;
		if (newSize == 0) newSize = 100;
		int half = newSize / 2;
		int middle = (getSelectedBounds()[1] - getSelectedBounds()[0]) / 2 + getSelectedBounds()[0];
		int left = middle - half;
		int right = middle + half;
		if (left < 0){
			right -= left;
			left = 0;
		}
		if (right >= size){
			left -=  right - size;
			right = size -1;
		}

		setSelectedBounds(left, right);
		performResampling(true);
		_main.gui.p_chart.setScrollbar(left);
		_main.gui.p_chart.doRepaint(true);
	}
	
	public void performResampling(final boolean resample){
		if (!selectedBoundsUpdated) return;
		
		_main.setCursor(Cursor.WAIT_CURSOR);
		
		ProgStates.DOWNSAMPLING_FINISHED = false;
		selectedBoundsUpdated = false;

		boolean performResampling = false;
		for (int i=0; i < 3; i++){
			if (messdaten[i] == null) continue;

			//Wenn Bereich kleiner als darstellbare Pixel
			if (selectedBounds[1] - selectedBounds[0] <= Messdaten.DOWNSAMPLINGSIZE){
				messdaten[i].copyDataToDrawableData(selectedBounds[0], selectedBounds[1]);
				continue;
			}
	
			//Wenn nicht gezoomt sondern nur verschoben wird, kopiere bereits gesampelte Daten in die drawableData Liste
			if (!resample && selectedBounds[0] > presampledBounds[0] && selectedBounds[1] < presampledBounds[1]){
				messdaten[i].copyDataFromPresample(selectedBounds[0], selectedBounds[1]);
				continue;
			}
			
			performResampling = true;
		}
		
		if (performResampling){
			presampledBounds[0] = selectedBounds[0] - (selectedBounds[1] - selectedBounds[0]);
			presampledBounds[1] = selectedBounds[1] + (selectedBounds[1] - selectedBounds[0]);
			if (presampledBounds[0] < 0){
				presampledBounds[1] += Math.abs(presampledBounds[0]);
				presampledBounds[0] = 0;
			}
			if (presampledBounds[1] >= size){
				presampledBounds[0] -= presampledBounds[1] - size;
				presampledBounds[1] = size -1;
				if (presampledBounds[0] < 0) presampledBounds[0] = 0;
			}
			
			ArrayList<Thread> threads = new ArrayList<Thread>();
			for (int i=0; i < 3; i++){
				if (messdaten[i] == null) continue;
				
				final int id = i;
				Thread t = new Thread(new Runnable() {			
					@Override
					public void run() {
						downsampledSize = messdaten[id].createDrawableData(Messdaten.DOWNSAMPLINGSIZE, ProgStates.RESAMPLEMETHODE, selectedBounds[0], selectedBounds[1], false,resample).size();
					}
				});
				threads.add(t);
				t.start();
			}
					
			for (Thread t : threads){
				try {
					t.join();							//Warte auf Downsampling
				} catch (InterruptedException e) {}
			}
		}
		ProgStates.DOWNSAMPLING_FINISHED = true;	
		for (int i=0; i < 3; i++) {
			messdaten[i].updateMinMax(selectedBounds[0], selectedBounds[1], true);
		}
		_main.gui.p_chart.doRepaint(true);
		_main.setCursor(Cursor.DEFAULT_CURSOR);

	}
	
	/**
	 * gibt die Runtergesamplelten Daten zurück
	 * @param id Messreihen ID
	 * @return Runtergesamplelten Daten
	 */
	public List<Float> getDrawableData(int id){
		return messdaten[id].getDrawableData();
	}
	
	/**
	 * fügt einen Wert einer Messreihe hinzu
	 * @param id MessreihenID
	 * @param val Wert
	 */
	public void addValueToMessdaten(int id, int i_val, float d_val, boolean isPrecalculated){
		if (id == -1){			//Messung abschließen
			if (ProgStates.IS_PERIODIC_MEASURE) savePeriodic();
			return;		//Messung beendet
		}
		if (id == 0){
			if (pSize == 0){		//Ouputpfad bestimmen	
				if (!ProgStates.LOADING_FILE) writeHeaderRDF(getDataOutputPath());
			}
			if (!ProgStates.LOADING_FILE) writeDataRDFInt(pSize);
			pSize++;		//Wenn neuer Datensatz kommt, counter erhöhen
		}
		if (id > 2) return;				//derzeit nur 2 Messwerte betrachten			
		synchronized (pSycn) {
			if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
				if (isPrecalculated){
					pValueDouble[id] = d_val;
				}else{
					if (id == 0){
						pValueDouble[id] = Power.getI(i_val);
					}
					else if (id == 1){
						pValueDouble[id] = Power.getU(i_val);
					}
				}
			}else{
				if (isPrecalculated) pValueDouble[id] = d_val;
				else pValueDouble[id] = i_val;
			}
			
			//Energie berechnen
			if (id ==1) {	
				//zSpannung und Strom korrigieren
				if (!isPrecalculated){
					//TODO Widerstand kompensieren/analysieren
					//pValueDouble[0] -= Power.getCurrentDiff(pValueDouble[1]);			//Widerstand kompensieren
					pValueDouble[1] += Power.getUShunt(pValueDouble[0]);				//Spannungsabfall am Messgerät (Shunt + Sicherung + Spuhle + Leitungen)  berücksichtigen
				}
				pValueDouble[0] = Range.convertFromTo(R_CURRENT, Range.AMPERE, pValueDouble[0]);
				pValueDouble[1] = Range.convertFromTo(Range.VOLT,Messdaten.R_VOLTAGE, pValueDouble[1]);
				
				//Messwerte speichern
				if (ProgStates.holdDataInRAM) {
					synchronized (Messdaten.DATASYNC) {
			    		messdaten[0].addValue(pValueDouble[0]);
			    		size = messdaten[1].addValue(pValueDouble[1]);
			    	}
				}
				pEnergy += Power.berechneEnergie(pValueDouble[0], pValueDouble[1]);
				pCharge += Power.calcQ(pValueDouble[0]);
				
				pMW[0] = plastMW[0] + (pValueDouble[0] - plastMW[0])/pSize;
				pSA[0] = plastSA[0] + (pValueDouble[0] - plastMW[0]) * (pValueDouble[0] - pMW[0]);
				plastMW[0] = pMW[0];
				plastSA[0] = pSA[0];
				
				//Messwert zum downsampling schicken
				downsampledSize = messdaten[0].addDataToDownsampling(pValueDouble[0], ProgStates.RESAMPLEMETHODE);
				messdaten[0].updateMean(pMW[0]);
				messdaten[0].updateDeviation(pSA[0]);

				pMW[1] = plastMW[1] + (pValueDouble[1] - plastMW[1])/pSize;
				pSA[1] = plastSA[1] + (pValueDouble[1] - plastMW[1]) * (pValueDouble[1] - pMW[1]);
				plastMW[1] = pMW[1];
				plastSA[1] = pSA[1];
				
				//Messwert zum downsampling schicken
				downsampledSize = messdaten[1].addDataToDownsampling(pValueDouble[1], ProgStates.RESAMPLEMETHODE);
				messdaten[1].updateMean(pMW[1]);
				messdaten[1].updateDeviation(pSA[1]);

				if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
					if (globalMax < pValueDouble[0] ) globalMax = pValueDouble[0];
					if (globalMin > pValueDouble[0] ) globalMin = pValueDouble[0];
					if (globalMax < pValueDouble[1] ) globalMax = pValueDouble[1];
					if (globalMin > pValueDouble[1] ) globalMin = pValueDouble[1];
				}

				//Leistung berechnen
				if (!ProgStates.IS_PERIODIC_MEASURE){
					if (messdaten[2] == null){
						messdaten[2] = new Daten(MeasureType.POWER, this);
						messdaten[2].setSize(estimatedSize);
					}
					int power = (int) Range.convertFromTo(Range.WATT, Range.MICROWATT, Power.berechneLeistung(pValueDouble[0], pValueDouble[1]));
					pMW[2] = plastMW[2] + (power - plastMW[2])/pSize;
					pSA[2] = plastSA[2] + (power - plastMW[2]) * (power - pMW[2]);
					plastMW[2] = pMW[2];
					plastSA[2] = pSA[2];
					messdaten[2].updateMean(pMW[2]);
					messdaten[2].updateDeviation(plastSA[2]);
					messdaten[2].setRange(Range.MICROWATT);
					messdaten[2].addValue(power);
					messdaten[2].addDataToDownsampling(power, ProgStates.RESAMPLEMETHODE);
				}	
				
				//writeRAW(val);
				if (!ProgStates.LOADING_FILE && ProgStates.MEASSURE_RUNS){
					writeDataRDFFloat(pValueDouble[0]);
					writeDataRDFFloat(pValueDouble[1]);
				}
				if (pSize == VPP -1 && id == 1 && ProgStates.IS_PERIODIC_MEASURE){
					savePeriodic();		
				}
			}	
			
		}
		
		selectedBounds[1] = size - 1;

		if (ProgStates.IS_PERIODIC_MEASURE && pSize == 0 && id == 1){
			messdaten[0] = new Daten(MeasureType.CURRENT, this);
			messdaten[1] = new Daten(MeasureType.VOLTAGE, this);
			messdaten[0].setSize(VPP);
			messdaten[1].setSize(VPP);
		}
	}
	
	public float getPEnergy(){
		synchronized (pSycn) {
    		return pEnergy;
    	}
	}
	
	public float getPCharge(){
		synchronized (pSycn) {
    		return pCharge;
    	}
	}
	
	public float getPPower(){
		synchronized (pSycn) {
    		return pMW[0] * pMW[1];
    	}		
	}
	
	public float getPMW(int id){
		synchronized (pSycn) {
    		return pMW[id];
    	}		
	}
	
	private String getDataOutputPath(){
		String data_out = "";
		if (ProgStates.IS_PERIODIC_MEASURE){
			if (ProgStates.SAVE_PATH_DATA.isEmpty()) setOutputPath(Property.getPropertiesBoolean(Property.SAVE_TIMESTAMP_ENABLE));
			data_out = ProgStates.SAVE_PATH_DATA.replace(".rdf", "_") + String.format("%05d", pCounter) + ".rdf"; 
		}
		return data_out;
	}
	
	private void savePeriodic(){
		for (int i=0;i<pMW.length;i++){
			plastMW[i] = 0;
			plastSA[i] = 0;
		}
		pSizeAll++;
		savePeriodicStatistics(pSizeAll,VPPSeconds,pMW,pSA,pEnergy,pCharge,pSize + 1);
		pEnergy = 0;
		pCharge = 0;
		pSize = 0;
		pCounter++;		
	}
	
	public float[] getRtPMW(){
		synchronized (pSycn) {
			return pMW;			
		}
	}

	public float[] getRtPMA(){
		synchronized (pSycn) {
			return pSA;			
		}
	}
	
	public float getRtPEnergy(){
		synchronized (pSycn) {
			return pEnergy;			
		}
	}

	/**
	 * Gibt die Messreiehe  anhand der ID zurück
	 * @param i Messreiehen ID
	 * @return
	 */
	public Daten getMessdaten(int id){
    	synchronized (Messdaten.DATASYNC) {
    		return messdaten[id];
    	}
	}
	
	/**
	 * Gibt einen Messwert aus der gewählten Messwertreihe zurück
	 * @param id	Messreien-ID (Strom, Spannung, Leistung)
	 * @param index	Messwert-Index
	 * @return Messwert
	 */
	public float getDataItem(int id, int index){
		synchronized (Messdaten.DATASYNC) {
			return messdaten[id].get(index);
		}
	}
		
	/**
	 * Leert alle Messreiehen
	 */
	public void clean(){
    	for (int i=0;i<3;i++){
    	    synchronized (Messdaten.DATASYNC) {
    	    	if (messdaten[i] == null) continue;
    	    	messdaten[i].clearAll();
    	    }
    	}
	}
	
	/**
	 * Löscht alle Messreiehen und resettet sie Messdatenkasse
	 */
	public void clearAll(){
		clean();
		size = 0;
		markBounds[0] = markBounds[1] = 0;
		selectedBounds[0] = selectedBounds[1] = lastSelectedBounds[0] = lastSelectedBounds[1]= 0;
		globalMax = globalMin = 0;
	    plastMW[0] = plastMW[1] = 0;
	    plastSA[0] = plastSA[1] = 0;
	    pMW[0] = pMW[1] = 0;
	    pSA[0] = pSA[1] = 0;
	    pEnergy = 0;
	    pSize = 0;
	    pSizeAll = 0;
	    pValueSample[0] = pValueSample[1] = 0;
	}
	
	/**
	 * gibt die Anzahl der Messwerte (einer Messreihe) zurück
	 * @return
	 */
	public int getSize(){
		synchronized (DATASYNC) {
			return size;
		}
	}
		
	/**
	 * gibt die Anzahl der Messwerte der runtergesamplelten Messreihe zurück
	 * @return
	 */
	public int getDownsampledSize(){
		return downsampledSize;
	}
	
	public int getPresampledSize(){
		return presampledBounds[1] - presampledBounds[0];
	}
	
	/**
	 * gibt das globale Maximum zurück
	 * @return
	 */
	public float getGlobalMax(){
		return globalMax;
	}
	
	/**
	 * gibt das globale Minimum zurück
	 * @return
	 */
	public float getGlobalMin(){
		return globalMin;
	}
		
	/**
	 * gibt die Anzahl der markierten Werte zurück
	 * @return
	 */
	public int getMarkedSize(){
		if (markBounds[0] >= 0 && markBounds[1] > markBounds[0])
			return markBounds[1] - markBounds[0];
		else
			return -1;
	}

	/**
	 * gibt die Anzahl der ausgewählten, sichtbaren Messdaten zurück
	 * @return
	 */
	public int getSelectedSize(){
		if (selectedBounds[0] >= 0 && selectedBounds[1] > selectedBounds[0])
			return selectedBounds[1] - selectedBounds[0];
		else
			return -1;
	}
	
	/**
	 * gibt die Markierungsgrenzen zurück
	 * @return
	 */
	public int[] getMarkedBounds(){
		return markBounds;
	}
	
	/**
	 * gibt die Selektierungsgrenzen zurück
	 * @return
	 */
	public int[] getSelectedBounds(){
		if (selectedBounds[0] == 0 && selectedBounds[1] == 0){
			selectedBounds[1] = getSize() -1;
		}
		return selectedBounds;
	}
	
	public int[] getPresampledBounds(){
		if (presampledBounds[0] == 0 && presampledBounds[1] == 0){
			presampledBounds[1] = getSize() -1;
		}
		return presampledBounds;
	}
	
	/**
	 * kopiert die Markierung in die Selektion
	 */
	public void copyMarkedBoundsToSelectedBounds(){
		setSelectedBounds(markBounds[0], markBounds[1]);
	}
		
	/**
	 * Gibt das Maximum der selektierten Daten zurück
	 * @param id MessreihenID
	 * @param round (Auf)runden des Maximums
	 * @return
	 */
	public float getSelectedMax(int id,boolean add20pc){
		if (getDaten(id) == null) return 0;
		if (ProgStates.MEASSURE_RUNS){
			try{
				if (messdaten == null || messdaten.length == 0 || getDaten(id) == null) return Integer.MIN_VALUE;
				float max = getDaten(id).getSelectedMax(false);
				if (max < 0 && add20pc) max *= 1 - addPcToValue;
				else if (max > 0 && add20pc) max *= 1 + addPcToValue;
				_main.setCursor(Cursor.DEFAULT_CURSOR);
				return max;
			}catch (Exception e){
				if (ProgStates.printError) e.printStackTrace();
				MyFile.saveError(e);
			}
		}
		if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
			float max = getDaten(id).getSelectedMax(false);
			if (max < 0 && add20pc) max *= 1 - addPcToValue;
			else if (max > 0 && add20pc) max *= 1 + addPcToValue;
			_main.setCursor(Cursor.DEFAULT_CURSOR);
			return max;
		}
		
		return messdaten[id].getMinMax(selectedBounds[0], selectedBounds[1], add20pc, true)[1];
	}
	
	/**
	 * gibt das Minimum der selektierten Daten zurück
	 * @param id MessreihenID
	 * @param round (Ab)runden des Minimums
	 * @return
	 */
	public float getSelectedMin(int id, boolean rem20pc){
		if (getDaten(id) == null) return 0;
		if (ProgStates.MEASSURE_RUNS){
			try{
				if (messdaten == null || messdaten.length == 0 || getDaten(id) == null) return Integer.MAX_VALUE;
				float min = getDaten(id).getSelectedMin(false);
				if (min < 0 && rem20pc) min *= 1 + addPcToValue;
				else if (min > 0 && rem20pc) min *= 1 - addPcToValue;
				return min;
			}catch (Exception e){
				if (ProgStates.printError) e.printStackTrace();
				MyFile.saveError(e);
			}
		}
		if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
			float min = getDaten(id).getSelectedMin(false);
			if (min < 0 && rem20pc) min *= 1 + addPcToValue;
			else if (min > 0 && rem20pc) min *= 1 - addPcToValue;
			return min;
		}
		return messdaten[id].getMinMax(selectedBounds[0], selectedBounds[1], rem20pc, true)[0];
	}
		
	/**
	 * gibt das Datenobjekt der Messreihe anhand der ID zurück
	 * @param i MessreihenID
	 * @return
	 */
	public Daten getDaten(int id){
		//if (isEmpty()) return null;
	    synchronized (Messdaten.DATASYNC) {
			return messdaten[id];
    	}
	}

	/**
	 * Gibt eine Liste der Statechanges zurück
	 * @return Liste mit Statechanges
	 */
	public ArrayList<State> getStateChanges() {
		return stateChanges;
	}
	
	/**
	 * setzt die Statechanges
	 * @param stateChanges Liste Mit Statechanges
	 */
	public void setStateChanges(ArrayList<State> stateChanges) {
		this.stateChanges = stateChanges;
	}
	
	/**
	 * initiallisiert die Messung
	 * Schreibt den Header der Messdatei
	 */
    public void writeHeader(String path){
    	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
    	if (ProgStates.SAVE_PATH_DATA.isEmpty() && path.isEmpty()) setOutputPath(Property.getPropertiesBoolean(Property.SAVE_TIMESTAMP_ENABLE));
    	
        try {
        	String out = ProgStates.SAVE_PATH_DATA;
        	if (!path.isEmpty()) out = path;
        	if (out.endsWith(".csv")) out = out.replace(".csv", ".cdf");       	
        	aOutputStream = new ObjectOutputStream(new FileOutputStream(out));
        	osIsOpen = true;
            aOutputStream.writeObject(Main.VERSION);
            aOutputStream.writeObject(kennung);
            aOutputStream.writeObject(timeStamp);
            aOutputStream.writeObject(TIMEUNIT);
            aOutputStream.writeObject(DT);
            aOutputStream.writeObject(R_CURRENT);
            aOutputStream.writeObject(ProgStates.ADC_MODE);
            aOutputStream.writeObject(estimatedSize);
            aOutputStream.flush();
        } catch (Exception ex) {
        	if (ProgStates.printError) ex.printStackTrace();
        	MyFile.saveError(ex);
        }
                
    }

	/**
	 * initiallisiert die Messung
	 * Schreibt den Header der Messdatei
	 */
    public void writeHeaderRDF(String path){
    	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
    	if ((ProgStates.SAVE_PATH_DATA.isEmpty() && path.isEmpty()) || 
    			ProgStates.SAVE_PATH_DATA.contains(Property.getProperties(Property.SAVE_TIMESTAMP_FORMAT))){
    		setOutputPath(Property.getPropertiesBoolean(Property.SAVE_TIMESTAMP_ENABLE));
    	}
    	String out = ProgStates.SAVE_PATH_DATA;
    	if (!path.isEmpty()) out = path;
    	if (out.endsWith(".csv")) out = out.replace(".csv", ".rdf");       	
    	
        try {
        	dout = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out)));
        	osIsOpen = true;
        	dout.writeDouble(Main.D_VERSION);
        	dout.writeByte(kennung);
        	dout.writeLong(timeStamp);
        	dout.writeInt(TIMEUNIT.index);
        	dout.writeDouble(DT);
        	dout.writeInt(R_CURRENT.index);
        	dout.writeByte(ProgStates.ADC_MODE);
        	dout.writeInt(estimatedSize);
        	dout.flush();
        } catch (Exception ex) {
        	if (ProgStates.printError) ex.printStackTrace();
        	MyFile.saveError(ex);
        }               
    }
    
    public void writeDataRDFInt(int val){
    	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
        if (dout == null || !osIsOpen) return;
        try {
        	dout.writeInt(val);
        } catch (Exception ex) {
        	if (ProgStates.printError) ex.printStackTrace();
        	MyFile.saveError(ex);
        }               
    }

    public void writeDataRDFFloat(float val){
    	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
        if (dout == null || !osIsOpen) return;
        try {
        	dout.writeFloat(val);
        } catch (Exception ex) {
        	if (ProgStates.printError) ex.printStackTrace();
        	MyFile.saveError(ex);
        }               
    }

    public void writeDataRDFLong(long val){
    	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
        if (dout == null || !osIsOpen) return;
        try {
        	dout.writeLong(val);
        } catch (Exception ex) {
        	if (ProgStates.printError) ex.printStackTrace();
        	MyFile.saveError(ex);
        }               
    }

    public void writeDataRDFByte(byte val){
    	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
        if (dout == null || !osIsOpen) return;
        try {
        	dout.writeByte(val);
        } catch (Exception ex) {
        	if (ProgStates.printError) ex.printStackTrace();
        	MyFile.saveError(ex);
        }               
    }
    
    /**
        * gespeichert werden folgende Daten in angegebener Reihenfolge:
        * index        (long)
        * messwert1    (int)
        * messwert2    (int)
        * ...
        * statechange  (index)
        * statechange  (type)
        */
    public void writeRAW(Object val){
    	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
        if (aOutputStream == null || !osIsOpen) return;
        try {
            aOutputStream.writeObject(val);
            aOutputStream.flush();
        } catch (IOException ex) {
        	if (ProgStates.printError) ex.printStackTrace();
        	MyFile.saveError(ex);
        }     
    }

    /**
     * schreibe Messwerte als RAW in Datei
     * @param val Array mit 2 Messwerten 0-Strom, 1-Spannung
     */
	public void writeRAW(Object[] val){
	 	if (!ProgStates.SAVE_DATA || ProgStates.LOADING_FILE) return;
	     if (aOutputStream == null  || !osIsOpen) return;
	     try {
	    	 for (Object o : val) aOutputStream.writeObject(o);
	         aOutputStream.flush();
	     } catch (IOException ex) {
	    	 if (ProgStates.printError) ex.printStackTrace();
	    	 MyFile.saveError(ex);
	     }     
	 }

    /**
     * läd gespeicherte Messdaten 
     * Header: version, kennung, timestamp, timeUnit, dT, messbereich, namesllength, names[i]
     * Messdaten
     * Statechanges
     * @param f	Pfad der gespeicherten Messdaten
     */
    public void readRaw(final File f, SwingWorker<Boolean,Void> myWorker){        
        ObjectInputStream aInputStream = null;
        DataInputStream din = null;
        if (f == null) return;
        if (f.getName().endsWith(".cdf")){
        	fileFormat = 0;
        }else if (f.getName().endsWith(".rdf")){
        	fileFormat = 1;
        }
        
        try {
        	ProgStates.LOADING_FILE = true;
        	ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(null, "lade Datei", new FileInputStream(f));
        	int max = pmis.getProgressMonitor().getMaximum();
        	pmis.close();
        	
        	FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            
            init();

            if (fileFormat == 0){
                aInputStream = new ObjectInputStream(bis);
            	loadHeader(aInputStream); 
            }else if (fileFormat == 1){
                din = new DataInputStream(bis);
            	loadHeaderRDF(din);
            }
            
            stateChanges = new ArrayList<State>();
            
            int length = 0;
        	
            for (Daten d : messdaten){
            	if (d != null && d.getSize() == 0 && estimatedSize == 0) d.setSize(max/30);
            }
        	
            //start Messdaten
            
            if (fileFormat == 0){
            	int step = max / 100;
	            while (bis.available() > 0){
	                int index = (Integer) aInputStream.readObject();
	                if (index >= 0){
			            for (int i=0;i<2;i++){	                	
			                int val = (Integer) aInputStream.readObject();
			                addValueToMessdaten(i, val,0,false);
			            }
	                	
		                length += 24;
		                if (length % step == 0) myWorker.firePropertyChange("VALUES" , max, length);
	                }else{	//stateChanges
	                	if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
	                    	int s_index = (Integer) aInputStream.readObject();
	                    	byte type = (Byte) aInputStream.readObject();
	                    	State s = new State(type, s_index);
	                    	stateChanges.add(s);
	                	}else{
	                    	long ts = (Long) aInputStream.readObject();
	                    	byte type = (Byte) aInputStream.readObject();
		                	State s = new State(type,ts);
		                	stateChanges.add(s);
	                	}
	                }                
	            }
            }else if (fileFormat == 1){
            	int step = estimatedSize / 100;
	            while (din.available() >= 12){
	                int index = din.readInt();
	                if (index >= 0){
			            for (int i=0;i<2;i++){	  
			            	if (d_version < 0.8){
			            		addValueToMessdaten(i, din.readInt(),0,false);
			            	}else if (d_version == 0.8){
			            		addValueToMessdaten(i, 0, (float) din.readDouble(), true);
			            	}else{
			            		addValueToMessdaten(i, 0, din.readFloat(), true);
			            	}
			            }
	                	
		                length++;
		                if (length % step == 0){
		                	myWorker.firePropertyChange("VALUES" , estimatedSize, length);
		                }
	                }else{	//stateChanges
	                	if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
	                    	int s_index = din.readInt();
	                    	byte type = din.readByte();
	                    	State s = new State(type, s_index);
	                    	stateChanges.add(s);
	                	}else{
	                    	long ts = din.readLong();
	                    	byte type = din.readByte();
		                	State s = new State(type,ts);
		                	stateChanges.add(s);
	                	}
	                }                
	            }
            	
            }
            myWorker.firePropertyChange("JOB", 99,"berechne Position der Statechanges");
            calcIndexOfStateChange();
        } catch (Exception ex) {
            System.out.println("Messdatei nicht mit der aktuellen Version Kompatibel!");
            ex.printStackTrace();
        } 
        finally{
            try {
                if (fileFormat == 0) aInputStream.close();
                else if (fileFormat == 1) din.close();
                ProgStates.LOADING_FILE = false;
            } catch (IOException ex) {
            	ProgStates.LOADING_FILE = false;
            	if (ProgStates.printError) ex.printStackTrace();
            	MyFile.saveError(ex);
            }
        }
                
    }
    
    /** neuer Header zum lesen
     * 
     * @param din
     */
    private void loadHeaderRDF(DataInputStream din){
        try {
			d_version = (float) din.readDouble();
	        kennung = din.readByte();
	        timeStamp = din.readLong();
	        int  timeUnit = din.readInt();
	        TIMEUNIT = Range.getRange(timeUnit, MeasureType.TIME);
	        
	        if (d_version >= 0.5){
	        	DT = (float) din.readDouble();
	        }else{
	        	DT =  din.readInt();
	        }
	        
	        int r_current = din.readInt();
	        R_CURRENT = Range.getRange(r_current, MeasureType.CURRENT);
	        
	        if (d_version == 0.1){
	        	Boolean useHarting = din.readBoolean();
	        	if (useHarting){
	        		ProgStates.ADC_MODE = ProgStates.ADC_HARTING;
	        	}
	        }
	        if (d_version >= 0.4){
	        	ProgStates.ADC_MODE = din.readByte();
	        }
	        
	        if (d_version >= 0.6){
	        	setSize(din.readInt());
	        }

	        if (ProgStates.ADC_MODE == ProgStates.ADC_HARTING){
	        	Power.setKalib("ADC_HARTING");
	        }else if(ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL){
	        	Power.setKalib("ADC_AD7765_EVAL");
	        }else if(ProgStates.ADC_MODE == ProgStates.ADC_INTERNAL){
	        	Power.setKalib("ADC_INTERNAL");
	        }
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
   	
    }

    private void loadHeader(ObjectInputStream in){
        try {
			version = (String) in.readObject();
			if (version.equals("1.0")) version = "0.1";
			d_version = Float.parseFloat(version);
	        kennung = (Byte) in.readObject();
	        timeStamp = (Long) in.readObject();
	        TIMEUNIT =(Range) in.readObject();
	        
	        if (d_version >= 0.5){
	        	double _dt = (Double) in.readObject();
	        	DT = (float)(_dt);
	        }else{
	        	DT =  (Integer) in.readObject();
	        }
	        
	        R_CURRENT = (Range) in.readObject();
	        
	        if (d_version == 0.1){
	        	Boolean useHarting = (Boolean) in.readObject();
	        	if (useHarting){
	        		ProgStates.ADC_MODE = ProgStates.ADC_HARTING;
	        	}
	        }
	        if (d_version >= 0.4){
	        	ProgStates.ADC_MODE = (Byte) in.readObject();
	        }
	        
	        if (d_version >= 0.6){
	        	setSize((Integer) in.readObject());
	        }

	        if (ProgStates.ADC_MODE == ProgStates.ADC_HARTING){
	        	Power.setKalib("ADC_HARTING");
	        }else if(ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL){
	        	Power.setKalib("ADC_AD7765_EVAL");
	        }else if(ProgStates.ADC_MODE == ProgStates.ADC_INTERNAL){
	        	Power.setKalib("ADC_INTERNAL");
	        }
	        
		} catch (ClassNotFoundException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		} catch (IOException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
   	
    }
    
    /**
     * setzt den Speicherpfad der Messdatei
     * @param addTimestamp true- fügt Timestamp an die Messdatei an
     */
    public void setOutputPath(boolean addTimestamp){
    	String path = "";
    	if (ProgStates.SAVE_PATH_MEAN.isEmpty()){
    		path = Property.getProperties(Property.SAVE_PATH_FOLDER);
    	}else{
    		if (ProgStates.SAVE_PATH_MEAN.contains(Property.getProperties(Property.SAVE_TIMESTAMP_FORMAT))){
    			ProgStates.SAVE_PATH_MEAN = ProgStates.SAVE_PATH_MEAN.replace(Property.getProperties(Property.SAVE_TIMESTAMP_FORMAT), "");
    		}
    	}
        String[] p = path.split(Pattern.quote(File.separator));
        String tPath = "";
        for (String dir: p){
            tPath += dir;
            File t_f = new File(tPath);
            if (!t_f.exists()){
                t_f.mkdir();
            }
            tPath += File.separator;
        }
        String prefix = Property.getProperties(Property.SAVE_PREFIX);
        prefix = path + File.separator + prefix;
        timeStamp = (new Date()).getTime();
        if (addTimestamp){
        	SimpleDateFormat sdf = new SimpleDateFormat(Property.getProperties(Property.SAVE_TIMESTAMP_FORMAT));
        	prefix += sdf.format(new Date());
        }

    	ProgStates.SAVE_PATH_DATA = prefix + ".rdf";
    }
    
	/**
	 * Schließt den Ausgabestream
	 */
    public void closeStream() {
        try {
            if (fileFormat == 0) aOutputStream.close();
            else if (fileFormat == 1){
            	dout.flush();
            	dout.close();
            }
            osIsOpen = false;
        } catch (Exception e) {}
    }
                
    /**
     * exportiert die Daten ins gewünschte Format
     * @param s startposition der Daten
     * @param e endposition der Daten
     * @param type 0-CSV, (weitere noch nicht vorhanden 1-VF)
     * @return
     */
    public boolean export(int s, int e, int type){
    	switch (type){
    	case 0:			//CSV
    		CSVOut csv = new CSVOut("", this);
     		csv.exportToCSV(s, e, true);
    		break;
    	case 1:
    		exportToVF(s, e);
    		break;
    	}
    	return true;
    }

    /**
     * exportiert in VF Format
     * @param s startposition
     * @param e endposition
     */
    private void exportToVF(int s, int e){
    	if (s < 0) s = 0;
    	if (e < 0) e = size;  	
    }

    /**
     * gibt den aktuellen Mausindex zurück
     * @return
     */
	public int getActualIndex() {
		return actualMouseIndex;
	}

	/**
	 * setzt den aktuellen Mausindex
	 * @param actualMouseIndex
	 */
	public void setActualIndex(int actualMouseIndex) {
		this.actualMouseIndex = actualMouseIndex;
	}
	
	/**
	 * Berechnet den Index der einzelnen Statechanges
	 */
	public void calcIndexOfStateChange(){
		for (int i=0;i<stateChanges.size();i++){
			State s = stateChanges.get(i);
			int index = (int) (s.getTimeStamp() / (Range.convertFromTo(Messdaten.TIMEUNIT, Range.MICROSECOND, Messdaten.DT)));
			s.setIndex(index);
		}
	}
	
	/**
	 * Überprüft, ob die Leistungsmessreihe bereits vorhanden ist
	 * @return
	 */
	public boolean containsPower(){
		return messdaten[2] != null;
	}
		
	public String[] getNamesOfData(){
		List<String> names = new ArrayList<String>();
		for (int i=0;i<3;i++){
			if (messdaten[i] != null){
				switch (i){
				case 0:
					names.add("Strom");
					break;
				case 1:
					names.add("Spannung");
					break;
				case 2:
					names.add("Leistung");
				}
			}
		}
		return names.toArray(new String[names.size()]);
	}
	
	/**
	 * return the number of measurements (Voltage, Current, Power etc.)
	 * @return measurements
	 */
	public int getAnz(){
		int length = 0;
		for (int i=0;i<3;i++){
			if (messdaten[i] != null) length++;
		}
		return length;
	}
	
	public void setSize(int size){
		estimatedSize = size;
		for (Daten d : messdaten) 
			if (d != null) 
				d.setSize(size);
	}
	
	public int getEstimatedSize(){
		return estimatedSize;
	}
	
	public void setupPeriodicCalculation(){
		selectedBounds[0] = 0;
		pMW = new float[3];
		pSA = new float[3];
		plastMW = new float[3];
		plastSA = new float[3];
		pValueSample = new int[2];
		pEnergy = 0;
		pCharge = 0;
		pSizeAll = 0;
		try {
			String line = "index;gesamt Zeit in Sekunden;Tag;Stunden;Minuten;Messwerte;";
			if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD){
				line += 	  "Mittelwert Strom [RAW];Standartabweichung Strom [RAW];" +
							  "Mittelwert Strom [mA];" +
							  "Mittelwert Spannung [RAW];" + "Standartabweichung Spannung [RAW];" +
							  "Mittelwert Spannung [mV];" +
							  "Mittelwert Leistung [mW];" + "Energie [mWs];" + "Ladung [mAs];" +
							  "Timestamp\n";
			}else{
				line += 	"Mittelwert Strom [mA];" +
						  	"Mittelwert Spannung [V];" +
						  	"Mittelwert Leistung [mW];" + "Energie [mWs];" + "Ladung [mAs];" +
						  	"Timestamp\n";				
			}
			int k = 1;
			
			if (!ProgStates.SAVE_PATH_MEAN.isEmpty()) fileName = ProgStates.SAVE_PATH_MEAN;
			if (fileName.endsWith(".cdf")) fileName.replace(".cdf", ".csv");
			while ((f = new File(fileName)).exists()){
				if (ProgStates.SAVE_PATH_MEAN.isEmpty()){					
					fileName = baseName + "-" + String.valueOf(k++) + ".csv";									
				}else{
					int result = JOptionPane.showConfirmDialog(null, "Soll die vorhandene Datei überschrieben werden?","Datei bereits vorhanden",JOptionPane.YES_NO_OPTION);
					if (result == JOptionPane.YES_OPTION){
						break;
					}else{
						fileName = tools.MyFile.selectFile(false, tools.MyFile.CSV);
					}
				}
			}
			
			FileWriter bw = new FileWriter(f,false);
			bw.write(line);
			bw.close();
			
			System.out.println("file save in : " + fileName);
		} catch (IOException e1) {
			_main.stopMeasure();
			if (ProgStates.printError) e1.printStackTrace();
			MyFile.saveError(e1);
		}				
	}
	
	private void savePeriodicStatistics(int size, int vppSeconds, float[] mw, float sa[], float energy, float charge, int anzSamples){
		int allSeconds = size * vppSeconds;
		int days = (int) (allSeconds / 86400);
		int hours = (int) ((allSeconds - days * 86400) / 3600);
		int minutes = (int) ((allSeconds - days * 86400 - hours * 3600) / 60);
		//String timeStamp = new SimpleDateFormat("dd-MM-YYYY hh:mm:ss").format(new Date());
		String timeStamp = "";
				
		String line = size + ";" + allSeconds + ";" + days + ";" + hours + ";" + minutes + ";" + anzSamples + ";";
		
		if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD){
			line += MyMath.roundString(mw[0],3) + ";" + MyMath.roundString(sa[0],3) + ";" +
					MyMath.roundString(Power.getI((int)mw[0]),3) + ";" +
					MyMath.roundString(mw[1],3) + ";" + MyMath.roundString(sa[1],3) + ";" +
					MyMath.roundString(Range.convertFromTo(Range.VOLT, Range.MILLIVOLT, Power.getU(mw[1])),3) + ";" +
					MyMath.roundString(Range.convertFromTo(Range.WATT, Range.MILLIWATT, Power.berechneLeistung(mw[0], mw[1])),3) + ";" + 
					MyMath.roundString(energy,5) + ";" +
					MyMath.roundString(charge, 3) + ";" +
					timeStamp + "\n";
		}else if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
			line += MyMath.roundString(Range.convertFromTo(R_CURRENT, Range.MILLIAMPERE, mw[0]),3) + ";" +		//Mittelwert Strom (mA)
					MyMath.roundString(Range.convertFromTo(R_VOLTAGE, Range.VOLT, mw[1]),3) + ";" +		//Mittelwert Spannung (V)
					MyMath.roundString(Range.convertFromTo(Range.WATT, Range.MILLIWATT, Power.berechneLeistung(mw[0], mw[1])),3) + ";" + 
					MyMath.roundString(energy,3) + ";" +
					MyMath.roundString(charge, 3) + ";" +
					timeStamp + "\n";
		}
		try {
			FileWriter bw = new FileWriter(f,true);
			line = line.replace(".", ",");			//für Excel lesbar machen
			bw.write(line);
			bw.close();
			System.out.print(line);
		} catch (IOException e1) {
			if (ProgStates.printError) e1.printStackTrace();
			MyFile.saveError(e1);
		}		
	}
	
	public HashMap<String, Double> getPowerValues(int start, int stop){
		if (messdaten[2] == null || messdaten[2].getType() != MeasureType.POWER || messdaten[2].getSize() == 0 || (start != 0 && stop != -1)) 
			return Power.getPowerValues(messdaten[0], messdaten[1], start, stop);
		
    	HashMap<String, Double> map = new HashMap<String, Double>();

    	double p = Range.convertFromTo(messdaten[2].messbereich, Range.MILLIWATT, (float) messdaten[2].getGlobalMean());
    	double e = p * Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, Messdaten.DT);
    	double q = messdaten[0].getSum() * Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, Messdaten.DT);
    	double u = (float) messdaten[1].getGlobalMean();
    	
    	q = Range.convertFromTo(Range.AMPERESEKUNDEN, Range.MILLIAMPERESEKUNDEN, (float) q);
    	
    	map.put("p", p);
    	map.put("e", e);
    	map.put("q", q);
    	map.put("u", u);

    	return map;
	}
		
}
