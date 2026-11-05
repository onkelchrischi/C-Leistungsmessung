package measuring;

import program.Main;
import program.ProgStates;

public class DummyHW extends Device{

	public final static int DELTA_T = 100;
	public DummyHW(Main main) {
		super(main);
	}

	@Override
	public void startMe() {

		while (ProgStates.MEASSURE_RUNS){
			float i = (float) (Math.random() / 1000);
			float u = (float) (Math.random() * 100 + 3900) / 1000;
			_main.data.addValueToMessdaten(0, 0,i,true);
			_main.data.addValueToMessdaten(1, 0,u,true);		
			anzMesswerte++;
			try {
				Thread.sleep(DELTA_T);
			} catch (InterruptedException e) {}
		}
		stopMeasure(true);
	}

	@Override
	public boolean stopMeasure() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stopMeasure(boolean forceStop) {
		//_main.data.addValueToMessdaten(-1,0,0,false);	//Bei periodischer Messung, schreibe die zuletzt gespeicherten Werte in die CSV Datei 
		stop2PortMeasure();	
		MeasureFinished(true);
		return true;
	}

}
