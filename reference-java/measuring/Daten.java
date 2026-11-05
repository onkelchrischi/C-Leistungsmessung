package measuring;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import program.ProgStates;
import tools.Power;
import tools.Range;
import tools.Statistics;

/**
 * Klasse für die Messdaten
 * @author Christoph
 *
 */
public class Daten{

	private List<Float> drawableData = new ArrayList<Float>();	//Arraylist zur Speicherung der anzuzeigenen Daten
	private List<Float> drawableDataPresample = new ArrayList<Float>();	//Arraylist zur Speicherung der anzuzeigenen Daten 
	private FloatBuffer fb_data;
	private ByteBuffer bb_data;										//Buffer für die RAW Werte
	private int size = 0;											//Anzahl der Messdaten

	private double localMean = 0;									//Mittelwert aller Messdaten	
	private double localDeviation = 0;							//Standartabweichung aller Messdaten
	private double globalMean = 0;									//Mittelwert aller Messdaten	
	private double globalDeviation = 0;							//Standartabweichung aller Messdaten
	public float globalMax = Integer.MIN_VALUE;					//Maximum aller Messdaten
	public float globalMin = Integer.MAX_VALUE;					//Minimum aller Messdaten
	private MeasureType type;										//Type der Messdaten (Voltage/Current/Power)
	public Range messbereich;										//Messbereich der Messdaten
	private Object valueSync = new Object();						//Synchronisationsobjekt für alle Datenzugriffe
	private int leftBound = 0;										//linke Grenze der ausgewählten Daten
	private int rigthBound = 0;										//rechte Grenze der ausgewählten Daten
	public float max = Integer.MIN_VALUE;							//Maximalwert des ausgewählten Bereichs
	public float min = Integer.MAX_VALUE;							//Minimum des ausgewählten Bereichs
	public float markedMin = Integer.MIN_VALUE;
	public float markedMax = Integer.MAX_VALUE;
	private int selectedMinPos = 0;									//Position des Minimums innerhalb des ausgewählten Bereichs
	private int selectedMaxPos = 0;									//Position des Maximums innerhalb des ausgewählten Bereichs
	private float sum = 0;											//Sum of calculated Measurement
	
	public boolean isShown = false;
	
	//realtime downsampling
	public Object DrawableDataSysnc = new Object();				//Synchronisationsobjekt für die Berechnung der anzuzeigenen Daten
	private int actualIndex = 0;									//Index zur Berechnung des liveresamplings
	private float actualMin = Integer.MAX_VALUE;					//Minimum
	private float actualMax = Integer.MIN_VALUE;					//Maximum
	private float actualMean = 0;									//Mittelwert
	private float lastMean = 0;									//letzter Mittelwert
	private int valuesPerDownsamplingFrame = 0;						//Messwerte pro Resampleintervall
	private float actualValue = 0;								//Messwert
	private Messdaten _allData;
	
	/**
	 * Konstruktor, übergibt den Messtyp
	 * @param type Messtyp
	 */
	public Daten(MeasureType type,Messdaten data){
		this.type = type;
		messbereich = Range.getRange(1, type);
		_allData = data;
	}
		
	/**
	 * gibt den Typ der Messreihe zurück
	 * @return Messtyp
	 */
	public MeasureType getType(){
		return type;
	}
	
	/**
	 * set the range or dimension of the measurement
	 * @param r Range
	 */
	public void setRange(Range r){
		messbereich = r;
	}
	
	/**
	 * creates a List of Data which can be used for visualisation
	 * @param newSize	set the new size for displaying
	 * @param mode		set the mode of resampling
	 * @param lBound	set the left bound for the stored Data
	 * @param rBound	set the rigth bound for the stored Data
	 * @param isMiniWindow	calculates the List without using global values
	 * @param forceDownsampling	force the downsampling. There is no check against the global value 'valuesPerDownsamplingFrame' 
	 * @return	ArrayList<Float>	List of resampled Values
	 */
	public List<Float> createDrawableData(int newSize, int mode, int lBound, int rBound, boolean calcLocal, boolean forceDownsampling){
		synchronized (DrawableDataSysnc) {
			int originSize = Math.abs(rBound - lBound);
			int vpd = originSize / newSize;
			
			if (calcLocal){
				return performResampling(lBound, rBound,newSize,mode, vpd);
			}
			
			if (!forceDownsampling && valuesPerDownsamplingFrame == vpd && vpd > 0 && drawableData != null && !drawableData.isEmpty()) return drawableData;
			
			//berechne presample
			drawableDataPresample = performResampling(_allData.getPresampledBounds()[0], _allData.getPresampledBounds()[1], newSize, mode, vpd);
						
			if (lBound == 0 && rBound == getSize()-1) drawableData = drawableDataPresample;
			else copyDataFromPresample(lBound, rBound);
			
			return drawableData;
		}
	}
	
	private List<Float> performResampling (int lBound, int rBound, int newSize, int mode, int vpd){

		List<Float> list = new ArrayList<Float>(newSize);
		float _min = Float.POSITIVE_INFINITY;
		float _max = Float.NEGATIVE_INFINITY;
		float val = 0;
		float _mean = 0;
		float _lastMean = 0;
		
		int k = 0;
		for (int i=lBound;i<rBound;i++){
			val = fb_data.get(i);
			if (_max < val && val < globalMax){
				_max = val;
				if (i >= leftBound && i <= rigthBound && max < _max){
					max = _max;
					selectedMaxPos = i;
				}
			}
			if (_min > val && val > globalMin){
				_min = val;
				if (i >= leftBound && i <= rigthBound && min > _min){
					min = _min;
					selectedMinPos = i;
				}
			}
			if (mode == 1){
				if (k == (vpd -1) * 2){
					list.add(_max);
					list.add(_min);
					_max = Float.NEGATIVE_INFINITY;
					_min = Float.POSITIVE_INFINITY;
					k = 0;
				}else k++;
			}else if (mode == 0){
				if (k == vpd - 1){
					list.add(_mean);
					_mean = _lastMean = 0;
					k = 0;
				}else{
					_mean = _lastMean + (val - _lastMean) / (++k);
					_lastMean = _mean;							
				}
			}
		}				
		return list;		
	}
	
	public void copyDataFromPresample(int lBound, int rBound){
		int presampledSize = _allData.getPresampledSize();
		int left = _allData.getPresampledBounds()[0];
		
		float pc0 = (float)(lBound - left) / (float) presampledSize;
		float pc1 = (float)(rBound - left) / (float) presampledSize;
		
		int x_0 = (int) (pc0 * drawableDataPresample.size());
		int x_1 = (int) (pc1 * drawableDataPresample.size());
		if (x_0 < 0) x_0 = 0;
		if (x_1 >= drawableDataPresample.size()) x_1 =  drawableDataPresample.size()-1;
		drawableData = drawableDataPresample.subList(x_0, x_1);

	}

	/**
	 * gibt die downgesampelten Daten zurück
	 * @return downgesampelte Daten
	 */
	public List<Float> getDrawableData() {
		synchronized (DrawableDataSysnc) {
			try{
			if (drawableData.isEmpty()) return drawableData;
			return drawableData.subList(0, drawableData.size()-1);
			}catch (Exception e){				
				return drawableData;
			}
		}
	}
	
	/**
	 * @param left
	 * @param rigth
	 */
	public void copyDataToDrawableData(int left, int rigth){
		synchronized (DrawableDataSysnc) {
			drawableData = new ArrayList<Float>(rigth - left);
			float val = 0;
			boolean err = false;
			for (int i=left;i<rigth;i++){
				try{
					if (!err) val = fb_data.get(i);
					drawableData.add(val);
					if (min > val){
						min = val;
						selectedMinPos = i;
					}
					if (max < val){
						max = val;
						selectedMaxPos = i;
					}
					err = false;
				}catch(ConcurrentModificationException e){
					i--;
					err = true;
				}
			}
		}
	}
			
	/**
	 * gibt die Größe der Messreihe zurück
	 * @return Größe der Messreihe
	 */
    public int getSize(){
    	return size;
    }
    
    /**
     * sets the size of the local Bytebuffer, calculates the values per resamplingframe and resets all local values which are used for resampling
     * @param size	estimated number of Values
     */
    public void setSize(int size){
    	bb_data = ByteBuffer.allocateDirect((int)(size * 4 * 1.05));
    	fb_data = bb_data.asFloatBuffer();
    	fb_data.rewind();
    	valuesPerDownsamplingFrame = size / Messdaten.DOWNSAMPLINGSIZE;
    	if (valuesPerDownsamplingFrame == 0) valuesPerDownsamplingFrame = 1;
		drawableData = new ArrayList<Float>(Messdaten.DOWNSAMPLINGSIZE);
		resetActualDataForDownSampling();
    }
        
    /**
     * fügt einen Messwert der Messreihe hinzu
     * @param val Messwert
     * @return neu Groesse
     */
    public int addValue(float val){
    	try{
    		synchronized (valueSync) {
    			if (fb_data.capacity() < size + 1){
    				return size;
    			}
    			fb_data.put(val);
    			sum += val;
		    	size++;
		    	actualValue = val;
				leftBound = 0;
				rigthBound = size -1;			
			}
			checkAndSetMinMax(val,size -1);
    	}catch (Exception e){
    		e.printStackTrace();
    	}
		return size;
	}

    public void updateMean(float mean){
    	globalMean = mean;
    }
    
    public void updateDeviation(float dev){
    	globalDeviation = dev;
    }

    /**
     * checks the given value against the global minimum and maximum. If it's lower/higher the corresponding position will be set.
     * @param value	Value to check
     * @param index Posion of the Value
     */
    public void checkAndSetMinMax(float value,int index){
		if (globalMax < value){
			globalMax = max = value;
			selectedMaxPos = index;
		}
		if (globalMin > value){
			globalMin = min = value;
			selectedMinPos = index;
		}    	
    }

	/**
	 * gibt das Maximum der Messreihe zurück
	 * @return Maximum
	 */
	public float getMax(){
		if (ProgStates.USE_FIX_BOUNDS){
			if (type == MeasureType.CURRENT) return Power.MAXIMUM_I;
			if (type == MeasureType.VOLTAGE) return Power.MAXIMUM_U;
		}

		return globalMax;
	}
	
	/**
	 * returns the global minimum
	 * @return Minimum
	 */
	public float getMin(){
		if (ProgStates.USE_FIX_BOUNDS){
			if (type == MeasureType.VOLTAGE) return Power.MINIMUM_U;
			if (type == MeasureType.CURRENT) return Power.MINIMUM_I;
		}

		return globalMin;
	}
	
	/**
	 * returns the selected maximum within the set left and rigth bounds.
	 * @return local maximum
	 */
	public float getSelectedMax(boolean isRecursiveCall){
		try{
			if (ProgStates.USE_FIX_BOUNDS){
				if (type == MeasureType.CURRENT) return Power.MAXIMUM_I;
				if (type == MeasureType.VOLTAGE) return Power.MAXIMUM_U;
			}
			if (selectedMaxPos <= rigthBound && selectedMaxPos >= leftBound){
				return max;
			}else{
				if (isRecursiveCall){											//if methode called recursivly set
					if (type == MeasureType.VOLTAGE) return Power.MAXIMUM_I;	//voltage minimum or
					if (type == MeasureType.CURRENT) return Power.MAXIMUM_U;	//current minimum
				}
				updateMinMax(leftBound, rigthBound, false);
				return getSelectedMax(true);
			}
		}catch (NullPointerException e){
			return 0;
		}
	}
	
	/**
	 * returns the selected minimum within the set left and rigth bounds.
	 * @return local minimum
	 */
	public float getSelectedMin(boolean isRecursiveCall){
		if (ProgStates.USE_FIX_BOUNDS){
			if (type == MeasureType.VOLTAGE) return Power.MINIMUM_U;
			if (type == MeasureType.CURRENT) return Power.MINIMUM_I;
		}
		if (selectedMinPos <= rigthBound && selectedMinPos >= leftBound){
			return min;
		}else{
			if (isRecursiveCall){											//if methode called recursivly set
				if (type == MeasureType.VOLTAGE) return Power.MINIMUM_U;	//voltage minimum or
				if (type == MeasureType.CURRENT) return Power.MINIMUM_I;	//current minimum
			}
			updateMinMax(leftBound, rigthBound, true);
			getSelectedMin(true);
			return 0;
		}
	}
	
	/**
	 * returns an array with the local minimum and maximum with the given left and rigth bound. 
	 * There's also the opportunity to add 10 percent to the minimum or maximum
	 * @param lBound left bound
	 * @param rBound rigth bound
	 * @param add10pc if true, adds/removes 10 per cent of the maximum or maximum
	 * @return	float[2] {local minimum, local maximum}
	 */
	public float[] getMinMax(int lBound, int rBound, boolean add10pc, boolean forceCheck){
		float[] out = {min,max};
		if (fb_data == null) return out;
		
		if (lBound == leftBound && rBound == rigthBound){
			
		}else{
			updateMinMax(lBound, rBound, forceCheck);
			out[0] = min;
			out[1] = max;
		}
		if (add10pc){
			out[0] *= 1.1;
			out[1] *= 1.1;
		}

		return out;
	}

	/**
	 * sets the left and rigth bound
	 * @param left left bound
	 * @param right rigth bound
	 */
	public void setSelectionBounds(int left, int right){
		if (fb_data == null || fb_data.capacity() == 0) return;
		rigthBound = right;
		leftBound = left;
	}
	
	/**
	 * resets the Buffer for RAW values, the local and global minimum, maximum, mean and deviation
	 */
	public void clearAll(){
		if (bb_data != null) bb_data.clear();	
		if (fb_data != null) fb_data.clear();
		
		drawableData = new ArrayList<Float>();
		globalMean = localMean =  0;
		globalDeviation = localDeviation = 0;
		max = globalMax = Integer.MIN_VALUE;
		min = globalMin = Integer.MAX_VALUE;
		selectedMinPos = selectedMaxPos = leftBound = rigthBound = 0;
		resetActualDataForDownSampling();
		size = 0;
	}
					
	/**
	 * gibt den Messwert an der Position i zurück
	 * @param i Index
	 * @return Messwert
	 */
	public float get(int i){
		try{
			if (fb_data == null || fb_data.capacity() == 0) return 0.F;
			if (i >= fb_data.capacity()) i = fb_data.capacity() - 1;
			
			return fb_data.get(i);
		}catch (ArrayIndexOutOfBoundsException e){
			return 0.F;
		}
	}
		
	public float getSum(){
		return sum;
	}
	
	/**
	 * gibt den Mittelwert der Messreihe zurück
	 * @return Mittelwert
	 */
	public double getMittelwert(int lBound, int rBound){
		
		if (globalMean != 0 && lBound == 0 && rBound == -1) return globalMean;
		else{
			double[] statistics = Statistics.getMeanSD(_allData, type.index, lBound, rBound);
			localMean = statistics[0];
			localDeviation = statistics[1];
		}
		return localMean;
	}
	
	public double getGlobalMean(){
		return globalMean;
	}

	/**
	 * gibt die Standartabweichung der messreihe zurück
	 * @return Standartabweichung
	 */
	public double getStandartAbweichung(int lBound, int rBound){
		if (globalDeviation != 0 && lBound == 0 && rBound == -1) return globalDeviation;
		else if (localDeviation == 0){
			double[] statistics = Statistics.getMeanSD(_allData, type.index, lBound, rBound);
			localMean = statistics[0];
			localDeviation = statistics[1];
		}
		return localDeviation;
	}
	
	/**
	 * check, if the Data is empty
	 * @return true, if empty
	 */
	public boolean isEmpty(){
		return size == 0;
	}
		
	/**
	 * 
	 * @return
	 */
	public float getActualValue(){
		try{
			synchronized (valueSync) {
				return actualValue;			
			}
		}catch (Exception e){
			return 0;
		}
	}
	
	public void updateMinMax(int start, int stop, boolean checknewBound){
		
		if (ProgStates.USE_FIX_BOUNDS) return;
		if (!checknewBound && selectedMaxPos <= rigthBound && selectedMaxPos >= leftBound && selectedMinPos <= rigthBound && selectedMinPos >= leftBound ){
			return;
		}
		if (start == 0 && stop == size -1){
			max = globalMax;
			min = globalMin;
			return;
		}

    	if (start > size) return;

    	min = Float.POSITIVE_INFINITY;
    	max = Float.NEGATIVE_INFINITY;
		for (int i=start;i<stop;i++){
			float val = get(i);
			if (val < min){
				min = val;
				selectedMinPos = i;
			}
			if (val > max){
				max = val;
				selectedMaxPos = i;
			}
		}
	}
	
	public float[] getMarkedMinMax(int start, int stop){
		float[] result = new float[2];
		if (selectedMaxPos >= start && selectedMaxPos <= stop) result[1] = max;
		if (selectedMinPos >= start && selectedMinPos <= stop) result[1] = min;
		
		for (int i=start;i<stop;i++){
			float val = get(i);
			if (val < markedMin){
				markedMin = val;
			}
			if (val > markedMax){
				markedMax = val;
			}
		}
		result[0] = markedMin;
		result[1] = markedMax;
		return result;
	}
	
	public float getMarkedMin(int start, int stop){
		if (selectedMinPos >= start && selectedMinPos <= stop) return min;
		for (int i=start;i<stop;i++){
			float val = get(i);
			if (val < markedMin){
				markedMin = val;
			}
		}
		return markedMin;
	}
		
	public float getMarkedMax(int start, int stop){
		if (selectedMaxPos >= start && selectedMaxPos <= stop) return max;
		for (int i=start;i<stop;i++){
			float val = get(i);
			if (val < markedMax){
				markedMax = val;
			}
		}
		return markedMax;
	}
		
	/**
	 * Sammelt daten für ein Echtzeit-Downsampling
	 * @param val	aktueller Wert
	 * @param mode	Downsampling Methode
	 * 				0 - MinMax
	 * 				1 - Mittelwert
	 */
	public int addDataToDownsampling(float val, int mode){
		if (actualMax < val) actualMax = val;
		if (actualMin > val) actualMin = val;
		actualMean = lastMean + (val - lastMean) / (actualIndex - 1);
		lastMean = actualMean;
		synchronized (DrawableDataSysnc) {
			switch (mode){
				case 1:
					if (actualIndex == (valuesPerDownsamplingFrame -1) * 2){
						drawableData.add(actualMax);
						drawableData.add(actualMin);
						resetActualDataForDownSampling();
					}else actualIndex++;
					break;
				case 0:
					if (actualIndex == valuesPerDownsamplingFrame - 1){
						drawableData.add(actualMean);
						resetActualDataForDownSampling();
					}else actualIndex++;
					break;
			}
			return drawableData.size();
		}
	}
	
	private void resetActualDataForDownSampling(){
		actualIndex = 0;
		actualMin = Integer.MAX_VALUE;
		actualMax = Integer.MIN_VALUE;
		actualMean = 0;
		lastMean = 0;		
	}

	
	public void setDrawableData(List<Float> list){
		drawableData = list;
	}

}
