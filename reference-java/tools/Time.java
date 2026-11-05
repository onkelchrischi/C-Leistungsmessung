package tools;

import java.text.SimpleDateFormat;
import java.util.Date;



import measuring.Messdaten;

public class Time {

	/**
	 * gibt einen String mit Zeit zurück
	 * @param d	Wert
	 * @param unit	0-Zeit, 1-Strom, 2-Spannung
	 * @return
	 */
	public static String getTimeString(float d){
		float d_val = d;
		while (d_val > 1000){
			d_val = d_val / 1000;
		}	
		d_val = MyMath.round(d_val, 3);
		String out = d_val + " " + Range.SECOND.unit;
		return out;
	}

    public static String berechneMesszeitToString(long anz, float dt, int roundPosition, boolean showSecondString){
    	float t = anz * dt;
    	t = Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, t);
    	String val = "";
    	if (roundPosition > 0) val =  tools.MyMath.roundString(t, roundPosition);
    	else val =  String.valueOf(t);
    	
    	if (showSecondString) return val + " " + Range.SECOND.unit;
    	return val;
    }
    
    public static String getMillistoTimeString(long time){
    	SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss:SSS");
    	Date d = new Date(time - 60*60*1000);
    	
    	String format =  sdf.format(d);
    	return format;
    }
    
    public static String formatTime(long time, String pattern){
    	SimpleDateFormat sdf = new SimpleDateFormat(pattern);
    	Date d = new Date(time - 60 * 60 * 1000);
    	sdf.getCalendar().setTimeInMillis(time);
    	
    	
    	String format =  sdf.format(d);
    	return format;
    }
    
    public static String getMicrosToTimeString(long time){
    	long micros = time % 1000;
    	long millis = time / 1000;
    	String res = getMillistoTimeString(millis) + ":" + String.valueOf(micros);
    	
    	return res;
    }
    
    public static String getDateFromMicroSeconds(long date){
    	int mil = 0;
    	int sec = 0;
    	int min = 0;
    	int h = 0;
    	int d = 0;
    	String out = "";
    	
    	mil = (int) (date % 1000);
    	date -= mil;
    	sec = (int) ((date / 1000) % 60);
     	date -= sec * 1000;
     	min = (int) ((date / (60 * 1000)) % 3600) ;
     	date -= min * 60 * 1000;
     	h = (int) ((date / (3600 * 1000)) % (3600 * 24));
     	date -= h * 3600 * 1000;
     	d = (int) (date / 3600 * 1000 * 24);
     	
     	if (d > 0) out = String.format("%02d Tage %02d:%02d:%02d", d, h, min, sec);
     	else out = String.format("%02d:%02d:%02d", h, min, sec);
    	return out;
    }
    
}
