package plugins.ofuica.internals.interpolate;

import plugins.ofuica.internals.dataTypes.FunctionMatrix;
import plugins.ofuica.internals.dataTypes.Matrix3D;
import plugins.ofuica.internals.jamlab.Polyfit;
import plugins.ofuica.internals.jamlab.Polyval;

public class ZernikeInterpolator {

public Polyfit[] getInterpolatorFunction(int order, double[][] zernikePolynoms, double[] x) {
	int numPoly = zernikePolynoms.length;
	Polyfit[] res = new Polyfit[numPoly];
	
	try{
		for (int i = 0; i < numPoly; i++) {
			res[i] =  new Polyfit(x,zernikePolynoms[i],order);
		}
	} catch (Exception e){
		System.err.println("Error Interpolating: " + e);
	}
	
	return res;
}

public Matrix3D interpolate(FunctionMatrix func, double[] x) {
	int numpsf = x.length;
	int plans = func.getPlannum();
	int ordern = func.getOrdernum();
	
	Matrix3D res = new Matrix3D(numpsf,plans,ordern);
	
	for (int plan = 0; plan < plans; plan++){
		for (int ord = 0; ord < ordern; ord++){
			Polyval polv = new Polyval(x,func.getFit(plan, ord));
			res.setMomentVar(ord, plan, polv.getYout());
		}
	}
	
	return res;
}

public double[][] interpolatePolynomials(Polyfit[] interpFunctions, double[] x){
	/* i,j , i: la coef interpol� et j les valeur pour chaque x
	 * interpfunct , i: coefzernike , j les coef du fonction
	 */
	
	int nbpsf = x.length;
	int numpoly = interpFunctions.length;
	double[][] res = new double[numpoly][nbpsf];
	for (int i = 0; i < numpoly; i++){
		Polyval polv = new Polyval(x,interpFunctions[i]);
		res[i] = polv.getYout();
	}
	
	return res;
}

}
