package measuring;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import program.Main;
import program.ProgStates;
import program.Property;
import tools.HM8112_3;
import tools.HM8143;
import tools.ManuSource;
import tools.MultiManu;
import tools.Multimeter;
import tools.MyFile;
import tools.PolynomialRegression;
import tools.Power;
import tools.Source;
import tools.Statistics;
import tools.UT804;
import frontend.Config;

/**
 * Klasse zur Halbautomatischen Kalibrierung des Messplatzes
 * TODO Kalibrierung für mehrere AD7765 Devices erweitern
 * @author Christoph
 * 
 */

public class Calibrate extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final ArrayList<CalibRun> runs = new ArrayList<Calibrate.CalibRun>();

	private Main _main;
	private Config conf;
	private String calibPath = "";
	private Source source;
	private Multimeter multimeter;

	private Thread t_source;
	private Thread t_mm;
	private Thread t_update;

	private double ref_temp_mean = 0;
	private boolean cancelCalib = false;
	private int range = -1;
	private boolean calibRuns = false;
	
	private double k0 = 0;
	private double k1 = 1;
	private double k2 = 0;
	private boolean isVisible = false;
	
	private boolean isTestRunning = false;
	private boolean useKalbibFileForTesting = false;
	
	public static int _15mA = 1;
	public static int _150mA = 2;
	public static int _2500mA = 3;
	public static int voltage = 0;


	private Runnable r_startMeasure = new Runnable() {
		@Override
		public void run() {
			try {
				if (chk_autoCalib.isSelected()) {
					performCalib();
					return;
				} else {
					performCalib();

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private Runnable r_update = new Runnable() {
		@Override
		public void run() {
			while (true) {
				if (!calibRuns && isVisible){
					if (isVisible()){
						cmd_save.setEnabled(!txt_k0.getText().isEmpty() && !txt_k1.getText().isEmpty() && !txt_k2.getText().isEmpty());
					}
					if (source != null) {
						if ((source.newValues() & 0x2) > 0)
							txt_hm8143_I.setText(String.format("%.3f", source.getI()));
						if ((source.newValues() & 0x1) > 0)
							txt_hm8143_U.setText(String.format("%.3f", source.getU()));
					}
					if (multimeter != null) {
						if ((multimeter.newValues() & 0x2) > 0)
							txt_mm_I.setText(String.format("%.8f", multimeter.getI()));
						if ((multimeter.newValues() & 0x1) > 0)
							txt_mm_U.setText(String.format("%.8f", multimeter.getU()));
					}
				}
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
				}
			}
		}
	};

	private ActionListener al_start = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					isTestRunning = false;
					setComponents();
				}
			});
			Thread t_start = new Thread(r_startMeasure);
			t_start.start();
		}
	};
	
	public Calibrate(Main main) {
		_main = main;
		conf = _main.gui.p_config;
		setTitle("Messplatzkalibrierung");
		setResizable(false);
		initComponents();
		setListener();
		chk_autoCalib.setSelected(true);
		performAutoCalibClick();
		source = new HM8143("");

		getContentPane().add(cmd_connectAll);
		
		pb_progress = new JProgressBar();
		pb_progress.setStringPainted(true);
		pb_progress.setBounds(10, 661, 686, 25);
		getContentPane().add(pb_progress);
		
		t_update = new Thread(r_update);
		t_update.start();

		if (calibPath.isEmpty())
			cmd_start.setEnabled(false);
		
		txt_hm8143_port.setText(Property.getProperties(Property.CALIB_PORT_HM8143));
		txt_MM_port.setText(Property.getProperties(Property.CALIB_PORT_HM8112));
		cmb_mm.setSelectedIndex(2);
		
		for (String s : Property.getCalibDevices()){
			if (s.contains("PMPP")) cmb_AD7765_DEV.addItem(s);
		}
	}

	private void setListener() {

		cmd_cancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disconnect(-1);
				setVisible(false);
			}
		});

		cmd_start.addActionListener(al_start);

		chk_autoCalib.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						performAutoCalibClick();
					}
				});
			}
		});

		cmd_hm8143_connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				_main.setCursor(Cursor.WAIT_CURSOR);
				if (source.isConnected()) {
					source.disconnect();
					cmd_hm8143_connect.setText("verbinden");
				} else {
					source.setPort(txt_hm8143_port.getText());
					if (source.connect()) {
						cmd_hm8143_connect.setText("trennen");
						t_source = new Thread(source);
						t_source.start();
					} else
						cmd_hm8143_connect.setText("verbinden");
				}
				_main.setCursor(Cursor.DEFAULT_CURSOR);
			}
		});

		cmd_setU.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						source.setU(txt_hm8143_U.getText());
					}
				});
			}
		});

		cmd_setI.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						try {
							source.setI(txt_hm8143_I.getText());
						} catch (NumberFormatException e) {

						}
					}
				});
			}
		});

		txt_hm8143_U.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				source.setU(txt_hm8143_U.getText());
			}
		});

		txt_hm8143_I.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				source.setI(txt_hm8143_I.getText());
			}
		});

		cmd_loadKalib.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						File f = new File("");
						range = -1;
						boolean cancel = false;
						while ((f == null || !f.exists()) && !cancel) {
							String p = tools.MyFile.selectFile(true, 3);
							if (p == null) {
								int res = JOptionPane.showConfirmDialog(null,
										"Keine Datei gewählt. Datei Auswählen?",
										"wähle Kalibrierungsdatei",
										JOptionPane.OK_CANCEL_OPTION,
										JOptionPane.ERROR_MESSAGE);
								cancel = (res == 2);
							}else{
								f = new File(p);
							}
						}
						calibPath = f.getAbsolutePath();
						txt_calib_file.setText(calibPath);
						cmd_start.setEnabled(!calibPath.isEmpty());
					}
				});
			}
		});

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				disconnect(-1);
			}
		});

		chk_activate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				source.setOut(chk_activate.isSelected());
			}
		});

		cmd_mm_connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				_main.setCursor(Cursor.WAIT_CURSOR);
				if (multimeter == null){
					if (cmb_mm.getSelectedItem().equals("UT804")){
						multimeter = new UT804("");
					}else if (cmb_mm.getSelectedItem().equals("HM8112-3")){
						multimeter = new HM8112_3("");
					}else if(cmb_mm.getSelectedItem().equals("Manuell")) {
						multimeter = new MultiManu("");
					}
				}
				
				if (multimeter.isConnected()) {
					multimeter.disconnect();
					cmd_mm_connect.setText("verbinden");
				} else {
					if (txt_MM_port.getText().isEmpty()) {
						JOptionPane.showMessageDialog(null,
								"Bitte ComPort eintragen",
								"ComPort nicht gesetzt",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					multimeter.setPort(txt_MM_port.getText());
					if (multimeter.connect()) {
						cmd_mm_connect.setText("trennen");
						t_mm = new Thread(multimeter);
						t_mm.start();
					} else
						cmd_mm_connect.setText("verbinden");
				}
				_main.setCursor(Cursor.DEFAULT_CURSOR);
			}
		});

		cmd_connectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (!source.isConnected())
							cmd_hm8143_connect.doClick();
						if (multimeter == null || !multimeter.isConnected())
							cmd_mm_connect.doClick();
					}
				});
			}
		});
		
		cmd_save.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						int res = JOptionPane.showConfirmDialog(null, "Sollen die Kalibrierungswerte wirklich überschrieben werden?", "Kalibrierung speichern", JOptionPane.YES_NO_OPTION);
						if (res == 0){
							int type = 0;
							String device = "";
							if (rb_harting.isSelected()){
								type = ProgStates.ADC_HARTING;
								device = "ADC_HARTING";
							}
							if (rb_preon32adc.isSelected()){
								type = ProgStates.ADC_INTERNAL;
								device = "ADC_INTERNAL";
							}
							if (rb_ad7765eval.isSelected()){
								type = ProgStates.ADC_AD7765_EVAL;
								device = "ADC_AD7765_EVAL";
							}
							if (rb_ad7765board.isSelected()){
								type = ProgStates.ADC_AD7765_BOARD;
								device = (String) cmb_AD7765_DEV.getSelectedItem();
							}
							
							
							saveKalib(type,Float.parseFloat(txt_k0.getText()),Float.parseFloat(txt_k1.getText()),Float.parseFloat(txt_k2.getText()),range, device);
							Property.saveCalibProperties();
							JOptionPane.showMessageDialog(null, "Kalibrierung gespeichert", "speichern", JOptionPane.INFORMATION_MESSAGE);
						}
					}
				});
			}
		});
		
		cmb_mm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				multimeter = null;
				if (cmb_mm.getSelectedItem().equals("UT804")) txt_MM_port.setText(Property.getProperties(Property.CALIB_PORT_UT804));
				else if (cmb_mm.getSelectedItem().equals("HM8112-3")) txt_MM_port.setText(Property.getProperties(Property.CALIB_PORT_HM8112));
			}
		});
		
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				Property.setProperties(Property.CALIB_PORT_HM8143, txt_hm8143_port.getText());
				if (cmb_mm.getSelectedItem().equals("UT804")) Property.setProperties(Property.CALIB_PORT_UT804, txt_MM_port.getText());
				else if (cmb_mm.getSelectedItem().equals("HM8112-3")) Property.setProperties(Property.CALIB_PORT_HM8112, txt_MM_port.getText());
				isVisible = false;
			}
			
			@Override
			public void windowOpened(WindowEvent arg0){
				isVisible = true;
			}
		});
		
		cmd_startTest.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {
				startTest();
			}
		});
		
		cmd_testCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				cancelCalib = true;
			}
		});
		
		cmd_saveCSV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txt_saveCSV.setText(MyFile.selectFile(false, MyFile.CSV));
			}
		});

		rb_range1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txt_I0.setText("0");
				txt_I1.setText("0,014");
				useKalbibFileForTesting = false;
			}
		});

		rb_range2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txt_I0.setText("0,016");
				txt_I1.setText("1");
				useKalbibFileForTesting = false;
			}
		});

		rb_range12.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txt_I0.setText("0");
				txt_I1.setText("1");
				useKalbibFileForTesting = false;
			}
		});
		
		rb_other.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				txt_I0.setText("");
				txt_I1.setText("");
				useKalbibFileForTesting = true;
			}
		});
		
		cmd_loadCalib.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!rb_rangeU.isSelected() && !rb_rangeI0.isSelected() && !rb_rangeI1.isSelected() && rb_ad7765board.isSelected()){
					JOptionPane.showMessageDialog(null, "Bitte erst einen Kalibrierungsbereich auswählen.", "Kalibrierung laden", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				if (rb_ad7765board.isSelected()){
					Power.setKalib((String) cmb_AD7765_DEV.getSelectedItem());
					if (rb_rangeU.isSelected()){
						range = 0;
						txt_k0.setText(String.valueOf(Power.getKalib(Power.RANGE_VOLTAGE,0)));
						txt_k1.setText(String.valueOf(Power.getKalib(Power.RANGE_VOLTAGE,1)));
						txt_k2.setText(String.valueOf(Power.getKalib(Power.RANGE_VOLTAGE,2)));
					}else if (rb_rangeI0.isSelected()){
						range = 1;
						txt_k0.setText(String.valueOf(Power.getKalib(Power.RANGE_15_MILIAMPS,0)));
						txt_k1.setText(String.valueOf(Power.getKalib(Power.RANGE_15_MILIAMPS,1)));
						txt_k2.setText(String.valueOf(Power.getKalib(Power.RANGE_15_MILIAMPS,2)));
					}else if (rb_rangeI1.isSelected()){
						range = 2;
						txt_k0.setText(String.valueOf(Power.getKalib(Power.RANGE_2500_MILIAMPS,0)));
						txt_k1.setText(String.valueOf(Power.getKalib(Power.RANGE_2500_MILIAMPS,1)));
						txt_k2.setText(String.valueOf(Power.getKalib(Power.RANGE_2500_MILIAMPS,2)));
					}
				}
			}
		});
	}
	
	private void performAutoCalibClick(){
		txt_hm8143_I.setEnabled(chk_autoCalib.isSelected());
		txt_hm8143_port.setEnabled(chk_autoCalib.isSelected());
		txt_hm8143_U.setEnabled(chk_autoCalib.isSelected());
		cmd_hm8143_connect.setEnabled(chk_autoCalib.isSelected());
		cmd_setU.setEnabled(chk_autoCalib.isSelected());
		cmd_setI.setEnabled(chk_autoCalib.isSelected());
		chk_activate.setEnabled(chk_autoCalib.isSelected());
		txt_mm_I.setEnabled(chk_autoCalib.isSelected());
		txt_MM_port.setEnabled(chk_autoCalib.isSelected());
		txt_mm_U.setEnabled(chk_autoCalib.isSelected());
		cmd_mm_connect.setEnabled(chk_autoCalib.isSelected());
		rb_ad7765board.setEnabled(chk_autoCalib.isSelected());
		rb_ad7765eval.setEnabled(chk_autoCalib.isSelected());
		rb_harting.setEnabled(chk_autoCalib.isSelected());
		rb_preon32adc.setEnabled(chk_autoCalib.isSelected());
		cmb_mm.setEnabled(chk_autoCalib.isSelected());
	}

	private void setComponents(){
		if (cmd_start.getText().equals("Kalibrierung starten")) {
			cmd_start.setText("Kalibrierung abbrechen");
			cancelCalib = false;
		} else if (cmd_start.getText().equals("Kalibrierung abbrechen")){
			cmd_start.setText("Kalibrierung starten");
			pb_progress.setValue(0);
			pb_progress.setString("");
			cancelCalib = true;
		}
		txt_hm8143_I.setEnabled(cancelCalib);
		txt_hm8143_U.setEnabled(cancelCalib);
		txt_hm8143_port.setEnabled(cancelCalib);
		cmd_connectAll.setEnabled(cancelCalib);
		cmd_hm8143_connect.setEnabled(cancelCalib);
		cmd_loadKalib.setEnabled(cancelCalib);
		cmd_mm_connect.setEnabled(cancelCalib);
		cmd_save.setEnabled(cancelCalib);
		cmd_setI.setEnabled(cancelCalib);
		cmd_setU.setEnabled(cancelCalib);
		rb_ad7765board.setEnabled(cancelCalib);
		rb_ad7765eval.setEnabled(cancelCalib);
		rb_harting.setEnabled(cancelCalib);
		rb_preon32adc.setEnabled(cancelCalib);
		
		cmd_testCancel.setEnabled(!cancelCalib);
		cmd_startTest.setEnabled(cancelCalib);

	}

	public void disconnect(int device) {
		if (device == 0 || device == -1){
			source.disconnect();
			
			cmd_hm8143_connect.setText("verbinden");
		}
		if (device == 1 || device == -1){
			if (multimeter != null) multimeter.disconnect();
			cmd_mm_connect.setText("verbinden");
		}
	}

	private void initComponents() {
		getContentPane().setLayout(null);

		cmd_cancel = new JButton("beenden");
		cmd_cancel.setBounds(507, 7, 189, 23);
		getContentPane().add(cmd_cancel);

		chk_autoCalib = new JCheckBox("automatische Kalibrierung");
		chk_autoCalib.setBounds(297, 7, 180, 23);
		getContentPane().add(chk_autoCalib);

		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "2. Multimeter", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panel.setBounds(10, 108, 686, 69);
		getContentPane().add(panel);
		panel.setLayout(null);

		lblMmPort = new JLabel("Port:");
		lblMmPort.setBounds(126, 25, 36, 20);
		panel.add(lblMmPort);

		txt_MM_port = new JTextField();
		txt_MM_port.setText("COM10");
		txt_MM_port.setBounds(172, 25, 57, 20);
		panel.add(txt_MM_port);
		txt_MM_port.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_MM_port.setEnabled(false);
		txt_MM_port.setColumns(10);

		cmd_mm_connect = new JButton("Verbinden");
		cmd_mm_connect.setBounds(239, 24, 99, 23);
		panel.add(cmd_mm_connect);
		cmd_mm_connect.setEnabled(false);

		label = new JLabel("U:");
		label.setBounds(348, 25, 23, 20);
		panel.add(label);

		txt_mm_U = new JTextField();
		txt_mm_U.setEditable(false);
		txt_mm_U.setBounds(381, 25, 99, 20);
		panel.add(txt_mm_U);
		txt_mm_U.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_mm_U.setEnabled(false);
		txt_mm_U.setColumns(10);

		label_1 = new JLabel("I:");
		label_1.setBounds(521, 25, 15, 20);
		panel.add(label_1);

		txt_mm_I = new JTextField();
		txt_mm_I.setEditable(false);
		txt_mm_I.setBounds(546, 25, 99, 20);
		panel.add(txt_mm_I);
		txt_mm_I.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_mm_I.setEnabled(false);
		txt_mm_I.setColumns(10);
		
		cmb_mm = new JComboBox<String>();
		cmb_mm.setModel(new DefaultComboBoxModel<String>(new String[] {"UT804", "HM8112-3", "Manuell"}));
		cmb_mm.setBounds(10, 25, 86, 20);
		panel.add(cmb_mm);
		
		JLabel lblMv = new JLabel("V");
		lblMv.setBounds(488, 28, 23, 14);
		panel.add(lblMv);
		
		JLabel lblMa = new JLabel("A");
		lblMa.setBounds(653, 28, 23, 14);
		panel.add(lblMa);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "1. Netzteil", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panel_1.setBounds(10, 37, 686, 65);
		getContentPane().add(panel_1);
		panel_1.setLayout(null);

		lblNewLabel = new JLabel("Port:");
		lblNewLabel.setBounds(10, 23, 50, 20);
		panel_1.add(lblNewLabel);

		txt_hm8143_port = new JTextField();
		txt_hm8143_port.setBounds(61, 23, 57, 20);
		panel_1.add(txt_hm8143_port);
		txt_hm8143_port.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_hm8143_port.setEnabled(false);
		txt_hm8143_port.setText("COM9");
		txt_hm8143_port.setColumns(10);

		cmd_hm8143_connect = new JButton("Verbinden");
		cmd_hm8143_connect.setBounds(128, 22, 99, 23);
		panel_1.add(cmd_hm8143_connect);
		cmd_hm8143_connect.setEnabled(false);

		JLabel lblU = new JLabel("U:");
		lblU.setBounds(237, 23, 23, 20);
		panel_1.add(lblU);

		txt_hm8143_U = new JTextField();
		txt_hm8143_U.setBounds(270, 23, 67, 20);
		panel_1.add(txt_hm8143_U);
		txt_hm8143_U.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_hm8143_U.setEnabled(false);
		txt_hm8143_U.setColumns(10);

		cmd_setU = new JButton("set");
		cmd_setU.setBounds(347, 22, 63, 23);
		panel_1.add(cmd_setU);
		cmd_setU.setEnabled(false);

		JLabel lblI = new JLabel("I:");
		lblI.setBounds(420, 23, 23, 20);
		panel_1.add(lblI);

		txt_hm8143_I = new JTextField();
		txt_hm8143_I.setBounds(438, 23, 67, 20);
		panel_1.add(txt_hm8143_I);
		txt_hm8143_I.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_hm8143_I.setEnabled(false);
		txt_hm8143_I.setColumns(10);

		cmd_connectAll = new JButton("Verbinde Messgerät und Netzteil");
		cmd_connectAll.setBounds(20, 7, 271, 23);

		cmd_setI = new JButton("set");
		cmd_setI.setBounds(515, 22, 57, 23);
		panel_1.add(cmd_setI);
		cmd_setI.setEnabled(false);

		chk_activate = new JCheckBox("aktivieren");
		chk_activate.setBounds(578, 22, 83, 23);
		panel_1.add(chk_activate);
		chk_activate.setEnabled(false);

		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "4. Kalibrierungskoeffizienten", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		panel_2.setBounds(10, 312, 686, 189);
		getContentPane().add(panel_2);
		panel_2.setLayout(null);

		JLabel lblK = new JLabel("k2:");
		lblK.setBounds(10, 31, 23, 14);
		panel_2.add(lblK);

		JLabel lblK_1 = new JLabel("k1:");
		lblK_1.setBounds(10, 60, 23, 14);
		panel_2.add(lblK_1);

		JLabel lblK_2 = new JLabel("k0:");
		lblK_2.setBounds(10, 89, 23, 14);
		panel_2.add(lblK_2);

		txt_k0 = new JTextField();
		txt_k0.setBounds(43, 86, 437, 20);
		panel_2.add(txt_k0);
		txt_k0.setColumns(10);

		txt_k1 = new JTextField();
		txt_k1.setBounds(43, 57, 437, 20);
		panel_2.add(txt_k1);
		txt_k1.setColumns(10);

		txt_k2 = new JTextField();
		txt_k2.setBounds(43, 28, 437, 20);
		panel_2.add(txt_k2);
		txt_k2.setColumns(10);

		cmd_save = new JButton("Kalibrirung übernehmen");
		cmd_save.setBounds(490, 28, 179, 34);
		panel_2.add(cmd_save);
		
		rb_preon32adc = new JRadioButton("Preon 32 ADC");
		bg_Hardware.add(rb_preon32adc);
		rb_preon32adc.setBounds(10, 123, 110, 23);
		panel_2.add(rb_preon32adc);
		
		rb_harting = new JRadioButton("Harting Current");
		bg_Hardware.add(rb_harting);
		rb_harting.setBounds(179, 123, 119, 23);
		panel_2.add(rb_harting);
		
		rb_ad7765eval = new JRadioButton("AD7765 Eval Board");
		bg_Hardware.add(rb_ad7765eval);
		rb_ad7765eval.setBounds(342, 123, 138, 23);
		panel_2.add(rb_ad7765eval);
		
		rb_ad7765board = new JRadioButton("AD7765 Board");
		bg_Hardware.add(rb_ad7765board);
		rb_ad7765board.setSelected(true);
		rb_ad7765board.setBounds(10, 157, 110, 23);
		panel_2.add(rb_ad7765board);
		
		JPanel panel_3 = new JPanel();
		panel_3.setBorder(new TitledBorder(null, "Kalibrierung laden", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_3.setBounds(490, 73, 179, 80);
		panel_2.add(panel_3);
		panel_3.setLayout(null);
		
		cmd_loadCalib = new JButton("laden");
		cmd_loadCalib.setBounds(10, 23, 159, 20);
		panel_3.add(cmd_loadCalib);
		
		rb_rangeU = new JRadioButton("U");
		rb_rangeU.setBounds(20, 50, 47, 23);
		panel_3.add(rb_rangeU);
		rb_rangeU.setToolTipText("Bereich Spannung");
		rb_rangeU.setSelected(true);
		bg_range.add(rb_rangeU);
		
		rb_rangeI0 = new JRadioButton("B0");
		rb_rangeI0.setBounds(73, 50, 47, 23);
		panel_3.add(rb_rangeI0);
		rb_rangeI0.setToolTipText("Bereich Strom 0 - 15 mA");
		bg_range.add(rb_rangeI0);
		
		rb_rangeI1 = new JRadioButton("B1");
		rb_rangeI1.setBounds(122, 50, 47, 23);
		panel_3.add(rb_rangeI1);
		rb_rangeI1.setToolTipText("Bereich Strom 15 - 2500 mA");
		bg_range.add(rb_rangeI1);
		
		cmb_AD7765_DEV = new JComboBox<String>();
		cmb_AD7765_DEV.setBounds(179, 158, 301, 20);
		panel_2.add(cmb_AD7765_DEV);

		JPanel p_test = new JPanel();
		p_test.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "5. Plausibilit\u00E4tstest", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		p_test.setBounds(10, 512, 686, 138);
		getContentPane().add(p_test);
		
		JLabel lblDtT = new JLabel("Zeit pro Messung [s]:");
		lblDtT.setBounds(16, 85, 143, 14);
		
		JLabel lblIStart = new JLabel("I start [A]:");
		lblIStart.setBounds(16, 33, 143, 14);
		
		JLabel lblIEnde = new JLabel("I ende [A]:");
		lblIEnde.setBounds(16, 59, 143, 14);
		
		txt_dt = new JTextField();
		txt_dt.setBounds(163, 82, 86, 20);
		txt_dt.setText("2");
		txt_dt.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_dt.setColumns(10);
		
		txt_I0 = new JTextField();
		txt_I0.setText("0");
		txt_I0.setBounds(163, 30, 86, 20);
		txt_I0.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_I0.setColumns(10);
		
		txt_I1 = new JTextField();
		txt_I1.setText("1");
		txt_I1.setBounds(163, 56, 86, 20);
		txt_I1.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_I1.setColumns(10);
		
		cmd_startTest = new JButton("Test starten");
		cmd_startTest.setBounds(279, 105, 185, 23);
		
		JLabel lblCsvSpeichernUnter = new JLabel("csv speichern unter:");
		lblCsvSpeichernUnter.setBounds(279, 59, 185, 14);
		
		txt_saveCSV = new JTextField();
		txt_saveCSV.setBounds(279, 80, 334, 20);
		txt_saveCSV.setColumns(10);
		
		cmd_saveCSV = new JButton("...");
		cmd_saveCSV.setBounds(621, 79, 45, 23);
		
		cmd_testCancel = new JButton("Test abbrechen");
		cmd_testCancel.setEnabled(false);
		cmd_testCancel.setBounds(474, 105, 192, 23);
		
		lblAnzahlDerMesspunkte = new JLabel("Anzahl der Messpunkte:");
		lblAnzahlDerMesspunkte.setBounds(16, 111, 143, 14);
		
		txt_anz_Point = new JTextField();
		txt_anz_Point.setBounds(163, 108, 86, 20);
		txt_anz_Point.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_anz_Point.setText("20");
		txt_anz_Point.setColumns(10);
		p_test.setLayout(null);
		p_test.add(lblAnzahlDerMesspunkte);
		p_test.add(lblDtT);
		p_test.add(lblIEnde);
		p_test.add(lblIStart);
		p_test.add(txt_dt);
		p_test.add(txt_I1);
		p_test.add(txt_I0);
		p_test.add(lblCsvSpeichernUnter);
		p_test.add(cmd_saveCSV);
		p_test.add(txt_saveCSV);
		p_test.add(cmd_startTest);
		p_test.add(cmd_testCancel);
		p_test.add(txt_anz_Point);
		
		JPanel panel1 = new JPanel();
		panel1.setBorder(new TitledBorder(null, "3. Kalibrirung starten", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel1.setBounds(10, 188, 680, 113);
		getContentPane().add(panel1);
		panel1.setLayout(null);
		
		cmd_start = new JButton("Kalibrierung starten");
		cmd_start.setBounds(287, 83, 383, 23);
		panel1.add(cmd_start);

		txt_calib_file = new JTextField();
		txt_calib_file.setBounds(10, 56, 660, 20);
		panel1.add(txt_calib_file);
		txt_calib_file.setColumns(10);

		cmd_loadKalib = new JButton("Kalibrierungdatei laden");
		cmd_loadKalib.setBounds(10, 22, 182, 23);
		panel1.add(cmd_loadKalib);

		chk_saveCalibMesure = new JCheckBox("Kalibrierungsmessdaten speichern");
		chk_saveCalibMesure.setBounds(10, 83, 271, 23);
		panel1.add(chk_saveCalibMesure);

		rb_range1 = new JRadioButton("1");
		bg_TestCurrent.add(rb_range1);
		rb_range1.setBounds(343, 29, 38, 23);
		p_test.add(rb_range1);
		
		rb_range2 = new JRadioButton("2");
		bg_TestCurrent.add(rb_range2);
		rb_range2.setBounds(383, 29, 38, 23);
		p_test.add(rb_range2);
		
		rb_range12 = new JRadioButton("1 + 2");
		bg_TestCurrent.add(rb_range12);
		rb_range12.setBounds(423, 29, 58, 23);
		p_test.add(rb_range12);
		
		rb_other = new JRadioButton("nutze Kalibrierungsdatei");
		bg_TestCurrent.add(rb_other);
		rb_other.setBounds(483, 30, 183, 20);
		p_test.add(rb_other);
		
		JLabel lblNewLabel_1 = new JLabel("Bereich:");
		lblNewLabel_1.setBounds(279, 33, 58, 14);
		p_test.add(lblNewLabel_1);JLabel lblKalibrierungsdatei = new JLabel("Kalibrierungsdatei:");
		lblKalibrierungsdatei.setBounds(392, 447, 111, 14);
		getContentPane().add(lblKalibrierungsdatei);
		setSize(710, 725);
		setLocationRelativeTo(null);

	}

	private void performCalib() {
		try {
			if (!chk_autoCalib.isSelected()) {
				source = new ManuSource("");
				multimeter = new MultiManu("");
			}
			if (!source.isConnected() && chk_autoCalib.isSelected()) {
				JOptionPane.showMessageDialog(this,"HM8143 Labornetzteil nicht verbunden. Bitter erst das Netzteil verbinden.","Verbindungsfehler", JOptionPane.ERROR_MESSAGE);
				cmd_start.setEnabled(true);
				setComponents();
				//return;
			}
			if (cancelCalib) {
				setComponents();
				return;
			}

			// Kalibrierungskonfiguration lesen
			if (!isTestRunning || useKalbibFileForTesting){
				File f = new File(calibPath);
				if (f == null || !f.exists()){
					JOptionPane.showMessageDialog(this,"Keine Kalibrierungsdatei geladen","Programmfehler", JOptionPane.ERROR_MESSAGE);
					cmd_start.setEnabled(true);
					setComponents();
					return;				
				}
			
				runs.clear();
				BufferedReader br = new BufferedReader(new FileReader(f));
				String line = "";
				while ((line = br.readLine()) != null) {
					if (line.startsWith("//"))
						continue;
									
					String[] conf = line.split(";");
					if (conf.length < 3) continue;
					if (range < 0){
						range = Integer.parseInt(conf[0].split("=")[1]);
						if (isTestRunning){
							Power.setKalib((String) cmb_AD7765_DEV.getSelectedItem());
							if (range == 0){
								txt_k0.setText(String.valueOf(Power.getKalib(Power.RANGE_VOLTAGE, 0)));
								txt_k1.setText(String.valueOf(Power.getKalib(Power.RANGE_VOLTAGE, 1)));
								txt_k2.setText(String.valueOf(Power.getKalib(Power.RANGE_VOLTAGE, 2)));
							}else{
								if (range == 1){
									txt_k0.setText(String.valueOf(Power.getKalib(Power.RANGE_15_MILIAMPS, 0)));
									txt_k1.setText(String.valueOf(Power.getKalib(Power.RANGE_15_MILIAMPS, 1)));
									txt_k2.setText(String.valueOf(Power.getKalib(Power.RANGE_15_MILIAMPS, 2)));
								}else if (range == 3){
									txt_k0.setText(String.valueOf(Power.getKalib(Power.RANGE_2500_MILIAMPS, 0)));
									txt_k1.setText(String.valueOf(Power.getKalib(Power.RANGE_2500_MILIAMPS, 1)));
									txt_k2.setText(String.valueOf(Power.getKalib(Power.RANGE_2500_MILIAMPS, 2)));
								}
							}
						}
					}
					String u = conf[1].split("=")[1];
					String i = conf[2].split("=")[1];
					int time = Integer.parseInt(conf[3].split("=")[1]);
	
					CalibRun run = new CalibRun(range, time, i, u);
					if (conf.length > 4){
						int r = Integer.parseInt(conf[4].split("=")[1]);
						run.setR(r);
					}
					runs.add(run);
				}
				br.close();
			}
			
			// Kalibrierungen durchführen
			boolean messageShown = false;
			int i_run = 1;
			calibRuns = true;
			if (multimeter instanceof HM8112_3){
				source.setOut(false);
				((HM8112_3) multimeter).setMeassureTime(100);
				((HM8112_3) multimeter).setNull();
			}

			pb_progress.setMaximum(runs.size());
			if (!isTestRunning) ProgStates.ISCALIB = true;
			String path = "Messung\\Kalibrierung\\" + (new SimpleDateFormat("yyyyMMdd_hhmmss").format(new Date()));
			if (chk_saveCalibMesure.isSelected()){
				File t_f = new File(path);
				if (!t_f.exists()){
					t_f.mkdirs();
				}
			}

			for (CalibRun cal : runs) {
				if (cancelCalib){
					break;
				}
				
				//Wenn Strom höher als Multimeter erlaubt, Run verwerfen
				float setI = Float.parseFloat(cal.i);
				if (setI >= multimeter.getMaxI()) continue;
				
				if (cal.r > 0){
					messageShown = false;
					source.setOut(false);
				}
				if (!messageShown) {
					int res = 0;
					String mes = getCustomizedMessage(cal);

					if (cal.range == 1 && cal.r > 2 && multimeter instanceof UT804)
						mes += "\n5. Stellen sie Das Spannungsmessgerät auf mA";
					else if (cal.range == 1 && cal.r > 0 && cal.r < 3  && multimeter instanceof UT804)
						mes += "\n5. Stellen sie Das Spannungsmessgerät auf uA";
					else if (multimeter instanceof MultiManu)
						mes += "\n5. Stellen sie Das Spannungsmessgerät auf mA";
					res = JOptionPane.showConfirmDialog(this,mes,"Verbindungsinfo",JOptionPane.OK_CANCEL_OPTION);
					if (res == 2) {
						cancelCalib = true;
						break;
					}
					messageShown = true;
				}

				if (!isTestRunning) ProgStates.CALIB_RANGE = cal.range;
				if (chk_saveCalibMesure.isSelected()){
					String p = path + "\\" + "Bereich=" + cal.rangeMap[cal.range] + "_Umax=" + cal.u + "_Imax=" + cal.i + ".cdf";
					conf.prepareCalib(cal.time,p,true);
				}else{
					conf.prepareCalib(cal.time,"",false);
				}
				
				source.setU(cal.u);
				source.setI(cal.i);
				if (!cal.u.equals("0.0")){
					if (multimeter instanceof HM8112_3){
						((HM8112_3) multimeter).setAutotrange(true);
						if (cal.range == 0) ((HM8112_3) multimeter).selectVoltage();
						else ((HM8112_3) multimeter).selectCurrent();
					}
					Thread.sleep(1000);
					source.setOut(true);
					Thread.sleep(500);
					//chk_activate.setSelected(true);
				}
				else {
					if (multimeter instanceof HM8112_3){
						((HM8112_3) multimeter).setNull();
					}
				}
				
				final int k = i_run++;
				final String progress = "Messung " + k + " von " + runs.size() + ": max Strom: " + cal.i + " A, Spannung: " + cal.u + " V";
				SwingUtilities.invokeLater(new Runnable() {					
					@Override
					public void run() {
						pb_progress.setValue(k - 1);
						pb_progress.setString(progress);
					}
				});
				System.out.println(progress);

				//Warte bis sich der Strom eingestellt hat
				if (cal.range > 1){
					int wait = (int) (Double.parseDouble(cal.i) * 2500);
					Thread.sleep(wait + 1500);
				}
				else Thread.sleep(1000);

				_main.connectAndStart();

				// Während dessen Messgerät auslesen und Werte Mitteln
				String res = "-";
				Thread collectRef = null;
				if (multimeter.isConnected()) {
					collectRef = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								double lastMean = 0;
								ref_temp_mean = 0;
								int size = 0;
								while (true) {
									// Messwerte ermitteln und aufaddieren
									if ((multimeter.newValues() & 0x1) > 0) { // Spannungswerte
																				// mitteln
										ref_temp_mean = lastMean + (multimeter.getU() - lastMean) / ++size;
										lastMean = ref_temp_mean;
									} else if ((multimeter.newValues() & 0x2) > 0) { // Stromwerte
																						// mitteln
										ref_temp_mean = lastMean + (multimeter.getI() - lastMean) / ++size;
										lastMean = ref_temp_mean;
									}
									Thread.sleep(50);
								}
							} catch (Exception e) {
							}
						}
					});
					collectRef.start();
				} else {
					while (true) {
						try {
							String unit = "";
							if (cal.range == 0)
								unit = "[mV]";
							else if (cal.range > 0)
								unit = "[mA]";
							res = JOptionPane.showInputDialog(this,
									"Bitte Referenzwert vom Multimeter in "
											+ unit + " eingeben.",
									"Referenzwerteingabe",
									JOptionPane.QUESTION_MESSAGE);
							System.out.println(res);
							double ref = Double.parseDouble(res);
							if (cal.range == 0)
								cal.ref_u = ref;
							else if (cal.range > 0)
								cal.ref_i = ref;
							break;
						} catch (NumberFormatException e) {
						}
					}

					while (ProgStates.MEASSURE_RUNS) {
						if (cancelCalib){
							_main.stopMeasure();
						}
						Thread.sleep(10);
					}
				}
				_main.t_cas.join();

				/*
				if (!ProgStates.ISCONNECTED && ){	//Wenn verbindungsfehler
					cancelCalib = true;
					break;
				}
				*/
				while (ProgStates.MEASSURE_RUNS) {
					Thread.sleep(1000);
				}

				if (collectRef != null) {
					collectRef.interrupt();
					if (cal.range == 0)
						cal.ref_u = ref_temp_mean;
					else
						cal.ref_i = ref_temp_mean;
				}

				if (cal.range == 0) { // Spannung
					cal.mean = Statistics.getMeanSD(_main.data, MeasureType.VOLTAGE.index, 0, _main.data.getSize()-1)[0];
					System.out.println("Referenzspannung mittel: " + cal.ref_u + " V, RAW mittel: " + cal.mean);
				} else {
					cal.mean = Statistics.getMeanSD(_main.data, MeasureType.CURRENT.index, 0, _main.data.getSize()-1)[0];
					System.out.println("Referenzstrom mittel: " + cal.ref_i + " A, RAW mittel: " + cal.mean);
				}
			}

			source.setOut(false);
			chk_activate.setSelected(false);
			calibRuns = false;
			if (multimeter instanceof HM8112_3) ((HM8112_3) multimeter).setMeassureTime(500);
			if (cancelCalib){
				setComponents();
				pb_progress.setValue(0);
				pb_progress.setString("");
				return;
			}
			// Kalibrierungskoeffizienten berechnen
			
			if (!isTestRunning){
				System.out.println("berechne Kalibrierungskoeffizienten");
				calcKoef();
				
				//korrigiere Offset
				for (CalibRun run : runs){
					if (run.u.equals("0.0")){	//gemessener Strom gleich 0
						double temp = k2 * Math.pow(run.mean, 2) + k1 * run.mean + k0;			//berechne mit den Koeffizieten 0A 
						System.out.println("diff: " + temp);
						k0 -= temp;																//ziehe offset dem Koeffizienten k0 ab
						//check
						temp = k2 * Math.pow(run.mean, 2) + k1 * run.mean + k0;
						System.out.println(temp);												//nullpunkt ausgeben
					}
				}
				
				txt_k0.setText(String.format(String.valueOf(k0), "%.24f"));
				txt_k1.setText(String.format(String.valueOf(k1), "%.24f"));
				txt_k2.setText(String.format(String.valueOf(k2), "%.24f"));
			}else{
				
				//Bei Testdurchlauf auswertung in CSV Datei schreiben
				saveTest();
				cmd_startTest.setEnabled(true);
				cmd_testCancel.setEnabled(false);
				
			}
			pb_progress.setValue(runs.size());
			setComponents();
			
			//debugging:
			for (CalibRun run : runs){
				if (run.range == 0) System.out.println((run.mean + ";" + run.ref_u).replace(".", ","));
				else System.out.println((run.mean + ";" + run.ref_i).replace(".", ","));
			}
		} catch (Exception e) {
			e.printStackTrace();
			setComponents();
		}
		ProgStates.ISCALIB = false;
	}
	
	private void saveTest(){
		if (runs.isEmpty()) return;
		File f = new File(txt_saveCSV.getText());
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			String unit = "[A]";
			if (runs.get(0).range == 0) unit = "[V]";
			String out = "i;Soll " + unit + ";Ist " + unit + ";delta I " + unit + ";Spannung [V]" + ";CDF(ist) " + unit + ";CDF(soll)" + unit + "\n";
			bw.write(out);
			float cdf_ist = 0;
			float cdf_soll = 0;
			
			int i=0;
			for (CalibRun run : runs){
				float ist = (float) run.mean;
				float soll = 0;
				if (run.range > 0) soll = (float) run.ref_i;
				else soll = (float) run.ref_u;
				float delta = soll - ist;
				cdf_ist += ist;
				cdf_soll += soll;
				out = i++ + ";" + String.format("%.6f", soll) + ";" + String.format("%.6f", ist) + ";" + String.format("%.6f", delta) + ";" + 
						run.u.replace(".", ",") + ";" + String.format("%.6f", cdf_ist) + ";" + String.format("%.6f", cdf_soll) + "\n";
				System.out.print(out);
				bw.write(out);
			}
			bw.close();
			
//			int res = JOptionPane.showConfirmDialog(this, "Wollen Sie die CSV-Datei öffnen?","Plausibilitätscheck öffnen",JOptionPane.YES_NO_OPTION);
//			if (res == JOptionPane.YES_OPTION){
//				Runtime.getRuntime().exec("excel \"" + f.getAbsolutePath() + "\"");
//			}
		} catch (IOException e) {
			setComponents();
		}
		

	}
	
	private void calcKoef(){
		//array mit mean
		//evtl. alle Messwerte (RAW) übergeben, nicht nur Mittelwerte
		k0 = 0;
		k1 = 1;
		k2 = 0;

		double[] mean = new double[runs.size()];
		double[] ref = new double[runs.size()];
		for (int i=0;i<runs.size();i++){
			if (runs.get(i).mean != 0){
				mean[i] = runs.get(i).mean;
				if (runs.get(i).range == 0) ref[i] = runs.get(i).ref_u;
				else ref[i] = runs.get(i).ref_i;
			}
		}

		if (mean.length == 2){
			PolynomialRegression poly = new PolynomialRegression(mean, ref, 1);
			k0 = poly.beta(0);
			k1 = poly.beta(1);
			k2 = 0;					
		}else{
			PolynomialRegression poly = new PolynomialRegression(mean, ref, 2);
			k0 = poly.beta(0);
			k1 = poly.beta(1);
			k2 = poly.beta(2);		
		}
	}
	
	public static void saveKalib(int type, float _k0, float _k1, float _k2, int range, String name){
		
		String k0 = String.format(String.valueOf(_k0), "%.24f");
		String k1 = String.format(String.valueOf(_k1), "%.24f");
		String k2 = String.format(String.valueOf(_k2), "%.24f");
		switch (type){
		case ProgStates.ADC_HARTING:
			Property.setCalib(name, Property.MESSUNG_KALIB_HARTING_0, k0);
			Property.setCalib(name, Property.MESSUNG_KALIB_HARTING_1, k1);
			Property.setCalib(name, Property.MESSUNG_KALIB_HARTING_2, k2);
			break;
		case ProgStates.ADC_INTERNAL:
			Property.setCalib(name, Property.MESSUNG_KALIB_INTERNAL_0, k0);
			Property.setCalib(name, Property.MESSUNG_KALIB_INTERNAL_1, k1);
			Property.setCalib(name, Property.MESSUNG_KALIB_INTERNAL_2, k2);			
			break;
		case ProgStates.ADC_AD7765_EVAL:
			Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_0, k0);
			Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_1, k1);
			Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_2, k2);						
			break;
		case ProgStates.ADC_AD7765_BOARD:
			switch (range){
			case 0:		//Spannung
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_U_0, k0);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_U_1, k1);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_U_2, k2);						
				break;
			case 1:		//Strom 0-10mA
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B0_0, k0);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B0_1, k1);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B0_2, k2);						
				break;
			case 2:		//Strom 0-100mA
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B1_0, k0);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B1_1, k1);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B1_2, k2);						
				break;
			case 3:		//Strom 0-2500mA
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B2_0, k0);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B2_1, k1);
				Property.setCalib(name, Property.MESSUNG_KALIB_AD7765_B2_2, k2);						
				break;					
		}			
			break;
		}
	}
	
	private String getCustomizedMessage(CalibRun run){
		String mes = "";
		switch (run.range) {
			case 0: // Spannung
				if(multimeter instanceof HM8112_3){
					((HM8112_3) multimeter).selectVoltage();
					mes = "Bitte verbinden sie das Messgerät HM8112-3 mit dem Ausgang des Labornetzteils HM8143:\n\n" +
							"1. Netzteil CH1 + mit Messgerät V SENSE HI\n" +
							"2. Netzteil CH1 - mit Messgerät V SENSE LO\n" +
							"3. Netzteil CH1 + mit PreonMeterPower BAT+\n" +
							"4. Netzteil CH1 - mit PreonMeterPower BAT-\n" +
							"5. DUT+ und DUT- nicht verbinden\n";
				}else if (multimeter instanceof UT804){
					mes = "Bitte verbinden sie das Messgerät UT804 mit dem Ausgang des Labornetzteils HM8143:\n\n"
							+ "1. Netzteil CH1 + mit Messgerät V\n"
							+ "2. Netzteil CH1 - mit Messgerät COM\n"
							+ "3. Netzteil CH1 + mit PreonMeterPower BAT+\n"
							+ "4. Netzteil CH1 - mit PreonMeterPower BAT-\n"
							+ "5. DUT+ und DUT- nicht verbinden\n"
							+ "6. Schalten sie den Messbereich des Messgerätes auf Spannungsmessung.";
				}else if (multimeter instanceof MultiManu) {
						mes = "Messwerte müssen vom Multimeter abgelesen und eingetragen werden.";
				}
				break;
			case 1: // Bereich 0-10mA
				if(multimeter instanceof HM8112_3){
					((HM8112_3) multimeter).selectCurrent();
					mes = "Bitte verbinden sie das Messgerät HM8112-3, das Labornetzteil HM8143 und das PreonMeter: \n\n" + 
							"1. Netzteil CH1 + mit PreonMeterPower BAT+\n" +
							"2. Netzteil CH1 - mit PreonMeterPower BAT-\n" +
							"3. PreonMeterPower DUT+ mit Messgerät A HI\n";
					if (!run.u.equalsIgnoreCase("0.0")){
						mes += "4. Messgerät A LO mit einem Pol des Wiederstands " + run.r + " ("+ run.rMap[run.r] + " )\n" +
								"5. PreonMeterPower DUT- mit dem anderen Pol des Wiederstands";
					}else{
						mes += "4. Messgerät A LO mit PreonMeterPower DUT-";
					}
				}else if (multimeter instanceof UT804){
					mes = "Bitte verbinden sie das Messgerät UT804, das Labornetzteil HM8143 und das PreonMeter: \n\n" + 
							"1. Netzteil CH1 + mit PreonMeterPower BAT+\n" +
							"2. Netzteil CH1 - mit PreonMeterPower BAT-\n" +
							"3. PreonMeterPower DUT+ mit Messgerät uA/mA\n";
					if (!run.u.equalsIgnoreCase("0.0")){
						mes += "4. Messgerät COM mit einem Pol des Wiederstands " + run.r + " ("+ run.rMap[run.r] + " )\n" +
								"5. PreonMeterPower DUT- mit dem anderen Pol des Wiederstands";
					}else{
						mes += "4. Messgerät COM mit PreonMeterPower DUT-";
					}
				}
				else if (multimeter instanceof MultiManu) {
					mes = "Messwerte müssen vom Multimeter abgelesen und eingetragen werden.";
				}
				break;
			case 3: // Bereich 0-2000mA
				if(multimeter instanceof HM8112_3){
					((HM8112_3) multimeter).selectCurrent();
					mes = "Bitte verbinden sie das Messgerät HM8112-3, das Labornetzteil HM8143 und das PreonMeter: \n\n" + 
							"1. Netzteil CH1 + mit PreonMeterPower BAT+\n" +
							"2. Netzteil CH1 - mit PreonMeterPower BAT-\n" +
							"3. PreonMeterPower DUT+ mit Messgerät A HI\n" +
							"4. Messgerät A LO mit PreonMeterPower DUT-";
				}else if (multimeter instanceof UT804){
					mes = "Bitte verbinden sie das Messgerät UT804, das Labornetzteil HM8143 und das PreonMeter: \n\n" + 
							"1. Netzteil CH1 + mit PreonMeterPower BAT+\n" +
							"2. Netzteil CH1 - mit PreonMeterPower BAT-\n" +
							"3. PreonMeterPower DUT+ mit Messgerät 10Amax\n" +
							"4. Messgerät COM mit PreonMeterPower DUT-\n" +
							"5. Stellen sie Das Spannungsmessgerät auf A";
				}else if (multimeter instanceof MultiManu) {
					mes = "Messwerte müssen vom Multimeter abgelesen und eingetragen werden.";
				}
				break;
		}		
		return mes;	
	}
	
	private void startTest(){
		try{			
			isTestRunning = true;
			if (!useKalbibFileForTesting){
				float i0 = Float.parseFloat(txt_I0.getText());
				float i1 = Float.parseFloat(txt_I1.getText());
				int dt = Integer.parseInt(txt_dt.getText());						//alle x Sekunden Wert ändern
				int anzSteps = Integer.parseInt(txt_anz_Point.getText());
				float dI = (i1 - i0) / anzSteps;		//Änderung des Stroms pro Sekunde
				if (dI < 0 ) return;
				runs.clear();
				float _current = i0;
				for (int i=0;i<=anzSteps;i++){
					String s_i = String.format("%.3f", _current);
					s_i = s_i.replace(",", ".");
					CalibRun run = new CalibRun(3, dt, s_i, "2.0");
					runs.add(run);
					_current += dI;
				}
			}
			if (txt_saveCSV.getText().isEmpty()) txt_saveCSV.setText("Plasibilitätscheck.csv");
			cmd_startTest.setEnabled(false);
			cmd_testCancel.setEnabled(true);
			setComponents();
			
			Thread t = new Thread(r_startMeasure);
			t.start();
		}catch (Exception ex){
			
		}
		
	}

	/**
	 * 
	 */
	private JTextField txt_k2;
	private JTextField txt_k1;
	private JTextField txt_k0;
	private JButton cmd_cancel;
	private JButton cmd_save;
	private JButton cmd_start;
	private JLabel lblNewLabel;
	private JLabel lblMmPort;
	private JTextField txt_hm8143_port;
	private JTextField txt_MM_port;
	private JButton cmd_mm_connect;
	private JButton cmd_hm8143_connect;
	private JTextField txt_hm8143_U;
	private JTextField txt_hm8143_I;
	private JButton cmd_setI;
	private JButton cmd_setU;
	private JCheckBox chk_activate;
	private JCheckBox chk_autoCalib;
	private JLabel label;
	private JTextField txt_mm_U;
	private JLabel label_1;
	private JTextField txt_mm_I;
	private JButton cmd_loadKalib;
	private JTextField txt_calib_file;
	private JButton cmd_connectAll;
	private final ButtonGroup bg_Hardware = new ButtonGroup();
	private JRadioButton rb_preon32adc;
	private JRadioButton rb_ad7765board;
	private JRadioButton rb_ad7765eval;
	private JRadioButton rb_harting;
	private JProgressBar pb_progress;
	private JCheckBox chk_saveCalibMesure;
	private JComboBox<String> cmb_mm;
	private JTextField txt_dt;
	private JTextField txt_I0;
	private JTextField txt_I1;
	private JButton cmd_startTest;
	private JTextField txt_saveCSV;
	private JButton cmd_testCancel;
	private JLabel lblAnzahlDerMesspunkte;
	private JTextField txt_anz_Point;
	private JButton cmd_saveCSV;
	private final ButtonGroup bg_TestCurrent = new ButtonGroup();
	private JRadioButton rb_range1;
	private JRadioButton rb_range2;
	private JRadioButton rb_range12;
	private JRadioButton rb_other;
	private final ButtonGroup bg_range = new ButtonGroup();
	private JRadioButton rb_rangeU;
	private JRadioButton rb_rangeI0;
	private JRadioButton rb_rangeI1;
	private JButton cmd_loadCalib;
	private JComboBox<String> cmb_AD7765_DEV;

	private class CalibRun {
		private int time = 0;
		private String u = "";
		private String i = "";
		private double mean = 0;
		private int range = 1;		//0-U, 1-10mA, 2-100mA, 3-2500mA
		private double ref_i;
		private double ref_u;
		private int r = 0;
		private String[] rMap = {"Direkt verkabelt","100 kOhm","2,7 kOhm","400 Ohm"};
		private String[] rangeMap = {"0-10V", "0-15mA", "0-100mA","0-2500mA"};
		
		CalibRun(int r, int time, String i, String u) {
			this.time = time;
			this.u = u;
			this.range = r;
			this.i = i;
		}
		
		private void setR(int r){
			this.r = r;
		}
		
		
	}
}
