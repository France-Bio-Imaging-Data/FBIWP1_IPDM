package plugins.ofuica.internals.utils;

public class PSF {

private String name;
private double position;

public PSF(String n, double pos){
	name = n;
	position = pos;
}

public void setName(String name) {
	this.name = name;
}
public String getName() {
	return name;
}
public void setPosition(double position) {
	this.position = position;
}
public double getPosition() {
	return position;
}

public String toString(){
	return (name + " at " + position);
}
}
