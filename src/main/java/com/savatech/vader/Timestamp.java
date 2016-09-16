package com.savatech.vader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timestamp {
	private static final Logger logger = LoggerFactory.getLogger(Timestamp.class);

	private long microseconds;

	public static final Timestamp in(String text, int at) {
		int ei = -1;
		while (at >= 0) {
			char c = text.charAt(at);
			if (']' == c) {
				ei = at;
			}

			if ('[' == c && ei >= 0) {
				String s = text.substring(at + 1, ei).trim();
				if (s.startsWith("@")) {
					try {
						s = s.substring(1);
						String[] se = s.split(":");
						int h = Integer.parseInt(se[0]);
						int m = Integer.parseInt(se[1]);
						String[] sse = se[2].split("\\.");
						int secs = Integer.parseInt(sse[0]);
						int millis = Integer.parseInt(sse[1]);
						long fullm = millis + secs * 1000 + m * 60 * 1000 + h * 60 * 60 * 1000;
						return new Timestamp(fullm);
					} catch (Exception e) {
						logger.error("Bad stamp", e);
						e.printStackTrace();
					}
				}
				ei = -1;
			}
			at--;
		}
		return null;
	}

	public Timestamp(long microseconds) {
		super();
		this.microseconds = microseconds;
	}

	public long getMicroseconds() {
		return microseconds;
	}
	
	public String getStampText() {
		return "[@" + TimeUitls.formatMillis(microseconds / 1000) + "]";
	}

	@Override
	public int hashCode() {
		return Long.hashCode(microseconds);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof Timestamp) {
			Timestamp tso = (Timestamp) obj;
			return microseconds == tso.microseconds;
		} else {
			return false;
		}
	}
}
