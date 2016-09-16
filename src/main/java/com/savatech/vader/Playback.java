package com.savatech.vader;

import java.io.IOException;
import java.nio.file.Path;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;

public class Playback {
	private Path path;
	private AudioInputStream ais;
	private AudioSpeedInputStream asis;
	private SourceDataLine sdl;
	private double durationInSeconds;
	private AudioFormat format;
	private boolean paused = false;

	private long microsSegmentStart = 0;
	private long microsSegmentOffset = 0;

	Playback(Path path, AudioInputStream ais, AudioSpeedInputStream asis, long skipSeconds) throws IOException {
		super();
		this.path = path;
		this.ais = ais;
		this.asis = asis;
		if (ais != null) {
			format = ais.getFormat();
			computeDuration();
			asis.skip((long) (skipSeconds * format.getFrameRate() * format.getFrameSize()));
			microsSegmentOffset = skipSeconds * 1000000;
		}

	}

	Path getPath() {
		return path;
	}

	double getActualDurationInSeconds() {
		return durationInSeconds;
	}

	double getDurationInSeconds() {
		return durationInSeconds / asis.getSpeed();
	}

	void setSdl(SourceDataLine sdl) {
		this.sdl = sdl;
	}

	SourceDataLine getSdl() {
		return sdl;
	}

	AudioInputStream getAis() {
		return ais;
	}

	float getSpeed() {
		return asis.getSpeed();
	}

	void  setSpeed(float speed) {
		microsSegmentOffset = getActualMicrosecondPosition();
		microsSegmentStart = sdl.getMicrosecondPosition();
		asis.setSpeed(speed);
	}

	void setPaused(boolean paused) {
		this.paused = paused;
	}

	boolean isPaused() {
		return paused;
	}

	private void computeDuration() {
		long frames = ais.getFrameLength();
		
		this.durationInSeconds = ((frames * getSpeed()) / format.getFrameRate() );
	}

	long getActualMicrosecondPosition() {
		long msp = (long) (Math.round(sdl.getMicrosecondPosition() - microsSegmentStart));
		return microsSegmentOffset + msp;
	}

	void dispose() {
		sdl.stop();
		sdl.close();
		try {
			ais.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}