package com.savatech.vader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.savatech.vader.Player.PlayerObeserver;

public class Project extends Model<ProjectObserver> implements PlayerObeserver {

	private static final Logger logger = LoggerFactory.getLogger(Project.class);

	private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("#####.##");

	private static final String VADER_IN = "vader.in";
	private static final String VADER_SLICED_IN = "vader.sliced.in";
	private static final String VADER_PRJ = "vader.prj";
	private static final String SLICES = "slices";
	private static final String TEXT = "vader.txt";

	public static boolean isProjectFile(File file) {
		return file.getName().equals(VADER_PRJ);
	}
	public static File projectFileFrom(File folder) {
		return new File(folder, VADER_PRJ);
	}

	public static Project newProject(Path wav) throws IOException {
		Path projectPath = wav.getParent();
		Properties properties = new Properties();
		String fileName = wav.getFileName().toString();
		properties.setProperty(VADER_IN, fileName);
		Path slicesPath = projectPath.resolve(SLICES);
		slicesPath.toFile().mkdirs();
		Path slicedFilePath = slicesPath.resolve(fileName);
		properties.store(new FileOutputStream(projectPath.resolve(VADER_PRJ).toFile()), "May The Force Be With You");
		new Slicer().slice(wav.toString(), slicedFilePath.toString());
		logger.info("New project at " + projectPath);
		return new Project(projectPath);
	}

	private Path path;
	private Path inPath;

	private List<Slice> slices = Collections.emptyList();

	private int loadedSlicesCount = 0;

	private Player player;
	private Thread playThread;
	private long actualMicroSecond;
	private int sliceIndex;
	private int pauseInterval;
	private boolean autoPause;
	private int lastPause;

	private Document doc;

	public Project(Path path) throws IOException {
		this(path, null);
	}

	public Project(Path path, ProjectObserver ui) throws IOException {
		super(ProjectObserver.class);
		this.path = path;
		this.player = new Player(this);
		loadProperties();
		loadAllSlices();
		this.player.setPlayback(this.inPath, 1f);
		if (ui != null) {
			addObserver(ui);
		}
	}

	@Override
	public void addObserver(ProjectObserver o) {
		super.addObserver(o);
		observersUpdated();
	}

	private synchronized void loadProperties() throws IOException {
		Properties props = new Properties();
		props.load(this.path.resolve(VADER_PRJ).toUri().toURL().openStream());
		this.inPath = this.path.resolve(props.get(VADER_IN).toString());
	}

	private synchronized void loadAllSlices() throws IOException {
		Path slicesPath = path.resolve(SLICES);

		Optional<Path> first = Files.walk(slicesPath).filter((p) -> p.toString().endsWith(".sgm")).findFirst();
		if (first.isPresent()) {
			AtomicInteger aindex = new AtomicInteger(0);
			slices = Files.lines(first.get()).map((l) -> l.split(","))
					.map((sd) -> new Slice(aindex.incrementAndGet(), Long.parseLong(sd[0]), Long.parseLong(sd[1])))
					.collect(Collectors.toList());
		}

		fireUpdateInfo();
	}

	private synchronized void observersUpdated() {
		fireUpdateInfo();
		at(0, 0);
	}


	private synchronized void fireUpdateInfo() {
		getObserver().updateInfo(this, getInfo());
	}

	public List<Slice> getSlices() {
		return slices;
	}

	public int getSliceCount() {
		return slices.size();
	}

	public int getLoadedSlicesCount() {
		return loadedSlicesCount;
	}

	public double getActualDurationInSeconds() {
		return player.getActualDurationInSeconds();
	}

	public String getInfo() {
		long actualDuration = Math.round(player.getActualDurationInSeconds() * 1000);
		long duration = Math.round(player.getDurationInSeconds() * 1000);
		long batchSize = 0;
		float speed = getPlaybackSpeed();
		return "" + this.path.getFileName() + "/" + inPath.getFileName() + " | " + slices.size() + " slices | "
				+ TimeUitls.formatMillis(actualDuration) + " | " + TimeUitls.formatMillis((long) duration) + "  @ "
				+ SPEED_FORMAT.format(speed) + "| " + (getCurrentSliceIndex() + 1) + "[" + batchSize + "]"
				+ (autoPause ? " | auto-pause " : " manual-pause ");
	}

	public boolean togglePlay() {
		try {
			if (playThread == null) {
				playThread = new Thread(() -> player.play());
				playThread.start();
				return true;
			} else {
				return this.player.togglePause();
			}
		} finally {
			fireUpdateInfo();
		}
	}

	public void setPlaybackSpeed(float speed) {
		this.player.setSpeed(speed);
		fireUpdateInfo();
	}

	public float getPlaybackSpeed() {
		return this.player.getSpeed();
	}

	public void speedUp() {
		setPlaybackSpeed(getPlaybackSpeed() + 0.10f);
	}

	public void speedDown() {
		if (getPlaybackSpeed() > 0.25) {
			setPlaybackSpeed(getPlaybackSpeed() - 0.10f);
		}
	}

	public void skip(long seconds) {
		this.player.skipAt(seconds);
		if (!this.isAutoPause() && this.player.isPaused())
		{
			this.player.togglePause();
		}
		fireUpdateInfo();
	}

	public int getCurrentSliceIndex() {
		Slice s;
		int direction = 0;
		int sMax = slices.size() - 1;
		long millis = actualMicroSecond / 1000;
		int lastIndex = sliceIndex;
		while (true) {
			s = slices.get(sliceIndex);
			if (s.end < millis) {
				if (direction < 0) {
					sliceIndex = Math.min(lastIndex, sliceIndex);
					break;
				}
				if (sliceIndex == sMax) {
					break;
				}
				lastIndex = sliceIndex++;
				direction = 1;
			} else if (s.start > millis) {
				if (direction > 0) {
					sliceIndex = Math.min(lastIndex, sliceIndex);
					break;
				}
				if (sliceIndex == 0) {
					break;
				}
				lastIndex = sliceIndex--;
				direction = -1;
			} else {
				break;
			}
		}
		return sliceIndex;
	}

	@Override
	public void at(long ams, long ms) {
		this.actualMicroSecond = ams;
		int i = getCurrentSliceIndex();
		if (this.autoPause && i - this.lastPause >= this.pauseInterval) {
			this.player.togglePause();
			this.lastPause = i;
		} else if (!this.autoPause) {
			this.lastPause=i;
		}
		getObserver().playing("slice " + i + " ", ams, ms);
	}

	public void replay(int sliceCount) {
		int skip = sliceIndex - sliceCount;
		if (skip < 0) {
			skip = 0;
		}
		if (skip > this.lastPause) {
			this.lastPause = skip;
		}
		
		this.lastPause=skip;
		
		skip(slices.get(skip).start / 1000);
		if (this.player.isPaused() && isAutoPause()) {
			this.player.togglePause();
		}
	}

	public void setAutoPause(boolean on, int slices) {
		this.autoPause = on;
		this.pauseInterval = slices;
		fireUpdateInfo();
	}

	public boolean isAutoPause() {
		return this.autoPause;
	}

	public void setTextDocument(Document doc) {
		this.doc = doc;
		File txtf = this.path.resolve(TEXT).toFile();
		if (!txtf.exists()){
			logger.info("Set no text document for "+txtf.getAbsolutePath());
			return;
		}
		try {
			byte[] bytes = Files.readAllBytes(txtf.toPath());
			doc.remove(0, doc.getLength());
			doc.insertString(0, new String(bytes), null);
		} catch (IOException | BadLocationException e) {
			logger.error("Could not read " + txtf.getAbsolutePath(), e);
		}

	}

	public void save() {
		logger.info("Saving " + this.path);
		this.doc.render(this::safeSave);
	}

	private void safeSave() {
		File txtf = this.path.resolve(TEXT).toFile();
		try (FileOutputStream fos = new FileOutputStream(txtf); PrintStream ps = new PrintStream(fos)) {

			int l = doc.getLength();
			String s = doc.getText(0, l);
			ps.print(s);
			ps.flush();
			logger.info("Sucessfuly saved text to " + txtf.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Could not save project " + this.path, e);
		}
	}

}
