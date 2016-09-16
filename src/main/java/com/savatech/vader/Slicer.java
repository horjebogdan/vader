package com.savatech.vader;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.frontend.SignalListener;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.WavWriter;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class Slicer {

	private static final Logger logger = LoggerFactory.getLogger(Slicer.class);

	/**
	 * Mimics {@link WavWriter}'s broken index generation algorithm. To be used
	 * with file order restoring
	 * 
	 * @param outPattern
	 * @return
	 */
	public static String getBuggyFileIndex(String outPattern, int fileIndex) {

		String indexString = Integer.toString(fileIndex);

		String fileName = outPattern.substring(0, Math.max(0, outPattern.length() - indexString.length())) + indexString
				+ ".wav";

		return fileName;
	}

	public void slice(String inputFile, String outputFile) throws MalformedURLException, IOException {
		URL configURL = Slicer.class.getResource("/segmenter.config.xml");

		ConfigurationManager cm = new ConfigurationManager(configURL);

		// boolean noSplit = false;
		//
		// if (noSplit) {
		// ConfigurationManagerUtils.setProperty(cm, "wavWriter",
		// "captureUtterances", "false");
		// }

		slice(inputFile, outputFile, cm);
	}

	public void slice(String inputFile, String outputFile, ConfigurationManager cm)
			throws MalformedURLException, IOException {
		traceFormat("Slicing into " + inputFile+" to "+outputFile, inputFile );

		PrintStream segments = new PrintStream(new File(outputFile + ".sgm"));

		FrontEnd frontend = (FrontEnd) cm.lookup("endpointer");
		frontend.addSignalListener(new SignalListener() {

			@Override
			public void signalOccurred(Signal signal) {
				if (signal instanceof SpeechStartSignal) {
					segments.print(signal.getTime());
				}
				if (signal instanceof SpeechEndSignal) {
					segments.print(",");
					segments.println(signal.getTime());
				}
			}
		});

		AudioFileDataSource dataSource = (AudioFileDataSource) cm.lookup("audioFileDataSource");
		dataSource.setAudioFile(new File(inputFile), null);
		WavWriter wavWriter = (WavWriter) cm.lookup("wavWriter");
		wavWriter.setOutFilePattern(outputFile);

		frontend.initialize();

		Data data = null;
		do {
			data = frontend.getData();
		} while (data != null);

	}

	public static void main(String[] args) throws IllegalArgumentException, Exception {
		final String inmp3 = "./data/in.mp3";
		final String inwav = "./data/inro.wav";
		final String samplewav = "./data/in_sample.wav";
		final String out = "./data/slices/slice000000";
		final String out2 = "./data/slices2/slice000000";

		new Slicer().slice(inwav, out2);

	}

	private static void traceFormat(String info, final String audioPath) {

		try {
			AudioFormat format = AudioSystem.getAudioInputStream(new File(audioPath)).getFormat();
			logger.info(info + ":" + audioPath + " : " + format.toString());

		} catch (UnsupportedAudioFileException | IOException e) {
			logger.error("Error tracing " + audioPath, e);
		}
	}
}
