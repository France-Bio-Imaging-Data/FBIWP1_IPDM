package plugins.ofuica.internals.utils;

public class Couple {
	
private String name1;
private String name2;

public Couple(String a, String b){
	name1 = a;
	name2 = b;
}

public void setName1(String name1) {
	this.name1 = name1;
}
public String getName1() {
	return name1;
}
public void setName2(String name2) {
	this.name2 = name2;
}
public String getName2() {
	return name2;
}

public String toString(){
	return (name1 + " with " + name2);
}

}
