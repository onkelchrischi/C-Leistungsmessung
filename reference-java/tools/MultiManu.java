package tools;

import javax.swing.JOptionPane;

public class MultiManu extends Multimeter{

	public MultiManu(String port) {
		super(port);
		System.out.println("nutze Manuelle Eingabe von Werten..");
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getI() {
		System.out.println("aktuellen Strom eingeben");
		String str_i = JOptionPane.showInputDialog(null, "Gib den aktuellen Strom ein (mA)");
		analyse(str_i);
		synchronized (syncValue) {
			newValue[1] = false;
			return _i;
		}
	}

	@Override
	public double getU() {
		System.out.println("aktuellen Strom eingeben");
		String str_u = JOptionPane.showInputDialog(null, "Gib die aktuelle Spannung ein (mV)");
		analyse(str_u);
		synchronized (syncValue) {
			newValue[1] = false;
			return _u;
		}
	}

	@Override
	protected void analyse(String in) {
		//Ziffern auslesen
		try{			
			double num = Double.parseDouble(in);
			synchronized (syncValue) {
				newValue[0] = newValue[1] = false;
				_i = num;
				_u = num;						
			}
		}catch (Exception e){
			_i = 0;
			_u = 0;
		}
	}

	@Override
	public float getMaxI() {
		// TODO Auto-generated method stub
		return 0.5f;
	}

	@Override
	public boolean connect() {
		// not used
		return true;
	}

	@Override
	public boolean disconnect() {
		// not used
		return true;
	}

}
