package plugins.ofuica.zernike;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import plugins.ofuica.internals.interpolate.Interpolator3D;
import plugins.ofuica.internals.utils.Mapper;

public class Zernike_3D_Interpolator implements PlugIn {

	public void run(String arg) {
		
		//String in = "C:\\inpsf\\nonoise";
		//	String in = "C:\\inpsf\\30.0db";
			String in = "C:\\inpsf";
		//	String in = "C:\\inpsf\\10.0db";
			String out = "C:\\inpsf\\interp";
			String inset = "posprots.set";
		
		//String inref = "c:\\inpsf";
		//String outset= "settings.set";
		
		int mapping = Mapper.INTERNAL_MAPPING;
		GenericDialog dlg = new GenericDialog("param");
		dlg.addNumericField("order:",45,0);
		dlg.addNumericField("interp order:",5,0);
		dlg.showDialog();
		
		if (dlg.wasCanceled()) 
		   return;
		
		int num = (int) dlg.getNextNumber();
		int intord = (int) dlg.getNextNumber();
		
		Interpolator3D intrp = new Interpolator3D(in,out,inset);
		intrp.print();
		intrp.interpolate3DPsf(num, intord,mapping);
		IJ.log("Interpolation Done...");
		
		//InterpolationError ir = new InterpolationError(inref, out , out + File.separator + outset);
		//ir.calculate(""); 
	}
}
