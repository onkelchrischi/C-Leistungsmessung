package program;

/*
 * @TODO
 * statische Klassen gegen dynamische austauschen
 */

import java.awt.Cursor;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import tools.MyFile;
import tools.Power;
import measuring.Calibrate;
import measuring.Messdaten;
import measuring.Measure;
import frontend.ChangeOffset;
import frontend.Gui;

public class Main {
	
	public final static String VERSION = "0.9";			//Version muss verändert werden, wenn der MessdatenHeader verändert wird! Damit wird die Gefahr von Einlesefehlern verringert
	public final static float D_VERSION = 0.9F;
	
	public final Gui gui = new Gui(this);
	public Messdaten data;
	public final Communication com  = new Communication(this);
	public final Measure measure = new Measure(this);
	private Thread t_gui;
	public Calibrate cal;
	public Thread t_cas;
	
	public static void main(String[] args){
		final Main m = new Main();
		
		if (args.length > 0){
			ProgStates.myPath = args[0] + "\\";
		}
		Property.loadProperties("properties\\Leistungsmessung.properties");
		ProgStates.printError = Property.getPropertiesBoolean(Property.ENABLE_JAVA_ERROR_MESSAGES);
		m.gui.p_config.initProperties();
		m.gui.myMenue.initProperties();
		
		m.t_gui = new Thread(new Runnable() {			
			@Override
			public void run() {
				m.gui.setVisible(true);
			}
		});
		m.t_gui.start();
		
		m.gui.startUpdateThread();
		m.checkProperties();
		m.cal = new Calibrate(m);
	}
	
	private void checkProperties(){

		if (Property.getPropertiesBoolean(Property.AUTOSTART_ENABLE)){
			try {
				com.connect();
			} catch (Exception e) {
				if (ProgStates.printError) e.printStackTrace();
				MyFile.saveError(e);
			}
		}
		if (Property.getPropertiesBoolean(Property.AUTOSTART_ENABLE)){
			gui.doAutostart();
		}
		if (Property.getPropertiesBoolean(Property.AUTOLOAD_ENABLE) && !Property.getPropertiesBoolean(Property.AUTOSTART_ENABLE)){
			try {
				gui.ladeMessung(Property.getProperties(Property.AUTOLOAD_FILE));
			} catch (IOException e) {
				if (ProgStates.printError) e.printStackTrace();
			}
		}
	}
	
	public void updateStatus(String text){
		gui.myMenue.updateStatus(text); 		
	}
	
	public void updateButton(int id, boolean setEnable){
		gui.myMenue.updateButton(id, setEnable);
	}
		
	public boolean prepareStart(){
		try {
			gui.p_config.prepareStart();
			return true;
		} catch (Exception e) {
			if (ProgStates.printError) e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(), "Messung abgebrochen", JOptionPane.ERROR_MESSAGE);
			MyFile.saveError(e);
			gui.myMenue.cmd_mnuStopMessung.doClick();
			return false;
		}
	}
	
	public void stopMeasure(){
		try {
			measure.stop();
		} catch (IOException e) {
			MyFile.saveError(e);
		} catch (InterruptedException e) {
			MyFile.saveError(e);
		}
		gui.messungStoppen();
	}
	
	public void startMeasure(){
		try {	
	        //ProgStates.MEASSURE_RUNS = true;
//			gui.messungStarten();
			Thread.sleep(200);
			measure.start();
			gui.messungStarten();
		} catch (Exception e) {
			gui.myMenue.updateStatus("Error: " + e.getMessage());			
		}
		//gui.messungStarten();
	}
	
	public void reset(boolean b){
		gui.reset(b);
		com.disconnectAll();
	}
	
	public void ladeMessung(String path){
		try {
	    	File f = new File(path);
			while (!f.exists()){
				String p = tools.MyFile.selectFile(true, 2);
				if (p == null) throw new Exception("keine Datei gewählt");
				f = new File(p);
			}
			data.clearAll();
			gui.frmLeistungsmessung.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			if (gui.p_config.chk_periodicMeasure.isSelected()) gui.p_config.chk_periodicMeasure.doClick();
			measure.loadMessung(f);			
		} catch (Exception e) {
			if (ProgStates.printError) System.out.println("e: " + e.getMessage());
			gui.frmLeistungsmessung.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			MyFile.saveError(e);
		}
	}
	
	public void loadSkript(String path){
		
	}
	
	public void connectAndStart(){
		t_cas = new Thread(new Runnable() {		
			@Override
			public void run() {
				if (ProgStates.ADC_MODE == ProgStates.ADC_DUMMY){
					try {
						ProgStates.ISCONNECTED = true;
						gui.p_config.prepareStart();
					} catch (Exception e) {
						System.out.println(e.getLocalizedMessage());
						JOptionPane.showMessageDialog(null, e.getLocalizedMessage() + "\nMessung wird abgebrochen!", "Fehler beim Vorbereiten der Messung", JOptionPane.ERROR_MESSAGE);
						reset(true);
						gui.myMenue.cmd_mnuStartMessung.setEnabled(true);
						return;
					}
				}else{
					boolean con_res = false;
					if (!ProgStates.useFT232HOnly){
						if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("Verbinde zu Preon");
						con_res = com.connect();
					}else if(ProgStates.useFT232HOnly && ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
						con_res = true;
					}
					try {
						if (con_res) con_res = prepareStart();
						if (!con_res){
							System.out.println("Startvorbereitungen fehlgeschlagen");
							com.disconnect();
							gui.myMenue.cmd_mnuStartMessung.setEnabled(true);
							return;
						}
					}catch (Exception e){
						System.out.println(e.getLocalizedMessage());
						JOptionPane.showMessageDialog(null, e.getLocalizedMessage() + "\nMessung wird abgebrochen!", "Fehler beim Vorbereiten der Messung", JOptionPane.ERROR_MESSAGE);
						com.disconnect();
						gui.myMenue.cmd_mnuStartMessung.setEnabled(true);
						return;						
					}
				}
				startMeasure();
			}
		});
		t_cas.start();
	}
	
	public void setCursor(int type){
		gui.frmLeistungsmessung.setCursor(Cursor.getPredefinedCursor(type));
	}
	
	public void exit(){
		System.exit(1);
	}
	
	public void showCalib(){
		cal.setVisible(true);
	}
	
	public void changeOffset(){
		if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD){
			JOptionPane.showMessageDialog(null, "Offsetänderung nur für das AD7765 Board möglich.");
			return;
		}
		String adcMode = ProgStates.getADC_Mode();
		if (adcMode.equalsIgnoreCase("ADC_AD7765_BOARD")) adcMode = (String) gui.p_config.cmb_ad7765Prop.getSelectedItem();
		Power.setKalib(adcMode);
		
		ChangeOffset co = new ChangeOffset();
		co.setModal(true);
		co.setVisible(true);
		double i1 = co.current1;
		double i2 = co.current2;
		double u = co.volatge;
		
		float newi1 = (float) (Power.getKalib(Power.RANGE_15_MILIAMPS, 0) + i1 / 1000000f);
		float newi2 = (float) (Power.getKalib(Power.RANGE_2500_MILIAMPS, 0) + i2 / 1000000f);
		float newu = (float) (Power.getKalib(Power.RANGE_VOLTAGE, 0) + u / 1000000f);
		
		int type = ProgStates.ADC_MODE;

		String text = "Wollen sie den Offset wirklich ändern?\n\n";
		text += "Bereich 1 (0 - 15 mA): " + i1 + " uA\n";
		text += "Bereich 2 (0 - 2500 mA): " + i2 + " uA\n";
		text += "Spannung: " + u + " mV";
		if (JOptionPane.showConfirmDialog(null, text, "Offsetänderung speichern", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION){
			return;
		}
		if (i1 != 0) Calibrate.saveKalib(type, (float) newi1, Power.getKalib(Power.RANGE_15_MILIAMPS, 1), Power.getKalib(Power.RANGE_15_MILIAMPS, 2), Calibrate._15mA, adcMode);
		if (i2 != 0) Calibrate.saveKalib(type, (float) newi2, Power.getKalib(Power.RANGE_2500_MILIAMPS, 1), Power.getKalib(Power.RANGE_2500_MILIAMPS, 2), Calibrate._2500mA, adcMode);
		if (u != 0) Calibrate.saveKalib(type, (float) newu, Power.getKalib(Power.RANGE_VOLTAGE, 1), Power.getKalib(Power.RANGE_VOLTAGE, 2), Calibrate.voltage, adcMode);
		Property.saveCalibProperties();
	}
}
