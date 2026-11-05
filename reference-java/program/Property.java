package program;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import tools.MyFile;


public class Property {
	
	public static String SAVE_PATH_FOLDER = "prog.savepath.folder";
	public static String SAVE_PATH_FULL_FOLDER = "prog.savepath.full";
	public static String SAVE_PREFIX = "prog.savepath.prefix";
	public static String SAVE_TIMESTAMP_FORMAT = "prog.savepath.timestamp";
	public static String AUTOSAVE = "prog.autosave";
	public static String LOGLEVEL = "prog.loglevel";
	public static String SAVE_TIMESTAMP_ENABLE = "prog.savepath.timestamp.enable";
	public static String GUI_UPDATE_INTERVAL = "prog.updateGUI.Interval";
	public static String GUI_LIVEUPDATE_ENABLE = "prog.updateGUI.liveupdateenable";
	public static String AUTOSTART_ENABLE = "prog.autostart.enable";
	public static String AUTOLOAD_FILE = "prog.autoload.file";
	public static String AUTOLOAD_ENABLE = "prog.autoload.enable";
	public static String CONFIG_MEASURE_INTERVAL = "prog.config.measure.interval";
	public static String CONFIG_MEASURE_TIMEUNIT = "prog.config.measure.timeunit";
	public static String CONFIG_MEASURE_FIXTIME = "prog.config.measure.fixtime";
	public static String CONFIG_MEASURE_FIXTIMEUNIT = "prog.config.measure.fixtimeunit";
	public static String CONFIG_MEASURE_FIXTIMEENABLE = "prog.config.measure.fixtimeenable";
	public static String CONFIG_MEASURE_USEHARTING = "prog.config.measure.useharting";
	public static String CONFIG_MEASURE_USEAD7765EVAL = "prog.config.measure.useAD7765EVAL";
	public static String CONFIG_MEASURE_USEAD7765 = "prog.config.measure.useAD7765";
	public static String CONFIG_MEASURE_PERIODIC_ENABLE = "prog.config.measure.periodic.enable";
	public static String CONFIG_MEASURE_PERIODIC_SAVEDATA = "prog.config.measure.periodic.saveData";
	public static String CONFIG_MEASURE_PERIODIC_TIME = "prog.confic.measure.periodic.time";
	public static String CONFIG_MEASURE_PERIODIC_UNIT = "prog.config.measure.periodic.unit";
	public static String CONFIG_MEASURE_SELECTED_AD7765 = "prog.config.measure.selectedAD7765";
	
	public static String ENABLE_JAVA_ERROR_MESSAGES = "prog.errormessage.enable";
	public static String CALIB_PORT_HM8143 = "prog.calib.port.HM8143";
	public static String CALIB_PORT_UT804 = "prog.calib.port.UT804";
	public static String CALIB_PORT_HM8112 = "prog.calib.port.HM8112-3";
	
	public static String COM_PORT = "communication.COM";
	public static String COM_BAUTRATE = "communication.bautrate";
	public static String COM_PORT_FAST = "communication.COM.fast";
	public static String COM_BAUTRATE_FAST = "communication.bautrate.fast";
	public static String COM_AUTOCONNECT = "communication.autoconect";
	public static String COM_USE_PREON_USART2USB_BRIDGE = "communication.usart2usb";
	public static String COM_USE_FT232H_ONLY = "communication.useFT232HOnly";
	public static String PREON32_PROGNAME = "communication.progname";
	public static String COM_TIMEOUT = "communication.timeout";
	public static String COM_UM232H_SN = "communication.UM232H.SN";
	public static String MESSUNG_KALIB_INTERNAL_0 = "internal.k0";
	public static String MESSUNG_KALIB_INTERNAL_1 = "internal.k1";
	public static String MESSUNG_KALIB_INTERNAL_2 = "internal.k2";
	public static String MESSUNG_KALIB_U_0 = "U.k0";
	public static String MESSUNG_KALIB_U_1 = "U.k1";
	public static String MESSUNG_KALIB_U_2 = "U.k2";
	public static String MESSUNG_KALIB_HARTING_0 = "harting.k0";
	public static String MESSUNG_KALIB_HARTING_1 = "harting.k1";
	public static String MESSUNG_KALIB_HARTING_2 = "harting.k2";
	public static String MESSUNG_KALIB_AD7765_0 = "AD7765.k0";
	public static String MESSUNG_KALIB_AD7765_1 = "AD7765.k1";
	public static String MESSUNG_KALIB_AD7765_2 = "AD7765.k2";
	public static String MESSUNG_KALIB_AD7765_B0_0 = "AD7765_B0.k0";
	public static String MESSUNG_KALIB_AD7765_B0_1 = "AD7765_B0.k1";
	public static String MESSUNG_KALIB_AD7765_B0_2 = "AD7765_B0.k2";
	public static String MESSUNG_KALIB_AD7765_B1_0 = "AD7765_B1.k0";
	public static String MESSUNG_KALIB_AD7765_B1_1 = "AD7765_B1.k1";
	public static String MESSUNG_KALIB_AD7765_B1_2 = "AD7765_B1.k2";
	public static String MESSUNG_KALIB_AD7765_B2_0 = "AD7765_B2.k0";
	public static String MESSUNG_KALIB_AD7765_B2_1 = "AD7765_B2.k1";
	public static String MESSUNG_KALIB_AD7765_B2_2 = "AD7765_B2.k2";
	public static String MESSUNG_KALIB_AD7765_U_0 = "AD7765_U.k0";
	public static String MESSUNG_KALIB_AD7765_U_1 = "AD7765_U.k1";
	public static String MESSUNG_KALIB_AD7765_U_2 = "AD7765_U.k2";
	public static String MESSUNG_AD7765_MCLK = "messung.kalib.AD7765.MCLK";
	public static String MESSUNG_AD7765_DEC_RATE = "messung.kalib.AD7765.DEC_RATE";
	public static String MESSUNG_AD7765_OSR = "messung.kalib.AD7765.OSR";
	public static String MESSUNG_AD7765_KALIB_ENABLE = "messung.kalib.enable";
	public static String MESSUNG_OVERSAMPLING = "messung.oversampling.factor";
	public static String MESSUNG_OVERSAMPLING_ENABLE = "messung.oversampling.enable";
	public static String MESSUNG_R_SHUNT = "messung.r.shunt";
	public static String MESSUNG_R_DIV = "messung.r.div";
	public static String TIME_CORRECTION_100 = "messung.kalib.timeCorrection.100";
	public static String TIME_CORRECTION_200 = "messung.kalib.timeCorrection.200";
	public static String TIME_CORRECTION_250 = "messung.kalib.timeCorrection.250";
	public static String TIME_CORRECTION_500 = "messung.kalib.timeCorrection.500";
	public static String TIME_CORRECTION_1000 = "messung.kalib.timeCorrection.1000";
	public static String RAW_TO_VAL = "messung.rawtoval";
	public static String HARTING_RAW_TO_VAL = "messung.harting.rawtoval";
	public static String ADC_OSF = "messung.adc.oversamplingfactor";
	public static String ADC_RES = "messung.adc.resolutio";
	public static String KALIB_UPDATE = "update";
	public static String R_MEAS_ALL = "messung.R.all";
	
	
	//AD7765 Properties
	public static String MCLK = "MCLK";
	public static String SHUNT = "r";
	public static String DEC_RATE = "DEC_RATE";
	public static String USE_FT232H = "USE_FT232H";
	
	private static String propFileName = "";
	private static Properties properties = new Properties();
		
	private static HashMap<String,Properties> calibDevices = new HashMap<String,Properties>();
	
	public static void loadProperties(String filename){
		propFileName = filename;
		BufferedInputStream stream;
		try {
			stream = new BufferedInputStream(new FileInputStream(filename));
			properties.load(stream);
			stream.close();	
			
			File dir = new File("properties");
			if (dir.isDirectory()){
				File[] props = dir.listFiles();
				for (File f : props){
					if (!f.getName().contains("calib")) continue;
					try{
						Properties calib = new Properties();
						stream = new BufferedInputStream(new FileInputStream(f));
						calib.load(stream);
						String name = calib.getProperty("name");
						calibDevices.put(name, calib);
						stream.close();	
					}catch (Exception e){}
				}
			}
		} catch (Exception e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
			System.exit(-1);
		}
		System.out.println("properties loaded");
	}
	
	public static void saveProperties(){
		BufferedOutputStream stream;
		try {
			stream = new BufferedOutputStream(new FileOutputStream(propFileName));
			properties.store(stream, "");
			stream.close();	
		} catch (Exception e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}	
		System.out.println("properties saved");
	}
	
	public static void saveCalibProperties(){
		BufferedOutputStream stream;
		try {
			for (String key : calibDevices.keySet()){
				String path = "properties\\calib_" + calibDevices.get(key).getProperty("name") + ".properties";
				stream = new BufferedOutputStream(new FileOutputStream(path));
				(calibDevices.get(key)).store(stream, "");
				stream.close();					
			}
		} catch (Exception e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}	
		System.out.println("calibration saved");		
	}
	
	public static String getProperties(String key){
		return properties.getProperty(key,"");
	}

	public static int getPropertiesInt(String key){
		int val = 0;
		try{
			val = Integer.parseInt(properties.getProperty(key,"0").trim());
		}catch(Exception e){
			System.out.println(e.getMessage() + "\n" + properties.getProperty(key));
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
		return val;
	}
	
	public static float getPropertiesFloat(String key){
		float val = 0;
		try{
			val = Float.parseFloat(properties.getProperty(key,"0.").trim());
		}catch(Exception e){
			System.out.println(e.getMessage() + "\n" + properties.getProperty(key));
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
		return val;
	}	
	
	public static boolean getPropertiesBoolean(String key){
		if (properties.getProperty(key,"false").equalsIgnoreCase("1") ||
				properties.getProperty(key,"false").equalsIgnoreCase("true")){
			return true;
		}
		return false;
	}

	public static Properties Props(){
		return properties;
	}
	
	public static void setProperties(String key, String value){
		properties.setProperty(key, value);
	}

	public static void setProperties(String key, boolean value){
		String val = "0";
		if (value) val = "1";
		properties.setProperty(key, val);
	}
	
	public static float getCalib(String device, String key){
		Properties props = Property.getCalibrationProperties(device);
		float val = 0;
		try{
			val = Float.parseFloat(props.getProperty(key,"0.").trim());
		}catch(Exception e){
			System.out.println(e.getMessage() + "\n" + props.getProperty(key));
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
		return val;		
	}
	
	public static void setCalib(String device, String key, float val){
		calibDevices.get(device).setProperty(key, String.valueOf(val));
		String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		calibDevices.get(device).setProperty(KALIB_UPDATE, now);
	}

	public static void setCalib(String device, String key, String val){
		calibDevices.get(device).setProperty(key, val);
		String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		calibDevices.get(device).setProperty(KALIB_UPDATE, now);
	}
	
	public static String[] getCalibDevices(){
		return calibDevices.keySet().toArray(new String[calibDevices.size()]);
	}
	
	public static Properties getCalibrationProperties(String name){
		return calibDevices.get(name);
	}

	
}
