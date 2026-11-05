package tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFileChooser;

import program.ProgStates;
import program.Property;

public class MyFile {
	
	public static String folder = "Messung";
	public static int CSV = 0;
	public static int VF = 1;
	public static int CDF = 2;
	public static int RDF = 2;
	public static int CALIBRATION = 3;
	

    public static String selectFile(boolean open, int type){
    	JFileChooser fc = null;
    	String folder = MyFile.folder;
    	if (type == CALIBRATION){
    		folder = "Kalibrierung";
    		fc = new JFileChooser(System.getProperty("user.dir") + File.separator + folder);
    	}else{
	    	String presetPath = Property.getProperties(Property.SAVE_PATH_FULL_FOLDER);
	    	if (!presetPath.isEmpty()){
	    		File presetFolder = new File(presetPath);
	    		if (!presetFolder.exists()){
	    			presetFolder.mkdirs();
	    		}
	    		if (presetFolder.isDirectory()){
	        		fc = new JFileChooser(presetFolder);
	        	}else{
	        		fc = new JFileChooser(System.getProperty("user.dir") + File.separator + folder);
	        	}
	    	}else{
	    		fc = new JFileChooser(System.getProperty("user.dir") + File.separator + folder);
	    	}
    	}
    	final String description;
    	final String[] fileEnding;
    	
    	switch (type){
    		case 0:	//CSV
    			description = "CSV (*.csv)";
    			fileEnding = new String[]{".csv"};
    			break;
    		case 1:
    			description = "Virtenio File Format (*.vf)";
    			fileEnding = new String[]{".vf"};
    			break;    			
    		case 2:
    			description = "RAW Data Format (*.cdf | *.rdf)";
    			fileEnding =  new String[]{".cdf", ".rdf"};
    			break;
    		case 3:
    			description = "Kalibrierungsdatei (*.txt)";
    			fileEnding = new String[]{ ".txt"};
    			break;
    		default:
    			description = "";
    			fileEnding =  new String[]{""};
    	}
    	
		fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
			
			@Override
			public String getDescription() {
				return description;
			}
			
			@Override
			public boolean accept(File f) {
				for (String format : fileEnding)
					if (f.getName().endsWith(format) || f.isDirectory()) return true;
				return false;
			}
		});

    			
    	if (open){
    		fc.setDialogType(JFileChooser.OPEN_DIALOG);
    		fc.setDialogTitle("öffne Messung");    		
        	fc.showDialog(null, "öffnen");
    	}else{
    		fc.setDialogType(JFileChooser.SAVE_DIALOG);
    		fc.setDialogTitle("exportiere Messung");    		    		
        	fc.showDialog(null, "speichern");
    	}
    	File f = fc.getSelectedFile();

    	if (f != null && f.isFile() && open){
    		return f.getAbsolutePath();
    	}
    	else if (f != null && !open){
    		String path = f.getAbsolutePath();
    		if (fileEnding.length > 1){
    			if (!path.endsWith(".rdf")) path += ".rdf";
    		}else if (fileEnding.length > 0){
    			if (!path.endsWith(fileEnding[0])) path += fileEnding[0];
    		} 
    		return path;
    	}
    	
		return null;
    }
       
    public static void saveError(Exception e){
    	File err = new File(ProgStates.errorLog);
    	String line = "";
    	try {
			FileWriter bw = new FileWriter(err,true);
			line = (new SimpleDateFormat("dd.MM HH:mm").format(new Date())) + ": ";
			bw.write(line);
			bw.write(e.getMessage() + "\n");
			for (StackTraceElement element:e.getStackTrace()){
				bw.write("\t" + element.toString() + "\n");
			}
			bw.close();
		} catch (IOException e1) {
			
		}
    }
}
