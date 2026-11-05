package measuring;

public interface StartMeasure {
	public void startMe();
	public boolean stopMeasure();
	public boolean stopMeasure(boolean forceStop);
}
