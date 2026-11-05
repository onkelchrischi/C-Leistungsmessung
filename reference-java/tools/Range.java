package tools;

import measuring.MeasureType;

public enum Range {
	
	//Current:
	AMPERE 		(0, 1,"A"),
	MILLIAMPERE (1, 1000, "mA"),
	MICROAMPERE (2, 1000000, "μA"),
	NANOAMPERE 	(3, 1000000000, "nA"),
	
	//Voltage
	VOLT 		(0, 1, "V"),
	MILLIVOLT 	(1, 1000, "mV"),
	MICROVOLT 	(2, 1000000, "μV"),
	NANOVOLT 	(3, 1000000000, "nV"),
	
	//POWER
	WATT		(0, 1, "W"),
	MILLIWATT 	(1, 1000, "mW"),
	MICROWATT 	(2, 1000000, "uW"),
	NANOWATT 	(3, 1000000000, "nW"),
	
	//Ladung
	AMPERESTUNDEN		(0, 1, "Ah"),
	MILLIAMPERESTUNDEN	(1, 1000, "mAh"),
	AMPERESEKUNDEN		(2, 3600, "As"),
	MILLIAMPERESEKUNDEN (3, 3600000, "mAs"),
	
	//Time
	DAY			(-3, 1/86400, "d"),
	HOUR 		(-2, 1/3600, "h"),
	MINUTE 		(-1, 1/60, "min"),
	SECOND 		(0, 1, "sek"),
	MILLISECOND (1, 1000,"msek"),
	MICROSECOND (2, 1000000, "μsek"),
	NANOSECOND  (3, 1000000000, "nsek");
	
	public float factor;
	public String unit;
	public int index;
	
	private Range (int index,float factor, String unit){
		this.factor = factor;
		this.unit = unit;
		this.index = index;
	}
	
	/**
	 * gibt die Einheit mit oder ohne eckige Klammern zurück
	 * @param r Range
	 * @param addBreakets setzt Klammern (true)
	 * @return String mit Einheit
	 */
	public static String unit(Range r, boolean addBreakets){
		String u = r.unit;
		if (addBreakets) u = "[" + u + "]";
		return u;
	}

	public static float convertFromTo(Range r1, Range r2, float value){
		return value / r1.factor * r2.factor;
	}
	
	public static Range getRange(int index, MeasureType type){
		switch (type){
			case VOLTAGE:
				switch (index){
					case 0:
						return VOLT;
					case 1:
						return MILLIVOLT;
					case 2:
						return MICROVOLT;
					case 3:
						return NANOVOLT;
				}
			case CURRENT:
				switch (index){
					case 0:
						return AMPERE;
					case 1:
						return MILLIAMPERE;
					case 2:
						return MICROAMPERE;
					case 3:
						return NANOAMPERE;
				}
			case TIME:
				switch (index){
					case -3:
						return DAY;
					case -2:
						return HOUR;
					case -1:
						return MINUTE;
					case 0:
						return SECOND;
					case 1:
						return MILLISECOND;
					case 2:
						return MICROSECOND;
					case 3:
						return NANOSECOND;
				}
			case POWER:
				switch (index){
				case 0:
					return WATT;
				case 1:
					return MILLIWATT;
				case 2:
					return MICROWATT;
				case 3:
					return NANOWATT;
				}
			case CHARGE:
				switch (index){
				case 0:
					return AMPERESTUNDEN;
				case 1:
					return MILLIAMPERESTUNDEN;
				case 2:
					return AMPERESEKUNDEN;
				case 3: 
					return MILLIAMPERESEKUNDEN;
				}
		default:
			return Range.AMPERE;
		}		
	}
	
}
