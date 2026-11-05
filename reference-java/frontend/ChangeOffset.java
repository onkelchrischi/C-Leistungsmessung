package frontend;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JTextField;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;

public class ChangeOffset extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField txt_current1;
	private JTextField txt_current2;
	private JTextField txt_voltage;
	
	public double current1 = 0;
	public double current2 = 0;
	public double volatge = 0;
	
	private JDialog thisDialog = this;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			ChangeOffset dialog = new ChangeOffset();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public ChangeOffset() {
		setTitle("Offsetkorrektur");
		setResizable(false);
		setBounds(100, 100, 345, 151);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("Strom Bereich (0-15 mA):");
		lblNewLabel.setBounds(10, 11, 158, 14);
		contentPanel.add(lblNewLabel);
		
		JLabel lblStromBereich = new JLabel("Strom Bereich (15 - 2500 mA):");
		lblStromBereich.setBounds(10, 36, 158, 14);
		contentPanel.add(lblStromBereich);
		
		JLabel lblSpannung = new JLabel("Spannung :");
		lblSpannung.setBounds(10, 61, 158, 14);
		contentPanel.add(lblSpannung);
		
		txt_current1 = new JTextField();
		txt_current1.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_current1.setText("0");
		txt_current1.setBounds(232, 8, 56, 20);
		contentPanel.add(txt_current1);
		txt_current1.setColumns(10);
		
		txt_current2 = new JTextField();
		txt_current2.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_current2.setText("0");
		txt_current2.setColumns(10);
		txt_current2.setBounds(232, 33, 56, 20);
		contentPanel.add(txt_current2);
		
		txt_voltage = new JTextField();
		txt_voltage.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_voltage.setText("0");
		txt_voltage.setColumns(10);
		txt_voltage.setBounds(232, 58, 56, 20);
		contentPanel.add(txt_voltage);
		
		JLabel lblUa = new JLabel("uA");
		lblUa.setBounds(298, 11, 24, 14);
		contentPanel.add(lblUa);
		
		JLabel label = new JLabel("uA");
		label.setBounds(298, 36, 24, 14);
		contentPanel.add(label);
		
		JLabel lblMv = new JLabel("mV");
		lblMv.setBounds(298, 61, 24, 14);
		contentPanel.add(lblMv);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try{
							current1 = Double.parseDouble(txt_current1.getText());
						}catch (Exception ex){
							JOptionPane.showMessageDialog(thisDialog, "Offset ist keine Zahl!","Fehler!",JOptionPane.ERROR_MESSAGE);
							return;
						}
						try{
							current2 = Double.parseDouble(txt_current2.getText());
						}catch (Exception ex){
							JOptionPane.showMessageDialog(thisDialog, "Offset ist keine Zahl!","Fehler!",JOptionPane.ERROR_MESSAGE);
							return;
						}
						try{
							volatge = Double.parseDouble(txt_voltage.getText());
						}catch (Exception ex){
							JOptionPane.showMessageDialog(thisDialog, "Offset ist keine Zahl!","Fehler!",JOptionPane.ERROR_MESSAGE);
							return;
						}
						thisDialog.setVisible(false);
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						current1 = 0;
						current2 = 0;
						volatge = 0;
						thisDialog.setVisible(false);
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}
}
