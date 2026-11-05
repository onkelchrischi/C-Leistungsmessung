package tools;

public abstract class Source  extends USART_Tools{
	public Source(String port) {
		super(port);
		// TODO Auto-generated constructor stub
	}

	public abstract double getI();
	public abstract double getU();
	protected abstract void analyse(String in);
	public abstract float getMaxI();

	public abstract void setI(String text);
	public abstract void setU(String text);

	public abstract void setOut(boolean selected);
}
