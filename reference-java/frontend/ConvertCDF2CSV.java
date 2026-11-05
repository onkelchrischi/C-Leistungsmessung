package frontend;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.SwingWorker;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import measuring.Messdaten;
import program.CSVOut;
import program.Main;
import program.Property;

public class ConvertCDF2CSV extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1340875615297836121L;
	private JPanel contentPane;
	private JButton cmd_selectFiles;
	private JButton cmd_startConvert;
	private JList<String> lst_files;

	private Main _main;
	private final JFrame me = this;
	private String actualFileName = "";
	private JButton cmd_cancel;
	private SwingWorker<Boolean,Void> myWorker;

	/**
	 * Create the frame.
	 */
	public ConvertCDF2CSV(Main m) {
		_main = m;
		setTitle("Konvertiere CDF zu CSV");
		setBounds(100, 100, 900, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		cmd_selectFiles = new JButton("Dateien auswählen");
		cmd_selectFiles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {					
					@Override
					public void run() {
						JFileChooser jfc = new JFileChooser(Property.getProperties(Property.SAVE_PATH_FOLDER));
						jfc.setFileFilter(new FileFilter() {				
							@Override
							public String getDescription() {
								return "*.cdf | Coded Data Format";
							}
							
							@Override
							public boolean accept(File f) {
								if (f.getName().endsWith(".cdf") || f.isDirectory()) return true;
								return false;
							}
						});
						jfc.setMultiSelectionEnabled(true);
						jfc.showOpenDialog(null);
						File[] files = jfc.getSelectedFiles();
						if (files == null || files.length == 0) return;
						DefaultListModel<String> lm = new DefaultListModel<String>();
						for (File f : files){
							lm.addElement(f.getAbsolutePath());
						}
						lst_files.setModel(lm);
					}
				});
			}
		});
		
		cmd_startConvert = new JButton("Konvertieren");
		cmd_startConvert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (lst_files.getModel().getSize()==0) return;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						cmd_startConvert.setEnabled(false);
						cmd_selectFiles.setEnabled(false);
						final ListModel<String> model = lst_files.getModel();
						final Messdaten data = new Messdaten(_main);
						myWorker = new SwingWorker<Boolean,Void>(){
							@Override
							protected Boolean doInBackground() throws Exception {
								for (int i=0;i<model.getSize();i++){
									_main.gui.myMenue.setProgressBar(true, false);
									final File f = new File((String) model.getElementAt(i));
									if (!f.exists()) continue;
									actualFileName = f.getName();
									System.out.println("lese Messdaten: " + f.getName());
									data.readRaw(f, this);
									CSVOut csv = new CSVOut(Property.getProperties(Property.SAVE_PATH_FOLDER) + "\\" + f.getName().replace(".cdf", ".csv"), data);
									System.out.println("exportiere zu csv: " + csv.getOutputPath());
									csv.exportToCSV(-1, -1, false);
									this.firePropertyChange("NEXT", 0, i);
								};
								return true;
							}
							
				        	@Override
				            public void done() {
								cmd_startConvert.setEnabled(true);
								cmd_selectFiles.setEnabled(true);
				        		_main.gui.myMenue.setProgressBar(false, false);
				        		me.setVisible(false);
				        		System.out.println("Konvertierung abgeschlossen.");
				            }				        	
						};
						
						myWorker.addPropertyChangeListener(new PropertyChangeListener() {						
							@Override
							public void propertyChange(PropertyChangeEvent arg0) {
								if (arg0.getOldValue() == SwingWorker.StateValue.PENDING || arg0.getNewValue() == SwingWorker.StateValue.DONE) return;
								if (arg0.getPropertyName().equals("NEXT")){
//									int index = (Integer) arg0.getNewValue();
//									removeListItem(index);
								}else if(arg0.getPropertyName().equals("VALUES")){
									int val = (Integer) arg0.getNewValue();
									int max = (Integer) arg0.getOldValue();
									int pc = (int) (((long)val * 100) / max);
									_main.gui.myMenue.setProgressbarValue(pc);
									_main.gui.myMenue.setProgressBarText("lade Messwerte (" + actualFileName + "): " + String.valueOf(pc) + "%");
								}
							}
						});
						_main.gui.myMenue.setProgressBar(true, false);						
						myWorker.execute();							
					}
				});
			}
		});
		
		lst_files = new JList<String>();
		lst_files.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		
		cmd_cancel = new JButton("Abbrechen");
		cmd_cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try{
					if (myWorker.cancel(true)) me.setVisible(false);
				}catch (Exception ex){}
			}
		});
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(
			gl_contentPane.createParallelGroup(Alignment.TRAILING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING)
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(lst_files, GroupLayout.DEFAULT_SIZE, 658, Short.MAX_VALUE)
							.addGap(1))
						.addGroup(gl_contentPane.createSequentialGroup()
							.addComponent(cmd_selectFiles)
							.addPreferredGap(ComponentPlacement.RELATED, 319, Short.MAX_VALUE)
							.addComponent(cmd_cancel)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(cmd_startConvert, GroupLayout.PREFERRED_SIZE, 122, GroupLayout.PREFERRED_SIZE)))
					.addContainerGap())
		);
		gl_contentPane.setVerticalGroup(
			gl_contentPane.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPane.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(cmd_selectFiles)
						.addComponent(cmd_startConvert)
						.addComponent(cmd_cancel))
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(lst_files, GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE))
		);
		contentPane.setLayout(gl_contentPane);
	}
	
}
