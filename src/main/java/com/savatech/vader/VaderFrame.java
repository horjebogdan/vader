package com.savatech.vader;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.apache.commons.math3.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaderFrame extends JFrame implements ProjectObserver {

	private static final Logger logger = LoggerFactory.getLogger(VaderFrame.class);
	private static final long serialVersionUID = 1L;

	private static final DecimalFormat PERC_FORMAT = new DecimalFormat("##0.00 %");
	private static final String WAV = ".wav";

	private Project project;
	private JLabel timer;
	private JEditorPane editorPane;
	private ProgressivePanel progressPanel;
	private JLabel info;

	private JLabel completedLabel;

	private List<Pair<AWTKeyStroke, String>> keys = new ArrayList<>();

	private JPanel keysHelp;

	private Timestamp timeStamp;
	private ImageIcon darthIcon;
	private JLabel vaderIconLabel;
	private ImageIcon imageWait;
	private ImageIcon imageVader;
	private JLabel lastSaved;
	private JLabel lastBackedup;
	private JPanel dirtyPanel;

	public VaderFrame() throws IOException {
		super("Vader 0.0.3"); // invoke the JFrame constructor

		BufferedImage darthPng = ImageIO.read(ClassLoader.getSystemResource("dvi16.png"));
		setIconImage(darthPng);
		darthIcon = new ImageIcon(darthPng);

		setSize(1500, 1000);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setJMenuBar(buildMenuBar());

		BufferedImage vh = ImageIO.read(ClassLoader.getSystemResource("vh.jpg"));
		imageVader = new ImageIcon(vh);
		BufferedImage wait = ImageIO.read(ClassLoader.getSystemResource("wait.png"));
		imageWait = new ImageIcon(wait);
		vaderIconLabel = new JLabel("", imageVader, JLabel.CENTER);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(vaderIconLabel, BorderLayout.CENTER);

	}

	private void buildUI() {

		getContentPane().removeAll();

		setLayout(new GridBagLayout()); // set the layout manager

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;

		timer = new JLabel(); // construct a JLabel
		timer.setText(TimeUitls.formatMicros(0));
		timer.setFont(new Font("monospaced", Font.PLAIN, 14));
		timer.setForeground(Color.BLACK);
		add(timer, c);

		JPanel controlPanel = buildControlPanel();
		c.gridy++;
		add(controlPanel, c);

		info = new JLabel();
		info.setFont(new Font("monospaced", Font.PLAIN, 12));
		c.gridy++;
		add(info, c);

		keysHelp = new JPanel();
		c.gridy++;
		add(keysHelp, c);

		progressPanel = new ProgressivePanel(new FlowLayout(), VaderColor.saneProgress());
		progressPanel.setProgressed(0f);
		completedLabel = new JLabel();
		completedLabel.setFont(new Font("monospaced", Font.PLAIN, 12));
		progressPanel.add(completedLabel);

		progressPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				float xPrc = (float) e.getX() / (float) progressPanel.getWidth();
				project.skip((long) (project.getActualDurationInSeconds() * xPrc));
			}
		});
		c.gridy++;
		add(progressPanel, c);
		
		c.gridy++;
		add(buildDocInfoPanel(), c);

		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		c.weighty = 100;
		add(buildEditorPanel(), c);

		registerKeys();
		updateKeyHelp();

	}

	private Component buildDocInfoPanel() {
		JPanel panel=new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		dirtyPanel=new JPanel();
		dirtyPanel.setPreferredSize(new Dimension(20,30));
		dirtyPanel.setOpaque(true);
		dirtyPanel.setBackground(Color.GREEN);
		panel.add(dirtyPanel);
		lastSaved=new JLabel("Saved @ "+LocalDate.now()+" "+LocalTime.now());
		lastSaved.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		lastBackedup=new JLabel("Backedup @ "+LocalDate.now()+" "+LocalTime.now());
		lastBackedup.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel.add(lastSaved);
		panel.add(lastBackedup);
		return panel;
	}

	private void registerKeys() {

		registrtKeyAction("ESC play/pause", KeyEvent.VK_ESCAPE, 0, (e) -> VaderFrame.this.project.togglePlay());

		registrtKeyAction("F1  decrease speed", KeyEvent.VK_F1, 0, (e) -> VaderFrame.this.project.speedDown());
		registrtKeyAction("F2 increase speed", KeyEvent.VK_F2, 0, (e) -> VaderFrame.this.project.speedUp());

		registrtKeyAction("F3 auto pause toggle", KeyEvent.VK_F3, 0,
				(e) -> VaderFrame.this.project.setAutoPause(!project.isAutoPause(), 2));

		registrtKeyAction("CTRL+T time stamp", KeyEvent.VK_T, KeyEvent.CTRL_MASK, (e) -> VaderFrame.this.stamp());

		registrtKeyAction("CTRL+G go to time stamp", KeyEvent.VK_G, KeyEvent.CTRL_MASK,
				(e) -> VaderFrame.this.goToStamp());

		registrtKeyAction("F7 short replay", KeyEvent.VK_F7, 0, (e) -> VaderFrame.this.project.replay(1));
		registrtKeyAction("F6 long replay", KeyEvent.VK_F6, 0, (e) -> VaderFrame.this.project.replay(2));

		registrtKeyAction("CTRL+S", KeyEvent.VK_S, KeyEvent.CTRL_MASK, (e) -> VaderFrame.this.saveProject());

	}

	private void goToStamp() {
		int caretPos = editorPane.getCaretPosition();
		Timestamp ts = Timestamp.in(editorPane.getText(), Math.max(0, caretPos - 1));
		if (ts != null) {
			this.project.skip(ts.getMicroseconds() / 1000);
		}
	}

	private void stamp() {
		System.err.println("stamping!");
		if (timeStamp != null) {
			int caretPos = editorPane.getCaretPosition();
			try {
				editorPane.getDocument().insertString(caretPos, "\n" + timeStamp.getStampText() + "\n", null);
			} catch (BadLocationException ex) {
				ex.printStackTrace();
			}
		}
	}

	private void updateKeyHelp() {
		keysHelp.removeAll();
		keysHelp.setLayout(new FlowLayout(FlowLayout.LEFT));
		for (Pair<AWTKeyStroke, String> k : keys) {
			JLabel label = new JLabel(" " + k.getSecond() + " ");
			label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			keysHelp.add(label);
		}
	}

	private void registrtKeyAction(String description, int keyEvent, int mod, Consumer<ActionEvent> action) {
		String name = "k" + (keys.size() + 1);
		KeyStroke keyStroke = KeyStroke.getKeyStroke(keyEvent, mod);
		JPanel central = (JPanel) getContentPane();
		Action a = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				action.accept(e);
			}
		};
		central.getActionMap().put(name, a);
		central.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, name);

		keys.add(new Pair<>(keyStroke, description));
	}

	private JComponent buildEditorPanel() {
		editorPane = new JEditorPane();

		UndoManager manager = new UndoManager();
		editorPane.getDocument().addUndoableEditListener(manager);

		Action undoAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					manager.undo();
				} catch (CannotUndoException ex) {
					Toolkit.getDefaultToolkit().beep();
				}

			}
		};
		Action redoAction = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent ev) {
				try {
					manager.redo();
				} catch (CannotRedoException ex) {
					Toolkit.getDefaultToolkit().beep();
				}

			}
		};
		// Assign the actions to keys
		editorPane.registerKeyboardAction(undoAction, KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK),
				JComponent.WHEN_FOCUSED);
		editorPane.registerKeyboardAction(redoAction,
				KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK),
				JComponent.WHEN_FOCUSED);
		JScrollPane editorScrollPane = new JScrollPane(editorPane);
		editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		return editorScrollPane;
	}

	private JMenuBar buildMenuBar() {
		JMenuBar newMenuBar = new JMenuBar();

		JMenu project = new JMenu("Project");
		project.setMnemonic(KeyEvent.VK_P);
		project.getAccessibleContext().setAccessibleDescription("New ,open and save projects.");
		newMenuBar.add(project);

		JMenuItem newp = new JMenuItem("New", KeyEvent.VK_N);
		newp.addActionListener(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				VaderFrame.this.newProject();
			}
		});
		newp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		newp.getAccessibleContext().setAccessibleDescription("New project");
		project.add(newp);

		JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O);
		open.addActionListener(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				VaderFrame.this.openProject();
			}
		});
		open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		open.getAccessibleContext().setAccessibleDescription("Open project");
		project.add(open);

		JMenuItem save = new JMenuItem("Save", KeyEvent.VK_S);
		save.addActionListener(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				VaderFrame.this.saveProject();
			}
		});
		save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		save.getAccessibleContext().setAccessibleDescription("Save project");
		project.add(save);

		JMenuItem export = new JMenuItem("Export to doc", KeyEvent.VK_E);
		export.addActionListener(new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				VaderFrame.this.exportToDoc();
			}
		});
		export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
		export.getAccessibleContext().setAccessibleDescription("Export to doc");
		project.add(export);

		return newMenuBar;
	}

	protected void exportToDoc() {
		if (this.project != null) {
			this.project.export();
		}
	}

	protected void saveProject() {
		if (this.project != null) {
			this.project.save();
		}
	}

	private void openProject() {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose project folder");
		fc.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "Project files";
			}

			@Override
			public boolean accept(File f) {
				return Project.isProjectFile(f);
			}
		});
		/*
		 * final FileView defview = ((FileChooserUI)
		 * UIManager.getDefaults().getUI(fc)).getFileView(fc);
		 * fc.setFileView(new FileView() {
		 * 
		 * @Override public String getDescription(File f) { return
		 * defview.getDescription(f); }
		 * 
		 * @Override public Icon getIcon(File f) { if
		 * (Project.projectFile(f).exists()) { return VaderFrame.this.darthIcon;
		 * } else { return defview.getIcon(f); } }
		 * 
		 * @Override public String getTypeDescription(File f) { return
		 * defview.getTypeDescription(f); }
		 * 
		 * @Override public Boolean isTraversable(File f) { return
		 * defview.isTraversable(f); } });
		 */
		int r = fc.showOpenDialog(this);
		if (r == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile().getParentFile();
			try {
				open(new Project(file.toPath()));
			} catch (IOException e) {
				logger.error("Could not open project.", e);
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}

		} else {
			logger.debug("Open command cancelled by user.");
		}

	}

	private void newProject() {

		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Choose file to import");
		fc.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "*.wav files";
			}

			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(WAV);
			}
		});
		int r = fc.showOpenDialog(this);
		if (r == JFileChooser.APPROVE_OPTION) {

			vaderIconLabel.setIcon(imageWait);
			File file = fc.getSelectedFile();
			new Thread(() -> concurrentNew(file)).start();
		} else {
			logger.debug("New command cancelled by user.");
		}
	}

	private void concurrentNew(File file) {

		try {
			logger.info("Importing: " + file.getName() + ".");
			File parent = file.getParentFile();
			String fname = file.getName();
			int wavIndex = fname.lastIndexOf(WAV);
			String pname = fname.substring(0, wavIndex);
			File project = new File(parent, pname);
			if (project.exists()) {
				JOptionPane.showMessageDialog(this, project.getAbsolutePath() + " already exists.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			project.mkdirs();
			Path projectWavPath = new File(project, fname).toPath();
			Files.copy(file.toPath(), new File(project, fname).toPath());
			open(Project.newProject(projectWavPath));

		} catch (Exception e) {
			logger.error("Could not create project.", e);
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			vaderIconLabel.setIcon(imageVader);
			return;
		} finally {

		}

	}

	private JPanel buildControlPanel() {
		JPanel panel = new JPanel(new FlowLayout());
		return panel;
	}

	public VaderFrame open(Project project) {
		if (this.project != null) {
			this.project.removeObserver(this);
			this.project.close();
		}
		

		buildUI();

		this.project = project;
		this.project.setTextDocument(editorPane.getDocument());
		this.project.addObserver(this);
		
		return this;
	}

	

	@Override
	public void updateInfo(Project project, String info) {
		this.info.setText(info);
		Color dirtyColor = project.isDirty()?Color.RED:Color.GREEN;
		this.dirtyPanel.setBackground(dirtyColor);
	}

	@Override
	public void playing(String name, long actualMicros, long micros) {
		double perc = (double) actualMicros / (project.getActualDurationInSeconds() * 1000 * 1000);
		this.timeStamp = new Timestamp(actualMicros);
		timer.setText(name + " " + TimeUitls.formatMicros(actualMicros) + " @ " + PERC_FORMAT.format(perc));
		this.progressPanel.setProgressed((float) perc);
		this.completedLabel.setText(PERC_FORMAT.format(perc));
	}
}
