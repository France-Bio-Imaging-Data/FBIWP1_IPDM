package plugins.ofuica.internals.dataTypes;

public class Matrix3D {
	
	private double mat[][][];

	public int getPsfNum(){
		return mat.length;
	}
	
	public int getPlanNumByPsf(){
		return mat[0].length;
	}

	public int getOrderNum(){
		return mat[0][0].length;
	}
	
	public Matrix3D(int psf, int plan, int order){
		mat = new double[psf][plan][order];
	}

	public void setPsf(int psf,double[][] psf3d){
		mat[psf] = psf3d;
	}
	
	public double[][] getPsf(int psf){
		return mat[psf];
	}
	
	public double[] getPlan(int psf, int plan){
		return mat[psf][plan];
	}
	
	public double getMoment(int psf, int plan, int ord){
		return mat[psf][plan][ord];
	}
	
	public double getMoment(int psf, int plan, int m , int n){
		return getMoment(psf,plan,getOrderPos(m,n));
	}
	
	public void setMoment(int psf, int plan, int ord, double val){
		mat[psf][plan][ord] = val;
	}
	
	public double[] getMomentVar(int m, int n, int plan){
		return getMomentVar(getOrderPos(m,n),plan);
	}
	
	public double[] getMomentVar(int orderNum, int plan){
		int z = this.getPsfNum();
		double[] res = new double[z];
		for(int psf = 0; psf < z; psf++){
			res[psf] = getMoment(psf,plan,orderNum);
		}
		return res;
	}
	
	public double[][] getGlobalMVar(int plan){
		int z = this.getPsfNum();
		int x = this.getOrderNum();
		
		double res[][] = new double[x][z];
		for (int psf = 0; psf < z; psf++){
				for (int ord = 0; ord < x; ord++){
					double v = mat[psf][plan][ord];
					res[ord][psf] = v;
				}
		}
		return res;
	}
	
	public void setMomentVar(int order, int plan, double[] pts){
		for(int psf = 0; psf < getPsfNum(); psf++){
			mat[psf][plan][order] = pts[psf];
		}
	}
	
	public double[][][] getMatrix(){
		return mat;
	}
	
	private int getOrderPos(int m , int n){
		int res = 0;
		if ((m % 2) == 0){
			res =   ((((m-1)/2) + 1) * (((m-1)/2)+2)) + (n / 2) + 1;
		} else {
			res = ((((m-2)/2) + 1) * (((m-2)/2)+2)) + ((m-1)/2) + (n / 2) + 2 ;
		}
		return res;
	}
}
