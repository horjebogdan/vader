package com.savatech.vader;

import java.util.List;

public class TestObserver implements ProjectObserver {

	private List<Slice> slices;
	private String info;

	public List<Slice> getSlices() {
		return slices;
	}

	@Override
	public void updateInfo(Project project, String info) {
		this.info = info;
	}

	@Override
	public void playing(String name, long amicros,long micros) {
		// TODO Auto-generated method stub

	}

}
