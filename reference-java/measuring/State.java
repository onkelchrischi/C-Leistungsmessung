package measuring;

import java.awt.Color;

public class State {
	private byte type = 0;
	private long timestamp = 0L;
	private int index = 0;
	
	public static byte S_START = 0;
	public static byte S_DUMMY = 1;
	public static byte S_NEW_INTERVAL = 2;
	
	public static Color C_START = Color.GREEN;
	public static Color C_NEW_INTERVAL = Color.ORANGE;
	public static Color C_DUMMY = Color.MAGENTA;
	
	/**
	 * Konstruktor der State Klasse
	 * Definiert den Typ des States und setzt den Timestamp
	 * @param type Typ des States
	 * @param timestamp Timestamp in der Messung
	 */
	public State(byte type, long timestamp){
		this.type = type;
		this.timestamp = timestamp;
	}
	
	/**
	 * Konstruktor der State Klasse
	 * Definiert den Typ des States und den Index
	 * @param type Typ des States
	 * @param index Position in der Messung
	 */
	public State(byte type, int index){
		this.type = type;
		this.index = index;
	}
	
	/**
	 * Setzt die Position des States relativ zu den Messwerten
	 * @param index	Position relativ zu den Messwerten
	 */
	public void setIndex(int index){
		this.index = index;
	}
	
	/**
	 * Gibt den Typ des States zurück
	 * @return Typ
	 */
	public byte getType(){
		return type;
	}
	
	/**
	 * gibt den Timestamp in der Messung zurück
	 * @return Timestamp
	 */
	public long getTimeStamp(){
		return timestamp;
	}
	
	/**
	 * gibt den Index relativ zu den Messwerten zurück
	 * @return Index
	 */
	public int getIndex(){
		return index;
	}
}
