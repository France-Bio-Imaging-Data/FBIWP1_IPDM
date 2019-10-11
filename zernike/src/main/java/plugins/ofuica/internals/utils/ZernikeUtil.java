package plugins.ofuica.internals.utils;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;
import icy.sequence.Sequence;
import icy.sequence.VolumetricImage;
import icy.type.DataType;
import ij.*;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import plugins.ofuica.internals.dataTypes.CartCoord;

public class ZernikeUtil {

public static CartCoord get3DMaxPoint(ImageStack ims){

	CartCoord pts = new CartCoord(0,0);
	float mval = 0;
	for (int z = 1 ; z <= ims.getSize(); z++){
		ImageProcessor ip = ims.getProcessor(z);
		if (ip.getMax() > mval) {
			for (int y = 0; y < ip.getHeight(); y++){
				for (int x = 0; x < ip.getWidth(); x++){
					if (ip.getPixelValue(x, y) > mval){
						mval = ip.getPixelValue(x, y);
						pts.setX(x);
						pts.setY(y);
					}
				}
			}
		}
	}
	
	return pts;
}

private static boolean checkpix2(ImageProcessor ip, int x, int y, double th){

	int tmpx1 = x + 1;
	if (tmpx1 >= ip.getWidth()){
		return true;
	}
	int tmpx2 = x - 1;
	if (tmpx2 < 0){
		return true;
	}
	int tmpy1 = y + 1;
	if (tmpy1 >= ip.getHeight()){
		return true;
	}
	int tmpy2 = y - 1;
	if (tmpy2 < 0){
		return true;
	}
	if ((ip.getPixelValue(tmpx1,tmpy1) <= th) || (ip.getPixelValue(tmpx2, tmpy2) <= th) || (ip.getPixelValue(tmpx1, tmpy2) <= th) || (ip.getPixelValue(tmpx2,tmpy1) <= th)){
		return true;
	}
	
	if ((ip.getPixelValue(tmpx1, y) <= th) || (ip.getPixelValue(tmpx2, y) <= th) || (ip.getPixelValue(x, tmpy1) <= th) || (ip.getPixelValue(x, tmpy2) <= th)){
		return true;
	} else {
		return false;
	}
}

private static boolean checkpix2(IcyBufferedImage ip, int x, int y, double th){
   int c = 0;
   int tmpx1 = x + 1;
   if (tmpx1 >= ip.getWidth()){
      return true;
   }
   int tmpx2 = x - 1;
   if (tmpx2 < 0){
      return true;
   }
   int tmpy1 = y + 1;
   if (tmpy1 >= ip.getHeight()){
      return true;
   }
   int tmpy2 = y - 1;
   if (tmpy2 < 0){
      return true;
   }
   if ((ip.getData(tmpx1,tmpy1, c) <= th) || (ip.getData(tmpx2, tmpy2, c) <= th) || (ip.getData(tmpx1, tmpy2, c) <= th) || (ip.getData(tmpx2,tmpy1, c) <= th)){
      return true;
   }
   
   if ((ip.getData(tmpx1, y, c) <= th) || (ip.getData(tmpx2, y, c) <= th) || (ip.getData(x, tmpy1, c) <= th) || (ip.getData(x, tmpy2, c) <= th)){
      return true;
   } else {
      return false;
   }
}

@SuppressWarnings("unused")
private static boolean checkpix(ImageProcessor ip, int x, int y, double th){

	int tmpx1 = x + 1;
	if (tmpx1 >= ip.getWidth()){
		return true;
	}
	int tmpx2 = x - 1;
	if (tmpx2 < 0){
		return true;
	}
	int tmpy1 = y + 1;
	if (tmpy1 >= ip.getHeight()){
		return true;
	}
	int tmpy2 = y - 1;
	if (tmpy2 < 0){
		return true;
	}
	
	if ((ip.getPixelValue(tmpx1, y) <= th) || (ip.getPixelValue(tmpx2, y) <= th) || (ip.getPixelValue(x, tmpy1) <= th) || (ip.getPixelValue(x, tmpy2) <= th)){
		return true;
	} else {
		return false;
	}	
}

public static ImageProcessor errode1p(ImageProcessor ip,double th) {
	FloatProcessor fp = new FloatProcessor(ip.getWidth(), ip.getHeight());
	
	for (int y = 0; y < ip.getHeight(); y++){
		for(int x = 0; x < ip.getWidth(); x++){
			if (checkpix2(ip,x,y,th) == true){
				fp.putPixelValue(x, y, 0.0);
			} else {
				fp.putPixelValue(x, y, 1.0);
			}
		}
	}
	
	return fp;
}

public static IcyBufferedImage errode1p(IcyBufferedImage image, double th) {
   
   IcyBufferedImage fp = new IcyBufferedImage(image.getWidth(), image.getHeight(), 1, DataType.DOUBLE);
   
   for (int y = 0; y < image.getHeight(); y++){
      for(int x = 0; x < image.getWidth(); x++){
         if (checkpix2(image,x,y,th) == true){
            fp.setData(x, y, 0, 0.0);
         } else {
            fp.setData(x, y, 0, 1.0);
         }
      }
   }
   
   return fp;
}

public static ImageStack getMask(ImageStack ims, ImageProcessor ip){
	ImageStack res = new ImageStack(ims.getWidth(), ims.getHeight());
	for (int z = 1; z <= ims.getSize(); z++){
		ImageProcessor ipp = ims.getProcessor(z).duplicate();
		ipp.copyBits(ip, 0, 0, Blitter.MULTIPLY);
		res.addSlice(String.valueOf(z),ipp);
	}
	return res;
}

public static VolumetricImage getMask(Sequence ims, IcyBufferedImage ip) {
   int t = 0;
   int c = 0;
   final VolumetricImage res = new VolumetricImage();
   for (int z = 0; z < ims.getSizeZ(); z++) {
      IcyBufferedImage ipp = IcyBufferedImageUtil.getCopy(ims.getImage(t, z));
      ipp.setDataXY(c, ArrayMath.multiply(ipp.getDataXY(c), ipp.getDataXY(c)));
      res.setImage(z, ipp);
   }
   return res;
}

public static double getMaxPoint(ImageStack ims){
	float mval = 0;
	for (int z = 1 ; z <= ims.getSize(); z++){
		ImageProcessor ip = ims.getProcessor(z);
		if (ip.getMax() > mval) {
			for (int y = 0; y < ip.getHeight(); y++){
				for (int x = 0; x < ip.getWidth(); x++){
					if (ip.getPixelValue(x, y) > mval){
						mval = ip.getPixelValue(x, y);
					}
				}
			}
		}
	}
	return mval;
}

public static CartCoord getMassCenter(ImageProcessor ip){
	float sum = 0;
	float xsum = 0;
	float ysum = 0;
	for (int y = 0; y < ip.getHeight(); y++){
		for (int x = 0; x < ip.getWidth(); x++){
			float val = ip.getPixelValue(x, y);
			sum = sum + val;
			xsum = xsum + (x * val);
			ysum = ysum + (y * val);
		}
	}
	
	int xpos = Math.round(xsum / sum);
	int ypos = Math.round(ysum / sum);
	
	CartCoord pts = new CartCoord(xpos, ypos);
	return pts;
}

public static CartCoord getMassCenter(final IcyBufferedImage image, int channel) {
   double sum = 0;
   double xsum = 0;
   double ysum = 0;
   
   for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
         double val = image.getData(x, y, channel);
         sum += val;
         xsum += x * val;
         ysum += y * val;
      }
   }
   
   if ( sum > 0 ) {
      int xpos = (int) Math.round(xsum/sum);
      int ypos = (int) Math.round(ysum/sum);
      return new CartCoord(xpos, ypos);
   }
   
   return new CartCoord(0, 0);
}

public static int getStackPos(int m , int n){
	int res = 0;
	if ((m % 2) == 0){
		res =   ((((m-1)/2) + 1) * (((m-1)/2)+2)) + (n / 2) + 1;
	} else {
		res = ((((m-2)/2) + 1) * (((m-2)/2)+2)) + ((m-1)/2) + (n / 2) + 2 ;
	}
	return res;
}


public static int polynum(int m){

	int res;
	if ((m % 2) == 0){
		res =   ((((m-2)/2) + 1) * (((m-2)/2) + 2)) + ((m/2) + 1) ;
	} else {
		res = ((m/2)+1) * ((m/2)+2) ;
	}
	return res;
}

public static int getStackPos2(int m , int n){
	int s = 0;
	for (int i = 0; i < m; i++){
		s = s + (i / 2) + 1 ;
	}
	
	s = s + (n/2) + 1;
	return s;
}

public static FloatProcessor makeFloat(ImageProcessor ip){
	FloatProcessor fp = new FloatProcessor(ip.getWidth(), ip.getHeight(), (float[])((ip instanceof FloatProcessor)?ip.duplicate().getPixels():ip.convertToFloat().getPixels()), null);
	return fp;
}

}
