package plugins.ofuica.internals.utils;

import ij.ImageStack;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class Noise {
	
public ImageStack addGaussianNoise(ImageStack ims, double db){
	 double pct = 100 / (Math.pow(10,(db / 20)));
	 return addGaussianNoisePCT(ims,pct);
}

private FloatProcessor makeFloat(ImageProcessor ip){
	FloatProcessor fp = new FloatProcessor(ip.getWidth(), ip.getHeight(), (float[])((ip instanceof FloatProcessor)?ip.duplicate().getPixels():ip.convertToFloat().getPixels()), null);
	return fp;
}

public ImageStack addGaussianNoisePCT(ImageStack img, double pct){
   
	   ImageStack noise = new ImageStack(img.getWidth(),img.getHeight());
	   for (int z = 1; z <= img.getSize(); z++){
		   FloatProcessor fp = new FloatProcessor(img.getWidth(),img.getHeight());
		   fp.setValue(0.0);
		   fp.fill();
		   fp.noise(pct);
		   noise.addSlice(String.valueOf(z),fp);
	   }
	   
	   double offset = 0;
	   double max = 0;
	   double maximg = 0;

	   ImageStack result = new ImageStack(noise.getWidth(), noise.getHeight());
	   for (int z = 1; z <= noise.getSize();z++){
		   ImageProcessor ip = noise.getProcessor(z);
		   FloatProcessor fp = makeFloat(img.getProcessor(z));
		   result.addSlice(String.valueOf(z),fp);
		   for (int y = 0; y < noise.getHeight();y++){
			   for (int x = 0; x < noise.getWidth();x++){
				   double val = ip.getPixelValue(x,y);
				   double valim = fp.getPixelValue(x,y);
				   if (val < offset){
					   offset = val;
				   }
				   
				   if (val > max){
					   max = val;
				   }
				   if (valim > maximg){
					   maximg = valim;
				   }
			   }
		   }
	   }
	   offset = offset * (-1);
	   pct = pct / 100;
	   max = max + offset;
	   for (int z = 1; z <= noise.getSize(); z++){
		   ImageProcessor ip = noise.getProcessor(z);
		   ImageProcessor ip2 = result.getProcessor(z);
		   ip.add(offset);
		   ip.multiply(1 / max);
		   ip2.multiply(1 / maximg);
		   ip.multiply(pct);
		   ip2.copyBits(ip,0,0,Blitter.ADD);
		   ip2.resetMinAndMax();
	   } 

	   return result;
}
}
