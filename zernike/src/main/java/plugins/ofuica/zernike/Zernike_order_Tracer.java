package plugins.ofuica.zernike;


import ij.IJ;

import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import plugins.ofuica.internals.tests.Courbe;

public class Zernike_order_Tracer implements PlugIn{

	public void run(String arg) {
		
		String in = "C:\\inpsf\\20.0db";
		String out = "c:\\outpsf\\var";
		String inset = "posprots.set";
		
		GenericDialog dlg = new GenericDialog("param");
		dlg.addNumericField("order:",45,0);
		dlg.showDialog();
		
		if (dlg.wasCanceled()) 
		   return;
		
		int num = (int) dlg.getNextNumber();
		
		Courbe cb = new Courbe(in,out,inset);
		cb.calculate(num);
		IJ.error("ok all done");
	}
}
