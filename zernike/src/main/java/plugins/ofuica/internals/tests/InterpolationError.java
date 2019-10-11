package plugins.ofuica.internals.tests;
import ij.IJ;
import ij.ImageStack;
import ij.gui.Plot;

import ij.process.ImageProcessor;
import plugins.ofuica.internals.utils.Couple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class InterpolationError {
	Vector <Couple> vect ;
	String inpath, outpath, set;
	
	public InterpolationError(String inpath, String outpath,String set){
		this.inpath = inpath;
		this.outpath = outpath;
		this.set = set;
		vect = new Vector<Couple>();
		loadP(set);
		print();
	}
	
	public void print() {
		for (Couple f : vect){
			IJ.log(f.toString());
		}
	}
	
	public void calculate(String name) {
		int numpsf = vect.size();
		double[] xs = new double[numpsf];
		double[] ys = new double[numpsf];
		
		for (int i = 0; i < numpsf; i++){
			xs[i] = i + 1;
			Couple f = vect.elementAt(i);
			String data1 = inpath + File.separator + f.getName1() + ".tif";
			String data2 = outpath + File.separator + f.getName2() + ".tif";
			ImageStack ims1 = IJ.openImage(data1).getImageStack();
			ImageStack ims2 = IJ.openImage(data2).getImageStack();
			ys[i] = errorCoef(ims1,ims2);
		}
		
		Plot stats_plot = new Plot ("Error in estimation " + name, "Plan","error",xs,ys);
		stats_plot.draw();
		stats_plot.show();
	}
	
	private double getMean(ImageStack ims){
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
	
	private double errorCoef(ImageStack im1, ImageStack im2){
		
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
	
	private void loadP(String url){
	   try {
	      BufferedReader input =  new BufferedReader(new FileReader(url));
	      try {
	        String line = null;
	        while (( line = input.readLine()) != null){
	        	String[] str = line.split("=");
	            vect.add(new Couple(str[0],str[1]));
	        }
	      }
	      finally {
	        input.close();
	      }
	    }
	    catch (IOException ex){
	      ex.printStackTrace();
	    }
	}
}
