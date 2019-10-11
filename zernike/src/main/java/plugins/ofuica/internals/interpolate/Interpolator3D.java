package plugins.ofuica.internals.interpolate;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import plugins.ofuica.internals.calculate.MomentCalculator;
import plugins.ofuica.internals.calculate.PolynomialCalculator;
import plugins.ofuica.internals.dataTypes.CartCoord;
import plugins.ofuica.internals.dataTypes.FunctionMatrix;
import plugins.ofuica.internals.dataTypes.Matrix3D;
import plugins.ofuica.internals.utils.Benchmarking;
import plugins.ofuica.internals.utils.ZernikeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

public class Interpolator3D {
private String inPath, outPath, fileName;	
private String[] paths;
private double[] pos;
private double[] interpPos;

public Interpolator3D(String inpath, String outpath, String set){
	inPath = inpath;
	outPath = outpath;
	fileName = set;
	
	loadP(inpath + File.separator + fileName);

}

public void setOut(String p){
	outPath = p;
}

private void reorg(){
	for (int i = 0; i < (pos.length -1); i++){
		int mini = i;
		for (int j = i+1; j < pos.length; j++){
			if (pos[mini] > pos[j]){
				mini = j;
			}
		}
		if (i != mini){
			double temp = pos[i];
			pos[i] = pos[mini];
			pos[mini] = temp;
			
			String tempo = paths[i];
			paths[i] = paths[mini];
			paths[mini] = tempo;
		}
	}
}



private void loadP(String url){
	Properties prop = new Properties();
	try{
		prop.load(new FileInputStream(url));
		pos = new double[prop.size() - 1];
		paths = new String[prop.size() - 1];
		int i = 0;
		for (Enumeration e = prop.keys() ; e.hasMoreElements() ;) {
	         String data = (String) e.nextElement();
	         data = data.trim();

	         if (data.equalsIgnoreCase("interp")){
	        	 
	        	 String positions = prop.getProperty(data).trim();
	        	 StringTokenizer strk = new StringTokenizer(positions,"-");
	        	 interpPos = new double[strk.countTokens()];
	        	 int k = 0;
	        	 while (strk.hasMoreElements()){
	        		 interpPos[k] = Double.valueOf(strk.nextToken().trim());
	        		 k = k + 1;
	        	 }
	        	 
	         } else {
	        	 
	        	paths[i] = data;
	        	pos[i] = Double.parseDouble(prop.getProperty(data).trim());
	        	i = i + 1;
	         }
	        
	     }
		reorg();
	
	} catch (IOException e){IJ.error(e.toString());}
}



public String[] getPath(){
	return paths;
}
public double[] getPos(){
	return pos;
}
public double[] getInterpX(){
	return interpPos;
}



public void print(){
	for (int i = 0; i < paths.length; i++){
		IJ.log(paths[i] + " at " + pos[i]);
	}
	IJ.log("###########################");
	IJ.log("interpolation...");
	for (int i = 0; i < interpPos.length; i++){
		IJ.log(""+ interpPos[i]);
	}
}


public void interpolate3DPsf(int zernikeOrder, int interpOrder,int mapping){
	String pathpsf = inPath + File.separator + paths[0] + ".tif";	
	IJ.log("input from path: " + pathpsf);
	Benchmarking bc = new Benchmarking("zernike");
	bc.start();
	ImagePlus tmppsf = IJ.openImage(pathpsf);
	
	int width = tmppsf.getWidth();
	int height = tmppsf.getHeight();
	
	int plans = tmppsf.getStackSize();
	int nbpsf = paths.length;
		
	CartCoord center = new CartCoord(ZernikeUtil.getMassCenter(tmppsf.getImageStack().getProcessor((tmppsf.getStackSize()/ 2) + 1)));
	IJ.log("Center Coord to be used: " + center);
	IJ.log("Applying mapping type: " + mapping);
	
	bc.getCurrent("center shit");
	PolynomialCalculator poly = new PolynomialCalculator(width, height,center,mapping);

	ImageStack sRpq = new ImageStack(width, height);
	ImageStack sVpq = new ImageStack(width, height);
	
	IJ.log("Calculating Zernike Polynomials ...");
	double cnt = poly.getVRpqc(sRpq, sVpq, zernikeOrder);
	
	bc.getCurrent("poly done");
	IJ.log("Polynomials ready... Extracting Mask .");
	
	ImageProcessor mask = ZernikeUtil.errode1p(sVpq.getProcessor(1), 0.0);
	Matrix3D result = new Matrix3D(nbpsf,plans,ZernikeUtil.polynum(zernikeOrder));
	
	int nborder = result.getOrderNum();
	MomentCalculator mc = new MomentCalculator();
	IJ.log(" Starting Moment calculation using " + nborder + " order for " + nbpsf);
	
	result.setPsf(0,mc.getPolynomsc(ZernikeUtil.getMask(tmppsf.getImageStack(),mask), sVpq, cnt));
	IJ.log("psf 0 done ..." );
	
	for (int psf = 1; psf < nbpsf; psf++){
		pathpsf = inPath + File.separator + paths[psf] + ".tif";
		ImageStack ims = ZernikeUtil.getMask(IJ.openImage(pathpsf).getImageStack(),mask);
		result.setPsf(psf, mc.getPolynomsc(ims, sVpq, cnt));
		IJ.log("psf " + psf + " done ..." );
	}
	
	ZernikeInterpolator interp = new ZernikeInterpolator();
	IJ.log("Preparing For Interpolation ..." );
	
	FunctionMatrix func = new FunctionMatrix(plans,nborder);
	
	for (int plan = 0; plan < plans; plan++){
		IJ.log("Function Estimating for plan " + plan + " variations" );
		func.setPlanFit(interp.getInterpolatorFunction(interpOrder, result.getGlobalMVar(plan), getPos()), plan);

	}
	bc.getCurrent("estimation done");
	
	result = null;
	IJ.freeMemory();
	IJ.log("Interpolation begins ..." );
	Matrix3D resInt = interp.interpolate(func, getInterpX()) ;
	
	IJ.log("Reconstructing..." );
	IJ.log("output Folder is: " + outPath );
	
	for (int psf = 0; psf < resInt.getPsfNum(); psf++){
		IJ.log("reconstructing PSF " + psf + " at depth "  + interpPos[psf]);
		ImageStack npsf = mc.invzerseZernikePO(resInt.getPsf(psf), sRpq, sVpq, zernikeOrder);
		IJ.save(new ImagePlus("res" + String.valueOf(psf),npsf), outPath + File.separator + interpPos[psf] + "res.tif");
	}
	bc.getCurrent("alldone");
	bc.end();
}


}