package tools;

import java.awt.FontMetrics;

import javax.swing.SwingUtilities;

public class UI {
	
	
	public static int getIntervalInMillis(String val, int timeBase){
		int t = 0;
		try{
			t = Integer.parseInt(val);
		}catch (Exception e){
			return 0;
		}
		switch (timeBase){
		case 0: 
			return t;
		case 1:
			return t * 1000;
		case 2: 
			return t * 1000 * 60;
		}
		return 0;
	}
	
	public static int getMaxStringLength(String[] s, FontMetrics fm){
		int max = 0;
		for (int i=0;i<s.length;i++){
			int l = SwingUtilities.computeStringWidth(fm, s[i]);
			if (l>max) max = l;
		}
		return max;
	}

	/**
	 * gibt die Einteilung der Skala zurück
	 * @param size	Messbereich
	 * @return	Skaleneinteilung
	 */
     public static int getIntervalEinteilung(int size){
    	 int dI = 0;
 		 dI = size / 15;
 		 dI = MyMath.roundInt(dI, String.valueOf(dI).length() -1, true);
    	 return dI;
     }
    
}
