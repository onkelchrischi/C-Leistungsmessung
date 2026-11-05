package measuring;

import java.util.ArrayList;

import program.Main;

public abstract class Device extends Measure implements StartMeasure{

	public Device(Main main) {
		super(main);
		stateList = new ArrayList<State>();
	}
	
}
