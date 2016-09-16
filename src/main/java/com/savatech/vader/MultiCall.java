package com.savatech.vader;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiCall implements InvocationHandler {

	private static final Logger logger = LoggerFactory.getLogger(MultiCall.class);

	private List<Object> objects = new LinkedList<>();

	public synchronized void add(Object o) {
		if (o == null) {
			logger.warn("Skipping null obsever", new RuntimeException("Null observer"));
			return;
		}
		objects.add(o);
	}

	public synchronized void remove(Object o) {
		objects.remove(o);
	}

	@Override
	public synchronized Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		for (Object o : objects) {
			try {
				method.invoke(o, args);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

}
