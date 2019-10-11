package plugins.ofuica.internals.dataTypes;

public class PolarCoord {
	private double r;
	private double theta;
	
	public PolarCoord(double rc, double tc){
		r = rc;
		theta = tc;
	}
	
	public void setR(double rc){
		r = rc;
	}
	public void setTheta(double tc){
		theta = tc;
	}
	
	public double getR(){
		return r;
	}
	public double getTheta(){
		return theta;
	}
	
	public PolarCoord(CartCoord cart){
		r = cart.getR();
		theta = Math.atan2(cart.getY(), cart.getX());
	}
}
