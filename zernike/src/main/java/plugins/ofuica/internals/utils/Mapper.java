package plugins.ofuica.internals.utils;

import plugins.ofuica.internals.dataTypes.CartCoord;

public class Mapper {
	
	public static final int INTERNAL_MAPPING = 1;
	public static final int EXTERNAL_MAPPING = 2;
	
	public static double getNormalConst(int mappingType, double diameter, double p){
		double res = 0.0;
		if (mappingType == INTERNAL_MAPPING){
			res = (p + 1) / Math.pow((diameter - 1),2);
		} else {
			res = (2 * (p + 1)) / (Math.PI * Math.pow((diameter - 1),2));
		}
		
		return res;
	}
	
	public static double getPixSize(CartCoord center){
		
		double r =  Math.sqrt(Math.pow(center.getX(), 2) + Math.pow(center.getY(), 2));
		return (1.0/r);
	}
	
	public static CartCoord mapToCercle(int mappingType, CartCoord pts, CartCoord center){
		
		double rayon = 1.0;
		
		if (mappingType == INTERNAL_MAPPING){
		  	rayon = center.getX();
		  	if (rayon > center.getY()){
		  		rayon = center.getY();
		  	}
			rayon = ((2 * rayon) - 1) / 2 ;
		} else {
			rayon = Math.sqrt(Math.pow(center.getX(), 2) + Math.pow(center.getY(), 2));
		}
		
		double x = (pts.getX() - center.getX()) / rayon;
		double y = (pts.getY() - center.getY()) / rayon;
		CartCoord res = new CartCoord(x,y);
		return res;
	}
}
