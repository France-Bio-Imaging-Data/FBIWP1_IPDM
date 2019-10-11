package plugins.ofuica.internals.dataTypes;

import plugins.ofuica.internals.jamlab.Polyfit;

public class FunctionMatrix {
	
	private Polyfit[][] func;

	public FunctionMatrix(int plans, int orders){
		func = new Polyfit[plans][orders];
	}
	
	public void setPlanFit(Polyfit[] pf, int plan){
		func[plan] = pf;
	}
	
	public void setFit(Polyfit pf, int plan, int ord){
		func[plan][ord] = pf;
	}
	
	public Polyfit[] getPlanFit(int plan){
		return func[plan];
	}
	
	public Polyfit getFit(int plan, int order){
		return func[plan][order];
	}
	public int getPlannum(){
		return func.length;
	}
	public int getOrdernum(){
		return func[0].length;
	}
}
