package frontend;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import measuring.DummyHW;
import measuring.MeasureType;
import measuring.Messdaten;
import program.Communication;
import program.Main;
import program.ProgStates;
import program.Property;
import tools.MyFile;
import tools.Power;
import tools.Range;


public class Config extends JPanel{

	private static final long serialVersionUID = 1L;
	private final Main _main;
	
	/**
	 * Kontruktor, benötigt Main Klasse für Zugriff auf GUI, Kommunikation und Messdaten
	 * @param main
	 */
	public Config(Main main){
		super();
		bg_hwselection.add(rb_hw_harting);
		rb_hw_harting.setEnabled(false);
		rb_hw_harting.setToolTipText("<HTML>\r\nMessung mit dem HARTING Current Messaufbau\r\n</HTML>");
		bg_hwselection.add(rb_hw_default);
		rb_hw_default.setEnabled(false);
		rb_hw_default.setToolTipText("<HTML>\r\nADC 0-1: Strom <br>\r\nADC 0-3: Spannung\r\n</HTML>");
		_main = main;
		initComponents();
		initListeners();
	}

	/**
	 * läd alle Propertyeinstellungen für die GUI
	 */
	public void initProperties(){
		SwingUtilities.invokeLater(new Runnable() {		
			@Override
			public void run() {				
				txt_measureInterval.setText(Property.getProperties(Property.CONFIG_MEASURE_INTERVAL));
				cmb_timeBase.setSelectedIndex(Property.getPropertiesInt(Property.CONFIG_MEASURE_TIMEUNIT));
				txt_Messdauer.setText(Property.getProperties(Property.CONFIG_MEASURE_FIXTIME));
				cmb_MessdauerUnit.setSelectedIndex(Property.getPropertiesInt(Property.CONFIG_MEASURE_FIXTIMEUNIT));
				txt_baudRate.setText(Property.getProperties(Property.COM_BAUTRATE));
				txt_port.setText(Property.getProperties(Property.COM_PORT));
				txt_fUSART_COM.setText(Property.getProperties(Property.COM_PORT_FAST));
				txt_fUSART_baud.setText(Property.getProperties(Property.COM_BAUTRATE_FAST));
				chk_useFT232HOnly.setSelected(Property.getPropertiesBoolean(Property.COM_USE_FT232H_ONLY));
				chk_autosave.setSelected(Property.getPropertiesBoolean(Property.AUTOSAVE));	
				
				if (Property.getPropertiesBoolean(Property.CONFIG_MEASURE_USEHARTING)) ProgStates.ADC_MODE = ProgStates.ADC_HARTING;
				else if (Property.getPropertiesBoolean(Property.CONFIG_MEASURE_USEAD7765EVAL)) ProgStates.ADC_MODE = ProgStates.ADC_AD7765_EVAL;
				else if (Property.getPropertiesBoolean(Property.CONFIG_MEASURE_USEAD7765)) ProgStates.ADC_MODE = ProgStates.ADC_AD7765_BOARD;
				else ProgStates.ADC_MODE = ProgStates.ADC_INTERNAL;
				
				rb_hw_default.setSelected(false);
				rb_hw_AD7765EVAL.setSelected(false);
				rb_hw_AD7765.setSelected(false);
				rb_hw_harting.setSelected(false);
				
				switch (ProgStates.ADC_MODE){
					case ProgStates.ADC_HARTING:
						rb_hw_harting.setSelected(true);
						break;
					case ProgStates.ADC_AD7765_EVAL:
						rb_hw_AD7765EVAL.setSelected(true);
						break;
					case ProgStates.ADC_AD7765_BOARD:
						rb_hw_AD7765.setSelected(true);
						break;
					default:
						rb_hw_default.setSelected(true);
				}
				
				lbl_savePath.setText(Property.getProperties(Property.SAVE_PATH_FULL_FOLDER) + "\\" + Property.getProperties(Property.SAVE_PREFIX) + Property.getProperties(Property.SAVE_TIMESTAMP_FORMAT) + ".rdf");
				lbl_savePathMean.setText(Property.getProperties(Property.SAVE_PATH_FULL_FOLDER) + "\\" + Property.getProperties(Property.SAVE_PREFIX) + Property.getProperties(Property.SAVE_TIMESTAMP_FORMAT) + ".csv");
				
				ProgStates.IS_PERIODIC_MEASURE = Property.getPropertiesBoolean(Property.CONFIG_MEASURE_PERIODIC_ENABLE);
				chk_periodicMeasure.setSelected(ProgStates.IS_PERIODIC_MEASURE);
				txt_periodicVal.setText(Property.getProperties(Property.CONFIG_MEASURE_PERIODIC_TIME));
				txt_periodicVal.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
				cmb_periodicUnit.setSelectedIndex(Property.getPropertiesInt(Property.CONFIG_MEASURE_PERIODIC_UNIT));
				cmb_periodicUnit.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
				lbl_savePathMean.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
				lblSpeicherortMittelwerte.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
				
				if (ProgStates.DEBUG) rb_hw_dummy.setVisible(true);
				setLabelAndTxt();
				
				//add AD7765 Props
				String names[] = Property.getCalibDevices();
				for (String name : names){
					if (name.contains("PMPP")){
						cmb_ad7765Prop.addItem(name);
					}
				}	
				String device = Property.getProperties(Property.CONFIG_MEASURE_SELECTED_AD7765);
				cmb_ad7765Prop.setSelectedItem(device);
			}
		});
	}

	/**
	 * aktuallisiert das Textfeld für den ComPort
	 * @param port
	 */
	public void updateTxtCom(final String port) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				txt_port.setText(port);
			}			
		});	
	}

	/**
	 * Führt notwendige Methode vor dem Start aus
	 * @throws Exception
	 */
	public void prepareStart() throws Exception{
		if ((txt_Messdauer.getText().isEmpty() || txt_Messdauer.getText().equals("0")) && !chk_periodicMeasure.isSelected()){
			JOptionPane.showMessageDialog(this, "Bitte gebe die Messdauer an.", "Messdauer nicht gesetzt", JOptionPane.ERROR_MESSAGE);
		}
		if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("initialisiere Start");
			
		//Überprüfe, ob Preon verbunden ist
		if (!ProgStates.ISCONNECTED && !ProgStates.useFT232HOnly) throw new Exception("Nicht verbunden!");

		// Messdauer berechnen
		calcMeassureTime();
		if (Messdaten.MEASURETIME > 900000 && !chk_periodicMeasure.isSelected()) {
			throw new Exception("Maximale Messdauer Überschritten! Die maximale Messdauert für nicht periodische Messungen beträgt 15 Minuten");
		}

		if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("deaktiviere UI Felder");

		_main.gui.clearLables();
		ProgStates.SAVE_DATA = chk_autosave.isSelected();
		ProgStates.SAVE_PATH_DATA = lbl_savePath.getText();
		ProgStates.SAVE_PATH_MEAN = lbl_savePathMean.getText();

		switch (ProgStates.ADC_MODE){
			case ProgStates.ADC_INTERNAL:
				Power.setKalib("ADC_INTERNAL");
				if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("konfiguriere Preon für internen ADC");				
				_main.com.writeString(_main.com.EN_ADC_CH + "1");
				_main.com.writeString(_main.com.EN_ADC_CH + "3");		
				_main.com.writeString(_main.com.DIS_ADC_CH + "9");
				_main.data.addChannel((short) 1);
				_main.data.addChannel((short) 3);
				break;
			case ProgStates.ADC_HARTING:
				Power.setKalib("ADC_HARTING");
				if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("konfiguriere Preon für Harting Current");
				_main.com.writeString(_main.com.EN_ADC_CH + "9");
				_main.com.writeString(_main.com.DIS_ADC_CH + "1");
				_main.com.writeString(_main.com.EN_ADC_CH + "3");
				_main.data.addChannel((short) 9);
				_main.data.addChannel((short) 3);
				_main.com.setHartingMode(true);
				Messdaten.R_CURRENT = Range.getRange(1,MeasureType.CURRENT);		//	1mA/1mV
				Messdaten.R_VOLTAGE = Range.getRange(1,MeasureType.VOLTAGE);		//	1mA/1mV
				Thread.sleep(1000);
				break;
			case ProgStates.ADC_AD7765_EVAL:
				Power.setKalib("ADC_AD7765_EVAL");
				_main.com.connectToFastUSART();
				if (!ProgStates.ISCONNECTED_FAST)  throw new Exception("Datenleitung nicht verbunden!");
				Messdaten.R_CURRENT = Range.getRange(0,MeasureType.CURRENT);		//	Ampere
				Messdaten.R_VOLTAGE = Range.getRange(0,MeasureType.VOLTAGE);		//	Volt
				break;
			case ProgStates.ADC_AD7765_BOARD:
				String devName = (String) cmb_ad7765Prop.getSelectedItem();
				Power.setKalib(devName);
				if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("konfiguriere Preon für AD7765 " + devName);			
				setAD7765Configs(devName);
				if (!ProgStates.ISCONNECTED_FAST) Communication.connectFTDI((String) cmb_ad7765Prop.getSelectedItem());
				else Communication.flushFTDI();
				if (!ProgStates.ISCONNECTED_FAST)  throw new Exception("Datenleitung nicht verbunden!");
				Messdaten.R_CURRENT = Range.getRange(0,MeasureType.CURRENT);		//	Ampere
				Messdaten.R_VOLTAGE = Range.getRange(0,MeasureType.VOLTAGE);		//	Volt
				break;
			case ProgStates.ADC_DUMMY:
				Messdaten.R_CURRENT = Range.getRange(0,MeasureType.CURRENT);		//	Ampere
				Messdaten.R_VOLTAGE = Range.getRange(0,MeasureType.VOLTAGE);		//	Volt
				break;
			default:
				throw new Exception("Kein Messmodus gewählt, Bitte ADC Modus auswählen");				
		}

		//überprüfe ob Multimeter benutzt werden soll
		if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("initialisiere Datenstruktur");
		_main.data.clearAll();
		_main.data.init();
		if (Property.getPropertiesInt(Property.LOGLEVEL) > 2) System.out.println("initialisiere Graphik");
		_main.gui.p_chart.clearAll();
		_main.gui.p_chart.setLOCK(true);
		_main.gui.p_chart.setAnzGraphen(2, new String[] { "Strom", "Spannung" });
		_main.gui.p_chart.hideCheckboxes(true);
		_main.gui.setYAxisFix(false);

		if (ProgStates.ADC_MODE == ProgStates.ADC_HARTING || ProgStates.ADC_MODE == ProgStates.ADC_INTERNAL) {
			String t = String.valueOf(getMeasureInterval());
			_main.com.writeString(_main.com.TMR_INT + t);
			// setze oversampling auf 1
			_main.com.writeString(_main.com.SET_OVERSAMPLING + 1);

			Messdaten.TIMEUNIT = Range.getRange(cmb_timeBase.getSelectedIndex(), MeasureType.TIME);
			Messdaten.DT = Integer.parseInt(txt_measureInterval.getText());
		} else if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL || ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
			float mclk = Property.getPropertiesInt(Property.MESSUNG_AD7765_MCLK);
			Messdaten.TIMEUNIT = Range.MICROSECOND;
			float dt = Range.convertFromTo(Range.SECOND,Range.MICROSECOND,1 / 
						(mclk / (2 * Property.getPropertiesInt(Property.MESSUNG_AD7765_DEC_RATE) * Property.getPropertiesInt(Property.MESSUNG_AD7765_OSR))));
			if (Property.getPropertiesBoolean(Property.MESSUNG_OVERSAMPLING_ENABLE)) {
				dt /= Property.getPropertiesInt(Property.MESSUNG_OVERSAMPLING);
			}
			
			if (ProgStates.ADC_MODE == ProgStates.ADC_DUMMY) dt = 100000;
			Messdaten.DT = dt;

			if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD
					&& ProgStates.CALIB_RANGE > -1) {
				switch (ProgStates.CALIB_RANGE) {
				case 1:
					_main.com.writeByte(Communication.CALIB_RANGE_1, 0);
					break;
				case 2:
					_main.com.writeByte(Communication.CALIB_RANGE_2, 0);
					break;
				case 3:
					_main.com.writeByte(Communication.CALIB_RANGE_3, 0);
					break;
				}
			}
		}else if (ProgStates.ADC_MODE == ProgStates.ADC_DUMMY){
			Messdaten.TIMEUNIT = Range.MILLISECOND;
			Messdaten.DT = DummyHW.DELTA_T;
		}

		if (ProgStates.IS_PERIODIC_MEASURE) {
			calcVPP(); // Wenn periodische Messung gestartet ermittle Anzahl der
						// Messwerte pro Periode
			_main.data.setupPeriodicCalculation();
			if (ProgStates.holdDataInRAM) _main.data.setSize(Messdaten.VPP);
		} else {
			_main.data.setSize(getTotalVPM());
		}
		
	}
	
	private void calcMeassureTime(){
		long measureTime = Long.parseLong(txt_Messdauer.getText());
		switch (cmb_MessdauerUnit.getSelectedIndex()){
			case 3:
				measureTime *= 24;
			case 2:
				measureTime *= 60;
			case 1:
				measureTime *= 60;
			case 0:
				measureTime *= 1000;
		}
		
		Messdaten.MEASURETIME = measureTime;
		
		SwingUtilities.invokeLater(new Runnable() {				
			@Override
			public void run() {
				_main.gui.myMenue.pb_status.setVisible(true);
				_main.gui.myMenue.updateButton(3, true);
			}
		});
		
		//setzte die Zeitkorrektur
		Messdaten.TIMECORRECTION = getTimeCorrection();
	}
	
	/**
	 * gibt die Zeitkorrektur für Statechanges zurück. Daten werden über Property eingelesen
	 * @return
	 */
	private float getTimeCorrection(){
		int dt = (int) Range.convertFromTo(Messdaten.TIMEUNIT, Range.MICROSECOND, Messdaten.DT);
		switch (dt){
			case 100:
				return Property.getPropertiesFloat(Property.TIME_CORRECTION_100);
			case 200:
				return Property.getPropertiesFloat(Property.TIME_CORRECTION_200);
			case 250:
				return Property.getPropertiesFloat(Property.TIME_CORRECTION_250);
			case 500:
				return Property.getPropertiesFloat(Property.TIME_CORRECTION_500);
			case 1000:
				return Property.getPropertiesFloat(Property.TIME_CORRECTION_1000);
			default:
				return 1;
		}
	}
	
	/**
	 * initiallisiert die SwingKomponenten
	 */
	private void initComponents(){
		panel_2.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Messkonfiguration", TitledBorder.LEADING, TitledBorder.TOP, null, null));	
		panel_5.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Verbindung", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		
		GroupLayout gl_this = new GroupLayout(this);
		gl_this.setHorizontalGroup(
			gl_this.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_this.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_this.createParallelGroup(Alignment.TRAILING)
						.addComponent(panel_2, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, 2147483647)
						.addComponent(panel_5, Alignment.LEADING, GroupLayout.PREFERRED_SIZE, 0, 2147483647))
					.addContainerGap())
		);
		gl_this.setVerticalGroup(
			gl_this.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_this.createSequentialGroup()
					.addContainerGap()
					.addComponent(panel_5, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addGap(18)
					.addComponent(panel_2, GroupLayout.PREFERRED_SIZE, 477, GroupLayout.PREFERRED_SIZE)
					.addGap(23))
		);
				
		txt_port.setText("COM31");
		txt_port.setHorizontalAlignment(SwingConstants.TRAILING);
		txt_port.setColumns(10);
	
		txt_baudRate.setText("115200");
		txt_baudRate.setHorizontalAlignment(SwingConstants.TRAILING);
		txt_baudRate.setColumns(10);
		
		
		txt_fUSART_COM.setText("COM31");
		txt_fUSART_COM.setHorizontalAlignment(SwingConstants.TRAILING);
		txt_fUSART_COM.setColumns(10);
				
		txt_fUSART_baud.setText("115200");
		txt_fUSART_baud.setHorizontalAlignment(SwingConstants.TRAILING);
		txt_fUSART_baud.setColumns(10);
				
		GroupLayout gl_panel_5 = new GroupLayout(panel_5);
		gl_panel_5.setHorizontalGroup(
			gl_panel_5.createParallelGroup(Alignment.LEADING)
				.addGroup(Alignment.TRAILING, gl_panel_5.createSequentialGroup()
					.addContainerGap()
					.addComponent(lblPortPreon)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(txt_port, GroupLayout.PREFERRED_SIZE, 74, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(label_3)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(txt_baudRate, GroupLayout.PREFERRED_SIZE, 74, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED, 48, Short.MAX_VALUE)
					.addComponent(lbl_fastUSART, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lbl_fUSART_con, GroupLayout.PREFERRED_SIZE, 76, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(txt_fUSART_COM, GroupLayout.PREFERRED_SIZE, 74, GroupLayout.PREFERRED_SIZE)
					.addGap(18)
					.addComponent(lbl_fUSART_baud, GroupLayout.PREFERRED_SIZE, 72, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(txt_fUSART_baud, GroupLayout.PREFERRED_SIZE, 74, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		gl_panel_5.setVerticalGroup(
			gl_panel_5.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_panel_5.createSequentialGroup()
					.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
					.addGroup(gl_panel_5.createParallelGroup(Alignment.BASELINE)
						.addComponent(lbl_fastUSART)
						.addComponent(txt_fUSART_baud, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lbl_fUSART_baud)
						.addComponent(txt_fUSART_COM, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lbl_fUSART_con)
						.addComponent(label_3)
						.addComponent(txt_baudRate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(txt_port, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPortPreon))
					.addContainerGap())
		);
		panel_5.setLayout(gl_panel_5);
		
		panel_3.setBorder(new TitledBorder(new LineBorder(new Color(184, 207, 229)), "Messkonfiguration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Hardwareauswahl", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GroupLayout gl_panel_2 = new GroupLayout(panel_2);
		gl_panel_2.setHorizontalGroup(
			gl_panel_2.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_2.createSequentialGroup()
					.addContainerGap()
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 280, GroupLayout.PREFERRED_SIZE)
					.addGap(18)
					.addComponent(panel_3, GroupLayout.PREFERRED_SIZE, 486, Short.MAX_VALUE)
					.addGap(18))
		);
		gl_panel_2.setVerticalGroup(
			gl_panel_2.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_2.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel_2.createParallelGroup(Alignment.TRAILING, false)
						.addComponent(panel_3, Alignment.LEADING, 0, 0, Short.MAX_VALUE)
						.addComponent(panel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
					.addGap(186))
		);
		
		bg_hwselection.add(rb_hw_AD7765EVAL);
		rb_hw_AD7765EVAL.setEnabled(false);
		rb_hw_AD7765EVAL.setToolTipText("<HTML>\r\nStrommessung über das AD7765 Evaluationsboard\r\n</HTML>");
		
		rb_hw_AD7765 = new JRadioButton("AD7765 Board");
		bg_hwselection.add(rb_hw_AD7765);
		rb_hw_AD7765.setToolTipText("");
		
		rb_hw_dummy = new JRadioButton("dummy daten");
		rb_hw_dummy.setVisible(false);
		bg_hwselection.add(rb_hw_dummy);
		
		cmb_ad7765Prop = new JComboBox<String>();
		
		chk_useFT232HOnly = new JCheckBox("nur FT232H");
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(rb_hw_AD7765EVAL, GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
						.addComponent(rb_hw_harting, GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
						.addComponent(rb_hw_default, GroupLayout.DEFAULT_SIZE, 256, Short.MAX_VALUE)
						.addGroup(gl_panel.createSequentialGroup()
							.addComponent(rb_hw_AD7765, GroupLayout.PREFERRED_SIZE, 155, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addComponent(chk_useFT232HOnly, GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE))
						.addComponent(rb_hw_dummy)
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(21)
							.addComponent(cmb_ad7765Prop, GroupLayout.PREFERRED_SIZE, 148, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addComponent(rb_hw_default)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(rb_hw_harting)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(rb_hw_AD7765EVAL)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
						.addComponent(rb_hw_AD7765)
						.addComponent(chk_useFT232HOnly))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(cmb_ad7765Prop, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(rb_hw_dummy)
					.addContainerGap(74, Short.MAX_VALUE))
		);
		panel.setLayout(gl_panel);
		
		lbl_messint.setFont(new Font("Tahoma", Font.PLAIN, 14));
		
		cmb_timeBase.setModel(new DefaultComboBoxModel<String>(new String[] {"μs", "ms", "s"}));
		cmb_timeBase.setSelectedIndex(0);
		chk_autosave.setFont(new Font("Tahoma", Font.PLAIN, 14));
		
		chk_autosave.setSelected(true);
		
		txt_measureInterval.setText("500");
		txt_measureInterval.setColumns(10);
		
		chk_periodicMeasure = new JCheckBox("periodische Messung");
		chk_periodicMeasure.setFont(new Font("Tahoma", Font.PLAIN, 14));
		
		txt_periodicVal = new JTextField();
		txt_periodicVal.setText("1");
		txt_periodicVal.setEnabled(false);
		txt_periodicVal.setColumns(10);
		
		cmb_periodicUnit = new JComboBox<String>();
		cmb_periodicUnit.setModel(new DefaultComboBoxModel<String>(new String[] {"Sekunden", "Minuten"}));
		cmb_periodicUnit.setSelectedIndex(1);
		cmb_periodicUnit.setEnabled(false);
		
		JLabel lblPeriodendauer = new JLabel("Periodendauer:");
		
		JLabel lblMessdauer = new JLabel("Messdauer");
		lblMessdauer.setFont(new Font("Tahoma", Font.PLAIN, 14));
		txt_Messdauer.setColumns(10);
		cmb_MessdauerUnit.setModel(new DefaultComboBoxModel<String>(new String[] {"Sekunden", "Minuten", "Stunden", "Tage"}));
		cmb_MessdauerUnit.setSelectedIndex(0);
		lbl_savePath.setHorizontalAlignment(SwingConstants.LEFT);
				
		GroupLayout gl_panel_3 = new GroupLayout(panel_3);
		gl_panel_3.setHorizontalGroup(
			gl_panel_3.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_3.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel_3.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_panel_3.createSequentialGroup()
							.addGroup(gl_panel_3.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel_3.createParallelGroup(Alignment.LEADING)
									.addComponent(chk_periodicMeasure, GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
									.addComponent(chk_autosave, GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
									.addComponent(lblSpeicherortMittelwerte, GroupLayout.PREFERRED_SIZE, 154, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_panel_3.createSequentialGroup()
									.addComponent(lblSpeicherort, GroupLayout.PREFERRED_SIZE, 154, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)))
							.addGroup(gl_panel_3.createParallelGroup(Alignment.LEADING)
								.addGroup(Alignment.TRAILING, gl_panel_3.createSequentialGroup()
									.addPreferredGap(ComponentPlacement.RELATED, 135, Short.MAX_VALUE)
									.addComponent(cmd_changeSavePath))
								.addGroup(Alignment.TRAILING, gl_panel_3.createSequentialGroup()
									.addGap(25)
									.addComponent(lblPeriodendauer)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(txt_periodicVal, GroupLayout.PREFERRED_SIZE, 81, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.UNRELATED)
									.addComponent(cmb_periodicUnit, GroupLayout.PREFERRED_SIZE, 108, GroupLayout.PREFERRED_SIZE))
								.addGroup(Alignment.TRAILING, gl_panel_3.createSequentialGroup()
									.addGap(10)
									.addComponent(lbl_savePath, GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE))
								.addGroup(gl_panel_3.createSequentialGroup()
									.addGap(10)
									.addComponent(lbl_savePathMean, GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE))))
						.addGroup(gl_panel_3.createSequentialGroup()
							.addComponent(lbl_messint, GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
							.addGap(103)
							.addComponent(txt_measureInterval, GroupLayout.PREFERRED_SIZE, 81, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(cmb_timeBase, GroupLayout.PREFERRED_SIZE, 108, GroupLayout.PREFERRED_SIZE))
						.addGroup(gl_panel_3.createSequentialGroup()
							.addComponent(lblMessdauer)
							.addPreferredGap(ComponentPlacement.RELATED, 193, Short.MAX_VALUE)
							.addComponent(txt_Messdauer, GroupLayout.PREFERRED_SIZE, 81, GroupLayout.PREFERRED_SIZE)
							.addGap(10)
							.addComponent(cmb_MessdauerUnit, GroupLayout.PREFERRED_SIZE, 108, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		gl_panel_3.setVerticalGroup(
			gl_panel_3.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_3.createSequentialGroup()
					.addGap(22)
					.addGroup(gl_panel_3.createParallelGroup(Alignment.LEADING)
						.addComponent(lblMessdauer)
						.addComponent(txt_Messdauer, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(cmb_MessdauerUnit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_panel_3.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_3.createParallelGroup(Alignment.BASELINE)
							.addComponent(lbl_messint)
							.addComponent(txt_measureInterval, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
						.addComponent(cmb_timeBase, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_panel_3.createParallelGroup(Alignment.BASELINE)
						.addComponent(chk_periodicMeasure)
						.addComponent(txt_periodicVal, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(cmb_periodicUnit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblPeriodendauer))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel_3.createParallelGroup(Alignment.LEADING)
						.addComponent(lblSpeicherortMittelwerte)
						.addComponent(lbl_savePathMean, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(12)
					.addGroup(gl_panel_3.createParallelGroup(Alignment.BASELINE)
						.addComponent(chk_autosave)
						.addComponent(cmd_changeSavePath))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addGroup(gl_panel_3.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblSpeicherort)
						.addComponent(lbl_savePath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addGap(79))
		);
		lbl_savePathMean.setHorizontalAlignment(SwingConstants.LEFT);
		
		panel_3.setLayout(gl_panel_3);
		panel_2.setLayout(gl_panel_2);
		this.setLayout(gl_this);
		
	}
	
	/**
	 * Initiallisiert die Listener
	 */
	private void initListeners(){
		txt_port.addFocusListener(new FocusListener() {		
			@Override
			public void focusLost(FocusEvent arg0) {
				Property.setProperties(Property.COM_PORT, txt_port.getText());
			}		
			@Override
			public void focusGained(FocusEvent arg0) {}
		});
		txt_baudRate.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (!Character.isDigit(e.getKeyChar())){
					e.consume();
				}
				Property.setProperties(Property.COM_BAUTRATE,txt_baudRate.getText());
			}
		});
		txt_baudRate.addFocusListener(new FocusListener() {		
			@Override
			public void focusLost(FocusEvent arg0) {
				Property.setProperties(Property.COM_BAUTRATE,txt_baudRate.getText());
			}		
			@Override
			public void focusGained(FocusEvent arg0) {}
		});
		txt_fUSART_COM.addFocusListener(new FocusListener() {		
			@Override
			public void focusLost(FocusEvent arg0) {
				Property.setProperties(Property.COM_PORT_FAST, txt_fUSART_COM.getText());
			}		
			@Override
			public void focusGained(FocusEvent arg0) {}
		});
		txt_fUSART_baud.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (!Character.isDigit(e.getKeyChar())){
					e.consume();
				}
				Property.setProperties(Property.COM_BAUTRATE_FAST,txt_fUSART_baud.getText());
			}
		});
		txt_fUSART_baud.addFocusListener(new FocusListener() {		
			@Override
			public void focusLost(FocusEvent arg0) {
				Property.setProperties(Property.COM_BAUTRATE_FAST,txt_fUSART_baud.getText());
			}		
			@Override
			public void focusGained(FocusEvent arg0) {}
		});
		chk_autosave.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				ProgStates.SAVE_DATA = chk_autosave.isSelected();
				lbl_savePath.setEnabled(ProgStates.SAVE_DATA);
				lblSpeicherort.setEnabled(ProgStates.SAVE_DATA);
			}
		});

		txt_measureInterval.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				if (!Character.isDigit(arg0.getKeyChar())){
					arg0.consume();
				}			
			}
		});		
		txt_Messdauer.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (!Character.isDigit(e.getKeyChar())){
					e.consume();
				}			
			}
		});

		chk_periodicMeasure.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ProgStates.IS_PERIODIC_MEASURE = chk_periodicMeasure.isSelected();
				txt_periodicVal.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
				cmb_periodicUnit.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
				chk_autosave.setSelected(!ProgStates.IS_PERIODIC_MEASURE);
				lbl_savePathMean.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
				lblSpeicherortMittelwerte.setEnabled(ProgStates.IS_PERIODIC_MEASURE);
			}
		});
		
		rb_hw_default.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (rb_hw_default.isSelected()) ProgStates.ADC_MODE = ProgStates.ADC_INTERNAL;
				setLabelAndTxt();
			}
		});
		
		rb_hw_harting.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (rb_hw_harting.isSelected()) ProgStates.ADC_MODE = ProgStates.ADC_HARTING;
				setLabelAndTxt();
			}
		});

		rb_hw_AD7765EVAL.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (rb_hw_AD7765EVAL.isSelected()) ProgStates.ADC_MODE = ProgStates.ADC_AD7765_EVAL;
				setLabelAndTxt();
			}
		});
		
		rb_hw_AD7765.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (rb_hw_AD7765.isSelected()) ProgStates.ADC_MODE = ProgStates.ADC_AD7765_BOARD;
				setLabelAndTxt();
			}
		});
		
		rb_hw_dummy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (rb_hw_dummy.isSelected()) ProgStates.ADC_MODE = ProgStates.ADC_DUMMY;
			}
		});
		
		cmd_changeSavePath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int type;
				if (chk_periodicMeasure.isSelected()) type = tools.MyFile.CSV;
				else type = tools.MyFile.RDF;
				
				String path = "";
				while (path.isEmpty()){
					path = tools.MyFile.selectFile(false, type);
					if (path == null) return;
				}

				ProgStates.SAVE_PATH_DATA = path;
				ProgStates.SAVE_PATH_MEAN = path;
				if (path.endsWith(".csv")) ProgStates.SAVE_PATH_DATA = path.replace(".csv", ".rdf");
				if (path.endsWith(".rdf")) ProgStates.SAVE_PATH_MEAN = path.replace(".rdf", ".csv");
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						lbl_savePath.setText(ProgStates.SAVE_PATH_DATA);
						lbl_savePathMean.setText(ProgStates.SAVE_PATH_MEAN);
					}
				});
			}
		});
		cmb_ad7765Prop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = (String) cmb_ad7765Prop.getSelectedItem();
				ProgStates.AD7765_DEV_NAME = name;
				Property.setProperties(Property.COM_USE_FT232H_ONLY, Property.getCalibrationProperties(name).getProperty(Property.USE_FT232H));
				chk_useFT232HOnly.setSelected(Property.getPropertiesBoolean(Property.COM_USE_FT232H_ONLY));
			}
		});
		chk_useFT232HOnly.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setLabelAndTxt();
			}
		});
	}
	
	private void calcVPP(){
		try{
			int t0 = Integer.parseInt(txt_periodicVal.getText());
			int tPeriode = 0;
			if (cmb_periodicUnit.getSelectedIndex() == 0) tPeriode = t0;				//Sekunden
			else if (cmb_periodicUnit.getSelectedIndex() == 1) tPeriode = t0 * 60;		//Minuten
			
			if (tPeriode > 900){
				ProgStates.holdDataInRAM = false;
				_main.gui.myMenue.chk_liveupdate.setSelected(false);
			}else{
				ProgStates.holdDataInRAM = true;
			}
			
			int vPP = 0; 	//Values per periode
			float mi = 0;
			if (ProgStates.ADC_MODE == ProgStates.ADC_AD7765_EVAL || ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD){
				mi = Messdaten.DT;
			}else{
				mi = getMeasureInterval();
			}
			vPP = (int) (tPeriode * (1000000/mi));
			
			System.out.println("Periode in Sekunden: " + tPeriode);
			System.out.println("Messwerte pro Periode: " + vPP);
			
			Messdaten.VPP = vPP;
			Messdaten.VPPSeconds = tPeriode;
		}catch (Exception e){
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}		
	}
	
	/**
	 * gibt die absolute Anzahl der Messpunkte pro Messung zurück
	 * @return	Anzahl der Messpunkte
	 */
	private int getTotalVPM(){
		int vpm = 0;
		try{
			int f = Integer.parseInt(txt_Messdauer.getText());
			float mi;
			switch (cmb_MessdauerUnit.getSelectedIndex()){
				case 1:		//Minuten
					f *= 60;
					break;
				case 2:		//Stunden
					f *= 60 * 60;
					break;
				case 3:		//Tage
					f *= 60 * 60 * 24;
					break;
			}
			switch (ProgStates.ADC_MODE){
				case ProgStates.ADC_AD7765_EVAL:
				case ProgStates.ADC_AD7765_BOARD:
				case ProgStates.ADC_DUMMY:
					mi = Messdaten.DT;
					break;
				case ProgStates.ADC_HARTING:
				case ProgStates.ADC_INTERNAL:
					mi = getMeasureInterval();
					break;
				default:
					mi = 1;
			}
			vpm = (int) (f * (Messdaten.TIMEUNIT.factor / mi));
			
			if (Property.getPropertiesBoolean(Property.MESSUNG_OVERSAMPLING_ENABLE)){
				vpm /= Property.getPropertiesInt(Property.MESSUNG_OVERSAMPLING);
			}
		}catch (Exception e){}
		return vpm;
	}
	
	private int getMeasureInterval(){
		int val = 0;
		try{
			val = Integer.parseInt(txt_measureInterval.getText());
		}catch (Exception e){}
		switch (cmb_timeBase.getSelectedIndex()){
		case 0:		//uSekunden
			return val;
		case 1: 	//millisekunden
			return val * 1000;
		case 2: 	// Sekunden
			return val * 1000 * 1000;
		default:
			return 500;
		}
	}
	
	private void setLabelAndTxt(){
		boolean b = ProgStates.ADC_MODE > 1;
		cmb_timeBase.setEnabled(!b);
		lbl_messint.setEnabled(!b);
		txt_measureInterval.setEnabled(!b);
		
		//USART
		b = ProgStates.ADC_MODE != ProgStates.ADC_AD7765_BOARD && b;
		lbl_fastUSART.setVisible(b);
		lbl_fUSART_con.setVisible(b);
		lbl_fUSART_baud.setVisible(b);
		txt_fUSART_COM.setVisible(b);
		txt_fUSART_baud.setVisible(b);
		chk_useFT232HOnly.setEnabled(!b);
		
		if (chk_useFT232HOnly.isSelected()){
			b = chk_useFT232HOnly.isSelected();
			ProgStates.useFT232HOnly = b;
			txt_baudRate.setEnabled(!b);
			txt_port.setEnabled(!b);				
		}
	}
	
	public void prepareCalib(final int time, final String path, final boolean saveCalib){
		SwingUtilities.invokeLater(new Runnable() {		
			@Override
			public void run() {
				rb_hw_AD7765.doClick();
				txt_Messdauer.setText(String.valueOf(time));
				cmb_MessdauerUnit.setSelectedIndex(0);
				chk_autosave.setSelected(saveCalib);
				ProgStates.SAVE_DATA = saveCalib;
				chk_periodicMeasure.setSelected(false);
			}
		});
		if (saveCalib){
			ProgStates.SAVE_PATH_DATA = path;
		}
	}
	
	private void setAD7765Configs(String name){
		Properties props = Property.getCalibrationProperties(name);
		Property.setProperties(Property.MESSUNG_AD7765_MCLK, props.getProperty(Property.MCLK));
		Property.setProperties(Property.MESSUNG_AD7765_DEC_RATE, props.getProperty(Property.DEC_RATE));
		Property.setProperties(Property.MESSUNG_R_SHUNT, props.getProperty(Property.SHUNT));
	}
	
    private final JPanel panel_2 					= new JPanel();
    private final JPanel panel_5 					= new JPanel();
    private final JLabel label_3 					= new JLabel("Baud Rate:");
    private final JLabel lblPortPreon 					= new JLabel("Port Preon:");
	private final JPanel panel_3 					= new JPanel();
	private final JLabel lbl_messint 				= new JLabel("Messinterval:");
	public final JTextField txt_port 				= new JTextField();
    public final JTextField txt_baudRate 			= new JTextField();
    public final JCheckBox chk_autosave 			= new JCheckBox("Messwerte speichern");
	public final JComboBox<String> cmb_timeBase 	= new JComboBox<String>();
    public final JTextField txt_Messdauer		 	= new JTextField();
    public final JTextField txt_measureInterval  	= new JTextField();
	public final JComboBox<String> cmb_MessdauerUnit  = new JComboBox<String>();
	public JCheckBox chk_periodicMeasure;
	public JTextField txt_periodicVal;
	public JComboBox<String> cmb_periodicUnit;
	private final JRadioButton rb_hw_default 		= new JRadioButton("Preon32 ADC");
	private final JRadioButton rb_hw_harting 		= new JRadioButton("Harting Current");
	private final ButtonGroup bg_hwselection 		= new ButtonGroup();
	private final JRadioButton rb_hw_AD7765EVAL		= new JRadioButton("AD7765 EVAL Board");
	private final JButton cmd_changeSavePath		= new JButton("anderen Speicherort wählen");
	private final JTextField lbl_savePath				= new JTextField("");
	private final JLabel lblSpeicherort 			= new JLabel("Speicherort Messwerte:");
	public final JTextField txt_fUSART_COM			= new JTextField();
	public final JTextField txt_fUSART_baud		= new JTextField();
	private final JLabel lbl_fastUSART				= new JLabel("AD7765 Daten-USART:");
	private final JLabel lbl_fUSART_con 				= new JLabel("Verbindung:");;
	private final JLabel lbl_fUSART_baud				= new JLabel("Baud Rate:");
	private final JLabel lblSpeicherortMittelwerte = new JLabel("Speicherort Mittelwerte:");
	private final JTextField lbl_savePathMean = new JTextField("");
	private JRadioButton rb_hw_AD7765;
	private JRadioButton rb_hw_dummy;
	public JComboBox<String> cmb_ad7765Prop;
	private JCheckBox chk_useFT232HOnly;
}
