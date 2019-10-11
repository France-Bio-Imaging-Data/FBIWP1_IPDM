package plugins.ofuica.internals.utils;

import ij.ImageStack;
import ij.process.ImageProcessor;

public class Errors {

public static double getSum(ImageStack ims){
	double sum = 0.0;
	for (int z = 1; z <= ims.getSize(); z++){
		ImageProcessor ip = ims.getProcessor(z);
		for (int y = 0; y < ip.getHeight(); y++){
			for (int x = 0; x < ip.getWidth(); x++){
				sum = sum + ip.getPixelValue(x, y);
			}
		}
	}
	return sum;
}

public static double getMean(ImageStack ims){
		double sum = 0.0;
		double num = 0.0;
		for (int z = 1; z <= ims.getSize(); z++){
			ImageProcessor ip = ims.getProcessor(z);
			for (int y = 0; y < ip.getHeight(); y++){
				for (int x = 0; x < ip.getWidth(); x++){
					sum = sum + ip.getPixelValue(x, y);
					num = num + 1;
				}
			}
			
		}
		return (sum/num);
}

public static double getMean(ImageProcessor ip){
		double sum = 0.0;
		double num = 0.0;
		for (int y = 0; y < ip.getHeight(); y++){
			for (int x = 0; x < ip.getWidth(); x++){
				sum = sum + ip.getPixelValue(x, y);
				num = num + 1;
			}
		}
			
		return (sum/num);
}

public static double coefCorrel2D(ImageProcessor ip1, ImageProcessor ip2){
	double num = 0.0;
	double x2 = 0.0;
	double y2 = 0.0;
	double m1 = getMean(ip1);
	double m2 = getMean(ip2);
	
	for (int y = 0; y < ip1.getHeight(); y++){
		for (int x = 0; x < ip1.getWidth(); x++){
			double val1 = ip1.getPixelValue(x, y);
			double val2 = ip2.getPixelValue(x, y);
			num = num + ((val1 - m1) * (val2 - m2));
			x2 = x2 + (Math.pow((val1 - m1), 2));
			y2 = y2 + (Math.pow((val2 - m2), 2));
		}
	}
	x2 = Math.sqrt(x2);
	y2 = Math.sqrt(y2);
	return (num / (x2 * y2));
}

public static double coefCorrel3D(ImageStack im1, ImageStack im2){
	double num = 0.0;
	double x2 = 0.0;
	double y2 = 0.0;
	
	double m1 = getMean(im1);
	double m2 = getMean(im2);
	
	for (int z = 1; z <= im1.getSize(); z++){
		ImageProcessor ip1 = im1.getProcessor(z);
		ImageProcessor ip2 = im2.getProcessor(z);
		for (int y = 0; y < ip1.getHeight(); y++){
			for (int x = 0; x < ip1.getWidth(); x++){
				double val1 = ip1.getPixelValue(x, y);
				double val2 = ip2.getPixelValue(x, y);
				num = num + ((val1 - m1) * (val2 - m2));
				x2 = x2 + (Math.pow((val1 - m1), 2));
				y2 = y2 + (Math.pow((val2 - m2), 2));
			}
		}
	}
	
	x2 = Math.sqrt(x2);
	y2 = Math.sqrt(y2);
	
	return (num / (x2 * y2));
}

public static double coefCorrel2D3(ImageStack ims1, ImageStack ims2){
	double sum = 0.0;
	for (int z=1; z <=ims1.getSize(); z++){
		ImageProcessor ip1 = ims1.getProcessor(z);
		ImageProcessor ip2 = ims2.getProcessor(z);
		sum = sum + coefCorrel2D(ip1,ip2);
	}
	
	return sum/(ims1.getSize() * 1.0);
}

public static double meanAbsPcntError(ImageStack ims1, ImageStack ims2){
	
	/* 100/n ( sigma(abs(x - ^x)))
	 * 
	 */

	double res = absoluteNormxError(ims1,ims2);
	double m = ((100 * 1.0) / (ims1.getSize() * ims1.getWidth() * ims1.getHeight() * 1.0));
	return res * m;
}

public static double absoluteNormxError(ImageStack ims1, ImageStack ims2){
	/*
	 * sigma(abs(x - ^x)/x)
	 */
	double sum = 0.0;
	for (int z = 1; z <= ims1.getSize(); z++){
		ImageProcessor ip1 = ims1.getProcessor(z);
		ImageProcessor ip2 = ims2.getProcessor(z);
		for (int y = 0; y < ip1.getHeight(); y++){
			for (int x = 0; x < ip1.getWidth(); x++){
				double val = ip1.getPixelValue(x, y);
				if (val != 0.0){
					sum = sum + (Math.abs(val - ip2.getPixelValue(x, y)) / Math.abs(val));
				}
			}
		}
	}
	return sum;
}

public static double meanAbsoluteError(ImageStack ims1, ImageStack ims2){
	/*
	 * sigma(abs(x - ^x)) * (1 / n)
	 */
	double sum = 0.0;
	int size = ims1.getSize();
	int w = ims1.getWidth();
	int h = ims1.getHeight();
	for (int z = 1; z <= size; z++){
		ImageProcessor ip1 = ims1.getProcessor(z);
		ImageProcessor ip2 = ims2.getProcessor(z);
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
	
				sum = sum + Math.abs(ip1.getPixelValue(x, y) - ip2.getPixelValue(x, y)) ;
			}
		}
	}
	double n = size * w * h;
	return (sum / n) ;
}

public static double meanSquaredError(ImageStack ims1, ImageStack ims2){
	/*
	 * sigma(pow(x - ^x),2) * (1/n)
	 */
	double sum = 0.0;
	int size = ims1.getSize();
	int w = ims1.getWidth();
	int h = ims1.getHeight();
	for (int z = 1; z <= size; z++){
		ImageProcessor ip1 = ims1.getProcessor(z);
		ImageProcessor ip2 = ims2.getProcessor(z);
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				sum = sum + Math.pow((ip1.getPixelValue(x, y) - ip2.getPixelValue(x, y)), 2);
			}
		}
	}
	double n = size * w * h;
	return (sum / n) ;
}

public static double normalizedSquareError(ImageStack ims1, ImageStack ims2){
	/*
	 * sigma(pow(((x - ^x) / sigma(x)),2)
	 */
	
	double sumx = getSum(ims1);
	double sum = 0.0;
	int size = ims1.getSize();
	int w = ims1.getWidth();
	int h = ims1.getHeight();
	for (int z = 1; z <= size; z++){
		ImageProcessor ip1 = ims1.getProcessor(z);
		ImageProcessor ip2 = ims2.getProcessor(z);
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){
				
				double val = (ip1.getPixelValue(x, y) - ip2.getPixelValue(x, y)) / sumx ;
				sum = sum + Math.pow(val, 2);
			}
		}
	}
	double n = 1.0;
	//double n = size * w * h;
	return (sum / n) ;
}

public static double normalisedSquared2Error(ImageStack ims1, ImageStack ims2){

	double sumx = getSum(ims1);
	double sumy = getSum(ims2);
	double sum = 0.0;
	int size = ims1.getSize();
	int w = ims1.getWidth();
	int h = ims1.getHeight();
	for (int z = 1; z <= size; z++){
		ImageProcessor ip1 = ims1.getProcessor(z);
		ImageProcessor ip2 = ims2.getProcessor(z);
		for (int y = 0; y < h; y++){
			for (int x = 0; x < w; x++){				
				double val = (ip1.getPixelValue(x, y)/sumx) - (ip2.getPixelValue(x, y)/ sumy) ;
				sum = sum + Math.pow(val, 2);
			}
		}
	}

	double n = 1.0;
	//double n = size * w * h;
	return (sum / n) ;
}


}