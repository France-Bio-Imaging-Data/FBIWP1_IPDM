package plugins.ofuica.internals.calculate;
import java.util.Map;
import java.util.TreeMap;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
import icy.type.DataType;
import ij.ImageStack;
import ij.process.FloatProcessor;
import plugins.ofuica.internals.dataTypes.CartCoord;
import plugins.ofuica.internals.dataTypes.PolarCoord;
import plugins.ofuica.internals.utils.MNPair;
import plugins.ofuica.internals.utils.Mapper;

public class PolynomialCalculator {

private int width, height;
private CartCoord center;
private int mapping;

public PolynomialCalculator(int w, int h, CartCoord cent, int map ){
	width = w;
	height = h;
	center = cent;
	mapping = map;
}

public double zer_pol_R(int m, int n_in, double rho) 
{ 
   double a; /* (m-s)! */ 
   double b; /*   s!   */ 
   double c; /* [((m+|n|)/2)-s]! */ 
   double d; /* [((m-|m|)/2)-s]! */ 
   int sign; 
   
   int n = Math.abs(n_in); 
   
   /* The code is optimized with respect to the faculty operations */ 
   
   double res = 0.0; 
   
   sign = 1; 
   a = 1; 
   
   for (int i=2; i<=m; i++){
      a = a * i; 
   }
   
   b=1;
   c = 1;
   
   for (int i=2; i <= (m+n)/2; i++){
      c= c *i;  
   }
   
   d = 1; 
   for (int i=2; i <= (m-n)/2; i++) {
      d = d * i;   
   }
   
   /* Before the loop is entered, all the integer variables (sign, a, */ 
   /* b, c, d) have their correct values for the s=0 case. */ 
   for (int s=0; s<= (m-n)/2; s++) 
   {
      res = res + (sign * (a*1.0/(b*c*d)) * Math.pow(rho,(m-(2*s)))); 
      
      /* Now update the integer variables before the next iteration of */ 
      /* the loop. */ 
      
      if (s < (m-n)/2) 
      { 
         sign = -sign; 
         a = a/(m-s); 
         b = b*(s+1); 
         c = c / (((m+n)/2) - s); 
         d = d / (((m-n)/2) - s); 
       } 
    } 
    
    return res;
} 

public double getVpqc(ImageStack polyv, int maxOrder){
	PolarCoord[] coord = getPolarCoords();
	double cnt = 0.0;
	for(int m = 0; m <=maxOrder; m++){
		for (int n = 0; n <= m; n++){
			if(((m-n)%2) == 0) {
				double[] valv = new double[width * height];
				cnt = 0.0;
				for (int i = 0; i < valv.length; i++){
					if (coord[i].getR() < 1){
						double phase = Math.abs(n) * coord[i].getTheta();
						valv[i] = zer_pol_R(m,n,coord[i].getR()) * Math.cos(phase);
						cnt = cnt + 1;
					}else{
						valv[i] = 0.0;
					}
				}
				
				polyv.addSlice(m + "," + n,new FloatProcessor(width, height,valv));
			}
		}
	}
	return cnt;
}

public double getVRpqc(ImageStack polyr, ImageStack polyv, int maxOrder){

	PolarCoord[] coord = getPolarCoords();
	double cnt = 0.0;
	for(int m = 0; m <=maxOrder; m++){
		for (int n = 0; n <= m; n++){
			if(((m-n)%2) == 0){
				double[] valr = new double[width * height];
				double[] valv = new double[width * height];
				cnt = 0.0;
				for (int i = 0; i < valr.length; i++){
					if (coord[i].getR() < 1){
						valr[i] = zer_pol_R(m,n,coord[i].getR());
						double phase = Math.abs(n) * coord[i].getTheta();
						valv[i] = valr[i] * Math.cos(phase);
						cnt = cnt + 1;
					}else{
						valr[i] = 0.0;
						valv[i] = 0.0;
					}
				}
				
				polyr.addSlice(m + "," + n,new FloatProcessor(width, height, valr));
				polyv.addSlice(m + "," + n,new FloatProcessor(width, height, valv));
			}
		}
	}
	return cnt;
}

public double getVRpqc(TreeMap<MNPair<Integer>, IcyBufferedImage> polyr, TreeMap<MNPair<Integer>, 
      IcyBufferedImage> polyv, int maxOrder) {

   int c = 0;
   PolarCoord[] coord = getPolarCoords();
   double cnt = 0.0;
   int idx = 0;
   for(int m = 0; m <= maxOrder; m++) {
      for (int n = 0; n <= m; n++) {
         if( ( (m-n) % 2 ) == 0) {
            double[] valr = new double[width * height];
            double[] valv = new double[width * height];
            cnt = 0.0;
            for (int i = 0; i < valr.length; i++) {
               if (coord[i].getR() < 1){
                  valr[i] = zer_pol_R(m,n,coord[i].getR());
                  double phase = Math.abs(n) * coord[i].getTheta();
                  valv[i] = valr[i] * Math.cos(phase);
                  cnt = cnt + 1;
               } else {
                  valr[i] = 0.0;
                  valv[i] = 0.0;
               }
            }
            
            final IcyBufferedImage valri = new IcyBufferedImage(width, height, 1, DataType.DOUBLE);
            valri.setDataXY(c, valr);
            polyr.put(new MNPair<>(m, n, idx), valri);
            
            final IcyBufferedImage valvi = new IcyBufferedImage(width, height, 1, DataType.DOUBLE);
            valvi.setDataXY(c, valv);
            polyv.put(new MNPair<>(m, n, idx), valvi);
            
            idx++;
         }
      }
   }
   
   return cnt;
}

private PolarCoord[] getPolarCoords() {
	PolarCoord[] res = new PolarCoord[width*height];
	int k = 0;
	for(int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
			CartCoord pt = new CartCoord(x,y);
			
			//pt = pt.mapToInternalCircle(center);
			pt = Mapper.mapToCercle(mapping, pt, center);
			res[k] = pt.getPolar();
			k = k +1;
		}
	}
	
	return res;	
}

}
