package measuring;

public enum MeasureType {
	//Type:
	CURRENT (0, "Strom","I"),
	VOLTAGE	(1, "Spannung","U"),
	POWER 	(2, "Leistung","P"),
	TIME	(3, "Zeit","T"),
	CHARGE  (4, "Ladung","Q");

	public String name;
	public int index;
	public String shortName;
		
	private MeasureType (int index, String name, String shortName){
		this.name = name;
		this.index = index;
		this.shortName = shortName;
	}
	
	public static MeasureType getType(int id){
		for (MeasureType r : MeasureType.values()){
			if (id == r.index) return r;
		}
		
		return null;
	}

	public static MeasureType getType(String name){
		for (MeasureType r : MeasureType.values()){
			if (name.equalsIgnoreCase(r.name)) return r;
		}
		
		return null;
	}

}
