package plugins.ofuica.zernike;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import ij.gui.GenericDialog;
import ij.plugin.filter.*;

import ij.process.ImageProcessor;
import plugins.ofuica.internals.calculate.MomentCalculator;
import plugins.ofuica.internals.calculate.PolynomialCalculator;
import plugins.ofuica.internals.dataTypes.CartCoord;
import plugins.ofuica.internals.utils.Mapper;
import plugins.ofuica.internals.utils.ZernikeUtil;

public class Reconst_zern_3D implements PlugInFilter{
private ImageStack ims;

	public void run(ImageProcessor ip) {
		CartCoord center = new CartCoord(ZernikeUtil.getMassCenter(ims.getProcessor((ims.getSize()/ 2)+1)));
		
		int mapping = Mapper.INTERNAL_MAPPING;
		GenericDialog dlg = new GenericDialog("param");
		dlg.addNumericField("order:",45,0);
		dlg.addCheckbox("External Circle", true);
		dlg.showDialog();
		
		if (dlg.wasCanceled())
		   return;
		
		int maxOrder = (int) dlg.getNextNumber();
		boolean mp = dlg.getNextBoolean();
		
		if (mp == true){
			mapping = Mapper.EXTERNAL_MAPPING;
		}
		
		PolynomialCalculator poly = new PolynomialCalculator(ims.getWidth(), ims.getHeight(),center, mapping);
		
		ImageStack sRpq = new ImageStack(ims.getWidth(), ims.getHeight());
		ImageStack sVpq = new ImageStack(ims.getWidth(), ims.getHeight());
		
		double cnt = poly.getVRpqc(sRpq, sVpq, maxOrder);
		
		ImageStack mims = ZernikeUtil.getMask(ims, ZernikeUtil.errode1p(sVpq.getProcessor(1), 0.0));
		ImagePlus pool1 = new ImagePlus("mim",mims);
		pool1.show();
		ImagePlus pool12 = new ImagePlus("pol",sVpq);
		pool12.show();
		IJ.log("Poly calculated....");
		MomentCalculator mc = new MomentCalculator();
		double[][] moments = mc.getPolynomsc(mims,sVpq,cnt);
		
		IJ.log("Moments calculated....");
		ImageStack res = mc.invzerseZernikePO(moments, sRpq, sVpq, maxOrder);
		IJ.log("estimate calculated....");
		ImagePlus imp = new ImagePlus("res",res);
		imp.show();
	}
	
	public int setup(String arg, ImagePlus imp) {
		ims = imp.getImageStack();
		return DOES_ALL;
	}
}
