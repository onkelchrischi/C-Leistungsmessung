package frontend;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.Scrollbar;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.GlyphVector;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

import measuring.MeasureType;
import measuring.Messdaten;
import measuring.State;
import program.ProgStates;
import tools.MyFile;
import tools.MyMath;
import tools.Power;
import tools.Range;
import tools.Statistics;
import tools.UI;

/**
 * JPanel zur graphischen Darstellung der Messwerte
 * TODO skalierung bei hohen zoomstufen fehlerhaft (es wird ein Wert zu viel angezeigt)
 * Tooltip Anzeige Optimieren
 * @author Christoph
 *
 */
public class MyChart extends JPanel implements  AdjustmentListener{
		
	private static final long serialVersionUID = 1L;										//VersionUID des Panels
	private static final String animationImg = "res" + File.separator + "animation.gif";	//Bild für Animation bei disabled live Darstellung
	private static final int jumpDX = 10;													//Sprungweite ei Pfeltasten hoch und runter
	private final int _yOffset = 40;														//unterer Rand des Koordinatensystems
	private int _xOffset = 50;														//linker Rand des Koordinatensystems
	private int _x2Offset = 50;														//rechter Rand des Koordinatensystems
    private final Color[] _c_lines = {tools.MyColors.DARKBLUE,Color.MAGENTA,tools.MyColors.ORANGE};	//Farben der Graphen
    private final float _yScale = 1;														//globale Skalierung des Graphen 
    private boolean _showLines = true;														//flag für die Anzeige der Gitternetzlinien
	private boolean _showSelectetPoint = true;												//flag für die aktuelle Punktanzeige
	private boolean _showStateChanges = true;												//flag für die Anzeige der Statechanges
	private float _xInc = 0;																//globale Skalierung der X-Achse (Von Fenstergröße abhängig)
	private boolean _wasZoomed = false;														//flag für Zoomstatus
	private int _markX[] = {-1,-1};															//Start und Endposion der Markierung in Pixel auf dem Panel
	private boolean _doMark = false;														//flag für Markierungsanzeige
	private boolean _LOCK = false;															//flag um komplexe Zeichnungen zu sperren
	private boolean[] _showGraphen = {true};												//flagArray zur Anzeige der einzelnen Graphen
	private int _p_heigth = 0;																//Panelhöhe
	private int _p_width = 0;																//Panelbreite
	private int chkWidth;																	//checkboxbreite
	private int _anzGraphen = 0;															//Anzahl der zu zeichnenden Graphen
	private boolean enableDownsampling = true;												//flag für aktives downsampling
	private boolean drawNewImage = true;													//flag um erneutes Zeichnen der Graphen zu steuern
	private boolean showToolTipValue = true;												//flag, um den aktuellen Wert per Tooltip zu zeigen
	private Object[][] markedPoint = {{0,0},{0,0},{0,0}};									//Daten des markierten Punktes
																							//[0][x/y] - MeasureType
																							//[1][x/y] - xy Punkt im Graphen
																							//[2][x/y] - xy Wert der Daten
	
	private Gui _gui;
	private Messdaten _data;																//Instanz der Messdatenklasse
	private final JPopupMenu context  			= new JPopupMenu();							//Popupmenue für Rechtsklick
    private ArrayList<JCheckBox> chk_graphen 	= new ArrayList<JCheckBox>();				//Array für Checkboxen zur Aktivierung der Graphen
    private final Scrollbar sb_scroll  			= new Scrollbar();							//Scrollbar
    private final int scrollBarHeigth			= 17;										//Scrollbarhöhe
    private long zoomScrollFreeze				= 200;										//min 200 Millisekunden Zoom und Scroll abstand zur Berechnung
    private long lastZoomScroll					= System.currentTimeMillis();				//letzter Zoom- oder Scrollbefehl 
    private boolean zoomFinished 				= true;										//Zoomen oder Verschieben fertig berechnet
    
	private BasicStroke defaultStroke = new BasicStroke(1.f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);	//Standart Strich
	private BasicStroke  coordinateStroke = new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);	//Koordinatenstrich
	private BasicStroke  axisStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);		//Koordinaten Achsenstrich
    BufferedImage img;																		//Bild für Graphenzeichnung 

	private int _selectedArea = -1;															//selektierter Bereich
	private float _showScale = 1;															//Skalierung der X-Achse, bei laufender Messung wird diese zur 
	private boolean useDownsampling = false;												//Minimierung der zu zeichnenden Daten benutzt
	
	private boolean enableGraphic = true;
	private boolean isDragging = false;
	private JPanel myPanel = this;
	@SuppressWarnings("unchecked")
	private List<Float>[] miniList = new ArrayList[3];
	
	//Verhalten bei Mausbewegungen
	private long mouseLastMoved = 0;			//letzte Mausbewegung
	private long mouseMovedUpdateTime = 50;		//Gui erst nach 50 ms ruhe neuzeichnen
	private int[] lastMousePosition = {0,0};
	private boolean mouseMoved = false;
	private boolean keyMoved = false;
	private int zoomSteps = 0;					//Zommstufen beim schnellscrollen
	private final int scrollTimeout = 100;		//200 ms scrolltimeout
	private long lastScrollTime = 0;
	private float[] drawMax = new float[3];
	private float[] drawMin = new float[3];
	
	public static int SHOWN_DX = 1;
	
	{
		new Thread(){
			public void run(){
				while (true){
					doRepaint(true);
					try {
						sleep(500);
					} catch (InterruptedException e) {}
				}
			}
		}.start();
	}
	
	private Runnable checkMouseMoving = new Runnable() {	
		@Override
		public void run() {
			while (true){
				try{
					Thread.sleep(20);

					if (_LOCK || _data.getSize()==0 || keyMoved) continue;		//Wenn Messung läuft return

					int x = lastMousePosition[0];
					if (lastMousePosition[0] < _xOffset){
						x = _xOffset;
					}else if (lastMousePosition[0] > _p_width - _x2Offset){
						x = _p_width - _x2Offset;
					}
										
					long now = System.currentTimeMillis();
					//Überprüfe Mousemove
					if (now - mouseLastMoved > mouseMovedUpdateTime && mouseMoved ){
						//setze ausgewählten Punkt
						mouseMoved = false;
						int index = getIndexFromMousPosition(x,true);
						
						_data.setActualIndex(index);
						
						markedPoint = getNearestGraphFromMouseIndex(index,lastMousePosition);
						
						if (showToolTipValue){
							if ((Integer) markedPoint[0][0] > 0 && (Integer)markedPoint[0][1] >= 0){
								float val = (Float) markedPoint[2][1];
								String graphName = _data.getNamesOfData()[(Integer)markedPoint[0][1]];
								String unit = "";
								
								if (_data.getDaten((Integer)markedPoint[0][1]).getType() == MeasureType.CURRENT){
									unit = Range.MILLIAMPERE.unit;
									val = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, val);
									val = MyMath.round(val,3);
								}else if (_data.getDaten((Integer)markedPoint[0][1]).getType() == MeasureType.VOLTAGE){
									unit = Messdaten.R_VOLTAGE.unit;
									val = MyMath.round(val,3);
								}else if (_data.getDaten((Integer)markedPoint[0][1]).getType() == MeasureType.POWER){
									unit = Range.MILLIWATT.unit;
									val = MyMath.round(Range.convertFromTo(Range.MICROWATT, Range.MILLIWATT, val),3);
								}
		
								String tt = "<html>" + graphName + ":<br>" + "Wert: " + String.valueOf(val) + " " + unit;			
								tt += "</html>";								
								MyChart.this.setToolTipText(tt);
							}else{
								 MyChart.this.setToolTipText(null);
							}
						}else{
							MyChart.this.setToolTipText(null);
						}
												
					}else{
						markedPoint[1][0] = x;
						markedPoint[1][1] = 0;
					}
					
					//Überprüfe Mousescroll 
					if (now - lastScrollTime > scrollTimeout && zoomFinished && Math.abs(zoomSteps) > 0){
						performZoom(x);
						zoomSteps = 0;
						setScrollbar(_data.getSelectedBounds()[0]);
					}else{
						repaint();
					}
				} catch (Exception e) {
					if (ProgStates.printError) e.printStackTrace();
					MyFile.saveError(e);
				}
				
			}
		}
	};	
	private Thread updateMouseMoving = new Thread(checkMouseMoving);
	
	/**
	 * Konstruktor des Panels
	 * Übergibt eine Instanz der Messdaten
	 * @param data Messdaten
	 */
	public MyChart(Messdaten data, Gui gui){
		super();
		_data = data;
		_p_heigth = getHeight();
		_p_width = getWidth();
		_gui = gui;
		final MyChart p = this;
		
		//initialisiere Mouselistener
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
				if (isDragging){
					isDragging = false;
					setMarkX1(arg0.getX());
				}else{
					_markX[0] = _p_width;
				}
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				if (_LOCK) return;
				mouseMoved = true;
				if (_markX[0] < arg0.getX() && _markX[1] >= arg0.getX()) return;	//Klick in Markierten Bereich
				if (arg0.getButton() == MouseEvent.BUTTON1){
					setMarkX0(arg0.getX());						//setze Markierungsstartpunkt
				}
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				p.requestFocusInWindow();			//erfrage Focus
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if (_data.getSize() == 0 || _LOCK) return;
				switch (arg0.getButton()){
					//Markiere Bereich oder zoome in den Bereich wenn er ausgewählt ist
					case MouseEvent.BUTTON1:
						//ZoomToggle
						if (arg0.isControlDown()){
							ProgStates.fixYAxis = !ProgStates.fixYAxis;
							_gui.setYAxisFix(ProgStates.fixYAxis);
							return;
						}
											
						if (_data.getMarkedSize() > 0
								&& _data.getMarkedBounds()[0] < (Integer) markedPoint[2][0]
								&& _data.getMarkedBounds()[1] > (Integer) markedPoint[2][0]
								&& _selectedArea < 0)
						{
							_data.copyMarkedBoundsToSelectedBounds();
							zoomFinished = false;
							updateDrawableGraph(_data.getMarkedBounds()[0]);
							setScrollbar(_data.getMarkedBounds()[0]);
							zoomFinished = true;
						}else{
							resetMark();
							markSelectedArea(arg0.getX());
						}
						break;
					//Zeige Popupmenü
					case MouseEvent.BUTTON3:
						showPopupMenu(arg0.getComponent(),arg0.getX(),arg0.getY());
						break;
				}	
			}
		});
		//initialisiere MouseMotionListener
		addMouseMotionListener(new MouseMotionListener() {	
			@Override
			public void mouseMoved(MouseEvent event) {				
				mouseLastMoved = System.currentTimeMillis();
				lastMousePosition[0] = event.getX();
				lastMousePosition[1] = event.getY();	
				mouseMoved = true;
				keyMoved = false;
			}
			
			@Override
			public void mouseDragged(MouseEvent arg0) {
				//Markiere Daten wenn Maus gedragged wird
				if (_LOCK) return;
				isDragging = true;
				_selectedArea = -1;
				mark(true);
				setMarkX1(arg0.getX());
			}
		});
		//initialisiere MouseWheelListener
		addMouseWheelListener(new MouseWheelListener() {			
			@Override
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				if (arg0.isControlDown()){		//zoom
					//erst zoomen wenn Scrollen aufgehört (Timeout = 200ms)
					zoomSteps += arg0.getWheelRotation();
					lastScrollTime = System.currentTimeMillis();
					
					//update miniwindow
				}else{			//Achsen Bewegung				
					int size = _data.getSelectedSize();
					if (size + 1 == _data.getSize()) return;
					//Rand prüfen
					if ((arg0.getWheelRotation() < 0 && _data.getSelectedBounds()[1] == _data.getSize()) ||
						(arg0.getWheelRotation() > 0 && _data.getSelectedBounds()[0] == 0)){
						return;
					}
					float _10pc = (size * 0.1F) * -1 * arg0.getWheelRotation();
					int x0 = (int) (_data.getSelectedBounds()[0] + _10pc);
					updateDrawableGraph(x0);
					setScrollbar(_data.getSelectedBounds()[0]);
					//System.out.println("mousemove, scrollbar: " + sb_scroll.getValue());
				}
			}
		});	
		//initialisiere ComponentenListener
		addComponentListener(new ComponentListener() {
			
			@Override
			public void componentShown(ComponentEvent arg0) {}
			
			@Override
			public void componentResized(ComponentEvent arg0) {
				//Wenn Fenstergröße Verändert passe Parameter an
				if (getHeight() == 0 || getWidth() == 0) return;
				_p_heigth = getHeight();
				_p_width = getWidth();
				Messdaten.DOWNSAMPLINGSIZE = _p_width - _xOffset - _x2Offset;
				if (sb_scroll != null){
					sb_scroll.setBounds(0, _p_heigth-scrollBarHeigth, _p_width, scrollBarHeigth);
				}
				int i= 0;
				for (JCheckBox box : chk_graphen){
					box.setBounds(getWidth() - chkWidth - 35 - _x2Offset, 1 + (i * 20), chkWidth + 35, 20);	
					i++;
				}
				img = new BufferedImage(_p_width, _p_heigth, BufferedImage.TYPE_INT_RGB);
			}
			
			@Override
			public void componentMoved(ComponentEvent arg0) {}
			
			@Override
			public void componentHidden(ComponentEvent arg0) {}
		});
		//initialisiere KeyListener
		addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent arg0) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				int dx = 0;
				//Wenn Pfeil nach links gedrückt, gehe eine Position zurück
				
				if (e.getKeyCode() == KeyEvent.VK_LEFT){
					if (_data.getActualIndex() > 0) dx = -1;
					keyMoved = true;
				}
				//Wenn Pfeil nach rechts gedrückt, gehe eine Position vor
				else if (e.getKeyCode() == KeyEvent.VK_RIGHT){
					if (_data.getActualIndex() < _data.getSize() - 1) dx = 1;			
					keyMoved = true;
				}
				//Wenn Pfeil nach unten gedrückt, gehe einen Sprung zurück
				else if (e.getKeyCode() == KeyEvent.VK_DOWN){
					if (_data.getActualIndex() > jumpDX) dx = -jumpDX;		
					keyMoved = true;
				}
				//Wenn Pfeil nach oben gedrückt, gehe einen Sprung vor
				else if (e.getKeyCode() == KeyEvent.VK_UP){
					if (_data.getActualIndex() < _data.getSize() - jumpDX - 1) dx = jumpDX;		
					keyMoved = true;
				}else if (e.getKeyCode() == KeyEvent.VK_S){
					//säubere Messung
					//_data.cleanData();
				}else if (e.getKeyChar() == 'a'){
					System.out.println("Anzahl der Messdaten: " +_data.getSize());
				}else if (e.getKeyChar() == 'i'){
					System.out.println("========================================================================");
					System.out.println("Informationen:");
					System.out.println("aktueller Index: " + _data.getActualIndex());
					System.out.println("aktuelle Grenzen: x[0] = " + _data.getSelectedBounds()[0] + ", x[1] = " + _data.getSelectedBounds()[1] + ", breite: " + _data.getSelectedSize());
					System.out.println("aktuelle Scrollbarposition: " + sb_scroll.getValue());
					System.out.println("Strom max: " + _data.getSelectedMax(0, false) + ", Strom min: " + _data.getSelectedMin(0, false));
					System.out.println("========================================================================");
				}else if (e.getKeyCode() == KeyEvent.VK_F5){
					doRepaint(true);
					return;
				}
				else {
					return;
				}
				
				_data.setActualIndex(_data.getActualIndex() + dx);
				if (_data.getActualIndex() >= _data.getSelectedBounds()[1] - 1 || _data.getActualIndex() < _data.getSelectedBounds()[0]){
					sb_scroll.setValue(sb_scroll.getValue() + dx);
				}
				
				markedPoint[1][0] = (int)((_data.getActualIndex() - _data.getSelectedBounds()[0]) * getXScale(_data.getSelectedSize() - 1) + _xOffset);
				repaint();
			}
		});
		
		ToolTipManager.sharedInstance().setInitialDelay(0);
		setToolTipText(null);
		setLayout(null);	
		updateMouseMoving.setName("MouseUpdateThread");
		updateMouseMoving.start();
	}
	
	
    /**
     * Initiallisiert das PopupMenü
     */
    public void setPopupMenu(){
    	
		//Eintrag, um die Statechanges zu zeigen oder zu verbergen
		JMenuItem mnu_showStateChanges = new JMenuItem();
		mnu_showStateChanges.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				_showStateChanges = !_showStateChanges;
				repaint();
			}
		});
		if (_showStateChanges) mnu_showStateChanges.setText("verberge Statechanges");
		else mnu_showStateChanges.setText("zeige Statechanges");

		//Eintrag, um das Downsampling zu (de)aktivieren
		JMenuItem mnu_enableDownSampling = new JMenuItem();
		mnu_enableDownSampling.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				enableDownsampling = !enableDownsampling;
				repaint();
			}
		});
		if (enableDownsampling) mnu_enableDownSampling.setText("deaktiviere Downsampling");
		else mnu_enableDownSampling.setText("aktiviere Downsampling");

		//Eintrag, um zu 100% zu zoomen
		JMenuItem mnu_zoomOut = new JMenuItem("Zoom zu 100%");
		mnu_zoomOut.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent arg0){
				SwingUtilities.invokeLater(new Runnable() {					
					@Override
					public void run() {
						resetZoom();			
					}
				});
			}
		});
		
		//Eintrag in einen ausgewählten Bereich zu zoomen (Bereich zwischen zwei Statechanges)
		JMenuItem mnu_zoomToSelectedArea = new JMenuItem("Zoome in den ausgewählten Bereich");
		mnu_zoomToSelectedArea.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent arg0){
				_data.copyMarkedBoundsToSelectedBounds();
				setScrollbar(_data.getSelectedBounds()[0]);
				_selectedArea = -1;
			}
		});
		
		//Eintrag, um den aktuellen Werte Zeiger anzuzeigen oder zu verbergen
		JMenuItem mnu_showSelectedPoint = new JMenuItem();
		mnu_showSelectedPoint.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				_showSelectetPoint = !_showSelectetPoint;
				repaint();
			}
		});
		if (_showLines) mnu_showSelectedPoint.setText("zeige aktuellen Punkt");
		else mnu_showSelectedPoint.setText("verberge aktuellen Punkt");

		//Eintrag, um den aktuellen Wert per Tooltip anzuzeigen oder zu verbergen
		JMenuItem mnu_showTooltipValue = new JMenuItem();
		mnu_showTooltipValue.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent arg0) {
				showToolTipValue = !showToolTipValue;
			}
		});
		if (!showToolTipValue) mnu_showTooltipValue.setText("zeige Wert als Tooltip");
		else mnu_showTooltipValue.setText("verberge Wert als Tooltip");

		context.removeAll();
		context.add(mnu_enableDownSampling);
		context.add(mnu_showSelectedPoint);
		context.add(mnu_showStateChanges);
		context.add(mnu_showTooltipValue);
		
		if (_wasZoomed) context.add(mnu_zoomOut);
		if (_selectedArea > 0) context.add(mnu_zoomToSelectedArea);
		
		this.add(context);
    }
        
	/**
	 * setzt die Sichtbarkeit der Checkboxen
	 * @param visible true setzt Sichtbar
	 */
	public void hideCheckboxes(boolean visible){
		for (int i=0;i<chk_graphen.size();i++){
			chk_graphen.get(i).setVisible(false);
			for (int j=0;j<_data.getAnz();j++){
				if (chk_graphen.get(i).getText().equalsIgnoreCase(_data.getNamesOfData()[j]) && visible){
					chk_graphen.get(i).setVisible(visible);
					continue;
				}
			}
			
		}
		for (JCheckBox box : chk_graphen){
			box.setVisible(visible);
		}
	}
	
	/**
	 * Setzt die Anzahl und Namen der zu zeichnenden Graphen
	 * @param names Namen der zu zeichnenden Graphen
	 */
	public void setAnzGraphen(int anz, String[] names){
		_showGraphen = null;
		_anzGraphen = names.length;
		_showGraphen = new boolean[names.length];
		chkWidth = UI.getMaxStringLength(names, getFontMetrics(getFont()));	//Berechne Stringbreite
		
		//Wenn Checkbox noch nicht erzeugt wurde, erzeuge diese und platziere sie auf dem Panel
		if (chk_graphen.size() < names.length){
			for (int i = 0; i < names.length; i++){
				if (chk_graphen.size() >= names.length || checkBoxExists(names[i])) continue;
				final JCheckBox box = new JCheckBox(names[i]);
				chk_graphen.add(box);
				add(box);
			}
		}
		int i=0;
		
		//Verändere Attribute der "Leer-Checkboxen" (Name, Position, Selected, Sichtbarkeit)
		for (final JCheckBox box : chk_graphen){
			final int index = i;
			boolean containsBox = false;
			for (String name : names){
				if (name.equalsIgnoreCase(box.getText())){
					containsBox = true;
					break;
				}
			}
			if (!containsBox){
				box.setVisible(false);
				continue;
			}
			box.setVisible(true);
			if (i >= chk_graphen.size()) return;
			if (box.getActionListeners().length == 0){
				box.addActionListener(new ActionListener() {				
					@Override
					public void actionPerformed(ActionEvent arg0) {
						_showGraphen[index] = box.isSelected();
						_data.getDaten(index).isShown = box.isSelected();
					}
				});
			}
			box.setBackground(Color.white);
			box.setBounds(getWidth() - chkWidth - 35 - _x2Offset, 1 + (i * 20), chkWidth + 35, 20);
			box.setForeground(_c_lines[i]);
			if (box.getText().equals(MeasureType.CURRENT.name)) box.setSelected(true);
			//if (box.getText().equals(MeasureType.VOLTAGE.name) && ProgStates.ADC_MODE == ProgStates.ADC_AD7765_BOARD) box.setSelected(true);			
			_showGraphen[i] = box.isSelected();
			_data.getDaten(i).isShown = _showGraphen[i];
			i++;
		}
		
		//Initialisiere Scrollbar
		sb_scroll.setBounds(0, _p_heigth-scrollBarHeigth, _p_width, scrollBarHeigth);
		sb_scroll.setMaximum(0);
		sb_scroll.setMinimum(0);
		sb_scroll.setValue(0);
		sb_scroll.setOrientation(JScrollBar.HORIZONTAL);
		sb_scroll.addAdjustmentListener(this);
		add(sb_scroll);	//Füge Scrollbar dem Panel hinzu
		drawNewImage = true;
	}

	public void setEnable(boolean b){
		enableGraphic = b;
	}
	
	/**
	 * Überprüfe, ob es die Checkbox schon gibt. Erforderlich bei hinzufügen weiterer Graphen
	 * @param name	Name des Graphen
	 * @return	true, wenn Checkbox bereits vorhanden
	 */
	private boolean checkBoxExists(String name){
		for (int i=0;i<chk_graphen.size();i++){
			if (chk_graphen.get(i).getText().equalsIgnoreCase(name)) return true;
			
		}
		return false;
	}
	
	/**
	 * Setzt eine aktuelle Messdateninstanz
	 * @param daten	Messdaten
	 */
	public void setMessdaten(Messdaten daten){
		this._data = daten;
	}
	
	/**
	 * Setzt den Endpunkt bei einer Markierung
	 * @param x	X-Position des Endpunktes
	 */
    public void setMarkX1(int x){
    	if (x > _p_width-_x2Offset) _markX[1] = _p_width-_x2Offset;
    	else if (x < _xOffset) _markX[1] = _xOffset +1;
    	else _markX[1] = x;
    	if (_markX[1] < _markX[0]){
    		_markX[1] = _markX[0];
    		_markX[0] = x;
    	}
    	int x1 = getIndexFromMousPosition(_markX[0], true);
    	int x2 = getIndexFromMousPosition(_markX[1], true);
    	if (!isDragging){
    		_data.setMarkBounds(x1, x2);
    	}
     	repaint();
    }

    /**
     * Setzt den Startpunkt der Markierung
     * @param x	X-Position des Startpounktes
     */
    public void setMarkX0(int x){
    	if (x < _xOffset) _markX[0] = _xOffset;
    	else if (x > _p_width-_x2Offset) _markX[0] = _p_width - _x2Offset - 1;
    	else _markX[0] = x;
    }
    
    /**
     * resettet die Markierungsgrenzen
     */
    public void clearMark(){
    	_markX[0] = _markX[1] = -1;
    }
      
    /**
     * zeige PopupMenü
     * @param c	Component
     * @param x x-Position
     * @param y y-Position
     */
    public void showPopupMenu(Component c, int x, int y){
    	setPopupMenu();
    	context.show(c, x, y);
    }
    
    /**
     * Ändere den Sichtbaren Bereich der Graphen, wenn dieScrollbar bewegt wurde
     * @param val aktueller Wert
     */
    public void updateDrawableGraph(int val){
    	val -= 1;
    	markedPoint[0][0] = 0;
    	markedPoint[1][0] = -1;
    	_doMark = false;
    	setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    	if (zoomFinished){
    		_data.setSelectedBounds(val, val + _data.getSelectedSize());
    		_data.performResampling(false);
    	}else{
    		_data.performResampling(true);
    		float factor = 100 - (float)_data.getSelectedSize() * 100 / (float)_data.getSize();
    		_gui.setZoom(factor);
    	}
    	setCursor(Cursor.getDefaultCursor());
    }
    
    /**
     * setzte den Zoom zu 100%
     */
	public void resetZoom(){
		_wasZoomed = false;
		_data.setSelectedBounds(0, _data.getSize()-1);
		_data.performResampling(true);
		sb_scroll.setValue(0);
		sb_scroll.setMaximum(0);
		sb_scroll.setMinimum(0);
		sb_scroll.setVisibleAmount(_data.getSize());
		
		for (int i=0;i<3;i++){
			drawMax[i] = _data.getDaten(i).getMax();
			drawMin[i] = _data.getDaten(i).getMin();			
		}
		_gui.setZoom(0);
    }
	
	/**
	 * lösche Markierungen
	 */
	public void resetMark(){
		_doMark = false;
		_data.setMarkBounds(0, 0);
		repaint();
	}
    
	/**
	 * ermittle aktuellen Index basierend auf der Mausposition
	 * @param x	x-Position
	 * @param withOffest mit Offset bei zoom
	 * @return gibt den aktuellen Index zurück
	 */
    public int getIndexFromMousPosition(int x, boolean withOffest){
    	float rel_x = (float)(x - _xOffset) / (float) (_p_width - _x2Offset - _xOffset);
    	int shownValues = _data.getSelectedSize();
    	int posInData = (int) (rel_x * shownValues);
    	if (withOffest) posInData += _data.getSelectedBounds()[0];
    	return posInData;
    }
        
    /**
     * ermittelt den nächsten Wert und Graphen abhängig von der Mausposition und dem Index
     * @param index	des aktuellen Werts
     * @param ypos	Y-Position des Mauszeigers
     * @return	Array mit Wert und GraphenID zurück => {Wert,GraphID}
     */
	private Object[][] getNearestGraphFromMouseIndex(int index, int[] mousePosition){
    	int dy = _p_heigth;
    	Object[][] res = new Object[3][2];
    	res[0][0] = 0;						//Wert nicht anzeigen
		res[1][0] = mousePosition[0];	//X-Position des Punktes
    	res[2][0] = index;			//X-Wert des Punktes
    	
    	//ypos = _p_heigth - ypos;
    	for (int i=0;i<_anzGraphen;i++){
    		if (!_showGraphen[i]) continue;
    		float val = _data.getDaten(i).get(index);
    		float max = _data.getSelectedMax(i, true);
    		float min = _data.getSelectedMin(i, true);

	    	float yscale = (float)(_p_heigth - _yOffset) / ((max - min) * _yScale);
			int y = (int)(_p_heigth - yscale * (val - min) - _yOffset);
    		if (Math.abs(y - mousePosition[1]) < dy && Math.abs(y - mousePosition[1]) < 30){	//beachte nur Positionen die max 30px entfernt sind
    			dy = Math.abs(y - mousePosition[1]);
    			res[0][0] = 1;				//Wert Anzeigen
    			res[0][1] = i;				//ID des Graphen
       			res[1][1] = y;				//Y-Position des Punktes
    			res[2][1] = val;			//Y-Wert des Punktes
    		}
    	}
    	
       	return res;
    }
    
	private void performZoom(int x){
		int size = _data.getSize();
		if (zoomSteps == 0 || _LOCK){
			zoomFinished = true;
			return;		//Wenn kein Zoom stattgefunden, return
		}

		//System.out.println("selectedBounds before zoom: " +_data.getSelectedBounds()[0] + " " + _data.getSelectedBounds()[1] );
		int index = getIndexFromMousPosition(x, true);

		int leftBound = _data.getSelectedBounds()[0];				//ermittle linke und rechte Grenze
		int rightBound = _data.getSelectedBounds()[1];
			
		if (zoomSteps > 0 && !_wasZoomed){
			zoomFinished = true;
			return;		//Wenn voll rausgezoomt wurde abbrechen
		}
		
		zoomFinished = false;
		keyMoved = false;
		
		int dx = (int) (_data.getSelectedSize() * 0.25);
		if (dx == 0) dx = 2;
		
		int dbounds = rightBound - leftBound;
		if (dbounds <= 40 && zoomSteps < 0){
			zoomFinished = true;
			return;
		}
		
		//Wenn Mausrad nach oben gescrollt wird, reinzoomen
		if (zoomSteps < 0){
			for (int i=0;i<Math.abs(zoomSteps);i++){
				leftBound = index - dx;
				rightBound = index + dx;
			}
		}
		//Ansonsten rauszoomen
		else if (zoomSteps > 0){
			for (int i=0;i<Math.abs(zoomSteps);i++){
				leftBound -= dx;
				rightBound += dx;
			}
		}else{
			zoomFinished = true;
			return;
		}
		if (leftBound < 0){		//wenn am linken Rand angekommen, rechte Grenze erweitern
			rightBound = rightBound - leftBound;
			leftBound = 0;
		}
		//Wenn rechte Grenze zu nah an data.size erweitere linke Grenze
		if (rightBound > size){
			leftBound = leftBound + size - rightBound;
			rightBound = size - 1;
		}	
		
		//Wenn linke und rechte Grenze 0 und data.size überschreiten, ist die originalgröße erreicht
		boolean reset = false;
		if (leftBound < 0 && rightBound == size -1){
			leftBound = 0;
		}
		if (leftBound >= 0 && rightBound > size){
			rightBound = size -1;
		}
		if (leftBound >= 0 && rightBound < size){
			_data.setMarkBounds(leftBound, rightBound);					
		}else{
			_data.setMarkBounds(0, size - 1);
			reset = true;
		}
		_data.copyMarkedBoundsToSelectedBounds();
		if (!reset) setScrollbar(_data.getMarkedBounds()[0]);
		else resetZoom();
		
		updateDrawableGraph(_data.getSelectedBounds()[0]);
		//System.out.println("selectedBounds after zoom: " +_data.getSelectedBounds()[0] + " " + _data.getSelectedBounds()[1] );
		
		float factor = 100 - (float)((rightBound - leftBound) * 100) / size;
		_gui.setZoom(factor);

		//Maus mittig Positionieren
		try {
			Robot r = new Robot(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice());
			r.mouseMove(myPanel.getLocationOnScreen().x+_p_width/2, myPanel.getLocationOnScreen().y+_p_heigth /2);
		} catch (HeadlessException e) {
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		} catch (AWTException e) {					
			if (ProgStates.printError) e.printStackTrace();
			MyFile.saveError(e);
		}
	}
	
	
    /**
     * zoomt in den ausgewählten bereich und passt die Scrollbar an
     * @param x aktueller index, setzt den Scrollbalken an die passende Stelle
     */
    public void setScrollbar(int x){
    	if (_data.getSelectedSize() == _data.getSize()){
    		resetZoom();
    		return;
    	}
		sb_scroll.setMaximum(_data.getSize());
		sb_scroll.setVisibleAmount(_data.getSelectedSize());
		int in = (int) (_data.getSelectedSize() * 0.1);
		int bIn = (int) (_data.getSelectedSize() * 0.95);
		sb_scroll.setUnitIncrement(in);
		sb_scroll.setBlockIncrement(bIn);
		sb_scroll.setValue(x);
		_doMark = false;
		_wasZoomed = true;
		resetMark();
    }
    
    /**
     * Markiert den ausgewählten Bereich zwischen zwei Statechanges abhängig des aktuellen Indexes
     * @param x aktueller Index
     */
    private void markSelectedArea(int x){
    	if (_data == null || _data.isEmpty() || _data.getStateChanges().size() < 2) return;
    	
    	clearMark();		//lösche aktuelle Markierung
    	
    	//setze ausgewählten bereich
    	_doMark = false;
		float d_x = (x - _xOffset) / _xInc + _data.getSelectedBounds()[0];
		if (d_x < 0) d_x = 1;

		int lastSelectedArea = _selectedArea;
    	_selectedArea = -1;
		int k = 0;
		for (State val : _data.getStateChanges()){
			if (d_x < val.getIndex()){
				break;
			}
			k++;
		}
		_selectedArea = k;
		//Wenn Bereich vorher selektiert wurde, Selektion aufheben
		if (_selectedArea == lastSelectedArea){		
			_selectedArea = -1;
			return;
		}
		int x0 = 0;
		int x1 = _data.getSize() -1;;
		if (_selectedArea > 0){
			if (_selectedArea >= _data.getStateChanges().size()) x0 = _data.getStateChanges().get(_data.getStateChanges().size()-1).getIndex();
			else x0 = _data.getStateChanges().get(_selectedArea).getIndex();
		}
		if (_selectedArea < _data.getStateChanges().size() -1){
			x1 = _data.getStateChanges().get(_selectedArea + 1).getIndex();
		}
		_data.setMarkBounds(x0, x1);
		repaint();
		return;		
    }
    
    /**
     * setzt flag zum markieren
     * @param b true - markiert ausgewählten bereich
     */
	public void mark(boolean b){
		_doMark = b;
	}
	
	/**
	 * gibt den marked-Flag zurück
	 * @return true, wenn markierung aktiv
	 */
	public boolean isMarked(){
		return _doMark;
	}

	/**
	 * setzt das LOCK-Flag, in der Regel, wenn eine Messung läuft
	 * @param b	true: lock
	 */
	public void setLOCK(boolean b){
		_LOCK = b;
		if (!b) {
			repaint();
			for (JCheckBox box : chk_graphen){
				box.setEnabled(!b);
			}
		}
	}
	
	/**
	 * setzt zoom und Markierungen zurück
	 */
	public void clearAll(){
		_wasZoomed = false;
		_markX = new int[] {-1,-1};
		_doMark = false;
		_LOCK = false;
		for (int i=0; i<3;i++) miniList[i] = null;
	}

	/**
	 * zeichnen das Panel neu
	 */
	@Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try{
	        if (!enableGraphic) return;
	        Graphics2D graph2D = (Graphics2D) g;
	        
	        g = img.getGraphics();					//graphic vom Temporären Bild
	        
	        Graphics2D g2 = (Graphics2D)g;
	        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	        
	        setBackground(Color.white);
	        g2.setStroke(defaultStroke);
	        
	        //Wenn Liveupdate deaktiviert und Lock gesetzt ist (Messung läuft) zeige Messanimation
	        if (!ProgStates.LIVE_UPDATE && _LOCK){
	            g2.setColor(Color.white);  
	            g2.fillRect(0, 0, _p_width, _p_heigth);  
	        	g2.setPaint(Color.RED);
	        	Font f = new Font( Font.SERIF, Font.BOLD, 48 );
	        	g2.setFont(f);
	        	String s = "live update ausgeschaltet!";
	            GlyphVector gv = g2.getFont().createGlyphVector(g2.getFontRenderContext(), s);
	            java.awt.Rectangle rec = gv.getPixelBounds(null, 0, 0);
	            int x = _p_width / 2 - rec.width / 2;
	            int y = _p_heigth / 2 - rec.height / 2;
	        	g2.drawString(s, x, y);
	        	Image img1 = Toolkit.getDefaultToolkit().getImage(animationImg);
	        	g2.drawImage(img1, _p_width / 2 - img1.getWidth(this) / 2 , _p_heigth / 2, this);
	            g2.finalize();
	            
	            graph2D.drawImage(img, 0 , 0, this);
	        	return;
	        }
	  
	        //Wenn keine Daten vorhanden return
	        if (_data == null || _data.isEmpty()){
	        	g.drawString("keine Daten erhalten...", _p_width/2, _p_heigth/2);
	        	return;
	        }
	
	        //Wenn ein neues "Bild" gezeichnet werden soll
	    	if (drawNewImage){
	            g.setColor(Color.white);  
	            g.fillRect(0, 0, _p_width, _p_heigth);  
	
		        g2.setPaint(Color.black);
	
	            //ermittle Datengröße
		    	int size = 0;		    	
		    	if (_wasZoomed){
		    		size = _data.getSelectedSize();
		    	}else{
		    		size = _data.getSize();
		    	}
		    	if (size == 0) return;
		    	
		    	if(_LOCK  && !ProgStates.IS_PERIODIC_MEASURE){	//Wenn feste Messzeit und Messung aktiv
		    		size = (int) (Messdaten.MEASURETIME / Range.convertFromTo(Messdaten.TIMEUNIT, Range.MILLISECOND, Messdaten.DT));		    		
		    	}else if(ProgStates.IS_PERIODIC_MEASURE && _LOCK){
			    	size = _data.getEstimatedSize();	    			
		    	}
		    	
		    	//_xInc = getXScale(_data.getSelectedSize() - 1);
		    	_xInc = getXScale(size - 1);
		    	
		    	useDownsampling = false;
			    if (!_LOCK && _data.getSelectedSize() > (_p_width - _xOffset - _x2Offset) && enableDownsampling){
			    	useDownsampling = true;
			    	size = _data.getDownsampledSize();
			    }else if (_LOCK) useDownsampling = true; 
		    	
		    	//Zeichne X-Achse
		    	if (!_LOCK) drawSkaleX(g2,_data.getSelectedSize() -1);	//Skala nach der Messung
		    	else drawSkaleX(g2,size - 1);		//Skala während der Messung
		    	
			    //highlight Area
			    if (_selectedArea >= 0 && !_LOCK && _data.getStateChanges().size() >= 2){
			    	float xScale = 1;
					        
			        xScale = getXScale(size);
			        
			        int x1 = 0;
			       	int x2 = _data.getSize() -1;
			       	if (x1 < 0) x1 = 0;
			       	if (x2 < 0) x2 = 0;
			       	if (_selectedArea > 0)
				     	x1 = (int) ((_data.getStateChanges().get(_selectedArea - 1).getIndex() - _data.getSelectedBounds()[0]) * xScale);
			       	
			       	if (_selectedArea < _data.getStateChanges().size())
			       		x2 = (int) ((_data.getStateChanges().get(_selectedArea).getIndex() - _data.getSelectedBounds()[0]) * xScale);
			       	
			       	if (x1 < _xOffset) x1 = _xOffset;
			       	else x1 += _xOffset;
			       	
			       	if (x2 > _p_width-_x2Offset) x2 = _p_width-_x2Offset;
			       	else x2 += _xOffset;
			       	
			       	g2.setPaint(Color.yellow);
			       	g2.fillRect(x1, 0, x2 - x1, _p_heigth - _yOffset);
			    }
		    	
		    	//Zeichne die Graphen nacheinander    
		    	Float[] arr_data;
		    	for (int graphID = 0 ; graphID < _anzGraphen; graphID++){
			    	float xScale = 1;
			    	if (!_showGraphen[graphID]) continue;
				    	
					synchronized (_data.getDaten(graphID).DrawableDataSysnc) {
						arr_data = _data.getDrawableData(graphID).toArray(new Float[_data.getDrawableData(graphID).size()]);
					}
					
					float min = 0;
					float max = 0;
					
					if (ProgStates.fixYAxis){
						max = drawMax[graphID];
						min = drawMin[graphID];
					}else{
						max = _data.getSelectedMax(graphID, true);
						min = _data.getSelectedMin(graphID, true);
					}
				    
					if (graphID == 2){
						max = Range.convertFromTo(_data.getDaten(graphID).messbereich, Range.MILLIWATT, max);
						min = Range.convertFromTo(_data.getDaten(graphID).messbereich, Range.MILLIWATT, min);
					}
										
					float yscale = (float)(_p_heigth - _yOffset) / ((max - min) * _yScale);
					drawSkaleY(g2,min, max,yscale,graphID);
	
					if (!_LOCK){
				        if (Messdaten.MEASURETIME > 0){
				        	if (Messdaten.DOWNSAMPLINGSIZE > 0 && useDownsampling){
				        		if (!_LOCK) xScale = getXScale(size);
				        		//if (size < Messdaten.DOWNSAMPLINGSIZE && !_LOCK) xScale = getXScale(size);
				        		else xScale = getXScale(Messdaten.DOWNSAMPLINGSIZE);
				        	}else{
				        		xScale = getXScale(size - 1);
				        	}	
				        }else{
				        	xScale = getXScale(arr_data.length - 1);
				        }
				        _xInc = xScale;
					}
					
					if (_LOCK){
						if (Messdaten.DOWNSAMPLINGSIZE > 0){
							_showScale = 1;
						}else{
							_showScale = size / (_p_width - _x2Offset - _xOffset);
						}
						if (_showScale == 0) _showScale = 1;
					}else{
						_showScale = 1;
					}
					
					synchronized (_data.getDaten(graphID).DrawableDataSysnc) {
			            for(int i = 1; i < arr_data.length / _showScale; i++) {
			            	
			            	float[] x = new float[2];
			            	float[] y = new float[2];
		
			            	float x0 = arr_data[(int) ((i-1)*_showScale)];
			            	float x1 = arr_data[(int) (i*_showScale)];
			            	
							if (graphID == 2){
								x0 = Range.convertFromTo(_data.getDaten(graphID).messbereich, Range.MILLIWATT, x0);
								x1 = Range.convertFromTo(_data.getDaten(graphID).messbereich, Range.MILLIWATT, x1);
							}
			            			            	
			                x[0] = ((i - 1) * _showScale) * xScale + _xOffset;
			                x[1] = i * _showScale * xScale + _xOffset;
			                y[0] = _p_heigth - yscale * (x0 - min) - _yOffset;
			                y[1] = _p_heigth - yscale * (x1 - min) - _yOffset;
			                	     
			                //draw line
			            	g2.setStroke(defaultStroke);
			                if (_data.getSelectedSize() < _p_width/2 && !_LOCK){
			                	BasicStroke stroke2 = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			                	g2.setStroke(stroke2);
			                }
			    	        g2.setPaint(_c_lines[graphID]);
			                g2.draw(new Line2D.Float(x[0],y[0],x[1],y[1]));
			                
			                g2.setStroke(defaultStroke);	                   	
				        }
					
					}
		  
		            //markiere StateChanges
		            if (!_LOCK && _showStateChanges){
		            	BasicStroke stroke2 = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		            	g2.setStroke(stroke2);
		
		            	//Überprüfe, wie viele statechanges zu zeichnen sind. wenn mehr als 50 Changes zu zeichnen sind, zeichne keine
		            	int x0 = _data.getSelectedBounds()[0];
		            	int x1 = _data.getSelectedBounds()[1];
		            	int k = 0;
		            	for (State sc : _data.getStateChanges()){
		            		int index = (int) (sc.getIndex());
		            		if (index > x0 && index < x1) k++;
		            	}
		            	
		            	if (k <= 50){           	
			            	for (State sc : _data.getStateChanges()){
			                	int index = (int) (sc.getIndex());
			                	if (index >= x0 && index <= x1){
			                		float x = (index - x0) * _showScale * (_p_width - _xOffset - _x2Offset) / _data.getSelectedSize() + _xOffset;
			                		if (sc.getType() == State.S_NEW_INTERVAL) g2.setPaint(State.C_NEW_INTERVAL);
			                		if (sc.getType() == State.S_START) g2.setPaint(State.C_START);
			                		if (sc.getType() == State.S_DUMMY) g2.setPaint(State.C_DUMMY);
				                	g2.draw(new Line2D.Float(x, 0, x, _p_heigth - _yOffset));	   
			                	}
			                }
		            	}
		            }
		            
		        }
		        if (!_LOCK && _wasZoomed && ProgStates.drawMiniWindow){
		        	drawMiniWindow(g2, 0, true, 0, 0);
		        }
		        
			    
		        drawNewImage = false;
		        zoomFinished = true;
	    	}
	    	
	    	//zeichnet alle das Bild mit allen Graphen
			graph2D.drawImage(img, 0 , 0, this);
	    	
	        //zeichne aktuellen X und Y Wert
	        if ((Integer)markedPoint[1][0] > 0  && _showSelectetPoint && !_LOCK){
	        	graph2D.setPaint(Color.red);
	        	if((Integer)markedPoint[1][0] < _xOffset ){		//linker Rand
	        		graph2D.drawLine(_xOffset, 0, _xOffset, _p_heigth -_yOffset);
	        	}else if ((Integer)markedPoint[1][0] > _p_width - _x2Offset ){		//rechter Rand
	        		graph2D.drawLine(_p_width - _x2Offset, 0, _p_width - _x2Offset, _p_heigth -_yOffset);
	        	}else if ((Integer)markedPoint[0][0] > 0 && _data.getSelectedSize() < 1000){		//naher Zoom
	            	graph2D.drawLine((Integer)markedPoint[1][0], 0, (Integer)markedPoint[1][0], _p_heigth -_yOffset);
	        	}else{    	
	        		graph2D.drawLine((Integer)markedPoint[1][0], 0, (Integer)markedPoint[1][0], _p_heigth -_yOffset);
	        	}
	        }	        
	
	        //markiere Daten
	        if (_doMark){
	        	graph2D.setPaint(Color.black);
	        	graph2D.setXORMode(Color.white);
	        	graph2D.fillRect(_markX[0], 0, _markX[1]-_markX[0], _p_heigth - _yOffset); 
	        	graph2D.setPaintMode();
	        }     
	        g2.setStroke(defaultStroke);
        }catch(Exception e){
        	e.printStackTrace();
        	MyFile.saveError(e);
        }
    }
    
    /**
     * zeichnet abhängig von den Daten die Achsenbeschriftung und Skala
     * @param g2
     */
    private void drawSkaleX(Graphics2D g2, int ISize){   	
    	if (ISize <= 0) return;
    	
    	g2.setStroke(axisStroke);
    	g2.setPaint(Color.BLACK);
	    g2.draw(new Line2D.Float(_xOffset, _p_heigth - _yOffset, _p_width - _x2Offset, _p_heigth - _yOffset));
	    
	    float[][] capture = getXScaleCapture();
	    if (capture == null) return;
		for (int i=0;i<capture.length;i++){
			if (capture[i][1] < _xOffset || capture[i][1] > _p_width - _x2Offset) continue;
	    	if (_showLines){
		    	g2.setStroke(coordinateStroke);
		    	g2.setPaint(Color.LIGHT_GRAY);
				g2.draw(new Line2D.Float(capture[i][1] ,1, capture[i][1], _p_heigth - _yOffset));
			}else{
				g2.draw(new Line2D.Float(capture[i][1] , _p_heigth - _yOffset -10,capture[i][1],  _p_heigth - _yOffset));
			}
	    	g2.setPaint(Color.BLACK);
	    	String s = "";
	    	float temp = capture[i][0] - (int) capture[i][0];
	    	if (temp == 0){
	    		s = String.valueOf(capture[i][0]);
	    	}else{
	    		s = tools.MyMath.roundString(capture[i][0], 3);
	    	}
			int ws= SwingUtilities.computeStringWidth(g2.getFontMetrics(), s);
			g2.drawString(s, (int) (capture[i][1] - ws/2), _p_heigth- _yOffset/2);	    	
		}
		
		g2.drawString("t [s]", _p_width - _x2Offset + 10, _p_heigth - _yOffset);
    } 
    
	/**
	 * Zeichnet die Y-Achse
	 * @param g2	Graphics2D Objekt
	 * @param min	Minimum der Messreihe
	 * @param rawToVal	Umrechnungsfactor von den RAW-Werten in die jeweilige Einheit
	 * @param scale	Skalierungsfaktor der Y-Werte
	 * @param id	Messreihen ID (zur bestimmung der Position der Skala links oder rechts)
	 */
    private void drawSkaleY(Graphics2D g2, float min, float max, float scale, int id){
    	if (id >= _showGraphen.length || !_showGraphen[id]) return;		//Wenn Graph nicht angezeigt, return.
    	    	
    	int xoffset = _xOffset;
    	Color c = _c_lines[id];
    	if (id > 0){
    		xoffset = _p_width - _x2Offset;
    	}
    	
    	//Graph in 10 Abschnitte unterteilen
    	float tmax = max;
    	float tmin = min;
    	
    	float dy_temp = (tmax - tmin) / 10;
    	float axis[] = new float[10];
		float sy = (_p_heigth - _yOffset) / 10;
		
		String unit = _data.getDaten(id).getType().shortName + " ["+ _data.getDaten(id).messbereich.unit + "]";
		if (id == 2) unit = "P [mW]";
		int posX = 0;
		
		//Anzuzeigende Messwerte berechnen
		int digits = 1;	//Anzahl der notwendigen Nachkommerstellen
    	for (int i=0;i<10;i++){
    		axis[i] = tmin + i*dy_temp;
    		if (id == 0){			//Strom in mA wandeln
    			axis[i] = Range.convertFromTo(Messdaten.R_CURRENT, Range.MILLIAMPERE, axis[i]);
    		}else if (id == 2){
    			axis[i] = Range.convertFromTo(Range.MICROWATT, Range.MILLIWATT, axis[i]);
    		}
    	}
    	for (digits = 0; digits < 6; digits++){
    		boolean doBreak = false;
	    	for (int i=1;i<10;i++){
	    		float round1 = (float) (Math.round(axis[i] * Math.pow(10, digits)) / Math.pow(10, digits));
	    		float round0 = (float) (Math.round(axis[i-1] * Math.pow(10, digits)) / Math.pow(10, digits));
	    		
	    		float difRound = round1 - round0;
	    		if (difRound == 0){		//Wenn Werte gleich sind
	    			break;
	    		}
	    		if (i==9) doBreak = true;
	    	}
	    	if (doBreak) break;
    	}
    	
    	
    	for (int i=0;i<10;i++){
    		//zeichne Linien
    		g2.setPaint(Color.black); 
    		float y = _p_heigth - sy * i - _yOffset;
    		if (_showLines && (id==0 || getAnzGranphToShow() == 1)){
	    		g2.setPaint(Color.LIGHT_GRAY);
	    		g2.setStroke(coordinateStroke);
	    		g2.draw(new Line2D.Float(_xOffset ,y, _p_width - _x2Offset, y));
	    	}else if (id==0){
	    		g2.draw(new Line2D.Float(3 * xoffset/4 + 1 , y, 5 * xoffset/4 +1, y));
	    	}
    		
    		//zeichne Werte an die Achse
    		g2.setPaint(c);
    		String pattern = "%." + digits + "f";
    		String s = String.format(pattern, axis[i]);
    		int stringWidth = SwingUtilities.computeStringWidth(g2.getFontMetrics(), s);
    		switch (id){
	    		case 0:																//Strom wird gezeichnet
	        		if (_xOffset < stringWidth + 2) _xOffset = stringWidth + 2 ;
	    			posX = 1;
	    			break;
	    		case 1:																//Spannung wird gezeichnet
	    			if (_showGraphen[0]){
	    	    		if (_x2Offset < stringWidth + 2) _x2Offset = stringWidth + 2 ;
	    	    		posX = _p_width - _x2Offset + 1;						//Strom wurde gezeichnet
	    			}
	    			else{
	    	    		if (_xOffset < stringWidth + 2) _xOffset = stringWidth + 2 ;
	    				posX = 1;
	    			}
	    			break;	    			
	    		case 2:																//Leistung wird gezeichnet
	    			if (!_showGraphen[0] && !_showGraphen[1]){
	    	    		if (_xOffset < stringWidth + 2) _xOffset = stringWidth + 2 ;
	    	    		posX =  1;	//Strom und Spannung werden nicht gezeichnet
	    			}
	    			else if (!_showGraphen[1]){
	    	    		if (_x2Offset < stringWidth + 2) _x2Offset = stringWidth + 2 ;
	    	    		posX = _p_width - _x2Offset + 1;					//Spannung wird nicht gezeichnet
	    			}
	    			else{
	    	    		if (_x2Offset < stringWidth + 2) _x2Offset = stringWidth + 2 ;
	    				posX = _p_width - _x2Offset - stringWidth - 1;							//Strom und Spannung wurde gezeichnet
	    			}
    		}
    		
			g2.drawString(s, posX, (int) (y + 5));
    	}
		g2.drawString(unit, posX, 12);			

		//Y-Achsen zeichnen
    	g2.setStroke(axisStroke);
    	g2.setPaint(Color.BLACK);
    	g2.draw(new Line2D.Float(_xOffset, 0, _xOffset, _p_heigth - _yOffset));		// Draw Y-Achse 1
    	g2.draw(new Line2D.Float(xoffset, 0, xoffset, _p_heigth - _yOffset));		// Draw Y-Achse 1
    }
    
    /**
     * Berechne die Skalierung in X Richtung
     * @param size	Anzahl der zu zeichnenden Messwerte
     * @return	Skalierungsfaktor
     */
    private float getXScale(int size){
    	return (float)(_p_width - _x2Offset - _xOffset) / (float) (size);
    }
    
    /**
     * Zeichnet das Miniaturfenster mit der Graphenübersicht
     * @param g2	Graphics2D Object
     * @param id	Graphen ID
     */
    private void drawMiniWindow(Graphics2D g2, int id, boolean updateFullWindow, int altLeft, int altRight){
    	int xOffsetMini = _p_width - 202  - _x2Offset;
    	g2.setStroke(defaultStroke);
    	  	
    	int ScaleWidth = 100;
    	int scaleHeigh = 50;
    	int size = _data.getSize();
    	
        float scaleMini = (float)(scaleHeigh) / (_data.getDaten(id).getMax() * 1.05F);

        int yOffsetMini = (int) (scaleMini * _data.getDaten(id).getMin() * 1.05); 
    	int miniHeigth = 50 - yOffsetMini + 2;

    	if (id == 0){
	        g2.setPaint(Color.LIGHT_GRAY);
	        g2.fillRect(xOffsetMini,0, 102, miniHeigth);
	        g2.setPaint(Color.black);
	        g2.drawRect(xOffsetMini,0, 100 + 2, miniHeigth);
	        g2.setPaint(Color.black);
    	}

    	if (miniList[id] == null){
    		miniList[id] = _data.getDaten(id).createDrawableData(ScaleWidth, Statistics.RESAMPLE_MINMAX, 0, size, true, true);
    	}
        float xIncMini = (float)(ScaleWidth)/(miniList[id].size());
        int i_old = 0;
        g2.setPaint(_c_lines[id]);
        for(int i = 1; i < miniList[id].size(); i++) {
            float x0 = xOffsetMini + (i_old)*xIncMini;
            float y0 = 50 - scaleMini * miniList[id].get(i_old);
            float x1 = xOffsetMini + i *xIncMini;
            float y1 = 50 - scaleMini * miniList[id].get(i);
            g2.draw(new Line2D.Float(x0,y0,x1,y1));
            i_old = i;
        }    

        g2.setPaint(Color.white);
        float x0mini = ((float)_data.getSelectedBounds()[0] / (float)size) * ScaleWidth;
        float x1mini = ((float)_data.getSelectedBounds()[1] / (float)size) * ScaleWidth;
        
        if (!updateFullWindow){
            x0mini = ((float)altLeft / (float)size) * ScaleWidth;
            x1mini = ((float)altRight / (float)size) * ScaleWidth;
        }
        g2.drawRect((int) (xOffsetMini + x0mini), 0, (int) (x1mini - x0mini), miniHeigth);
    }
    
    /**
     * Gibt die Anzahl der zu zeichnenden Graphen zurück
     * @return Anzahl der zu zeichnenden Graphen
     */
    private int getAnzGranphToShow(){
    	int k = 0;
    	for (int i=0;i<_showGraphen.length;i++) if (_showGraphen[i]) k++;
    	return k;
    }    
    
  
    /**
     * erzwingt das komplette neuzeichnen der Graphen
     * @param b
     */
    public void doRepaint(boolean b){
    	drawNewImage = b;
    	repaint();
    }
    
    public float getMinMax(boolean isMax, int graphID){
    	float dMin = 0;
    	float dMax = 0;
    	
    	switch (graphID){
	    	case 0:
	    		if (ProgStates.USE_FIX_BOUNDS && !isMax) dMin = Power.MINIMUM_I;
	    		else if (ProgStates.USE_FIX_BOUNDS && isMax) dMax = Power.MAXIMUM_I;
	    		else if (!isMax) dMin = _data.getSelectedMin(graphID, true);
	    		else dMax = _data.getSelectedMax(graphID, true);
	    		break;
	    	case 1:
	    		if (ProgStates.USE_FIX_BOUNDS && !isMax) dMin = Power.MINIMUM_U;
	    		else if (ProgStates.USE_FIX_BOUNDS && isMax) dMax = Power.MAXIMUM_U;
	    		else if (!isMax) dMin = _data.getSelectedMin(graphID, true);		
	    		else dMax = _data.getSelectedMax(graphID, true);
	    		break;
	    	default:
	    		if (!isMax) dMin = _data.getSelectedMin(graphID, true);		
	    		else dMax = _data.getSelectedMax(graphID, true);
    	}

		if (!isMax) return dMin;
		else return dMax;
    }
    
    public void setFixesMinMax(int id, float min, float max){
    	drawMax[id] = max;
    	drawMin[id] = min;
    }
    
    private float [][] getXScaleCapture(){
    	float[][] res = null;
    	
    	int leftBound = _data.getSelectedBounds()[0];
    	int rightBound = _data.getSelectedBounds()[1];
    	
    	if (_LOCK){
    		leftBound = 0;
    		rightBound = _data.getEstimatedSize() -1;
    	}
    	
    	int size = rightBound - leftBound;

    	//Gesamtzeit von Bereich ermitteln
    	long time = (long) (size * Messdaten.DT);	//Microsekunden
    	
    	//Zeitskala berechnen
    	if (time > 2* 60 * 1000 * 1000){		// 2 Minuten < t
    		int sek = (int) (time / (1000 * 1000));
    		int dt = 60;

			res = new float[(sek/dt) + 1][2];
			int start = (int)(((int)(leftBound * Messdaten.DT / 1000) * 1000) / 1000.);
			int k = 0;
			for (int i=start;i<=(start + sek );i += dt){
				res[k][0] = (float)i;			//Bechriftung
				//x-position der Beschriftung berechnen
				float x = 0;
				int pos = 0;
				pos = (int) (i * 1000000 / Messdaten.DT) - leftBound;
				x = getXScale(size) * pos + _xOffset;
				res[k][1] = (int) x;
				if (x >= _xOffset)
					k++;
				else sek +=dt;
			}   			    		
    	}else if (time > 1000 * 1000){	// 1 Sekunde < t <= 1 Minute
    		int sek = (int) (time / (1000 * 1000));
    		int dt = 1;
    		if (time > 30 * 1000 * 1000){	// 30 sek < t <= 60 sek
    			dt = 5;
    		}else if (time > 10 * 1000 * 1000){ 	// 10 sek < t <= 30 sek
    			dt = 2;
    		}else{
    			dt = 1;
    		}

			res = new float[(sek/dt) + 1][2];
			int start = (int)(leftBound * Messdaten.DT / 1000000);
			int k = 0;
			for (int i=start;i<=(start + sek );i += dt){
				res[k][0] = (float)i;			//Bechriftung
				//x-position der Beschriftung berechnen
				float x = 0;
				int pos = 0;
				pos = (int) (i * 1000 * 1000 / Messdaten.DT) - leftBound;
				x = getXScale(size) * pos + _xOffset;
				res[k][1] = (int) x;
				if (x >= _xOffset)
					k++;
				else sek += dt;
			}   			    		
    	}else if (time > 1000){			// 1 Millisekunde < t <= 1 Sekunde
    		int sek = (int) (time / 1000);
    		int dt = 1;
    		if (time > 200 * 1000){	// 200 millis < t <= 1 sek
    			dt = 100;
    		}else if (time > 10 * 1000){
    			dt = 10;
    		}else{
    			dt = 1;
    		}

			res = new float[(sek/dt) + 1][2];
			int start = (int)(((int)(leftBound * Messdaten.DT / 1000) / 1000) * 1000.);
			int k = 0;
			for (int i=start;i<=(start + sek );i += dt){
				res[k][0] = (float)i / 1000;			//Bechriftung
				//x-position der Beschriftung berechnen
				float x = 0;
				int pos = 0;
				pos = (int) (i * 1000 / Messdaten.DT) - leftBound;
				x = getXScale(size) * pos + _xOffset;
				res[k][1] = (int) x;
				if (x >= _xOffset)
					k++;
				else sek += dt;
			}   			    		
    	}   	
    	return res;
    }

//    private float [][] getYScaleCapture(float min, float max){
//    	float[][] res = null;
//    	float range = max - min;		//gesamtrange ermitteln
//    	
//    	//Bereiche aufteilen
//    	float start = 0;
//    	float dI = 1;
//    	int k = 0;
//    	if (range >= 0.5){		
//    		System.out.println("range >= 500mA");
//    		dI = 0.1f;
//    		res = new float[(int) (range/dI + 1)][2];			
//    		start = ((float)((int)(min * 1000)) / 1000);
//    	}else if (range >= 0.05){
//    		System.out.println("range >= 50mA");
//    		dI = 0.01f;
//    		res = new float[(int) (range/dI + 1)][2];			
//    		start = ((float)((int)(min * 1000)) / 1000);
//    	}else if (range >= 0.001){
//    		System.out.println("range >= 1mA");
//    		dI = 0.001f;
//    		res = new float[(int) (range/dI + 1)][2];			
//    		start = ((float)((int)(min * 1000)) / 1000);
//    	}else{
//    		System.out.println("range < 1mA");
//    		dI = 0.00005f;
//    		res = new float[(int) (range/dI + 1)][2];			
//    		start = ((float)((int)(min * 1000)) / 1000);
//    	}
//		for (float i=start; i<=max;i += dI){
//			if (k >= res.length) break;
//			res[k][0] = i;		//Beschriftung			
//			//y Position der Beschriftung berechnen
//			float pos = Math.abs((i - min) / range);
//			float ypos = _p_heigth - (_p_heigth - _yOffset) * pos - _yOffset;
//			res[k][1] = ypos;
//			k++;
//		}
//
//    	return res;
//    }

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (_LOCK) return;
		int x0 = 0;
		if (e.getAdjustmentType() == AdjustmentEvent.UNIT_INCREMENT){
		}
		if (e.getAdjustmentType() == AdjustmentEvent.UNIT_DECREMENT){
		}
		if (e.getAdjustmentType() == AdjustmentEvent.BLOCK_INCREMENT){
		}
		if (e.getAdjustmentType() == AdjustmentEvent.BLOCK_DECREMENT){
		}
		if (e.getAdjustmentType() == AdjustmentEvent.TRACK){
		}
		x0 = sb_scroll.getValue();
		//System.out.println("adjustemnt scrollbar value: " + x0);
		if (System.currentTimeMillis() < lastZoomScroll + zoomScrollFreeze && !zoomFinished) return;
		lastZoomScroll = System.currentTimeMillis();
		updateDrawableGraph(x0);
		drawNewImage = true;			

	}
}
