package com.savatech.vader;

import java.io.IOException;
import java.io.InputStream;

public class AudioSpeedInputStream extends InputStream {

	private static final int LARGE_SKIP_SIZE = 128 * 1024;

	private InputStream is;

	private final int frameSize;

	private double speed = 1f;

	public AudioSpeedInputStream(InputStream is, int frameSize, float speed) {
		super();
		this.is = is;
		this.frameSize = frameSize;
		this.speed = speed;
	}

	public float getSpeed() {
		return (float) speed;
	}

	public synchronized void setSpeed(float speed) {
		this.speed = speed;
	}

	@Override
	public synchronized long skip(long n) throws IOException {
		long lenToRead = (long) Math.ceil(n * speed);
		lenToRead = ((lenToRead / frameSize) * frameSize);
		byte[] b = new byte[LARGE_SKIP_SIZE];
		int nr = 0;
		int cr = 0;
		while ((cr <= lenToRead) && ((nr = is.read(b, 0, b.length)) != -1)) {
			cr += nr;
		}

		return cr;
	}

	@Override
	public synchronized int read() throws IOException {
		throw new IOException("one byte frame");
	}

	@Override
	public synchronized int read(byte[] b, int off, int len) throws IOException {

		long lenToRead = (long) Math.ceil(len * speed);
		lenToRead = ((lenToRead / frameSize) * frameSize);

		byte[] buff = new byte[(int) lenToRead];
		int speedReadLen = is.read(buff, 0, (int) lenToRead);
		if (speedReadLen <= 0) {
			return speedReadLen;
		}

		long readLen = (Math.round(speedReadLen / speed) / frameSize) * frameSize;
		if (readLen > len) {
			readLen = len;
		}

		for (int i = 0; i < readLen; i++) {
			int foff = i % frameSize;
			long speedi = (long) Math.floor(i * speed);
			int si = (int) ((speedi / frameSize) * frameSize + foff);

			b[off + i] = buff[si];
		}

		return (int) readLen;
	}

}
