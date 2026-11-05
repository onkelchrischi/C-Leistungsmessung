package tools;

public abstract class Multimeter extends USART_Tools{

	public Multimeter(String port) {
		super(port);
	}

	public abstract double getI();
	public abstract double getU();
	protected abstract void analyse(String in);
	public abstract float getMaxI();
	
}
