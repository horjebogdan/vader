package com.savatech.vader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Timestamp {
	private static final Logger logger = LoggerFactory.getLogger(Timestamp.class);

	private long microseconds;

	private static long[] parse(String stamp) {
		String[] se = stamp.split(":");
		int h = Integer.parseInt(se[0]);
		int m = Integer.parseInt(se[1]);
		String[] sse = se[2].split("\\.");
		int secs = Integer.parseInt(sse[0]);
		int millis = Integer.parseInt(sse[1]);
		long fullm = millis + secs * 1000 + m * 60 * 1000 + h * 60 * 60 * 1000;
		return new long[] { fullm, millis, secs, m, h };
	}

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
						long[] t = parse(s);
						return new Timestamp(t[0]);
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

	public static String formatTimeStamps(String text) {
		StringBuilder f = new StringBuilder();
		int l = text.length();
		for (int i = 0; i < l; i++) {
			char c = text.charAt(i);
			StringBuilder b = new StringBuilder();
			if ('[' == c) {
				while (c != ']' && i < l) {
					b.append(c);
					i++;
					c = text.charAt(i);
				}
				b.append(c);
				if (c == ']' && b.length() == 15) {
					String ts = b.substring(2, 14);
					try {
						long[] time = parse(ts);
						if (time[4]>0){
							f.append("Ora ");
							f.append(time[4]);
							f.append(" minutul ");
						}
						else {
							f.append("Minutul ");
						}
						f.append(""+time[3]+":"+time[2]);
					} catch (Exception e) {
						f.append(b);
					}
				} else {
					f.append(b);
				}
			} else {
				f.append(c);
			}
		}
		
		return f.toString();
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
