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

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.savatech.vader.Player.PlayerObeserver;

public class Project extends Model<ProjectObserver> implements PlayerObeserver {

	private static final int BACKUP_SAVE_INT = 10;

	private static final int BACKUP_ROLL_COUNT = 10;

	private static final String BAK = "bak";

	private static final Logger logger = LoggerFactory.getLogger(Project.class);

	private static final DecimalFormat SPEED_FORMAT = new DecimalFormat("#####.##");

	private static final String VADER_IN = "vader.in";
	private static final String VADER_SLICED_IN = "vader.sliced.in";
	private static final String VADER_PRJ = "vader.prj";
	private static final String SLICES = "slices";
	private static final String TEXT = "vader.txt";
	private static final String DOC = "vader.docx";

	private Exporter exporter = new DocxExporter();

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

	private boolean open;

	private DocumentListener docListener;

	private boolean dirty;

	private int saveCount;
	private int backupCount;

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
		this.open = true;

		for (backupCount = 1; backupCount < BACKUP_ROLL_COUNT; backupCount++) {
			if (!backPath(backupCount).toFile().exists()) {
				break;
			}
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
		if (!this.isAutoPause() && this.player.isPaused()) {
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
			this.lastPause = i;
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

		this.lastPause = skip;

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
		if (!txtf.exists()) {
			logger.info("Set no text document for " + txtf.getAbsolutePath());
			return;
		}
		try {
			byte[] bytes = Files.readAllBytes(txtf.toPath());
			doc.remove(0, doc.getLength());
			doc.insertString(0, new String(bytes,"UTF-8"), null);
		} catch (IOException | BadLocationException e) {
			logger.error("Could not read " + txtf.getAbsolutePath(), e);
		}

		this.docListener = new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				Project.this.dirty();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				Project.this.dirty();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				Project.this.dirty();
			}
		};

		this.doc.addDocumentListener(this.docListener);

	}

	protected void dirty() {
		if (!this.dirty) {
			this.dirty = true;
			fireUpdateInfo();
		}
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public void save() {
		logger.info("Saving " + this.path);
		this.doc.render(this::safeSave);
	}

	private synchronized void safeSave() {
		File txtf = this.path.resolve(TEXT).toFile();
		internalSave(txtf);
		saveCount++;
		if (saveCount % BACKUP_SAVE_INT == 0) {
			saveCount = 1;
			backupCount++;
			int backupNo = backupCount % BACKUP_ROLL_COUNT;
			backup(backupNo);
		}
	}

	private void internalSave(File f) {
		try (FileOutputStream fos = new FileOutputStream(f)) {
			int l = doc.getLength();
			String s = doc.getText(0, l);
			byte[] b = s.getBytes("UTF-8");
			fos.write(b);
			fos.flush();
			this.dirty = false;
			fireUpdateInfo();
			logger.info("Sucessfuly saved text to " + f.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Could not save project " + this.path, e);
		}
	}

	public void export() {
		logger.info("Saving " + this.path);
		this.doc.render(this::safeExport);
	}

	private void safeExport() {
		File docf = this.path.resolve(DOC).toFile();
		safeExport(docf);
	}

	private void safeExport(File f) {
		try {
			int l = doc.getLength();
			String text= doc.getText(0, l);
			exporter.export(f, text);
		} catch (BadLocationException e) {
			logger.error("Error exporting "+f,e);
		}
	}

	public boolean isOpen() {
		return this.open;
	}

	public void close() {
		this.open = false;
	}

	public synchronized void backup() {
		long time = System.currentTimeMillis();
		backup(time);
	}

	public synchronized void backup(long backup) {
		Path bakFile = backPath(backup);
		internalSave(bakFile.toFile());
	}

	private Path backPath(long backup) {
		Path bakFolderPath = this.path.resolve(BAK);
		File bakFolder = bakFolderPath.toFile();
		bakFolder.mkdirs();
		Path bakFile = bakFolderPath.resolve(backupFileName(backup));
		return bakFile;
	}

	private String backupFileName(long backup) {
		return "b" + backup + ".txt";
	}
}
