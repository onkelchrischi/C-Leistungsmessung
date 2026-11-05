package program;

public class ProgStates {

	public final static boolean DEBUG = false;

	public static volatile boolean MEASSURE_RUNS = false;
	public static volatile boolean ISCONNECTED = false;
	public static volatile boolean ISCONNECTED_FAST = false;
	public static volatile boolean WAITFORCONNECTION = false;
	public static boolean WAIT_FOR_VF_INPUT = false;
	public static boolean LIVE_UPDATE = false;
	public static boolean LOADING_FILE = false;
	public static long LASTALIVE = 0;
	public static boolean SHOW_PROGRESS = false;
	public static byte ADC_MODE = 0;
	public static boolean useFT232HOnly = false;
	public static String AD7765_DEV_NAME = "";
	public static boolean IS_PERIODIC_MEASURE = false;
	public static int RESAMPLEMETHODE = 1;
	public static boolean SAVE_DATA = true;
	public static boolean USE_FIX_BOUNDS = false;
			
	public final static Object syncAlive = new Object();
	
	public final static int OUTPUT_BUFFER = 2048;
	
	public final static byte ADC_INTERNAL = 0;
	public final static byte ADC_HARTING = 1;
	public final static byte ADC_AD7765_EVAL = 2;
	public final static byte ADC_AD7765_BOARD = 3;
	public final static byte ADC_DUMMY = 99;
	
	public final static int MEASURE_STARTING = 0;
	public final static int MEASURE_STARTED = 1;
	public final static int MEASURE_STOPPING = 2;
	public final static int MEASURE_STOPPED = 3;
	public static int MEASURE_STATE = 3;
	
	public static String SAVE_PATH_MEAN = "";
	public static String SAVE_PATH_DATA = "";
	
	public static String actuallProcess = "";
	public static int actuallProgress = 0;
	public static boolean DOWNSAMPLING_FINISHED = true;
	
	public static boolean waitForPreamble = true;
	
	public static boolean printError = true;
	public static String errorLog = "error.txt";
	public static int loglevel = 0;
	public static String myPath = "";
	
	public static boolean WINDOW_IS_MINIMIZED = false;
	
	public static int CALIB_RANGE = -1;
	public static boolean ISCALIB = false;
	public static boolean holdDataInRAM = true;
	
	public static volatile boolean fixYAxis = true;
	public static boolean drawMiniWindow = true;
	
	public static String getADC_Mode(){
		switch (ADC_MODE){
		case ADC_INTERNAL:
			return "ADC_INTERNAL";
		case ADC_HARTING:
			return "ADC_HARTING";
		case ADC_AD7765_EVAL:
			return "ADC_AD7765_EVAL";
		case ADC_AD7765_BOARD:
			return "ADC_AD7765_BOARD";
		case ADC_DUMMY:
			return "ADC_DUMMY";
		}
		return "";
	}
}
