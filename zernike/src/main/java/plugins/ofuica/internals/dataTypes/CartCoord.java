package plugins.ofuica.internals.dataTypes;

public class CartCoord 
{
	private double x;
	private double y;
	
	public CartCoord(CartCoord cc){
		this.x = cc.getX();
		this.y = cc.getY();
	}
	public CartCoord(double xc, double yc){
		x = xc;
		y = yc;
	}
	
	public void setX(double xc){
		x = xc;
	}
	public void setY(double yc){
		y = yc;
	}
	
	public double getX(){
		return x;
	}
	public double getY(){
		return y;
	}
	
	public double getR(){
		return (Math.sqrt((x * x) + (y * y)));
	}
	
	public PolarCoord getPolar(){
		return (new PolarCoord(this));
	}
	
	public String toString(){
		return ("point(" + x + ", " + y);
	}
}
