package com.savatech.vader;

public class TimeUitls {
	public static String format(long h, long m, long s, long millis) {
		String hs = h < 10 ? "0" + h : "" + h;
		String ms = m < 10 ? "0" + m : "" + m;
		String ss = s < 10 ? "0" + s : "" + s;
		if (millis >= 0) {
			String miliss = millis < 100 ? "0" + millis : "" + millis;
			if (millis < 10) {
				miliss = "0" + miliss;
			}
			return hs + ":" + ms + ":" + ss + "." + miliss;
		}
		else
		{
			return hs + ":" + ms + ":" + ss; 
		}
	}

	public static String formatMicros(long micros) {
		long ms = micros / 1000;
		return formatMillis(ms);
	}

	public static String formatSeconds(long s) {
		long h = s / (60 * 60 );
		s = s - h * (60 * 60 );
		long m = s / 60 ;
		s = s - m * 60 ;
		
		return format(h, m, s, -1);
	}
	
	public static String formatMillis(long ms) {
		long h = ms / (60 * 60 * 1000);
		ms = ms - h * (60 * 60 * 1000);
		long m = ms / (60 * 1000);
		ms = ms - m * (60 * 1000);
		long s = ms / 1000;
		ms = ms - s * 1000;
		return format(h, m, s, ms);
	}
}
