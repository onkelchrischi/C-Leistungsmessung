package frontend;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import program.Main;
import program.ProgStates;
import program.Property;

public class MyMenue extends JMenuBar{

	private final static Icon CONNECTED = new ImageIcon("res" + File.separator + "connected.png");
	private final static Icon DISCONNECTED = new ImageIcon("res" + File.separator + "disconnected.png");
	private final static Icon MEASURE = new ImageIcon("res" + File.separator + "measure.png");

	private final Main _main;
	private static final long serialVersionUID = 1L;
	
	/**
	 * Konstruktor, benötigt Main Klasse für Zugriff auf Gui-klassen und Messwertklasse
	 * @param main
	 */
	public MyMenue(Main main){
		super();
		_main = main;
		initComponents();
		setListeners();
	}
	
	/**
	 * Properties für die Menüleiste laden
	 */
	public void initProperties(){
		chk_liveupdate.setSelected(Property.getPropertiesBoolean(Property.GUI_LIVEUPDATE_ENABLE));
	}
	
	public void setUpdateLiveUpdate(boolean set){
		chk_liveupdate.setSelected(set);
	}
	
	/**
	 * Statuslabel updaten
	 * @param status neuer Status
	 */
	public void updateStatus(final String status) {
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				lbl_statusInfo.setText(status);
			}
			
		});		
	}
	
	/**
	 * Setzt das Stautusicon 
	 * @param id 0-getrennt, 1-verbunden, 2-Messungläuft
	 */
	public void setStatusIcon(final int id){
		SwingUtilities.invokeLater(new Runnable() {		
			@Override
			public void run() {
				switch (id){
				case 0:
					lbl_connectStatus.setIcon(DISCONNECTED);
					break;
				case 1:
					lbl_connectStatus.setIcon(CONNECTED);
					break;
				case 2:
					lbl_connectStatus.setIcon(MEASURE);
					break;
				}
			}
		});
	}
	
	/**
	 * setzt die Sichtbarkeit und das Verhalten des Fortschrittsbalken
	 * @param visible
	 * @param indeterminate
	 */
	public void setProgressBar(final boolean visible, final boolean indeterminate){
		SwingUtilities.invokeLater(new Runnable() {		
			@Override
			public void run() {
				pb_status.setIndeterminate(indeterminate);
				pb_status.setVisible(visible);
			}
		});
	}
	
	/**
	 * Setzt den Text des Statusbalkens
	 * @param text
	 */
	public void setProgressBarText(final String text){
		SwingUtilities.invokeLater(new Runnable() {		
			@Override
			public void run() {
				pb_status.setString(text);
			}
		});
	}
	/**
	 * setzt den Wert des Statusbalkens 
	 * @param val 0-100
	 */
	
	public void setProgressbarValue(final int val){
		SwingUtilities.invokeLater(new Runnable() {		
			@Override
			public void run() {
				pb_status.setValue(val);
			}
		});
	}
	
	/**
	 * deaktiviert den Statusbalken
	 */
	public void disablePB(){
		pb_status.setString("");
		pb_status.setValue(0);
		pb_status.setVisible(false);
	}

	/**
	 * Aktuallisiert die Aktivierung der Knöpfe
	 * @param button 0-Verbinden, 1-Trennen, 2-Messung starten, 3-Messung beenden
	 * @param enable (de)aktivieren
	 */
	public void updateButton(final int button,final  boolean enable) {
		SwingUtilities.invokeLater(new Runnable(){			
			@Override
			public void run() {
				switch (button){
					case 0:		//connect Button
						cmd_mnuStartMessung.setEnabled(enable);
						break;
					case 3:		//Messung beenden Button
						cmd_mnuStopMessung.setEnabled(enable);
						break;
				}
			}			
		});				
	}
	
	/**
	 * Komponenten initiallisieren
	 */
	private void initComponents(){
		this.add(m_Datei);
		m_Datei.add(mnu_loadMessung);
		m_Datei.add(mnu_loadMeasureExportToCSV);		
		m_Datei.add(mnu_calib);
		m_Datei.add(separator);		
		m_Datei.add(mnu_neustart);	
		m_Datei.add(m_Beenden);
		
		this.add(mnMessung);
		
		mnMessung.add(mnu_exportToCSV);		
		
		mnMessung.add(mnu_exportMarkedToCSV);
		
		mnMessung.add(separator_2);
		
		mnMessung.add(mnu_changeOffset);
		
		this.add(mnu_Graphic);
		mnu_Graphic.add(mnu_mean);
		mnu_minMax.setSelected(true);
		mnu_Graphic.add(mnu_minMax);

		JSeparator separator_1 = new JSeparator();
		separator_1.setAlignmentX(Component.LEFT_ALIGNMENT);
		separator_1.setMaximumSize(new Dimension(20, 32767));
		separator_1.setOrientation(SwingConstants.VERTICAL);
		this.add(separator_1);

		lbl_statusInfo.setPreferredSize(new Dimension(100, 16));
		lbl_statusInfo.setMaximumSize(new Dimension(1000, 16));
		lbl_statusInfo.setMinimumSize(new Dimension(100, 16));
		lbl_statusInfo.setAlignmentX(0.5f);
		this.add(lbl_statusInfo);
			
		pb_status = new JProgressBar();
		pb_status.setVisible(false);
		pb_status.setStringPainted(true);
		this.add(pb_status);
		this.add(chk_liveupdate);		
		
		this.add(label_1);
		
		lbl_connectStatus.setPreferredSize(new Dimension(30, 21));
		lbl_connectStatus.setMaximumSize(new Dimension(30, 21));
		lbl_connectStatus.setMinimumSize(new Dimension(21, 21));
		lbl_connectStatus.setAlignmentX(0.5f);
		lbl_connectStatus.setIcon(DISCONNECTED);
		this.add(lbl_connectStatus);
		
		this.add(cmd_mnuStartMessung);		
		//this.add(cmd_mnuStartMessung);
		this.add(cmd_mnuStopMessung);	
		this.add(label_5);
		
		bg_downsample.add(mnu_mean);
		bg_downsample.add(mnu_minMax);
	}
	
	private void setListeners(){
		mnu_neustart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				_main.reset(true);
			}
		});
		mnu_exportToCSV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				_main.data.export(-1, -1, 0);
			}
		});
		mnu_exportMarkedToCSV.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				_main.data.export(_main.data.getMarkedBounds()[0], _main.data.getMarkedBounds()[0], 0);
			}
		});
		
		m_Beenden.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				_main.exit();
			}
		});
		mnu_loadMessung.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				_main.ladeMessung("");
			}
		});
		chk_liveupdate.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				ProgStates.LIVE_UPDATE = chk_liveupdate.isSelected();
			}
		});
		cmd_mnuStartMessung.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {					
					@Override
					public void run() {
						cmd_mnuStartMessung.setEnabled(false);
						_main.connectAndStart();
					}
				});
			}
		});
		cmd_mnuStopMessung.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {					
					@Override
					public void run() {
						_main.stopMeasure();
					}
				});			
			}
		});
		
		mnu_mean.addActionListener(new ActionListener() {		
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {				
					@Override
					public void run() {
						if (mnu_mean.isSelected()){
							ProgStates.RESAMPLEMETHODE = 0;
							doResample();
						}
					}
				});
			}
		});
		mnu_minMax.addActionListener(new ActionListener() {		
			public void actionPerformed(ActionEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {				
					@Override
					public void run() {
						if (mnu_minMax.isSelected()){
							ProgStates.RESAMPLEMETHODE = 1;
							doResample();
						}
					}
				});
			}
		});
		mnu_loadMeasureExportToCSV.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ConvertCDF2CSV cdf2csv = new ConvertCDF2CSV(_main);
				cdf2csv.setVisible(true);
			}
		});

		mnu_calib.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						_main.showCalib();
					}
				});
			}
		});
		
		mnu_changeOffset.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						_main.changeOffset();
					}
				});
			}
		});
	}
	
	private void doResample(){
		_main.data.selectedBoundsUpdated = true;
		_main.data.performResampling(true);
	}
	
	private final JMenu mnu_Graphic					= new JMenu("Graphik");
    private final JRadioButton mnu_mean 			= new JRadioButton("Mittelwertbetrachtung");
    private final JRadioButton mnu_minMax 			= new JRadioButton("Maximalwertbetrachtung");	
   
    
    private final JMenu mnMessung 					= new JMenu("Messung");
    private final JMenuItem mnu_neustart 			= new JMenuItem("Neustart");
    private final JMenuItem mnu_exportToCSV 		= new JMenuItem("Messdaten als CSV exportieren");
    //private final JMenuItem 
    final JCheckBox chk_liveupdate 					= new JCheckBox("Echtzeitansicht   ");
    private final JMenuItem m_Beenden 				= new JMenuItem("Beenden");
    private final JMenu m_Datei 					= new JMenu("Datei");
    private final JSeparator separator 				= new JSeparator();
    private final JMenuItem mnu_loadMessung			= new JMenuItem("lade Messung");
    private final JLabel lbl_statusInfo 			= new JLabel("#Status#");
    private final JLabel lbl_connectStatus			= new JLabel();
    public final JButton cmd_mnuStartMessung 		= new JButton("Messung starten");
    public final JButton cmd_mnuStopMessung 		= new JButton("Messung beenden");
    private final JLabel label_5 					= new JLabel("          ");
    public JProgressBar pb_status;
	private final JLabel label_1 = new JLabel("   ");
	private final ButtonGroup bg_downsample 		= new ButtonGroup();
	private final JMenuItem mnu_exportMarkedToCSV = new JMenuItem("markierte Messdaten als CSV exportieren");
	private final JMenuItem mnu_loadMeasureExportToCSV = new JMenuItem("Messungen laden und als CSV konvertieren");
	private final JMenuItem mnu_calib = new JMenuItem("Kalibrierung");
	private final JMenuItem mnu_changeOffset = new JMenuItem("korrigiere Offset Strom / Spannung");
	private final JSeparator separator_2 = new JSeparator();

}
