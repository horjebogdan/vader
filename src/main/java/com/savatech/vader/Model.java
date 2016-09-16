package com.savatech.vader;

import java.lang.reflect.Proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model<O> {
	
	private static final Logger logger=LoggerFactory.getLogger(Model.class);
	
	private MultiCall multicall;
	private O observer;
	
	public Model(Class<O> oClass) {
		multicall=new MultiCall();
		observer=(O) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{oClass}, multicall);
	}
	
	public void addObserver(O o){
		multicall.add(o);
	}

	public void removeObserver(O o){
		multicall.remove(o);
	}
	
	protected O getObserver(){ 
		return observer;
	}

}
