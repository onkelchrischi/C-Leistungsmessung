package tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import measuring.Messdaten;


public class Statistics {
		
	public static int RESAMPLE_MEAN = 0;
	public static int RESAMPLE_MINMAX = 1;
	
    public static float[] getMinMax(List<Float> _data, boolean add5pc){
    	
    	float max = Integer.MIN_VALUE;
    	float min = Integer.MAX_VALUE;
    	float[] minmax = {min,max};
    	if (_data == null) return minmax;
    	for (float val : _data){
    		if (val > max) max = val;
    		if (val < min) min = val;
    	}

    	float diff = max - min;
    	if (add5pc){
    		max += diff * 0.05;
    		min -= diff * 0.05;
    	}
    	minmax[0] = min;
    	minmax[1] = max;
    	return minmax;
    }
    
    public static float getMittelwert(List<Float> _data){
    	if (_data == null) return 0;
    	if (_data.size() == 0) return 0;
    	float sum = 0;
    	for (Float val : _data){
    		sum += val;
    	}
    	
    	return sum/(float)_data.size();
    }
  
    /**
     * gibt Mittelwert und Standartabweichung zurück
     * @param buff	IntBuffer mit Messdaten
     * @param type	Messtype
     * @return	float[2] array mit {Mittelwert, Standartabweichung}
     */
    public static double[] getMeanSD(Messdaten data, int id, int lBound, int rBound){
    	if (data.getDaten(id) == null) return new double[2];
    	if (data.getSize() == 0) return new double[2];
    	
    	double actualMean = 0;
    	double lastMean = 0;
    	double actualSD = 0;
    	double lastSD = 0;
    	
    	if (lBound == -1) rBound = data.getSize() - 1;
    	
    	float val = 0;
    	int counter = 1;
    	for (int i = lBound; i < rBound ;i++){
    		val = data.getDaten(id).get(i);
    		actualMean = lastMean + (val - lastMean) / (counter++);
    		actualSD = lastSD + (val - lastMean) * (val - actualMean);
    		lastMean = actualMean;
    	}
    	double[] out = {actualMean,actualSD};
    	return out;
    }

    public static HashMap<String,Float> getStatistics(List<Float> l){
    	float lastMW = 0;
    	float lastSA = 0;
    	float mw = 0;
    	float sa = 0;
    	float max = Float.MIN_VALUE;
    	HashMap<String, Float> map = new HashMap<String,Float>();

    	for (int i=0;i<l.size();i++){
    		mw = lastMW + (l.get(i) - lastMW)/(i+1);
    		sa = lastSA + (l.get(i) - lastMW) * (l.get(i) - mw);
    		lastMW = mw;
    		lastSA = sa;
    		if (l.get(i) > max) max = l.get(i);
    	}
    	map.put("mw", mw);
    	map.put("sa", sa);
    	map.put("max", max);
    	return map;
    }

    public static HashMap<String,Float> getStatisticsFromValues(List<Float> l){
    	float lastMW = 0;
    	float lastSA = 0;
    	float mw = 0;
    	float sa = 0;
    	float max = Float.MIN_VALUE;
    	HashMap<String, Float> map = new HashMap<String,Float>();
    	float val = 0;
    	for (int i=0;i<l.size();i++){
    		val = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, l.get(i));
    		mw = lastMW + (val - lastMW)/(i+1);
    		sa = lastSA + (l.get(i) - lastMW) * (l.get(i) - mw);
    		lastMW = mw;
    		lastSA = sa;
    		if (val > max) max = val;
    	}
    	map.put("mw", mw);
    	map.put("sa", sa);
    	map.put("max", max);
    	return map;
    }

    private static float getVarianz(List<Float> _data){
    	if (_data == null) return 0;
    	if (_data.size() == 0) return 0;
    	float u = getMittelwert(_data);
    	float sum = 0;
    	for (Float val : _data){
    		sum += Math.pow(val - u,2); 
    	}   	
    	return sum / _data.size();
    }
    
    private static float getVarianz(List<Float> _data, float mittelwert){
    	if (_data == null) return 0;
    	if (_data.size() == 0) return 0;
    	float sum = 0;
    	for (Float val : _data){
    		sum += Math.pow(val - mittelwert,2); 
    	}   	
    	return sum / _data.size();
    	
    }
    
    public static float getStandardabweichung(List<Float> _data){
    	return (float) Math.sqrt(getVarianz(_data));
    }
    
    public static float getStandardabweichung(List<Float> _data, float Mittelwert){
    	return (float) Math.sqrt(getVarianz(_data,Mittelwert));
    }
        
    /**
     * entferne Messfehler und ersetzt sie durch den Mittelwert der beiden Nachbarn
     * @return
     */
    public static List<Float> cleanData(List<Float> daten){
		float m = getMittelwert(daten);
		float s = getStandardabweichung(daten, m);
    	for (int i=0;i<daten.size();i=i+10){
    		ArrayList<Float> temp_list = new ArrayList<Float>();
    		for (int j=i;j<i+10;j++) {
    			if (j >= daten.size()) continue;
    			temp_list.add(daten.get(j));
    		}
    		for (int j=0;j<temp_list.size();j++) {
    			try{
    				Float val = daten.get(i+j);
	        		if (val > m + 2.576 * s || val < m - 2.576 * s){
	        			if (j + i>0 && j + i<daten.size()){
	        				val = (daten.get(j+i-1) + daten.get(j+i+1)) / 2;
	        			}
	        			else if (j+i < daten.size()) val = daten.get(j+i+1);
	        			else if (j+i >= daten.size()) val = daten.get(j+i-1);
	        			daten.set(j+i, val);
	        		}   
    			}catch (IndexOutOfBoundsException e){
    				
    			}
    		}
    	}    	
    	return daten;
    }
    
    /**
     * Resampled die Daten
     * @param methode	0-Mittelwerte, 1-MinMax
     * @param data		List<Float> Liste mit Messdaten
     * @param newSize	neue Größe
     * @return			List<Float> Liste mit downsampled Messwerten
     */
    public static List<Float> resample(int methode, List<Float> data, int newSize){
    	switch(methode){
	    	case 0:
	    		return resample1(data, newSize);
	    	case 1: 
	    		return resample2(data, newSize);
	    }
    	return data;
    }
        
    /**
     * resamplen mit Mittelwerten
     * @param daten
     * @param newSize
     * @return
     */
 	private static List<Float> resample1(List<Float> daten, int newSize){
 		int size = daten.size();
 		int ds = (size / newSize);
 		if (size <= newSize) return daten;
 		List<Float> newList = new ArrayList<Float>();
 		for (int i=0;i<size;i += ds){
 			if (i+ds >= size) break;
 			float mw = getMittelwert(daten.subList(i, i+ds));
 			newList.add( mw);
 		}
 		return newList;
    }
 
 	/**
 	 * resamplen mit MinMaxBetrachtung
 	 * @param daten
 	 * @param newSize
 	 * @return
 	 */
    private static List<Float> resample2(List<Float> daten, int newSize){
 		int size = daten.size();
 		float d_dx = (float) size / (float) newSize * 2;
 		int ds = (int) d_dx;

 		if (size <= newSize) return daten;
 		List<Float> newList = new ArrayList<Float>();
 		for (int i=0;i<size;i += ds){
 			float max = Integer.MIN_VALUE;
 			float min = Integer.MAX_VALUE;
 			for (int j=0;j<ds;j++){
 				if (i+j >= size) break;
 				if (daten.get(i+j) > max) max = daten.get(i+j);
 				if (daten.get(i+j) < min) min = daten.get(i+j);
 			}
 			newList.add(min);
 			newList.add(max);
 		}
 		return newList;
 	}
 	 	
}

	
