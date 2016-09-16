package com.savatech.vader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Player {
	private static final Logger logger = LoggerFactory.getLogger(Player.class);

	private static final int DEF_BUFFER_SIZE = 64;

	public static interface PlayerObeserver {

		void at(long amicros, long micros);

	}

	private PlayerObeserver obeserver;
	private Playback playback;
	private final int playBufferSize = DEF_BUFFER_SIZE;

	public Player(PlayerObeserver obbesrver) throws IOException {
		super();
		this.obeserver = obbesrver;
		this.playback = new Playback(null, null, new AudioSpeedInputStream(null, 0, 1.0f), 0);
	}

	private Playback accelerate2(Path path, float playBackSpeed, long skipSeconds)
			throws MalformedURLException, UnsupportedAudioFileException, IOException {
		URL url = path.toUri().toURL();
		AudioInputStream ais = AudioSystem.getAudioInputStream(url);
		AudioFormat af = ais.getFormat();

		long length = ais.getFrameLength();
		long newLength = Math.round((float) length / playBackSpeed);

		AudioSpeedInputStream asis = new AudioSpeedInputStream(ais, af.getFrameSize(), playBackSpeed);
		AudioInputStream aisAccelerated = new AudioInputStream(asis, af, newLength);
		logger.info("Accelerated length " + aisAccelerated.getFrameLength() * af.getFrameSize());
		logger.info("Original length " + ais.getFrameLength() * af.getFrameSize());
		return new Playback(path, aisAccelerated, asis, skipSeconds);
	}

	private void loop() {

		AtomicBoolean finished = new AtomicBoolean(false);

		Thread timerThread = new Thread(() -> time(finished));
		timerThread.start();

		// FloatControl p =
		// (FloatControl)playback.sdl.getControl(Type.MASTER_GAIN);
		// p.setValue(4.5f);
		long bc = 0;
		try {
			int nBytesRead = 0;
			byte[] abData = new byte[playBufferSize];
			try {

				while (nBytesRead != -1) {

					SourceDataLine sdl = this.playback.getSdl();
					AudioInputStream ais = playback.getAis();
					nBytesRead = ais.read(abData, 0, abData.length);

					if (nBytesRead != -1) {
						int nBytesWritten = sdl.write(abData, 0, nBytesRead);
						bc += nBytesWritten;
						if (nBytesWritten < nBytesRead) {
							nBytesWritten = sdl.write(abData, nBytesWritten, nBytesRead - nBytesWritten);
							bc += nBytesWritten;
						}
						if (playback.isPaused()) {
							synchronized (this) {
								this.playback.getSdl().stop();
								while (playback.isPaused()) {
									try {
										logger.info("Pausing.");
										this.wait();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
								logger.info("Resuming.");
								this.playback.getSdl().start();
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			this.playback.getSdl().drain();
		} finally {

			finished.set(true);
			try {
				timerThread.join();
			} catch (InterruptedException e) {
				logger.error("Interrupted", e);
			}
			observeTime();
			SourceDataLine sdl = this.playback.getSdl();
			sdl.close();
			logger.debug(bc + " bytes writen on-line");

		}
	}

	private void time(AtomicBoolean stop) {
		while (!stop.get()) {
			observeTime();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private void observeTime() {
		Playback pb;
		synchronized (this) {
			pb = this.playback;
		}
		long ms = (long) pb.getActualMicrosecondPosition();
		if (obeserver != null) {
			obeserver.at(ms, (long) (ms / getSpeed()));
		}
	}

	private Playback createPlayback(Path path, float speed, long skipSeconds) {
		Playback pb = null;
		try {
			pb = accelerate2(path, speed, skipSeconds);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		AudioFormat audioFormat = pb.getAis().getFormat();

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

		SourceDataLine sourceLine = null;
		try {
			sourceLine = (SourceDataLine) AudioSystem.getLine(info);
			pb.setSdl(sourceLine);
			sourceLine.open(audioFormat);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return pb;
	}

	public void skipAt(long skip) {
		boolean paused = isPaused();
		if (!paused) {
			togglePause();
		}
		float s = this.playback.getSpeed();
		setPlayback(createPlayback(this.playback.getPath(), 1.0f, skip));
		setSpeed(s);
		observeTime();
		if (!paused) {
			synchronized (this) {
				this.notifyAll();
			}
		} else {
			this.playback.setPaused(true);
		}
	}

	public void setPlayback(Path path, float speed) {
		this.playback = createPlayback(path, speed, 0);
	}

	public void setPlayback(Playback playback) {
		if (this.playback != null) {
			this.playback.dispose();
		}
		this.playback = playback;
	}

	public void play(Path path, float speed) {
		if (speed <= 0.0f) {
			speed = 0.0f;
		}
		setPlayback(path, speed);
		play();
	}

	public void play() {
		SourceDataLine sdl = this.playback.getSdl();
		sdl.start();
		loop();
	}

	public boolean togglePause() {
		synchronized (this) {
			if (!playback.isPaused()) {
				playback.setPaused(true);
			} else {
				playback.setPaused(false);
				this.notifyAll();
			}

			return playback.isPaused();
		}

	}

	public boolean isPaused() {
		return this.playback.isPaused();
	}

	public float getSpeed() {
		return this.playback.getSpeed();
	}

	public double getActualDurationInSeconds() {
		return playback.getActualDurationInSeconds();
	}

	public double getDurationInSeconds() {
		return playback.getDurationInSeconds();
	}

	public void setSpeed(float speed) {
		if (speed >= 0.1f) {
			// togglePause();
			this.playback.setSpeed(speed);
			// togglePause();
		}
	}
}
