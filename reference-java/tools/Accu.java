package tools;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Properties;

import program.ProgStates;

public class Accu {
	//Akkuspecs:
	public float U_NOM = 0;
	public float U_MAX = 0;
	public float U_MIN = 0;
	public float CAPACITY = 0;
	public float U_CHARGE = 0;
	public float I_CHARGE_NOM = 0;
	public float I_CHARGE_MAX = 0;
	public float I_DISCHARGE_NOM = 0;
	public float I_DISCHARGE_MAX = 0;
	public String NAME = "";
	
	public Accu (String propFile){
		//lade akkuproperties:
		Properties props = new Properties();
		BufferedInputStream stream;
		try {
			stream = new BufferedInputStream(new FileInputStream(propFile));
			props.load(stream);
			stream.close();	
		} catch (Exception e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
			System.exit(-1);
		}
		try{
			U_NOM = Float.parseFloat(props.getProperty("voltage.nominal"));
			U_MAX = Float.parseFloat(props.getProperty("voltage.maximum"));
			U_MIN = Float.parseFloat(props.getProperty("voltage.minimum"));
			CAPACITY = Float.parseFloat(props.getProperty("capacity.nominal"));
			U_CHARGE = Float.parseFloat(props.getProperty("charge.voltage"));
			I_CHARGE_NOM = Float.parseFloat(props.getProperty("charge.current.standart"));
			I_CHARGE_MAX = Float.parseFloat(props.getProperty("charge.current.maximum"));
			I_DISCHARGE_NOM = Float.parseFloat(props.getProperty("discharge.current.standart"));
			I_DISCHARGE_MAX = Float.parseFloat(props.getProperty("discharge.current.maximum"));
			NAME = props.getProperty("name");
		}catch(Exception e){}
	}
}
