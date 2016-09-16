package com.savatech.vader;

public class Slice  {

	public final long start;
	public final long end;
	public final long duration;
	public final int index;

	public Slice(int index, long start, long end) {
		this.index = index;
		this.start = start;
		this.end = end;
		this.duration = end - start;
	}



	public String getLabel() {
		return "slice " + index + " time:" + TimeUitls.formatMillis(duration);
	}

	public float getDurationInMillis() {
		return duration;
	}

	@Override
	public String toString() {
		return getLabel();
	}


	

}
