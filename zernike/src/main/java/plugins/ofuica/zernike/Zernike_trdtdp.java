package plugins.ofuica.zernike;

import java.io.File;

import ij.IJ;

import ij.plugin.PlugIn;
import plugins.ofuica.internals.interpolate.Interpolator3DV;
import plugins.ofuica.internals.tests.InterpolationError;
import plugins.ofuica.internals.utils.Mapper;

public class Zernike_trdtdp implements PlugIn {

	public void run(String arg) {
		
		String in = "C:\\inpsf\\nonoise";
	//	String in = "C:\\inpsf\\30.0db";
	//	String in = "C:\\inpsf\\20.0db";
	//	String in = "C:\\inpsf\\10.0db";
		String out = "C:\\outpsf\\polyOrd";
		String inset = "posprot.set";
		String outset1= "settings1.set";
		String outset7= "settings7.set";
		String outset15= "settings15.set";
		
		int mapping = Mapper.INTERNAL_MAPPING;
		int num = 45;
		Interpolator3DV intrp = new Interpolator3DV(in,out,inset);
		intrp.print();
		intrp.interpolate3DPsf(num, 0, mapping);
		
		IJ.log("Interpolation Done...");
		InterpolationError ir = new InterpolationError(in, out , out + File.separator + outset1);
		ir.calculate("1�m"); 
		ir = new InterpolationError(in, out , out + File.separator + outset7);
		ir.calculate("7�m");
		ir = new InterpolationError(in, out , out + File.separator + outset15);
		ir.calculate("15�m");
	}
}
