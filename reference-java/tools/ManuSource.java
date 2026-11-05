package tools;

import javax.swing.JOptionPane;

public class ManuSource  extends Source{

	public ManuSource(String port) {
		super(port);
	}

	public boolean connect(){
		System.out.println("Manuelle Spannungs- und Stromeingabe aktiv.");
        return true;
	}
	
	public boolean disconnect(){
		return true;
	}
	
	public double getI(){
		synchronized(syncValue){
			newValue[1] = false;
			return _i;
		}
	}
	
	public double getU(){
		synchronized(syncValue){
			newValue[0] = false;
			return _u;
		}
	}

	@Override
	public void run() {
		//not used
	}
	
	
	public void setU(String u){
		JOptionPane.showMessageDialog(null, "Bitte stellen Sie die Spannung " + u + " V ein.", "Spannung einstellen", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void setI(String i){
		JOptionPane.showMessageDialog(null, "Bitte stellen Sie den Strom " + i + " mA ein.", "Strom einstellen", JOptionPane.INFORMATION_MESSAGE);
	}
	
	public void setI(float i){
		JOptionPane.showMessageDialog(null, "Bitte stellen Sie den Strom " + i + " mA ein.", "Strom einstellen", JOptionPane.INFORMATION_MESSAGE);		
	}

	public void setU(float u){
		JOptionPane.showMessageDialog(null, "Bitte stellen Sie die Spannung " + u + " V ein.", "Spannung einstellen", JOptionPane.INFORMATION_MESSAGE);
	}

	public void setOut(boolean setEnable){
		
	}
	
	public void enableRemote(boolean enable){
		
	}

	@Override
	protected void analyse(String in) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getMaxI() {
		// TODO Auto-generated method stub
		return 0;
	}

}
