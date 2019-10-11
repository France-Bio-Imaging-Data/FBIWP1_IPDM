package plugins.ofuica.internals.calculate;

import java.util.Map;
import java.util.TreeMap;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
import icy.type.DataType;
import ij.ImageStack;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import plugins.ofuica.internals.utils.MNPair;
import plugins.ofuica.internals.utils.ZernikeUtil;

public class MomentCalculator {
public static final double M_PI=3.14159265358979323846; 
public static final double minm = 1e-10;	

private double getAmnc(ImageProcessor img, ImageProcessor pol,int m, double cnt) {
	double res = 0.0;
	
	for (int y = 0; y < img.getHeight(); y++){
		for (int x = 0; x < img.getWidth(); x++){
			res = res + (img.getPixelValue(x, y) * pol.getPixelValue(x, y));
		}
	}
	
	res = res * ((m + 1.0) / cnt);
	
	if (Math.abs(res) < minm){
		res = 0.0;
	}
	
	return res;
}

private double getAmnc(IcyBufferedImage img, IcyBufferedImage pol,int m, double cnt) {
   double res = 0.0;
   int c = 0;
   for (int y = 0; y < img.getHeight(); y++){
      for (int x = 0; x < img.getWidth(); x++){
         res = res + (img.getData(x, y, c) * pol.getData(x, y, c));
      }
   }
   
   res = res * ((m + 1.0) / cnt);
   
   if (Math.abs(res) < minm){
      res = 0.0;
   }
   
   return res;
}

private int getm(String name) {
	String m = name.split(",")[0];
	return Integer.parseInt(m);
}

public ImageStack invzerseZernikePO(double[][] planOrderStack, ImageStack sRpq,ImageStack sVpq, int maxorder ) {
	int plans = planOrderStack.length;
	int width = sRpq.getWidth();
	int height = sRpq.getHeight();
	
	ImageStack res = new ImageStack(width, height);
	
	for (int plan = 0; plan < plans; plan++) {
		FloatProcessor fp = new FloatProcessor(width,height);
		fp.setValue(0.0);
		fp.fill();
		
		for (int m = 0; m <= maxorder ; m++) {
			for (int n = 1; n <= m; n++){
				if (((m-n)%2) == 0){
					int pos = ZernikeUtil.getStackPos(m, n);
					ImageProcessor vmn = sVpq.getProcessor(pos).duplicate();
					vmn.multiply(2 * planOrderStack[plan][pos - 1] );
					fp.copyBits(vmn, 0, 0, Blitter.ADD);
				}
			}
			int zerp = ZernikeUtil.getStackPos(m, 0);
			ImageProcessor cp0 = sRpq.getProcessor(zerp).duplicate();
			cp0.multiply(planOrderStack[plan][zerp-1]);
			fp.copyBits(cp0, 0, 0, Blitter.ADD);
			
		}
		
		fp.min(0.0);
		res.addSlice(String.valueOf(plan+1), fp);
	}
	
	return res;
}

public Sequence invzerseZernikePO(double[][] planOrderStack, TreeMap<MNPair<Integer>, IcyBufferedImage> sRpq,
      TreeMap<MNPair<Integer>, IcyBufferedImage> sVpq, int maxorder) {
   
//   int plans = planOrderStack.length;
//   IcyBufferedImage sPgqFirst = sRpq.get(new MNPair<>(0,0,0));
//   int width = sPgqFirst.getWidth();
//   int height = sPgqFirst.getHeight();
//   final Sequence res = new Sequence();
//   
//   for (int plan = 0; plan < plans; plan++) {
//      FloatProcessor fp = new FloatProcessor(width, height);
//      fp.setValue(0.0);
//      fp.fill();
//      
//      for (int m = 0; m <= maxorder ; m++) {
//         for (int n = 1; n <= m; n++){
//            if (((m-n)%2) == 0){
//               int pos = ZernikeUtil.getStackPos(m, n);
//               ImageProcessor vmn = sVpq.getProcessor(pos).duplicate();
//               vmn.multiply(2 * planOrderStack[plan][pos - 1] );
//               fp.copyBits(vmn, 0, 0, Blitter.ADD);
//            }
//         }
//         int zerp = ZernikeUtil.getStackPos(m, 0);
//         ImageProcessor cp0 = sRpq.getProcessor(zerp).duplicate();
//         cp0.multiply(planOrderStack[plan][zerp-1]);
//         fp.copyBits(cp0, 0, 0, Blitter.ADD);
//      }
//      
//      fp.min(0.0);
//      final IcyBufferedImage slice = new IcyBufferedImage(width, height, 1, DataType.DOUBLE);
//      
//      // res.addSlice(String.valueOf(plan+1), fp);
//      
//      res.addImage(slice);
//   }
//   
//   return res;
   return new Sequence();
}

public double[][] getPolynomsc(ImageStack psfs,ImageStack vmn,double cnt) {
	int plans = psfs.getSize();
	int polynum = vmn.getSize();
	double[][] res = new double[plans][polynum];
	
	for (int psf = 0; psf < plans; psf++){
		ImageProcessor psfIp = psfs.getProcessor(psf+1);
		for (int p = 0; p < polynum; p++){
			ImageProcessor pol = vmn.getProcessor(p+1);
			int m = getm(vmn.getSliceLabel(p+1));
			if ((m%2) != 0){
				res[psf][p] = 0.0;
			} else {
				if ((p % 2) != 0){
					res[psf][p] = 0.0;
				} else {
					res[psf][p] = getAmnc(psfIp,pol,m,cnt);
				}
			}
		}
	}
	
	return res;
}

public double[][] getPolynomsc(VolumetricImage psfs, TreeMap<MNPair<Integer>, IcyBufferedImage> vmn,double cnt) {
   int plans = psfs.getSize();
   int polynum = vmn.size();
   double[][] res = new double[plans][polynum];
   
   for (int psf = 0; psf < plans; psf++) {
      final IcyBufferedImage psfIp = psfs.getImage(psf);
      for ( MNPair<Integer> pair : vmn.keySet() ) {
         final IcyBufferedImage pol = vmn.get(pair);
         int m = pair.getM();
         int p = pair.getIndex();
         if ((m%2) != 0){
            res[psf][p] = 0.0;
         } else {
            if ((p % 2) != 0){
               res[psf][p] = 0.0;
            } else {
               res[psf][p] = getAmnc(psfIp,pol,m,cnt);
            }
         }
         p++;
      }
   }
   
   return res;
}

}
