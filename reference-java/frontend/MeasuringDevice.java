package frontend;

import java.awt.Color;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import tools.MyColors;

public class MeasuringDevice extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JLabel lbl_sampleRate;
	private JSlider sl_sampleRate;	
	private JLabel lbl_adc1Val;
	private JLabel lbl_adc2Val;
	private JLabel lbl_adc3Val;
	private JLabel lbl_adc1;
	private JLabel lbl_adc2;
	private JLabel lbl_adc3;
	private boolean[] activeLables = {false,false,false};
	private JLabel lblX;
	private JCheckBox chk_convert;
	private JTextField txt_uint1;
	private JTextField txt_p1_1;
	private JTextField txt_p0_1;
	private JTextField txt_p1_2;
	private JTextField txt_p0_2;
	private JTextField txt_p1_3;
	private JTextField txt_p0_3;
	private JComboBox<String> cmb_opp1;
	private JComboBox<String> cmb_opp2;
	private JLabel label;
	private JLabel label_1;
	private JTextField txt_unit3;
	private JTextField txt_unit2;

	private double[] p1 = {1,1,1};
	private double[] p0 = {0,0,0};

	/**
	 * Create the panel.
	 */
	public MeasuringDevice() {
		super();
		initComponents();
		initListeners();
		
	}
	
	public int getSampleInterval(){
		return sl_sampleRate.getValue() * 1000;
	}
	
	public void setSliderEnable(final boolean enabled){
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				sl_sampleRate.setEnabled(enabled);
			}
		});
	}
	
	public void setValues(final int[] vals){
		if (vals == null) return;
		if (chk_convert.isSelected()){
			for (int i=0;i<activeLables.length;i++){
				try{
					
					
				}catch (Exception e){}
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				boolean[] temp = activeLables.clone();
				for (int i=0;i<vals.length;i++){
					if (activeLables[0] && temp[0]){
						lbl_adc1Val.setText(String.valueOf(vals[i] * p1[0] + p0[0]));
						temp[0] = false;
						continue;
					}
					if (activeLables[1]  && temp[1]){
						lbl_adc2Val.setText(String.valueOf(vals[i] * p1[1] + p0[1]));
						temp[1] = false;
						continue;
					}
					if (activeLables[2] && temp[2]){
						lbl_adc3Val.setText(String.valueOf(vals[i] * p1[2] + p0[2]));
						temp[2] = false;
						continue;
					}
				}
			}
		});
	}
	
	public void enableADCLables(final int index,final boolean activate){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Color active = null;
				if (activate) active = MyColors.DARK_GREEN;
				else active = Color.lightGray;
				activeLables[index] = activate;
				switch (index){
					case 0:
						if (!activate) lbl_adc1Val.setText("not uesed");
						lbl_adc1Val.setForeground(active);
						lbl_adc1.setForeground(active);
						break;
					case 1:
						if (!activate) lbl_adc2Val.setText("not uesed");
						lbl_adc2Val.setForeground(active);
						lbl_adc2.setForeground(active);
						break;
					case 2:
						if (!activate) lbl_adc3Val.setText("not uesed");
						lbl_adc3Val.setForeground(active);
						lbl_adc3.setForeground(active);
						break;
				}
			}
		});
	}
	
	private void initComponents(){
		
		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(null, "Messdaten", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		
		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "Konfiguration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(87)
					.addGroup(groupLayout.createParallelGroup(Alignment.TRAILING, false)
						.addComponent(panel_1, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(panel, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 858, Short.MAX_VALUE))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(100)
					.addComponent(panel_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(panel, GroupLayout.PREFERRED_SIZE, 286, GroupLayout.PREFERRED_SIZE)
					.addContainerGap(45, Short.MAX_VALUE))
		);
		
		sl_sampleRate = new JSlider();
		sl_sampleRate.setMajorTickSpacing(50);
		sl_sampleRate.setMinorTickSpacing(10);
		sl_sampleRate.setPaintTicks(true);
		sl_sampleRate.setSnapToTicks(true);
		sl_sampleRate.setPaintLabels(true);
		sl_sampleRate.setMinimum(50);
		sl_sampleRate.setMaximum(1000);
		
		JLabel lblSampelrate = new JLabel("Sampelrate:");
		lblSampelrate.setFont(new Font("Tahoma", Font.PLAIN, 14));
		
		lbl_sampleRate = new JLabel("50 ms");
		lbl_sampleRate.setFont(new Font("Tahoma", Font.PLAIN, 14));
		GroupLayout gl_panel_1 = new GroupLayout(panel_1);
		gl_panel_1.setHorizontalGroup(
			gl_panel_1.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel_1.createSequentialGroup()
					.addGap(18)
					.addGroup(gl_panel_1.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_1.createSequentialGroup()
							.addComponent(lblSampelrate)
							.addPreferredGap(ComponentPlacement.UNRELATED)
							.addComponent(lbl_sampleRate, GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE))
						.addComponent(sl_sampleRate, GroupLayout.DEFAULT_SIZE, 763, Short.MAX_VALUE))
					.addContainerGap())
		);
		gl_panel_1.setVerticalGroup(
			gl_panel_1.createParallelGroup(Alignment.LEADING)
				.addGroup(Alignment.TRAILING, gl_panel_1.createSequentialGroup()
					.addContainerGap(56, Short.MAX_VALUE)
					.addGroup(gl_panel_1.createParallelGroup(Alignment.BASELINE)
						.addComponent(lblSampelrate)
						.addComponent(lbl_sampleRate))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(sl_sampleRate, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap())
		);
		panel_1.setLayout(gl_panel_1);
		
		lbl_adc1 = new JLabel("ADC0 - 1:");
		lbl_adc1.setFont(new Font("Arial Narrow", Font.BOLD, 65));
		
		lbl_adc1Val = new JLabel("0,000");
		lbl_adc1Val.setFont(new Font("Arial Narrow", Font.BOLD, 65));
		
		lbl_adc2Val = new JLabel("0,000");
		lbl_adc2Val.setFont(new Font("Arial Narrow", Font.BOLD, 65));
		
		lbl_adc2 = new JLabel("ADC0 - 3:");
		lbl_adc2.setFont(new Font("Arial Narrow", Font.BOLD, 65));
		
		lbl_adc3 = new JLabel("ADC0 - 9:");
		lbl_adc3.setFont(new Font("Arial Narrow", Font.BOLD, 65));
		
		lbl_adc3Val = new JLabel("0,000");
		lbl_adc3Val.setFont(new Font("Arial Narrow", Font.BOLD, 65));
		
		lblX = new JLabel("x");
		lblX.setHorizontalAlignment(SwingConstants.RIGHT);
		
		chk_convert = new JCheckBox("Umrechnen in");
		
		txt_uint1 = new JTextField();
		txt_uint1.setHorizontalAlignment(SwingConstants.CENTER);
		txt_uint1.setText("mA");
		txt_uint1.setColumns(10);
		
		txt_p1_1 = new JTextField();
		txt_p1_1.setText("0,86");
		txt_p1_1.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_p1_1.setColumns(10);
		
		txt_p0_1 = new JTextField();
		txt_p0_1.setText("0");
		txt_p0_1.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_p0_1.setColumns(10);
		
		cmb_opp1 = new JComboBox<String>();
		cmb_opp1.setModel(new DefaultComboBoxModel<String>(new String[] {"+", "-"}));
		
		txt_p1_2 = new JTextField();
		txt_p1_2.setText("0,86");
		txt_p1_2.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_p1_2.setColumns(10);
		
		cmb_opp2 = new JComboBox<String>();
		cmb_opp2.setModel(new DefaultComboBoxModel<String>(new String[] {"+", "-"}));
		
		txt_p0_2 = new JTextField();
		txt_p0_2.setText("0");
		txt_p0_2.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_p0_2.setColumns(10);
		
		txt_p1_3 = new JTextField();
		txt_p1_3.setText("0,053");
		txt_p1_3.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_p1_3.setColumns(10);
		
		JComboBox<String> cmb_opp3 = new JComboBox<String>();
		cmb_opp3.setModel(new DefaultComboBoxModel<String>(new String[] {"+", "-"}));
		
		txt_p0_3 = new JTextField();
		txt_p0_3.setText("0");
		txt_p0_3.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_p0_3.setColumns(10);
		
		label = new JLabel("x");
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		
		label_1 = new JLabel("x");
		label_1.setHorizontalAlignment(SwingConstants.RIGHT);
		
		txt_unit2 = new JTextField();
		txt_unit2.setText("mA");
		txt_unit2.setHorizontalAlignment(SwingConstants.CENTER);
		txt_unit2.setColumns(10);
		
		txt_unit3 = new JTextField();
		txt_unit3.setText("mA");
		txt_unit3.setHorizontalAlignment(SwingConstants.CENTER);
		txt_unit3.setColumns(10);
		GroupLayout gl_panel = new GroupLayout(panel);
		gl_panel.setHorizontalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addGap(41)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(lbl_adc1)
						.addComponent(lbl_adc2)
						.addComponent(lbl_adc3, GroupLayout.PREFERRED_SIZE, 322, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING, false)
						.addComponent(lbl_adc3Val, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lbl_adc2Val, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addComponent(lbl_adc1Val, GroupLayout.PREFERRED_SIZE, 192, GroupLayout.PREFERRED_SIZE))
					.addGap(46)
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addComponent(chk_convert)
						.addGroup(gl_panel.createSequentialGroup()
							.addGap(9)
							.addGroup(gl_panel.createParallelGroup(Alignment.TRAILING)
								.addComponent(lblX, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
								.addComponent(label, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE)
								.addComponent(label_1, GroupLayout.PREFERRED_SIZE, 18, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel.createSequentialGroup()
									.addComponent(txt_p1_1, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addComponent(cmb_opp1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
										.addComponent(txt_uint1, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
										.addComponent(txt_p0_1, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)))
								.addGroup(gl_panel.createSequentialGroup()
									.addComponent(txt_p1_2, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
									.addGap(6)
									.addComponent(cmb_opp2, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(ComponentPlacement.RELATED)
									.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
										.addComponent(txt_unit2, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
										.addComponent(txt_p0_2, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)))
								.addGroup(gl_panel.createSequentialGroup()
									.addComponent(txt_p1_3, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
									.addGap(6)
									.addComponent(cmb_opp3, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE)
									.addGap(6)
									.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
										.addComponent(txt_unit3, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
										.addComponent(txt_p0_3, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE))))))
					.addContainerGap())
		);
		gl_panel.setVerticalGroup(
			gl_panel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_panel.createSequentialGroup()
					.addContainerGap()
					.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel.createSequentialGroup()
							.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_adc1)
								.addComponent(lbl_adc1Val))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(lbl_adc2)
								.addComponent(lbl_adc2Val))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addComponent(lbl_adc3, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)
								.addComponent(lbl_adc3Val, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE))
							.addContainerGap())
						.addGroup(gl_panel.createSequentialGroup()
							.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(chk_convert)
								.addComponent(txt_uint1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
								.addComponent(txt_p1_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(cmb_opp1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(txt_p0_1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(lblX))
							.addGap(30)
							.addComponent(txt_unit2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
									.addComponent(txt_p1_2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(label))
								.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
									.addComponent(cmb_opp2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(txt_p0_2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
							.addPreferredGap(ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
							.addComponent(txt_unit3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_panel.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel.createParallelGroup(Alignment.BASELINE)
									.addComponent(txt_p1_3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
									.addComponent(label_1))
								.addComponent(cmb_opp3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addComponent(txt_p0_3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
							.addGap(47))))
		);
		panel.setLayout(gl_panel);
		setLayout(groupLayout);	
	}
	
	private void initListeners(){
		sl_sampleRate.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				lbl_sampleRate.setText(sl_sampleRate.getValue() + " ms");
			}
		});		
	}
}
