package plugins.ofuica.internals.utils;

import ij.text.*;
import java.util.Date;
import java.text.SimpleDateFormat;
public class Benchmarking {
public static final int Millisec = 0;
public static final int Second = 1;
public static final int SecondPlus = 2;
private TextWindow tw;
private long timer;
private int mode = 0;
public Benchmarking(String name){
	tw = new TextWindow("Benshmark of :" + name,"ready...",300,200);
	timer = 0;
}

public Benchmarking(String name, int mode){
	tw = new TextWindow("Benshmark of :" + name,"ready...",300,200);
	timer = 0;
	this.mode = mode;
}
public String getTime() {
	    Date today = new Date();
	    SimpleDateFormat formatter = new SimpleDateFormat("H:mm:ss:SSS");
	    String datenewformat = formatter.format(today);
	    return  datenewformat;
}

public void start(){
	tw.append("Benchmarking started @ " + getTime());
    timer = System.currentTimeMillis();
}
	
public void getCurrent(String tag){
	long tempo = System.currentTimeMillis() - timer;
	String data = "";
	switch (mode){
	case 0 :{
		data = String.valueOf(tempo) + "ms" ;
		break;
	}
	case 1 : {
		data = String.valueOf(tempo / 1000) + "Seconds" ;
		break;
	}
	case 2 :
		
		int hours = (int) Math.floor(tempo / 3600000);
		int min = (int) (Math.floor(tempo /60000) - (hours * 60));
		int sec = (int) (Math.floor(tempo / 1000) - (hours * 3600) - (min * 60));
		data = hours + "H : " + min + "M : " + sec + "S " ;
		break;
	}
	
	
	tw.append("Benshmark tag: " + tag + " @ " + getTime());
	tw.append("time elapsed: " + data );
}

public void raz(){
	timer = 0;
}
public void write(String str){
	tw.append(str);
}

public void end(){
	this.getCurrent("All ended");
	timer = 0;
}
}
