package plugins.ofuica.internals.tests;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import plugins.ofuica.internals.calculate.MomentCalculator;
import plugins.ofuica.internals.calculate.PolynomialCalculator;
import plugins.ofuica.internals.dataTypes.CartCoord;
import plugins.ofuica.internals.dataTypes.Matrix3D;
import plugins.ofuica.internals.utils.Mapper;
import plugins.ofuica.internals.utils.ZernikeUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

public class Courbe {
private String inPath, outPath, fileName;	
private String[] paths;
private double[] pos;
private double[] interpPos;

public Courbe(String inpath, String outpath, String set){
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

public String[] getPath() {
	return paths;
}

public double[] getPos() {
	return pos;
}

public void print() {
	for (int i = 0; i < paths.length; i++){
		IJ.log(paths[i] + " at " + pos[i]);
	}
	IJ.log("###########################");
	IJ.log("interpolation...");
	for (int i = 0; i < interpPos.length; i++){
		IJ.log(""+ interpPos[i]);
	}
}

public void calculate(int zernikeOrder) {
	String pathpsf = inPath + File.separator + paths[0] + ".tif";
	IJ.log("input from path: " + pathpsf);
	ImagePlus tmppsf = IJ.openImage(pathpsf);
	
	int mapping  = Mapper.INTERNAL_MAPPING;
	int width = tmppsf.getWidth();
	int height = tmppsf.getHeight();
	
	int plans = tmppsf.getStackSize();
	int nbpsf = paths.length;
	
	CartCoord center = new CartCoord(ZernikeUtil.getMassCenter(tmppsf.getImageStack().getProcessor((tmppsf.getStackSize()/ 2) + 1)));
	IJ.log("Center Coord to be used: " + center);
	
	PolynomialCalculator poly = new PolynomialCalculator(width, height,center,mapping);
	
	ImageStack sRpq = new ImageStack(width, height);
	ImageStack sVpq = new ImageStack(width, height);
	IJ.log("Calculating Zernike Polynomials ...");
	
	double cnt = poly.getVRpqc(sRpq, sVpq, zernikeOrder);
	
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
	
	for (int plan = 0; plan < plans; plan++){
		String name = outPath + File.separator + plan + "var.dat";
		IJ.log("printing file " + name);
		printResults(result.getGlobalMVar(plan),zernikeOrder,name);
	}
}

public void printResults(double[][] res, int maxorder, String filep) {
	int nbpsf = res[0].length;
	int nbmom = res.length;
	try {
		PrintWriter out = new PrintWriter(new FileWriter(filep));
		out.print("PSF;");
		for (int m = 0; m <= maxorder; m++){
			for (int n = 0; n <= m ; n++){
				if(((m - n) % 2) == 0){
					out.print("Z" + m + "" + n + "; ");
				}
			}
		}
		out.print("\n");
		for (int i = 0; i < nbpsf; i++){
			out.print(i + "; ");
			for (int j = 0; j < nbmom; j++){
				out.print(res[j][i] + "; ");
			}
			out.print("\n");
		}
		out.close();

		} catch (IOException e){
			System.err.println(e);
		}
}

}