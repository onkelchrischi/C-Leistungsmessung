package frontend;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import measuring.MeasureType;
import measuring.Messdaten;
import program.Communication;
import program.Main;
import program.ProgStates;
import program.Property;
import tools.Accu;
import tools.MyFile;
import tools.MyMath;
import tools.Power;
import tools.Range;
import tools.Time;

public class Gui{

	private final Main _main;
	
	public JFrame frmLeistungsmessung;   
    private JTabbedPane tp_main;
    public final MyChart p_chart;
    public final Config p_config;
    public final MyMenue myMenue;

    
    private int[] lastSeletedIndex = {-1,-1};
    private int[] lastMarkedIndex = {-1,-1};
    private boolean doUpdate = true;
    
    private Thread updateGUI;

    /**
     * Erstellt die GUI, benötigt die Mainklasse
     * @param main
     */
    public Gui(Main main) {
		_main = main;
		if (_main.data == null) _main.data = new Messdaten(main);
		p_chart = new MyChart(_main.data, this);
		p_chart.setEnable(true);
		p_config = new Config(_main);
		myMenue = new MyMenue(_main);
		initialize();
		setComponentListeners();
		for (int i=1;i<4;i++) myMenue.updateButton(i, false);
		loadAccu();
	}
	
    /**
     * gibt den Frame der GUI zurück
     * @return
     */
	public JFrame getFrame(){
		return frmLeistungsmessung;
	}
	
	/**
	 * startet den updateThread
	 */
	public void startUpdateThread(){
		updateGUI = new Thread(r_updateGUI);	
		updateGUI.setName("UpdateGUI");
		updateGUI.start();		
	}
	
	/**
	 * setzt die Sichtbarkeit der GUI
	 * @param b true-Sichtbar
	 */
	public void setVisible(final boolean b){
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				frmLeistungsmessung.setVisible(b);
			}
		});
		myMenue.updateStatus("nicht verbunden");
	}
		
	/**
	 * startet die Messung direkt nach dem Programstart (dafür muss autoconnect in den Properties auf 1 gesetzt werden
	 */
	public void doAutostart(){
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				try {
					p_config.prepareStart();
					messungStarten();
				} catch (Exception e){ 
					if (ProgStates.printError) e.printStackTrace();
					MyFile.saveError(e);
				}
			}
		});
	}
	
	/**
	 * läd die Messung
	 * @param path Pfad der Messdatei
	 * @throws IOException
	 */
	public void ladeMessung(final String path) throws IOException{
		
	}
	
	/**
	 * wird ausgeführt wenn die Messdatei erfolgreich geladen wurde
	 * übergibt die Messdaten an das Grapikpanel und wechselt den Tab dahin
	 */
	public void loadDone(){
		p_chart.clearAll();
		p_chart.setAnzGraphen(_main.data.getAnz(), _main.data.getNamesOfData());
		
		updateStatistics();
		changeTab(1);
		myMenue.setProgressBar(false, false);
		p_chart.setLOCK(false);
		frmLeistungsmessung.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}
	
	/**
	 * triggert das startetn der Messung und aktuiviert den repaintTimer
	 */
	public void messungStarten(){
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				changeTab(1);
		        myMenue.updateButton(3, true);   
		        myMenue.updateStatus("Messung läuft...");				
			}
		});
		Thread measureTimer;
		Thread repaintTimer;

		//setze "Aktueller Messwert" zurück
		clearActualValue();
		
		measureTimer = new Thread(r_MeasureTimer);
		measureTimer.setName("Measure Timer");
		measureTimer.start();

		repaintTimer = new Thread(r_repaintTimer);
		repaintTimer.setName("repaint GUI Timer");
		repaintTimer.start();
	}
		
	/**
	 * triggert das stoppen der Messung
	 */
	public void messungStoppen(){
		try{
	        myMenue.updateButton(3, false);     
	        myMenue.updateStatus("Messung beendet");
			p_chart.setLOCK(false);
			p_chart.repaint();
			myMenue.disablePB();
			p_chart.hideCheckboxes(true);
			if (!ProgStates.SAVE_DATA) changeTab(0);
		} catch (Exception e1) {
			myMenue.updateStatus("Error: " + e1.getMessage());
			if (ProgStates.printError) e1.printStackTrace();
			MyFile.saveError(e1);
		}
	}
	
	/**
	 * resettet das Programm und trennt ggf. die Verbindung
	 * @param disconnect
	 */
	public void reset(boolean disconnect){
		_main.data.clearAll();
		p_chart.clearAll();
		changeTab(0);
		if (!disconnect) return;
		if (ProgStates.ISCONNECTED) {
			myMenue.cmd_mnuStopMessung.doClick();
		}
		myMenue.cmd_mnuStartMessung.setEnabled(true);
		myMenue.cmd_mnuStopMessung.setEnabled(false);
	}
	
	/**
	 * Wechselt das Tab
	 * @param index
	 */
	public void changeTab(final int index) {
		SwingUtilities.invokeLater(new Runnable(){			
			@Override
			public void run(){
				tp_main.setSelectedIndex(index);
			}
		});		
	}
	
	/**
	 * (de)aktiviert das angegebene Tab
	 * @param index
	 * @param enabled
	 */
	public void enableTab(final int index, final boolean enabled){
		SwingUtilities.invokeLater(new Runnable(){			
			@Override
			public void run(){
				tp_main.setEnabledAt(index, enabled);
			}
		});				
	}

	/**
	 * Zeit das Aktuelle Messergebnis an
	 * @param anzMesswwerte	Anzahl der Messwerte
	 * @param anzStateChanges Anzahl der Statechanges
	 * @param messzeit Gesamtmesszeit
	 */
	public void updateMessergebnis(final long anzMesswwerte,final int anzStateChanges, final long messzeit) {
		SwingUtilities.invokeLater(new Runnable(){			
			@Override
			public void run(){
				lbl_anzMesswerte.setText(String.valueOf(anzMesswwerte));
				lbl_anzStates.setText(String.valueOf(anzStateChanges));
				lbl_messzeit.setText(Time.berechneMesszeitToString(_main.data.getSize(), Messdaten.DT,3,true));
			}
		});	
	}
	
	/**
	 * aktuallisiert die globalen Statistiken
	 */
	public void updateStatistics() {
		if (ProgStates.MEASSURE_RUNS || _main.data.isEmpty()) return;
		SwingUtilities.invokeLater(new Runnable(){			
			@Override
			public void run(){
				_main.setCursor(Cursor.WAIT_CURSOR);
				lbl_anzMesswerte.setText(String.valueOf(_main.data.getSize()));
				lbl_anzStates.setText(String.valueOf(_main.data.getStateChanges().size()));
				lbl_messzeit.setText(Time.berechneMesszeitToString(_main.data.getSize(), Messdaten.DT,4,true));
				
				if (_main.data.getDaten(0) == null){
					_main.setCursor(Cursor.DEFAULT_CURSOR);
					return;
				}

				HashMap<String,Double> statistics;
				float m = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, (float) _main.data.getDaten(0).getMittelwert(0,-1));
				float max = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, _main.data.getDaten(0).getMax());
				float u_max = Range.convertFromTo(Messdaten.R_VOLTAGE, Range.VOLT, _main.data.getDaten(1).getMax());

		    	synchronized (Messdaten.DATASYNC) {
		    		statistics = _main.data.getPowerValues(0, -1);
		    	}
		    	double p = statistics.get("p");
		    	p = Range.convertFromTo(Range.MICROWATT, Range.MILLIWATT, (float) p);
		    	double mU = statistics.get("u");
		    	double q = statistics.get("q");
		    	double e = statistics.get("e");

				lbl_max_ges.setText(MyMath.roundString(max, 3));
				lbl_MittelwertI_ges.setText(MyMath.roundString(m,3));
				lbl_leistung_ges.setText(MyMath.roundString((float) p,3));
				lbl_energie_ges.setText(MyMath.roundString((float) e, 3));
				lbl_MittelwertU_ges.setText(MyMath.roundString((float) mU, 3));
				lbl_ladung_ges.setText(MyMath.roundString((float) q, 3));
				
				lbl_stat_anzData.setText(lbl_anzMesswerte.getText());
				lbl_stat_measureTime.setText(lbl_messzeit.getText());
				lbl_stat_e.setText(lbl_energie_ges.getText());
				lbl_stat_imax.setText(lbl_max_ges.getText());
				lbl_stat_imean.setText(lbl_MittelwertI_ges.getText());
				lbl_stat_pmean.setText(lbl_leistung_ges.getText());
				lbl_stat_q.setText(lbl_ladung_ges.getText());
				lbl_stat_umax.setText(MyMath.roundString((float) u_max, 3));
				lbl_stat_umean.setText(lbl_MittelwertU_ges.getText());
				
				//berechne Samplingrate
				if (ProgStates.ADC_MODE >= ProgStates.ADC_AD7765_EVAL){
					float sampling = Property.getPropertiesFloat(Property.MESSUNG_AD7765_MCLK) / ( 2* Property.getPropertiesFloat(Property.MESSUNG_AD7765_DEC_RATE));
					lbl_stat_samplingRate.setText(String.format("%.1f", sampling));
				}

				_main.setCursor(Cursor.DEFAULT_CURSOR);
			}
		});
	}

	/**
	 * Aktuallisiert die lokale Statistik, des ausgewählten Bereichs
	 */
	public void updateSelectedStatistics() {
		if (ProgStates.LOADING_FILE) return;
		SwingUtilities.invokeLater(new Runnable(){			
			@Override
			public void run(){
				try{
					_main.setCursor(Cursor.WAIT_CURSOR);
					int index[] = {0,0};
												
					if (_main.data.getMarkedSize() <= 0 && _main.data.getSelectedSize() <=0) return;
					if (_main.data.getMarkedSize() > 0){
						index = _main.data.getMarkedBounds();
					}else{
						index = _main.data.getSelectedBounds();
					}
					
					float m = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, (float) _main.data.getDaten(0).getMittelwert(index[0], index[1]));
					float max = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, _main.data.getDaten(0).getMarkedMin(index[0], index[1]));

					HashMap<String,Double> statistics;
			    	synchronized (Messdaten.DATASYNC) {
			    		statistics = _main.data.getPowerValues(index[0], index[1]);
			    	}
	
			    	double p = statistics.get("p");
			    	p = Range.convertFromTo(Range.MICROWATT, Range.MILLIWATT, (float) p);
			    	double mU = statistics.get("u");
			    	double q = statistics.get("q");
			    	double e = statistics.get("e");
					
	
					lbl_max_i.setText(MyMath.roundString(max,3));
					lbl_Mittelwert_i.setText(MyMath.roundString(m,3));
					lbl_leistungBereich.setText(MyMath.roundString((float) p,3));
					lbl_energieBereich.setText(MyMath.roundString((float) e, 3));		
					lbl_Mittelwert_u.setText(MyMath.roundString((float) mU,3));
					float t0 = index[0]*Range.convertFromTo(Messdaten.TIMEUNIT, Range.MICROSECOND, Messdaten.DT);
					float t1 = index[1]*Range.convertFromTo(Messdaten.TIMEUNIT, Range.MICROSECOND, Messdaten.DT);
					if (t0 > t1){
						float t = t1;
						t1 = t0;
						t0 = t;
					}
					String charge = MyMath.roundString((float) q, 3) + " (" + MyMath.roundString(
									Power.calcMaxQ(
											Range.convertFromTo(Range.MILLIAMPERE, Messdaten.R_CURRENT, max), 
											Range.convertFromTo(Range.MICROSECOND, Messdaten.TIMEUNIT	, t1 - t0))
											,3) + ")";
					lbl_ladung.setText(charge);
					lbl_t0.setText(Time.getMicrosToTimeString((long) (t0)));
					lbl_t1.setText(Time.getMicrosToTimeString((long) (t1)));
					lbl_dt.setText(Time.getMicrosToTimeString((long) (t1-t0)));
					
					//p_chart.doRepaint(true);
				}catch (Exception e){}
				_main.setCursor(Cursor.DEFAULT_CURSOR);
			}
		});
	}
	
	/**
	 * erzeugt die aktuallisierung der globalen Statistik
	 */
	public void updateMessung() {
		updateStatistics();
	}
	
	public void clearActualValue(){		
		//setze Label zurück
		lbl_pos.setText("0");
		lbl_index.setText("0");
		lbl_strom.setText("0");
		lbl_spannung.setText("0");
		lbl_leistung.setText("0");
	}
	
	private void loadAccu(){
		File f = new File(System.getProperty("user.dir"));
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
		Power.ACCUS.clear();
		
		if (f.isDirectory()){
			String[] files = f.list();
			for (String name : files){
				if (name.startsWith("AKKU_") && name.endsWith(".properties")){
					Accu _accu = new Accu(name);
					Power.ACCUS.add(_accu);
					model.addElement(_accu.NAME);
				}
			}
		}else{
			return;
		}
		cmb_akku.setModel(model);
		setAccu(cmb_akku.getItemAt(0));
	}
	
	/**
	 * Runnable der RepaintTimer
	 */
    private Runnable r_repaintTimer = new Runnable(){	
		@Override
		public void run() {
			if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("starte repaint Thread");

			while(ProgStates.MEASSURE_RUNS){
				try{
					if (ProgStates.WINDOW_IS_MINIMIZED){
						Thread.sleep(1000);						
						continue;
					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							if (_main.data.getDaten(0) == null || _main.data.getDaten(1) == null) return;
							float i = 0;
							float u = 0;
							if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD){
								i = _main.data.getRtPMW()[0];
								u = _main.data.getRtPMW()[1];
							}else{
								i = _main.data.getRtPMW()[0];
								u = _main.data.getRtPMW()[1];								
							}
							lbl_Mittelwert_i.setText(MyMath.roundString(Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, i), 3));
							lbl_Mittelwert_u.setText(MyMath.roundString(u, 3));		
							lbl_max_i.setText(MyMath.roundString(Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, _main.data.getSelectedMax(0, false)), 3));
							lbl_leistungBereich.setText(MyMath.roundString(Range.convertFromTo(Range.WATT, Range.MILLIWATT, _main.data.getPPower()), 3));
							lbl_ladung.setText(MyMath.roundString(_main.data.getPCharge(), 3));
							lbl_energieBereich.setText(MyMath.roundString(_main.data.getPEnergy(), 3));
							
							//aktuellen Messwert anzeigen
							lbl_pos.setText(Time.berechneMesszeitToString(_main.measure.getAnzMesswerte(), Messdaten.DT, 3,false));
							
							float iVal = _main.data.getDaten(0).getActualValue();
							float uVal = _main.data.getDaten(1).getActualValue();
							
							float p = Power.berechneLeistung(iVal, uVal);				//(mA * mV / 1000 = mW)
							
							lbl_strom.setText(tools.MyMath.roundString(Range.convertFromTo(Messdaten.R_CURRENT,Range.MILLIAMPERE, iVal),4));
							lbl_spannung.setText(tools.MyMath.roundString(Range.convertFromTo(Messdaten.R_VOLTAGE, Range.VOLT, uVal), 3));
							lbl_leistung.setText(tools.MyMath.roundString(Range.convertFromTo(Range.WATT, Range.MILLIWATT, p) ,3));
							
							lbl_anzStates.setText(String.valueOf(_main.measure.getAnzStateChanges()));
							lbl_messzeit.setText(Time.berechneMesszeitToString(_main.data.getSize(), Messdaten.DT,3,true));
							lbl_anzMesswerte.setText(String.valueOf(_main.data.getSize()));
						}
					});
					Thread.sleep(Property.getPropertiesInt(Property.GUI_UPDATE_INTERVAL));
				} catch (InterruptedException e) {}
			}
		}
	};
	
	/**
	 * Runnable des Messtimers
	 */
	private Runnable r_MeasureTimer = new Runnable() {
		
		@Override
		public void run() {
        	final long endTime = new Date().getTime() + Messdaten.MEASURETIME + 300;
        	final long startTime = new Date().getTime();
        	final long time = endTime - startTime;
        	if (Messdaten.MEASURETIME == 0 && ProgStates.IS_PERIODIC_MEASURE){
        		myMenue.pb_status.setIndeterminate(true);
        		while (ProgStates.MEASSURE_RUNS){
        			try{
        				SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								long t = new Date().getTime() - startTime;
								//myMenue.pb_status.setString("gemessene Zeit: " + Time.formatTime(t,"d HH:mm:ss"));
								myMenue.pb_status.setString("gemessene Zeit: " + Time.getDateFromMicroSeconds(t));
							}
						});
        				Thread.sleep(1000);
        			}catch (InterruptedException e){}
        		}
        	}else{
        		myMenue.pb_status.setIndeterminate(false);
        		while (ProgStates.MEASSURE_RUNS && new Date().getTime() < endTime ){
	        		try {
	        			SwingUtilities.invokeLater(new Runnable() {				
							@Override
							public void run() {
								long t = new Date().getTime() - startTime;
								float progress = (float)t / (float)time * 100;
								String remainTime = " verbleibende Zeit: ";
								long l_remainTime = time - t;
								myMenue.pb_status.setValue((int) progress);
								DecimalFormat df =   new DecimalFormat  ( "0.00" );
								myMenue.pb_status.setString(df.format(progress) + " %   " + remainTime + Time.getMillistoTimeString(l_remainTime));
							}
						});
						Thread.sleep(Property.getPropertiesInt(Property.GUI_UPDATE_INTERVAL));
					} catch (InterruptedException e) {}
	        	}
        	}
        	if (ProgStates.MEASSURE_RUNS){
        		_main.stopMeasure();
        		myMenue.pb_status.setVisible(false);
        	}
		}
	};

	/**
	 * Runnable zur aktuallisierung der GUI
	 */
    private Runnable r_updateGUI = new Runnable(){	
		@Override
		public void run() {
			float valI = 0;
			float valU = 0;
			float valP = 0;
			
			while(true){
				//Überprüfe, ob Verbindung aufgebaut ist
				try{
					if (ProgStates.ISCONNECTED){
						myMenue.updateButton(0, false);		//Verbinden Button deaktivieren
						if (ProgStates.MEASSURE_RUNS){
							myMenue.setStatusIcon(2);
						}
					}else{
						myMenue.updateButton(0, true);		//Verbinden Button deaktivieren
						if (!ProgStates.WAITFORCONNECTION) myMenue.updateStatus("getrennt");
						myMenue.setStatusIcon(0);					
					}
					if (ProgStates.ISCALIB){
						myMenue.chk_liveupdate.setSelected(false);
						ProgStates.LIVE_UPDATE = false;
					}
					//Update Statistik, aktueller Wert etc.
					if (!ProgStates.MEASSURE_RUNS && !ProgStates.LOADING_FILE && !_main.data.isEmpty()){
						//überprüfe, ob update notwendig ist
						if ((lastSeletedIndex[0] == _main.data.getSelectedBounds()[0] && lastSeletedIndex[1] == _main.data.getSelectedBounds()[1]) &&
								(lastMarkedIndex[0] == _main.data.getMarkedBounds()[0] && lastMarkedIndex[1] == _main.data.getMarkedBounds()[1])){
							doUpdate = false;
						}else{
							doUpdate = true;
						}
						lastMarkedIndex[0] = _main.data.getMarkedBounds()[0];
						lastMarkedIndex[1] = _main.data.getMarkedBounds()[1];
						lastSeletedIndex[0] = _main.data.getSelectedBounds()[0];
						lastSeletedIndex[1] = _main.data.getSelectedBounds()[1];		
						
						int index = _main.data.getActualIndex();
						
						valI = _main.data.getDataItem(MeasureType.CURRENT.index, index);		//in mA
						valU = _main.data.getDataItem(MeasureType.VOLTAGE.index, index);		//in mV
						
						valP = valI * valU;

						valI = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, valI);
						valU = Range.convertFromTo(Messdaten.R_VOLTAGE, Range.VOLT, valU);

						lbl_leistung.setText(MyMath.roundString(valP, 3));
						float posT = Range.convertFromTo(Messdaten.TIMEUNIT, Range.SECOND, index * Messdaten.DT);
						lbl_pos.setText(MyMath.roundString(posT,6));
						lbl_strom.setText(MyMath.roundString(valI,3));
						lbl_index.setText(String.valueOf(index));
						lbl_spannung.setText(MyMath.roundString(valU, 3));
						
						if (!_main.data.isEmpty() && (_main.data.getMarkedSize() > 0 || _main.data.getSelectedSize() > 0)){
							if (doUpdate){
								updateSelectedStatistics();
							}
						}
					}
					try {
						Thread.sleep(Property.getPropertiesInt(Property.GUI_UPDATE_INTERVAL));
					} catch (InterruptedException e) {
						e.printStackTrace();
						MyFile.saveError(e);
					}									
				}catch (Exception e){
					if (ProgStates.printError) e.printStackTrace();
					MyFile.saveError(e);
				}
			}
		}
	};
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		panel.setLayout(null);
		panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Akku", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		lblSpannungNominalv.setHorizontalAlignment(SwingConstants.LEFT);
		lblSpannungNominalv.setBounds(10, 105, 163, 14);
		
		panel.add(lblSpannungNominalv);
		lblKapazittmah.setHorizontalAlignment(SwingConstants.LEFT);
		lblKapazittmah.setBounds(10, 130, 163, 14);
		
		panel.add(lblKapazittmah);
		lbl_accu_capacity.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_capacity.setBounds(183, 130, 57, 14);
		
		panel.add(lbl_accu_capacity);
		lbl_accu_Unom.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_Unom.setBounds(183, 105, 57, 14);
		
		panel.add(lbl_accu_Unom);
		lblLadestromNormala.setBounds(10, 180, 163, 14);
		
		panel.add(lblLadestromNormala);
		lblLadestromMaxa.setHorizontalAlignment(SwingConstants.LEFT);
		lblLadestromMaxa.setBounds(10, 205, 163, 14);
		
		panel.add(lblLadestromMaxa);
		lblEntladestromStandarta.setHorizontalAlignment(SwingConstants.LEFT);
		lblEntladestromStandarta.setBounds(10, 230, 163, 14);
		
		panel.add(lblEntladestromStandarta);
		lblEntladestromMaxa.setHorizontalAlignment(SwingConstants.LEFT);
		lblEntladestromMaxa.setBounds(10, 255, 163, 14);
		
		panel.add(lblEntladestromMaxa);
		lbl_accu_discharge_iMax.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_discharge_iMax.setBounds(183, 254, 57, 16);
		
		panel.add(lbl_accu_discharge_iMax);
		lbl_accu_charge_imax.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_charge_imax.setBounds(183, 205, 57, 14);
		
		panel.add(lbl_accu_charge_imax);
		lbl_accu_discharge_iNom.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_discharge_iNom.setBounds(183, 230, 57, 14);
		
		panel.add(lbl_accu_discharge_iNom);
		lbl_accu_charge_iNom.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_charge_iNom.setBounds(183, 180, 57, 14);
		
		panel.add(lbl_accu_charge_iNom);
		lblLadespannungv.setHorizontalAlignment(SwingConstants.LEFT);
		lblLadespannungv.setBounds(10, 155, 163, 14);
		
		panel.add(lblLadespannungv);
		lbl_accu_charge_U.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_charge_U.setBounds(183, 155, 57, 14);
		
		panel.add(lbl_accu_charge_U);
		lblSpannungMaxv.setHorizontalAlignment(SwingConstants.LEFT);
		lblSpannungMaxv.setBounds(10, 83, 163, 14);
		
		panel.add(lblSpannungMaxv);
		lbl_accu_Umin.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_Umin.setBounds(183, 58, 57, 14);
		
		panel.add(lbl_accu_Umin);
		lbl_accu_Umax.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_accu_Umax.setBounds(183, 83, 57, 14);
		
		panel.add(lbl_accu_Umax);
		lblSpannungMinv.setHorizontalAlignment(SwingConstants.LEFT);
		lblSpannungMinv.setBounds(10, 58, 163, 14);
		
		panel.add(lblSpannungMinv);
		lbl_stat_umean.setBounds(197, 180, 123, 14);
		lbl_stat_umean.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_e.setBounds(197, 230, 123, 14);
		lbl_stat_e.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_pmean.setBounds(197, 205, 123, 14);
		lbl_stat_pmean.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_q.setBounds(197, 254, 123, 16);
		lbl_stat_q.setHorizontalAlignment(SwingConstants.RIGHT);
		label_8.setBounds(10, 255, 177, 14);
		label_8.setHorizontalAlignment(SwingConstants.LEFT);
		label_7.setBounds(10, 230, 177, 14);
		label_7.setHorizontalAlignment(SwingConstants.LEFT);
		label_6.setBounds(10, 205, 177, 14);
		label_6.setHorizontalAlignment(SwingConstants.LEFT);
		lbl_stat_imax.setBounds(197, 105, 123, 14);
		lbl_stat_imax.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_imean.setBounds(197, 130, 123, 14);
		lbl_stat_imean.setHorizontalAlignment(SwingConstants.RIGHT);
		label_2.setBounds(10, 130, 177, 14);
		label_2.setHorizontalAlignment(SwingConstants.LEFT);
		label_1.setBounds(10, 105, 177, 14);
		label_1.setHorizontalAlignment(SwingConstants.LEFT);
		p_stat_all.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Statistik Messung", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		txt_maxBoundU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmd_updateBounds.doClick();
			}
		});
		txt_maxBoundU.setEnabled(false);
		txt_maxBoundU.setColumns(10);
		txt_minBoundU.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmd_updateBounds.doClick();
			}
		});
		txt_minBoundU.setEnabled(false);
		txt_minBoundU.setColumns(10);
		lbl_t1.setHorizontalAlignment(SwingConstants.RIGHT);
		lblT.setHorizontalAlignment(SwingConstants.LEFT);
		lbl_t0.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_t0ms.setHorizontalAlignment(SwingConstants.LEFT);
		lbl_ladung_ges.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setHorizontalAlignment(SwingConstants.LEFT);
		lbl_ladung.setHorizontalAlignment(SwingConstants.RIGHT);
		lblImas.setHorizontalAlignment(SwingConstants.LEFT);
		frmLeistungsmessung = new JFrame();
		frmLeistungsmessung.getContentPane().setMaximumSize(new Dimension(1920, 1200));
		frmLeistungsmessung.setMinimumSize(new Dimension(1100, 800));
		frmLeistungsmessung.setTitle("Preon Current Meter");
		frmLeistungsmessung.setBounds(100, 100, 1200, 800);
		frmLeistungsmessung.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);		
		frmLeistungsmessung.setJMenuBar(myMenue);
				
		
		tp_main = new JTabbedPane(JTabbedPane.TOP);
		
		tp_main.addTab("Konfiguration", null, p_config, null);		
		tp_main.addTab("Messung", null, p_messung, null);
		
		p_graphik.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_8.setBorder(new TitledBorder(null, "Messinformationen", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		GroupLayout gl_panel_8 = new GroupLayout(panel_8);
		gl_panel_8.setHorizontalGroup(
			gl_panel_8.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel_8.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel_8.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_8.createParallelGroup(Alignment.TRAILING, false)
							.addComponent(lblAnzahlDerState, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, 2147483647)
							.addComponent(lblAnzahlDerMesswerte, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 105, GroupLayout.PREFERRED_SIZE))
						.addComponent(lblMesszeit, GroupLayout.DEFAULT_SIZE, 107, 2147483647))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_8.createParallelGroup(Alignment.TRAILING)
						.addComponent(lbl_messzeit, GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE)
						.addComponent(lbl_anzStates, GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE)
						.addComponent(lbl_anzMesswerte, GroupLayout.DEFAULT_SIZE, 91, Short.MAX_VALUE))
					.addContainerGap())
		);
		gl_panel_8.setVerticalGroup(
			gl_panel_8.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_8.createSequentialGroup()
					.addGroup(gl_panel_8.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_8.createSequentialGroup()
							.addComponent(lbl_anzMesswerte)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lbl_anzStates)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lbl_messzeit, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel_8.createSequentialGroup()
							.addComponent(lblAnzahlDerMesswerte)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblAnzahlDerState)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lblMesszeit)))
					.addContainerGap(49, Short.MAX_VALUE))
		);
		lbl_anzMesswerte.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_anzStates.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_messzeit.setHorizontalAlignment(SwingConstants.RIGHT);
		
		panel_8.setLayout(gl_panel_8);
		panel_4.setBorder(new TitledBorder(null, "aktueller Messwert", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		lbl_pos.setHorizontalAlignment(SwingConstants.RIGHT);		
		lbl_strom.setHorizontalAlignment(SwingConstants.RIGHT);	
		lbl_spannung.setHorizontalAlignment(SwingConstants.RIGHT);		
		lbl_leistung.setHorizontalAlignment(SwingConstants.RIGHT);	
		lbl_index.setHorizontalAlignment(SwingConstants.RIGHT);
		
				GroupLayout gl_panel_4 = new GroupLayout(panel_4);
				gl_panel_4.setHorizontalGroup(
					gl_panel_4.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_4.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_panel_4.createParallelGroup(Alignment.LEADING)
								.addComponent(lblPosition, GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
								.addComponent(lblStrom, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblStromRaw, GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
								.addComponent(lblSpannung, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblLeistung, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_4.createParallelGroup(Alignment.TRAILING)
								.addComponent(lbl_leistung, GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
								.addComponent(lbl_index, GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
								.addComponent(lbl_pos, GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
								.addComponent(lbl_strom, GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE)
								.addComponent(lbl_spannung, GroupLayout.DEFAULT_SIZE, 108, Short.MAX_VALUE))
							.addContainerGap())
				);
				gl_panel_4.setVerticalGroup(
					gl_panel_4.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_4.createSequentialGroup()
							.addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblPosition)
								.addComponent(lbl_pos))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblStromRaw)
								.addComponent(lbl_index))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_strom)
								.addComponent(lblStrom))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblSpannung)
								.addComponent(lbl_spannung, GroupLayout.PREFERRED_SIZE, 15, GroupLayout.PREFERRED_SIZE))
							.addGap(4)
							.addGroup(gl_panel_4.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_leistung, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblLeistung))
							.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				);
				
				panel_4.setLayout(gl_panel_4);
		panel_6.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Statistik gesamt", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		lblMax.setHorizontalAlignment(SwingConstants.LEFT);		
		lbl_max_ges.setHorizontalAlignment(SwingConstants.RIGHT);		
		lbl_MittelwertI_ges.setHorizontalAlignment(SwingConstants.RIGHT);		
		lblMittelwert.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);		
		lbl_leistung_ges.setHorizontalAlignment(SwingConstants.RIGHT);		
		lbl_energie_ges.setHorizontalAlignment(SwingConstants.RIGHT);		
		lblE.setHorizontalAlignment(SwingConstants.LEFT);
				
				JLabel lblU = new JLabel("Uμ [V]:");
				
				lbl_MittelwertU_ges = new JLabel("0");
				lbl_MittelwertU_ges.setHorizontalAlignment(SwingConstants.RIGHT);
		
				GroupLayout gl_panel_6 = new GroupLayout(panel_6);
				gl_panel_6.setHorizontalGroup(
					gl_panel_6.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_6.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_panel_6.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel_6.createSequentialGroup()
									.addGroup(gl_panel_6.createParallelGroup(Alignment.LEADING, false)
										.addComponent(lblMax, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
										.addComponent(lblMittelwert, GroupLayout.PREFERRED_SIZE, 101, GroupLayout.PREFERRED_SIZE))
									.addPreferredGap(ComponentPlacement.RELATED)
									.addGroup(gl_panel_6.createParallelGroup(Alignment.LEADING)
										.addComponent(lbl_MittelwertI_ges, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(lbl_max_ges, GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE)))
								.addGroup(gl_panel_6.createSequentialGroup()
									.addGroup(gl_panel_6.createParallelGroup(Alignment.LEADING, false)
										.addComponent(lblU)
										.addComponent(lblNewLabel)
										.addComponent(lblE, GroupLayout.PREFERRED_SIZE, 101, GroupLayout.PREFERRED_SIZE)
										.addComponent(label, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE))
									.addPreferredGap(ComponentPlacement.RELATED)
									.addGroup(gl_panel_6.createParallelGroup(Alignment.TRAILING)
										.addComponent(lbl_ladung_ges, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(lbl_leistung_ges, GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
										.addComponent(lbl_energie_ges, GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
										.addComponent(lbl_MittelwertU_ges, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 120, Short.MAX_VALUE))))
							.addContainerGap())
				);
				gl_panel_6.setVerticalGroup(
					gl_panel_6.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_panel_6.createSequentialGroup()
							.addGroup(gl_panel_6.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_max_ges)
								.addComponent(lblMax))
							.addGap(6)
							.addGroup(gl_panel_6.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_MittelwertI_ges)
								.addComponent(lblMittelwert))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_6.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_MittelwertU_ges)
								.addComponent(lblU))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_6.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_leistung_ges)
								.addComponent(lblNewLabel))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_6.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_energie_ges)
								.addComponent(lblE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_6.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_ladung_ges, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
								.addComponent(label))
							.addGap(142))
				);
				
						panel_6.setLayout(gl_panel_6);
		
		panel_7.setBorder(new TitledBorder(null, "Statistik Bereich", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		lbl_maxI.setHorizontalAlignment(SwingConstants.LEFT);	
		lbl_max_i.setHorizontalAlignment(SwingConstants.RIGHT);	
		lbl_Mittelwert_i.setHorizontalAlignment(SwingConstants.RIGHT);		
		lbl_uI.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel_1.setHorizontalAlignment(SwingConstants.LEFT);
		lbl_leistungBereich.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_energieBereich.setHorizontalAlignment(SwingConstants.RIGHT);		
		lblEmws.setHorizontalAlignment(SwingConstants.LEFT);
				
				JLabel lblUv = new JLabel("Uμ [V]:");
				
				lbl_Mittelwert_u = new JLabel("0");
				lbl_Mittelwert_u.setHorizontalAlignment(SwingConstants.RIGHT);
				
				JLabel lblDtmmssmilmic = new JLabel("dT [mm:ss:mil:mic]:");
				
				lbl_dt = new JLabel("0");
				lbl_dt.setHorizontalAlignment(SwingConstants.RIGHT);
		
				GroupLayout gl_panel_7 = new GroupLayout(panel_7);
				gl_panel_7.setHorizontalGroup(
					gl_panel_7.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_7.createSequentialGroup()
							.addContainerGap()
							.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel_7.createSequentialGroup()
									.addGroup(gl_panel_7.createParallelGroup(Alignment.TRAILING)
										.addComponent(lbl_uI, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
										.addComponent(lbl_maxI, GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE))
									.addGap(12)
									.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
										.addGroup(gl_panel_7.createSequentialGroup()
											.addPreferredGap(ComponentPlacement.UNRELATED)
											.addComponent(lbl_max_i, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE))
										.addComponent(lbl_Mittelwert_i, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)))
								.addGroup(gl_panel_7.createSequentialGroup()
									.addGroup(gl_panel_7.createParallelGroup(Alignment.TRAILING)
										.addGroup(gl_panel_7.createSequentialGroup()
											.addComponent(lblNewLabel_1, GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
											.addPreferredGap(ComponentPlacement.RELATED))
										.addGroup(gl_panel_7.createSequentialGroup()
											.addComponent(lblUv, GroupLayout.DEFAULT_SIZE, 82, Short.MAX_VALUE)
											.addPreferredGap(ComponentPlacement.UNRELATED))
										.addGroup(gl_panel_7.createSequentialGroup()
											.addComponent(lblEmws, GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
											.addGap(18)))
									.addGap(12)
									.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
										.addComponent(lbl_energieBereich, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
										.addComponent(lbl_leistungBereich, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
										.addComponent(lbl_Mittelwert_u, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)))
								.addGroup(gl_panel_7.createSequentialGroup()
									.addComponent(lblImas, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(lbl_ladung, GroupLayout.DEFAULT_SIZE, 112, Short.MAX_VALUE))
								.addGroup(gl_panel_7.createSequentialGroup()
									.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
										.addComponent(lblT)
										.addComponent(lbl_t0ms, GroupLayout.PREFERRED_SIZE, 117, GroupLayout.PREFERRED_SIZE)
										.addComponent(lblDtmmssmilmic))
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
										.addComponent(lbl_dt, GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
										.addComponent(lbl_t0, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
										.addComponent(lbl_t1, GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE))))
							.addContainerGap())
				);
				gl_panel_7.setVerticalGroup(
					gl_panel_7.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_7.createSequentialGroup()
							.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel_7.createSequentialGroup()
									.addComponent(lbl_max_i)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lbl_Mittelwert_i))
								.addGroup(gl_panel_7.createSequentialGroup()
									.addComponent(lbl_maxI)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lbl_uI)))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_7.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel_7.createSequentialGroup()
									.addComponent(lbl_Mittelwert_u)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lbl_leistungBereich, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lbl_energieBereich, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_panel_7.createSequentialGroup()
									.addComponent(lblUv)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblNewLabel_1, GroupLayout.PREFERRED_SIZE, 14, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(lblEmws)))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblImas)
								.addComponent(lbl_ladung))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_t0ms)
								.addComponent(lbl_t0))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblT)
								.addComponent(lbl_t1))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel_7.createParallelGroup(Alignment.BASELINE)
								.addComponent(lblDtmmssmilmic)
								.addComponent(lbl_dt))
							.addContainerGap(61, Short.MAX_VALUE))
				);
				
				panel_7.setLayout(gl_panel_7);
		
		chk_fixBounds = new JCheckBox("benutze feste Grenzen:");
		
		lbl_minBoundI = new JLabel("Imin:");
		lbl_minBoundI.setEnabled(false);
		
		txt_minBoundI = new JTextField();
		txt_minBoundI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmd_updateBounds.doClick();
			}
		});
		txt_minBoundI.setEnabled(false);
		txt_minBoundI.setColumns(10);
		
		lbl_minBoundIUnit = new JLabel("mA");
		lbl_minBoundIUnit.setEnabled(false);
		
		lbl_maxBoundI = new JLabel("Imax:");
		lbl_maxBoundI.setEnabled(false);
		
		txt_maxBoundI = new JTextField();
		txt_maxBoundI.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cmd_updateBounds.doClick();
			}
		});
		txt_maxBoundI.setEnabled(false);
		txt_maxBoundI.setColumns(10);
		
		lbl_maxBoundIUnit = new JLabel("mA");
		lbl_maxBoundIUnit.setEnabled(false);
		
		cmd_updateBounds = new JButton("aktuallisieren");
		cmd_updateBounds.setEnabled(false);
		
		GroupLayout gl_p_messung = new GroupLayout(p_messung);
		gl_p_messung.setHorizontalGroup(
			gl_p_messung.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_p_messung.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_p_messung.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_p_messung.createSequentialGroup()
							.addComponent(chk_fixBounds, GroupLayout.PREFERRED_SIZE, 203, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lbl_minBoundI)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txt_minBoundI, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lbl_minBoundIUnit, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lbl_maxBoundI, GroupLayout.PREFERRED_SIZE, 41, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txt_maxBoundI, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lbl_maxBoundIUnit, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
							.addGap(42)
							.addComponent(lbl_minBoundU)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txt_minBoundU, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lbl_minBoundUUnit)
							.addGap(18)
							.addComponent(lbl_maxBoundU)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(txt_maxBoundU, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(lbl_maxBoundUUnit)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(cmd_updateBounds))
						.addGroup(gl_p_messung.createSequentialGroup()
							.addGroup(gl_p_messung.createParallelGroup(Alignment.TRAILING)
								.addComponent(panel_6, GroupLayout.PREFERRED_SIZE, 261, GroupLayout.PREFERRED_SIZE)
								.addComponent(panel_7, GroupLayout.PREFERRED_SIZE, 262, GroupLayout.PREFERRED_SIZE)
								.addGroup(gl_p_messung.createParallelGroup(Alignment.LEADING)
									.addComponent(panel_4, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(panel_8, GroupLayout.PREFERRED_SIZE, 261, GroupLayout.PREFERRED_SIZE)))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(p_graphik, GroupLayout.DEFAULT_SIZE, 871, Short.MAX_VALUE)))
					.addContainerGap())
		);
		gl_p_messung.setVerticalGroup(
			gl_p_messung.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_p_messung.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_p_messung.createParallelGroup(Alignment.LEADING)
						.addComponent(p_graphik, 0, 0, Short.MAX_VALUE)
						.addGroup(gl_p_messung.createSequentialGroup()
							.addComponent(panel_8, GroupLayout.PREFERRED_SIZE, 87, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(panel_4, GroupLayout.PREFERRED_SIZE, 127, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(panel_6, GroupLayout.PREFERRED_SIZE, 158, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(panel_7, GroupLayout.PREFERRED_SIZE, 228, GroupLayout.PREFERRED_SIZE)))
					.addGap(7)
					.addGroup(gl_p_messung.createParallelGroup(Alignment.BASELINE)
						.addComponent(chk_fixBounds)
						.addComponent(lbl_minBoundI)
						.addComponent(txt_minBoundI, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lbl_minBoundIUnit)
						.addComponent(lbl_maxBoundI)
						.addComponent(lbl_minBoundU)
						.addComponent(txt_minBoundU, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(txt_maxBoundI, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lbl_maxBoundIUnit)
						.addComponent(lbl_minBoundUUnit)
						.addComponent(lbl_maxBoundU)
						.addComponent(txt_maxBoundU, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lbl_maxBoundUUnit)
						.addComponent(cmd_updateBounds))
					.addContainerGap())
		);
		lbl_minBoundU.setEnabled(false);
		lbl_minBoundUUnit.setEnabled(false);
		lbl_maxBoundU.setEnabled(false);
		lbl_maxBoundUUnit.setEnabled(false);
		
		lbl_zoom = new JLabel("Zoom:");
		
		chk_showMiniWindow = new JCheckBox("zeige Minifenster");
		chk_showMiniWindow.setSelected(true);
		
		chk_fixYAxis = new JCheckBox("fix Y-Achse");
		chk_fixYAxis.setSelected(true);
		
		cmd_resetZoom = new JButton("Zoom zurücksetzen");
				
		GroupLayout gl_p_graphik = new GroupLayout(p_graphik);
		gl_p_graphik.setHorizontalGroup(
			gl_p_graphik.createParallelGroup(Alignment.TRAILING)
				.addGroup(Alignment.LEADING, gl_p_graphik.createSequentialGroup()
					.addContainerGap()
					.addComponent(lbl_zoom, GroupLayout.PREFERRED_SIZE, 99, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(cmd_resetZoom)
					.addPreferredGap(ComponentPlacement.RELATED, 419, Short.MAX_VALUE)
					.addComponent(chk_showMiniWindow)
					.addGap(18)
					.addComponent(chk_fixYAxis, GroupLayout.PREFERRED_SIZE, 109, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
				.addComponent(p_chart, GroupLayout.DEFAULT_SIZE, 867, Short.MAX_VALUE)
		);
		gl_p_graphik.setVerticalGroup(
			gl_p_graphik.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_p_graphik.createSequentialGroup()
					.addComponent(p_chart, GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_p_graphik.createParallelGroup(Alignment.BASELINE)
						.addComponent(lbl_zoom)
						.addComponent(chk_fixYAxis)
						.addComponent(chk_showMiniWindow)
						.addComponent(cmd_resetZoom))
					.addContainerGap())
		);
		
		p_graphik.setLayout(gl_p_graphik);
		p_messung.setLayout(gl_p_messung);

		GroupLayout groupLayout = new GroupLayout(frmLeistungsmessung.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(tp_main, GroupLayout.DEFAULT_SIZE, 1160, Short.MAX_VALUE)
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(tp_main, GroupLayout.DEFAULT_SIZE, 722, Short.MAX_VALUE)
					.addContainerGap())
		);
		
		tp_main.addTab("Statistik", null, p_statistik, null);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "Laufzeitabsch\u00E4tzung", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GroupLayout gl_p_statistik = new GroupLayout(p_statistik);
		gl_p_statistik.setHorizontalGroup(
			gl_p_statistik.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_p_statistik.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_p_statistik.createParallelGroup(Alignment.LEADING)
						.addComponent(panel_1, GroupLayout.DEFAULT_SIZE, 1139, Short.MAX_VALUE)
						.addGroup(gl_p_statistik.createSequentialGroup()
							.addComponent(p_stat_all, GroupLayout.PREFERRED_SIZE, 343, GroupLayout.PREFERRED_SIZE)
							.addGap(18)
							.addComponent(panel, GroupLayout.PREFERRED_SIZE, 278, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		gl_p_statistik.setVerticalGroup(
			gl_p_statistik.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_p_statistik.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_p_statistik.createParallelGroup(Alignment.LEADING)
						.addComponent(p_stat_all, GroupLayout.PREFERRED_SIZE, 287, GroupLayout.PREFERRED_SIZE)
						.addComponent(panel, GroupLayout.PREFERRED_SIZE, 287, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(panel_1, GroupLayout.DEFAULT_SIZE, 383, Short.MAX_VALUE)
					.addContainerGap())
		);
		
		cmb_akku = new JComboBox<String>();
		cmb_akku.setBounds(135, 27, 105, 20);
		panel.add(cmb_akku);
		
		JLabel lblAkku = new JLabel("Akku:");
		lblAkku.setBounds(10, 30, 46, 14);
		panel.add(lblAkku);
		p_stat_all.setLayout(null);
		p_stat_all.add(label_1);
		p_stat_all.add(label_2);
		p_stat_all.add(lbl_stat_imean);
		p_stat_all.add(lbl_stat_imax);
		label_5.setBounds(10, 180, 177, 14);
		p_stat_all.add(label_5);
		p_stat_all.add(label_6);
		p_stat_all.add(label_7);
		p_stat_all.add(label_8);
		p_stat_all.add(lbl_stat_q);
		p_stat_all.add(lbl_stat_pmean);
		p_stat_all.add(lbl_stat_e);
		p_stat_all.add(lbl_stat_umean);
		lblUmaxma.setHorizontalAlignment(SwingConstants.LEFT);
		lblUmaxma.setBounds(10, 155, 177, 14);
		
		p_stat_all.add(lblUmaxma);
		lbl_stat_umax.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_umax.setBounds(197, 155, 123, 14);
		
		p_stat_all.add(lbl_stat_umax);
		lblMesszeit_1.setHorizontalAlignment(SwingConstants.LEFT);
		lblMesszeit_1.setBounds(10, 33, 177, 14);
		
		p_stat_all.add(lblMesszeit_1);
		lblSamplingrate.setHorizontalAlignment(SwingConstants.LEFT);
		lblSamplingrate.setBounds(10, 83, 177, 14);
		
		p_stat_all.add(lblSamplingrate);
		lbl_stat_anzData.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_anzData.setBounds(197, 58, 123, 14);
		
		p_stat_all.add(lbl_stat_anzData);
		lbl_stat_measureTime.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_measureTime.setBounds(197, 33, 123, 14);
		
		p_stat_all.add(lbl_stat_measureTime);
		lbl_stat_samplingRate.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_stat_samplingRate.setBounds(197, 83, 123, 14);
		
		p_stat_all.add(lbl_stat_samplingRate);
		lblAnzahlMesswerte.setHorizontalAlignment(SwingConstants.LEFT);
		lblAnzahlMesswerte.setBounds(10, 58, 177, 14);
		
		p_stat_all.add(lblAnzahlMesswerte);
		p_statistik.setLayout(gl_p_statistik);
		frmLeistungsmessung.getContentPane().setLayout(groupLayout);
		frmLeistungsmessung.setExtendedState(Frame.MAXIMIZED_BOTH);

	}

	/**
	 * setzt alle Komponenten-Listener der GUI
	 */
	private void setComponentListeners(){
		frmLeistungsmessung.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent arg0) {
				Property.saveProperties();
			}
			@Override
			public void windowClosing(WindowEvent arg0) {
				//speichere Konfiguration
				Property.setProperties(Property.AUTOSAVE,p_config.chk_autosave.isSelected());
				Property.setProperties(Property.COM_PORT,p_config.txt_port.getText());
				Property.setProperties(Property.COM_BAUTRATE,p_config.txt_baudRate.getText());
				Property.setProperties(Property.COM_BAUTRATE_FAST,p_config.txt_fUSART_baud.getText());
				Property.setProperties(Property.COM_PORT_FAST,p_config.txt_fUSART_COM.getText());
				Property.setProperties(Property.COM_USE_FT232H_ONLY,ProgStates.useFT232HOnly);
				
				Property.setProperties(Property.CONFIG_MEASURE_TIMEUNIT,String.valueOf(p_config.cmb_timeBase.getSelectedIndex()));
				Property.setProperties(Property.CONFIG_MEASURE_INTERVAL,p_config.txt_measureInterval.getText());

				Property.setProperties(Property.CONFIG_MEASURE_FIXTIMEENABLE,true);
				Property.setProperties(Property.CONFIG_MEASURE_FIXTIMEUNIT,String.valueOf(p_config.cmb_MessdauerUnit.getSelectedIndex()));			
				Property.setProperties(Property.CONFIG_MEASURE_FIXTIME,p_config.txt_Messdauer.getText());
				
				Property.setProperties(Property.CONFIG_MEASURE_PERIODIC_ENABLE, ProgStates.IS_PERIODIC_MEASURE);
				Property.setProperties(Property.CONFIG_MEASURE_USEAD7765, ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD);
				Property.setProperties(Property.CONFIG_MEASURE_USEAD7765EVAL, ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL);
				Property.setProperties(Property.CONFIG_MEASURE_USEHARTING, ProgStates.ADC_MODE == ProgStates.ADC_HARTING);
				
				Property.setProperties(Property.CONFIG_MEASURE_PERIODIC_TIME, p_config.txt_periodicVal.getText());
				Property.setProperties(Property.CONFIG_MEASURE_PERIODIC_UNIT, String.valueOf(p_config.cmb_periodicUnit.getSelectedIndex()));
				Property.setProperties(Property.CONFIG_MEASURE_SELECTED_AD7765, (String) p_config.cmb_ad7765Prop.getSelectedItem());
				Property.saveProperties();
				
				_main.cal.disconnect(-1);
				_main.com.disconnect();
				Communication.disconnectFTDI();				
			}
			
			public void windowIconified(WindowEvent e){
		        if (myMenue.chk_liveupdate.isSelected()){
		        	ProgStates.LIVE_UPDATE = false;
		        	ProgStates.WINDOW_IS_MINIMIZED = true;
		        	System.out.println("Live update abgeschaltet");
		        }
		    }
			
			public void windowDeiconified(WindowEvent e){
		        if (myMenue.chk_liveupdate.isSelected()){
		        	ProgStates.LIVE_UPDATE = true;
		        	ProgStates.WINDOW_IS_MINIMIZED = false;
		        	System.out.println("Live update aktiviert");
		        }				
			}		
		});
		chk_fixBounds.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD) return;
				ProgStates.USE_FIX_BOUNDS = chk_fixBounds.isSelected();
				lbl_maxBoundI.setEnabled(ProgStates.USE_FIX_BOUNDS);
				lbl_maxBoundIUnit.setEnabled(ProgStates.USE_FIX_BOUNDS);
				txt_maxBoundI.setEnabled(ProgStates.USE_FIX_BOUNDS);
				lbl_minBoundI.setEnabled(ProgStates.USE_FIX_BOUNDS);
				lbl_minBoundIUnit.setEnabled(ProgStates.USE_FIX_BOUNDS);
				txt_minBoundI.setEnabled(ProgStates.USE_FIX_BOUNDS);
				lbl_maxBoundU.setEnabled(ProgStates.USE_FIX_BOUNDS);
				lbl_maxBoundUUnit.setEnabled(ProgStates.USE_FIX_BOUNDS);
				txt_maxBoundU.setEnabled(ProgStates.USE_FIX_BOUNDS);
				lbl_minBoundU.setEnabled(ProgStates.USE_FIX_BOUNDS);
				lbl_minBoundUUnit.setEnabled(ProgStates.USE_FIX_BOUNDS);
				txt_minBoundU.setEnabled(ProgStates.USE_FIX_BOUNDS);
				cmd_updateBounds.setEnabled(ProgStates.USE_FIX_BOUNDS);
				if (ProgStates.USE_FIX_BOUNDS){
					if (txt_maxBoundI.getText().isEmpty()) txt_maxBoundI.setText("2500");
					if (txt_minBoundI.getText().isEmpty()) txt_minBoundI.setText("0");
					if (txt_maxBoundU.getText().isEmpty()) txt_maxBoundU.setText("10000");
					if (txt_minBoundU.getText().isEmpty()) txt_minBoundU.setText("0");
				}
				chk_fixYAxis.setSelected(!ProgStates.USE_FIX_BOUNDS);
			}
		});
		cmd_updateBounds.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ProgStates.USE_FIX_BOUNDS = chk_fixBounds.isSelected();
				try{
					Power.MAXIMUM_I = Range.convertFromTo(Range.MILLIAMPERE, Range.AMPERE, Float.parseFloat(txt_maxBoundI.getText()));
					Power.MINIMUM_I = Range.convertFromTo(Range.MILLIAMPERE, Range.AMPERE, Float.parseFloat(txt_minBoundI.getText()));
					Power.MAXIMUM_U = Range.convertFromTo(Range.MILLIAMPERE, Range.AMPERE, Float.parseFloat(txt_maxBoundU.getText()));
					Power.MINIMUM_U = Range.convertFromTo(Range.MILLIAMPERE, Range.AMPERE, Float.parseFloat(txt_minBoundU.getText()));
					setYAxisFix(false);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							p_chart.doRepaint(true);
						}
					});
				}catch (Exception e){}
			}
		});
		chk_fixYAxis.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				setYAxisFix(chk_fixYAxis.isSelected());
			}
		});
		
		cmb_akku.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				String name = (String) e.getItem();
				setAccu(name);
			}
		});
		
		chk_showMiniWindow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ProgStates.drawMiniWindow = chk_showMiniWindow.isSelected();
			}
		});
		
		cmd_resetZoom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				p_chart.resetZoom();
			}
		});

	}
	
	private void setAccu(final String name){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				for (Accu _acc : Power.ACCUS){
					if (_acc.NAME.equalsIgnoreCase(name)){
						lbl_accu_capacity.setText(String.valueOf(_acc.CAPACITY));
						lbl_accu_charge_iNom.setText(String.valueOf(_acc.I_CHARGE_NOM));
						lbl_accu_charge_imax.setText(String.valueOf(_acc.I_CHARGE_MAX));
						lbl_accu_charge_U.setText(String.valueOf(_acc.U_CHARGE));
						lbl_accu_discharge_iMax.setText(String.valueOf(_acc.I_DISCHARGE_MAX));
						lbl_accu_discharge_iNom.setText(String.valueOf(_acc.I_CHARGE_NOM));
						lbl_accu_Umax.setText(String.valueOf(_acc.U_MAX));
						lbl_accu_Umin.setText(String.valueOf(_acc.U_MIN));
						lbl_accu_Unom.setText(String.valueOf(_acc.U_NOM));
						break;
					}
				}
			}
		});
	}
	
	public void setYAxisFix(final boolean fix){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				chk_fixYAxis.setSelected(fix);
			}
		});
		ProgStates.fixYAxis = fix;
		if (ProgStates.fixYAxis){
			for (int i=0;i<3;i++){
				p_chart.setFixesMinMax(i,_main.data.getSelectedMin(i, true),_main.data.getSelectedMax(i, true));
			}
		}

		p_chart.doRepaint(true);
	}
	
	public void setZoom(final float zoom){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				lbl_zoom.setText("Zoom: " + String.format("%.2f", zoom) + " %");
			}
		});
	}
	
	public void clearLables(){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				lbl_anzMesswerte.setText("0");
				lbl_anzStates.setText("0");
				lbl_messzeit.setText("0");
				lbl_energieBereich.setText("0");
				lbl_energie_ges.setText("0");
				lbl_leistung.setText("0");
				lbl_leistungBereich.setText("0");
				lbl_leistung_ges.setText("0");
				lbl_max_ges.setText("0");
				lbl_max_i.setText("0");
				lbl_MittelwertI_ges.setText("0");
				lbl_Mittelwert_i.setText("0");
				lbl_pos.setText("0");
				lbl_spannung.setText("0");
				lbl_strom.setText("0");
				lbl_index.setText("0");
				lbl_Mittelwert_u.setText("0");
				lbl_MittelwertU_ges.setText("0");
				lbl_dt.setText("0");
				lbl_t0.setText("0");
				lbl_t1.setText("0");
				lbl_ladung.setText("0");
				lbl_ladung_ges.setText("0");
			}
		});
	}
    private final JLabel lbl_leistung 				= new JLabel("0");
    private final JLabel lblStromRaw 				= new JLabel("Index:");
    private final JLabel lbl_index 					= new JLabel("0");
    private final JLabel lbl_anzMesswerte 			= new JLabel("0");
    private final JLabel lbl_anzStates 				= new JLabel("0");
    private final JLabel lbl_messzeit 				= new JLabel("0");
    private final JPanel p_graphik 					= new JPanel();
    private final JLabel lbl_pos 					= new JLabel("0");
    private final JLabel lbl_strom 					= new JLabel("0");
    private final JLabel lbl_MittelwertI_ges 		= new JLabel("0");
    private final JLabel lblMax 					= new JLabel("Imax [mA]:");
    private final JLabel lbl_max_ges 				= new JLabel("0");
    private final JLabel lbl_max_i 					= new JLabel("0");
    private final JLabel lbl_Mittelwert_i 			= new JLabel("0");
    private final JPanel panel_6 					= new JPanel();
    private final JPanel panel_7 					= new JPanel();
    private final JPanel panel_8 					= new JPanel();
    private final JLabel lblSpannung 				= new JLabel("U(t) [V]:");
    private final JLabel lbl_spannung 				= new JLabel("0");
    private final JLabel lblNewLabel 				= new JLabel("Pμ [mW]:");
    private final JLabel lbl_leistung_ges 			= new JLabel("0");
    private final JLabel lblNewLabel_1 				= new JLabel("Pμ [mW]:");
    private final JLabel lbl_leistungBereich 		= new JLabel("0");
    private final JLabel lbl_energie_ges 			= new JLabel("0");
    private final JLabel lblE 						= new JLabel("E [mWs]:");
    private final JLabel lbl_energieBereich 		= new JLabel("0");
    private final JLabel lblEmws 					= new JLabel("E [mWs]:");
    private final JLabel lblMittelwert 				= new JLabel("Iμ [mA]:");
    private final JLabel lbl_maxI 					= new JLabel("Imax [mA]:");
	private final JPanel p_messung 					= new JPanel();
	private final JPanel panel_4 					= new JPanel();	
	private final JLabel lblPosition 				= new JLabel("t [s]:");	
	private final JLabel lblStrom 					= new JLabel("I(t) [mA]:");
	private final JLabel lblLeistung 				= new JLabel("P(t) [mW]:");
	private final JLabel lbl_uI 					= new JLabel("Iμ [mA]:");
	private final JLabel lblAnzahlDerMesswerte 		= new JLabel("Messwerte:");		
	private final JLabel lblAnzahlDerState 			= new JLabel("state changes:");	
	private final JLabel lblMesszeit 				= new JLabel("Messzeit:");
	private JLabel lbl_MittelwertU_ges;
	private JLabel lbl_Mittelwert_u;
	private final JLabel lblImas 					= new JLabel("Q [mAs]:");
	private final JLabel lbl_ladung 				= new JLabel("0");
	private final JLabel label 						= new JLabel("Q [mAs]:");
	private final JLabel lbl_ladung_ges 			= new JLabel("0");
	private final JLabel lbl_t0ms 					= new JLabel("T0 [mm:ss:mil:mic]:");
	private final JLabel lbl_t0 					= new JLabel("0");
	private final JLabel lblT 						= new JLabel("T1 [mm:ss:mil:mic]:");
	private final JLabel lbl_t1 					= new JLabel("0");
	private JLabel lbl_dt;
	private final JLabel lbl_minBoundU = new JLabel("Umin:");
	private final JTextField txt_minBoundU = new JTextField();
	private final JLabel lbl_minBoundUUnit = new JLabel("mV");
	private final JLabel lbl_maxBoundU = new JLabel("Umax:");
	private final JTextField txt_maxBoundU = new JTextField();
	private final JLabel lbl_maxBoundUUnit = new JLabel("mV");
	private JCheckBox chk_fixBounds;
	private JLabel lbl_minBoundI;
	private JTextField txt_minBoundI;
	private JLabel lbl_minBoundIUnit;
	private JLabel lbl_maxBoundI;
	private JTextField txt_maxBoundI;
	private JLabel lbl_maxBoundIUnit;
	private JButton cmd_updateBounds;
	public JCheckBox chk_fixYAxis;
	private final JPanel p_statistik = new JPanel();
	private final JPanel p_stat_all = new JPanel();
	private final JLabel label_1 = new JLabel("Imax [mA]:");
	private final JLabel label_2 = new JLabel("Iμ [mA]:");
	private final JLabel lbl_stat_imean = new JLabel("0");
	private final JLabel lbl_stat_imax = new JLabel("0");
	private final JLabel label_5 = new JLabel("Uμ [V]:");
	private final JLabel label_6 = new JLabel("Pμ [mW]:");
	private final JLabel label_7 = new JLabel("E [mWs]:");
	private final JLabel label_8 = new JLabel("Q [mAs]:");
	private final JLabel lbl_stat_q = new JLabel("0");
	private final JLabel lbl_stat_pmean = new JLabel("0");
	private final JLabel lbl_stat_e = new JLabel("0");
	private final JLabel lbl_stat_umean = new JLabel("0");
	private final JLabel lblUmaxma = new JLabel("Umax [mA]:");
	private final JLabel lbl_stat_umax = new JLabel("0");
	private final JLabel lblMesszeit_1 = new JLabel("Messzeit [s]:");
	private final JLabel lblSamplingrate = new JLabel("Samplingrate [Hz]:");
	private final JLabel lbl_stat_anzData = new JLabel("0");
	private final JLabel lbl_stat_measureTime = new JLabel("0");
	private final JLabel lbl_stat_samplingRate = new JLabel("0");
	private final JLabel lblAnzahlMesswerte = new JLabel("Anzahl Messwerte:");
	private final JPanel panel = new JPanel();
	private final JLabel lblSpannungNominalv = new JLabel("Spannung nominal [V]:");
	private final JLabel lblKapazittmah = new JLabel("Kapazität [mAh]:");
	private final JLabel lbl_accu_capacity = new JLabel("0");
	private final JLabel lbl_accu_Unom = new JLabel("0");
	private final JLabel lblLadestromNormala = new JLabel("Ladestrom standart [mA]:");
	private final JLabel lblLadestromMaxa = new JLabel("Ladestrom max [mA]:");
	private final JLabel lblEntladestromStandarta = new JLabel("Entladestrom standart [mA]:");
	private final JLabel lblEntladestromMaxa = new JLabel("Entladestrom max [mA]:");
	private final JLabel lbl_accu_discharge_iMax = new JLabel("0");
	private final JLabel lbl_accu_charge_imax = new JLabel("0");
	private final JLabel lbl_accu_discharge_iNom = new JLabel("0");
	private final JLabel lbl_accu_charge_iNom = new JLabel("0");
	private final JLabel lblLadespannungv = new JLabel("Ladespannung [V]:");
	private final JLabel lbl_accu_charge_U = new JLabel("0");
	private final JLabel lblSpannungMaxv = new JLabel("Spannung max [V]:");
	private final JLabel lbl_accu_Umin = new JLabel("0");
	private final JLabel lbl_accu_Umax = new JLabel("0");
	private final JLabel lblSpannungMinv = new JLabel("Spannung min [V]:");
	private JComboBox<String> cmb_akku;
	private JCheckBox chk_showMiniWindow;
	private JLabel lbl_zoom;
	private JButton cmd_resetZoom;
}
