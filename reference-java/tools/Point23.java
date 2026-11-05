package tools;
/************************************************************************
 *                           Point23 v2.5                               *
 ************************************************************************
 * Copyright (C) 2006-07 by Michael Loesler, http://derletztekick.com   *
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

class Point23 {
  private boolean pp = true;
  private Double x, y, z, weightX, weightY, weightZ;
  private String pkt, code;

  public Point23 () { }

  public Point23 (String pkt, Double x, Double y) {
    this(pkt, x, y, null, true);
  }

  public Point23 (String pkt, Double x, Double y, Double z) {
    this(pkt, x, y, z, true);
  }

  public Point23(Point23 P, String pkt, double s, double t){
    this(pkt, P.getX() + s*Math.cos(t), P.getY() + s*Math.sin(t), null, true);
  }
  
  public Point23(Point23 P, String pkt, double s, double t, double h){
    this(pkt, P.getX() + s*Math.cos(t), P.getY() + s*Math.sin(t), h, true);
  }

  public Point23(double Xs, double Ys, String Pkt, double s, double t){
    this(Pkt, Xs + s*Math.cos(t), Ys + s*Math.sin(t), null, true);
  }
  
  public Point23(double Xs, double Ys, String Pkt, double s, double t, double h){
    this(Pkt, Xs + s*Math.cos(t), Ys + s*Math.sin(t), h, true);
  }

  public Point23 (String pkt, Double x, Double y, Double z, boolean pp) {
    boolean isPoint = true;
    if (pkt==null || x==null || y==null)
      isPoint = false;
    try { x.doubleValue(); } catch (Exception error){ isPoint = false; }
    try { y.doubleValue(); } catch (Exception error){ isPoint = false; }
    if (isPoint) {
      this.pkt = pkt;
      this.x = x;
      this.y = y;
      this.z = z;
      this.pp = pp;
      this.weightX = 1.0;
      this.weightY = 1.0;
      this.weightZ = 1.0;
    }
  }

  public boolean is2DPoint(){
    return (this.pkt!=null&&this.x!=null&&this.y!=null);
  }
  
  public boolean is3DPoint(){
    return (this.is2DPoint()&&this.z!=null);
  }
  
  public boolean equals2D(Point23 p){
    if (this.x == p.x && this.y == p.y && this.pkt.equals(p.pkt))
      return true;
    return false;
  }

  public boolean equals2DIgnoreNumber(Point23 p){
    if (this.x == p.x && this.y == p.y)
      return true;
    return false;
  }

  public boolean equals(Point23 p){
    if (this.x == p.x && this.y == p.y && this.z == p.z && this.pkt.equals(p.pkt))
      return true;
    return false;
  }

  public boolean equalsIgnoreNumber(Point23 p){
    if (this.x == p.x && this.y == p.y && this.z == p.z)
      return true;
    return false;
  }

  public String toString(){
    return (!this.is3DPoint()?new String("Pkt "+this.pkt+" ["+this.x+"/"+this.y+"]"):new String("Pkt "+this.pkt+" ["+this.x+"/"+this.y+"/"+this.z+"]"));
  }
  public void setX(double x){
    this.x = x;
  }
  public void setY(double y){
    this.y = y;
  }
  public void setZ(double z){
    this.z = z;
  }
  public void setPP(boolean pp){
    this.pp = pp;
  }
  public void setWeightX(double wx){
    this.weightX = wx;
  }
  public void setWeightY(double wy){
    this.weightY = wy;
  }
  public void setWeightZ(double wz){
    this.weightZ = wz;
  }
  public void setPKT(String pkt) {
    this.pkt = pkt;
  }
  public void setCode(String code) {
    this.code = code;
  }
  public boolean getPP(){
    return this.pp;
  }
  public Double getX(){
    return this.x;
  }
  public Double getY(){
    return this.y;
  }
  public Double getZ(){
    return this.z;
  }
  public Double getWeightX(){
    return this.weightX;
  }
  public Double getWeightY(){
    return this.weightY;
  }
  public Double getWeightZ(){
    return this.weightZ;
  }
  public String getPKT(){
    return this.pkt;
  }
  public String getCode(){
    return this.code;
  }
  public double distance2D(Point23 P){
    return Math.sqrt(Math.pow(this.x-P.getX(),2)+Math.pow(this.y-P.getY(),2));
  }
  public double distance2D(double x, double y){
    return Math.sqrt(Math.pow(this.x-x,2)+Math.pow(this.y-y,2));
  }
  public double distance3D(Point23 P){
    return Math.sqrt(Math.pow(this.x-P.getX(),2)+Math.pow(this.y-P.getY(),2)+Math.pow(this.z-P.getZ(),2));
  }
  public double distance3D(double x, double y, double z){
    return Math.sqrt(Math.pow(this.x-x,2)+Math.pow(this.y-y,2)+Math.pow(this.z-z,2));
  }
  public double riwi(double X0, double Y0){
    return Math.atan2( (this.y-Y0) , (this.x-X0) );
  }
  //Zielpunkt minus Standpunkt --> Standpunkt.riwi(Zielpunkt)
  public double riwi(Point23 P){
    return Math.atan2((P.getY()-this.y), (P.getX()-this.x));
  }
}

