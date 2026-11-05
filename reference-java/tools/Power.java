package tools;

import java.util.ArrayList;
import java.util.HashMap;

import measuring.AD7765;
import measuring.Daten;
import measuring.Messdaten;
import program.ProgStates;
import program.Property;

/**
 * Klasse expliziet für Strom, Spannung und Leistungsmessung
 * @author Christoph
 *
 */
public class Power {
	
	public static float[] kalibI = {1,1,0,0.001F};	//Kalibrierungsparameter
	public static float[] kalibUP = {1,2.5107958050586F,0,1000};				//Kalibrierungsparameter
	
	public static float[][] kalibRangesI = new float[3][3];	//Kalibrierungsparameter
	public static float r_shunt = 0;
	public static float r_measurment = 0;
	
	public static float MAXIMUM_I = 2500;
	public static float MINIMUM_I = 0; 
	public static float MAXIMUM_U = 10000;
	public static float MINIMUM_U = 0;
	
	public static final int RANGE_15_MILIAMPS = 2;
	public static final int RANGE_2500_MILIAMPS = 0;
	public static final int RANGE_VOLTAGE = -1;
	
	
	public static final ArrayList<Accu> ACCUS = new ArrayList<Accu>();

	/**
     * gibt den aktuellen kalibrierten Spannungswert in V zurück
     * @param val Messwert (RAW)
     * @return	kalibrierter Spannungswert in V
     */
    public static float getU(float val){
    	float u = (float) (kalibUP[2] * Math.pow(val,2) + kalibUP[1] * val + kalibUP[0]);
    	return u;
    }
    
    /**
    * gibt den aktuellen kalibrierten Stromwert in RAW zurück
     * @param val Messwert (RAW)
     * @param messbereich	kalibrierung ist abhängig vom Messbereich
     * @return	kalibrierter Stromwert in A
     */
    public static float getI(int val){
    	if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
    		int x = val & AD7765.VALUE_MASK;
    		
    		switch ((int)val & AD7765.RANGE_MASK){
    			case 1:
    				kalibI[0] = kalibRangesI[0][0];
    				kalibI[1] = kalibRangesI[0][1];
    				kalibI[2] = kalibRangesI[0][2];
    				break;
    			case 2:
    				kalibI[0] = kalibRangesI[1][0];
    				kalibI[1] = kalibRangesI[1][1];
    				kalibI[2] = kalibRangesI[1][2];
    				break;
    			case 4:
    				kalibI[0] = kalibRangesI[2][0];
    				kalibI[1] = kalibRangesI[2][1];
    				kalibI[2] = kalibRangesI[2][2];
    				break;
    		}
    		
    		return (float) (kalibI[2] * Math.pow(x,2) + kalibI[1] * x + kalibI[0]);
    	}
		return (float) (kalibI[2] * Math.pow(val,2) + kalibI[1] * val + kalibI[0]);
    }

    public static float getCurrentDiff(float u){
    	return u / r_measurment;
    }
    
    public static float getUShunt(float i){
    	return r_shunt * i;
    }
    
    /**
     * berechnet die Leistung
     * @param iRaw	RAW-Wert vom Strom
     * @param uRaw	RAW-Wert der Spannung
     * @return	Leistung
     */
    public static float berechneLeistung(float i, float u){
    	return i * u;
    }
            
    public static float berechneEnergie(float i, float u){
		return Range.convertFromTo(Range.WATT, Range.MILLIWATT, i * u) * Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, Messdaten.DT);
    }
    
    public static float calcQ(float i){
		return Range.convertFromTo(Range.AMPERESEKUNDEN, Range.MILLIAMPERESEKUNDEN , i * Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, Messdaten.DT));
    }

    public static float calcMaxQ(float i, float dt){
		float I = Range.convertFromTo(Messdaten.R_CURRENT, Range.AMPERE, i);
		return Range.convertFromTo(Range.AMPERESEKUNDEN, Range.MILLIAMPERESEKUNDEN , I * Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, dt));		
    }
    
    /**
     * Gibt Leistungsdaten für einen gewähltn Bereich zurück
     * @param lI Liste mit Strom(RAW)Werten
     * @param lU Liste mit Spannungs(RAW) Werten
     * @return	HashMap mit "p"-Leistung [mW], "e"-Energie [mWs], "q"-Ladung [mAs], u-Mittelwert Spannung [V]
     */
    public static HashMap<String, Double> getPowerValues(Daten I,Daten U, int start, int stop){
    	HashMap<String, Double> map = new HashMap<String, Double>();

    	if (I.getSize() != U.getSize()) return null;
    	
    	int size = stop - start;
    	if (stop == -1) size = I.getSize() - start;
    	double p = 0;
    	double e = 0;
    	double q = 0;
    	double u = 0;

    	for (int i=start;i<stop;i++){
    		float _i = Range.convertFromTo(Messdaten.R_CURRENT, Range.AMPERE, I.get(i));
    		float _u = Range.convertFromTo(Messdaten.R_VOLTAGE, Range.VOLT, U.get(i));
    		p += _i * _u;
    		q += _i;
    		u += _u;
    	}
    	p = Range.convertFromTo(Range.WATT, Range.MILLIWATT, (float) p);
    	e = p * Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, (float) Messdaten.DT);
    	p /= size;
    	q *= Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, Messdaten.DT);
    	q = Range.convertFromTo(Range.AMPERESEKUNDEN, Range.MILLIAMPERESEKUNDEN, (float) q);
    	u /= size;
    	
    	map.put("p", p);
    	map.put("e", e);
    	map.put("q", q);
    	map.put("u", u);

    	return map;
    }
    
    public static void setKalib(String name){
    	if (ProgStates.ISCALIB){
			for (int i=-1;i<3;i++) Power.setCalibrateKalib(i);
			return;
    	}
    	r_shunt = Property.getPropertiesFloat(Property.MESSUNG_R_SHUNT);
    	r_measurment = Property.getPropertiesFloat(Property.MESSUNG_R_DIV);
    	if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL){	//AD7765
    		kalibUP[0] = 0;
    		kalibUP[1] = 0;
    		kalibUP[2] = 0;
    		kalibI[0] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_0);
    		kalibI[1] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_1);
    		kalibI[2] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_2);
    	}else if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
    		kalibUP[0] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_U_0);
    		kalibUP[1] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_U_1);
    		kalibUP[2] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_U_2);
    		kalibRangesI[2][0] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B0_0);
    		kalibRangesI[2][1] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B0_1);
    		kalibRangesI[2][2] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B0_2);    				
    		kalibRangesI[1][0] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B1_0);
    		kalibRangesI[1][1] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B1_1);
    		kalibRangesI[1][2] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B1_2);    				
    		kalibRangesI[0][0] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B2_0);
    		kalibRangesI[0][1] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B2_1);
    		kalibRangesI[0][2] = Property.getCalib(name, Property.MESSUNG_KALIB_AD7765_B2_2);    				
    	}else if (ProgStates.ADC_MODE == ProgStates.ADC_HARTING){	//Harting Current
    		kalibI[0] = Property.getCalib(name, Property.MESSUNG_KALIB_HARTING_0);
    		kalibI[1] = Property.getCalib(name, Property.MESSUNG_KALIB_HARTING_1);
    		kalibI[2] = Property.getCalib(name, Property.MESSUNG_KALIB_HARTING_2);
    		kalibUP[0] = Property.getCalib(name, Property.MESSUNG_KALIB_U_0);
	    	kalibUP[1] = Property.getCalib(name, Property.MESSUNG_KALIB_U_1);
	    	kalibUP[2] = Property.getCalib(name, Property.MESSUNG_KALIB_U_2);
    	}else{
    		kalibI[0] = Property.getCalib(name, Property.MESSUNG_KALIB_INTERNAL_0);
    		kalibI[1] = Property.getCalib(name, Property.MESSUNG_KALIB_INTERNAL_1);
    		kalibI[2] = Property.getCalib(name, Property.MESSUNG_KALIB_INTERNAL_2);
    		kalibUP[0] = Property.getCalib(name, Property.MESSUNG_KALIB_U_0);
	    	kalibUP[1] = Property.getCalib(name, Property.MESSUNG_KALIB_U_1);
	    	kalibUP[2] = Property.getCalib(name, Property.MESSUNG_KALIB_U_2);
    	}   	
    }
    
    public static void setCalibrateKalib(int range){
    	
    	if (range >= 0){
	    	kalibRangesI[range][0] = 0;
	    	kalibRangesI[range][1] = 1;
	    	kalibRangesI[range][2] = 0;
    	}else{
	    	kalibUP[0] = 0;
	    	kalibUP[1] = 1;
	    	kalibUP[2] = 0;
    	}
    }
    
    public static float getKalib(int range, int k){
    	switch (range){
    	case RANGE_15_MILIAMPS:
    		return kalibRangesI[RANGE_15_MILIAMPS][k];
    	case RANGE_2500_MILIAMPS:
    		return kalibRangesI[RANGE_2500_MILIAMPS][k];
    	case RANGE_VOLTAGE:
    		return kalibUP[k];
    	}
    	return 0;
    }
         
}
