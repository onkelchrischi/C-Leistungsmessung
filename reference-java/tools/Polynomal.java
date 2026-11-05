package tools;
/************************************************************************
 *                           Polynomal v1.01                            *
 ************************************************************************
 * Copyright (C) 2007 by Michael Loesler, http://derletztekick.com      *
 *                                                                      *
 * This program is free software; you can redistribute it and/or modify *
 * it under the terms of the GNU General Public License as published by *
 * the Free Software Foundation; either version 2 of the License, or    *
 * (at your option) any later version.                                  *
 *                                                                      *
 * This program is distributed in the hope that it will be useful,      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 * GNU General Public License for more details.                         *
 *                                                                      *
 * You should have received a copy of the GNU General Public License    *
 * along with this program; if not, write to the                        *
 * Free Software Foundation, Inc.,                                      *
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.            *
 ************************************************************************/

import java.util.Vector;
import java.util.Locale;
public class Polynomal extends MatrixOperation{
  private Vector<Point23> points = new Vector<Point23>();
  private int order = 0;
  protected String errMsg = new String("");
  private double X[];
  private String messages[] = new String[] {
                                "Error in Polynomal: ",
                                "Not enough points!",
                                "No Polynomal is calculated!"
                              };
  private boolean isComplete = false;
  public Polynomal() {  }
  
  public Polynomal(Vector<Point23> p){
    this(p, -1);
  }
  
  public Polynomal(Vector<Point23> p, int o) {
    this(p, o, null);
  }
  
  public Polynomal(Vector<Point23> p, int o, String[] msg) {
    for (int i=0; i<p.size(); i++)
      if (p.get(i).getPP())
        this.points.add(p.get(i));
    if (this.points.size() < 1){
      this.errMsg += this.messages[0]+this.messages[1]+"\r\n";
      return;
    }
    this.setMsg(msg);
    if (o<0 || o>=this.points.size())
      this.order = this.points.size()-1;
    else
      this.order = o;

    this.isComplete = this.fittingPolynomal();
    if (!this.isComplete){
      this.errMsg += this.messages[0]+this.messages[2]+"\r\n";
      return;
    }

  }
  
  public void setMsg(String[] msg){
    if (msg != null && msg.length == this.messages.length)
      this.messages = msg;
  }
  
  private boolean fittingPolynomal(){
    int n = this.points.size();
    double A[][] = new double[n][this.order+1];
    double l[] = zeros(A.length);
    this.X = ones(this.order+1);
    int maxIteration = 1;
    double max_dx = 2147483647.0;
    while (max_dx > 1.0E-13 && maxIteration-->0) {
      for (int i=0; i<this.points.size(); i++){
        for (int j=0; j<A[i].length; j++) {
          A[i][j] = Math.pow(this.points.get(i).getX(),A[i].length-1-j);
          l[i] += X[j]*Math.pow(this.points.get(i).getX(),X.length-1-j);
        }
        l[i] = this.points.get(i).getY() - l[i];
      }

      try {
        double nn[] = multi(trans(A),l);     // n=A'*l
        double N[][] = multi(trans(A),A);    // N=A'*A
        double Q[][] = inv(N);               // Q=inv(N)
        double dx[] = multi(Q,nn);           // dx = Q*n
        this.X = sum(this.X, dx);
      } catch(Exception err) {
        this.errMsg += this.messages[0]+" "+err+"\r\n";
        return false;
      }
    }
    return true;
  }
  
  public Double calculate(double xValue){
    if (!this.isComplete)
      return null;
    double y = 0;
    for (int i=0; i<this.X.length; i++)
      y += Math.pow(xValue, this.X.length-1-i) * this.X[i];
    return new Double(y);
  }
  
  public String toString(){
    if (!this.isComplete)
      return this.messages[2]+"\r\n"+this.errMsg;
    String strP = "f(x) = a<sub>n</sub>x<sup>n</sup> + a<sub>n-1</sub>x<sup>n-1</sup> + ... + a<sub>1</sub>x + a\n";
    String strR = "";
    
    strP += String.format(Locale.ENGLISH, "  n = %2d\n\n", this.order);
    for (int i=0; i<this.X.length; i++)
        strP += String.format(Locale.ENGLISH, "  a<sub>%1d</sub> = %+.15f\n", (this.X.length-i-1), this.X[i]);
    for (int i=0; i<this.points.size(); i++)
        strR += String.format(Locale.ENGLISH, "  %5s = %+.15f\n", this.points.get(i).getPKT(), (this.points.get(i).getY()-this.calculate(this.points.get(i).getX())));
      //  "\t"+this.points.get(i).getPKT()+" = "+(this.points.get(i).getY()-this.calculate(this.Points.get(i).getX()))+"\n";
    return (strP+"\r\n"+strR+"\r\n");
  }
  
}

