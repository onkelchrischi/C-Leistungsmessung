package measuring;

import java.util.ArrayList;

import tools.Statistics;

public class Sector implements Comparable<Sector>{
	private int start;
	private int ende;
	private float Mittelwert;
	private float Standartabweichung;
	private ArrayList<Float> data = new ArrayList<Float>();
	private float wertung = -1;
	private int klasse = -1;
	
	public Sector(int s, int e, ArrayList<Float> data){
		start = s;
		ende = e;
		for (long i=s;i<e;i++){
			this.data.add(data.get((int) i));
		}
		Mittelwert = Statistics.getMittelwert(this.data);
		if (Mittelwert < 1) Mittelwert = 0;
		Standartabweichung = Statistics.getStandardabweichung(this.data, Mittelwert);
		classify();
	}
	
	public int start(){
		return start;
	}
	
	public int stop(){
		return ende;
	}
	
	public float Standartabweichung(){
		return Standartabweichung;
	}
	
	public float Mittelwert(){
		return Mittelwert;
	}
	
	public void setWertung(float w){
		wertung = w;
	}
	
	public float Wertung(){
		return wertung;
	}

	@Override
	public int compareTo(Sector o) {
		float mw = o.Mittelwert;
		float sa = o.Standartabweichung;
		int result = 0;
		if (Mittelwert <= mw * 1.1 && Mittelwert >= mw * 0.9 &&
				Standartabweichung <= sa * 3 && Standartabweichung >= sa*0.3){
			result = 1;
		}		
		if (Mittelwert == mw) result = 1;
		return result;
	}
	
	public int getKlasse(){
		return klasse;
	}
	
	public void setKlasse(int klasse){
		this.klasse = klasse;
	}
	
	private void classify(){
		
	}
}
