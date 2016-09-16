package com.savatech.vader;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.swing.JPanel;

public class ProgressivePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private float progressed = 0.0f;
	private Color color;

	public ProgressivePanel(LayoutManager layout,Color color) {
		super(layout);
		this.color=color;
		setOpaque(false);
	}

	public void paintComponent(Graphics g) {
		Color c = g.getColor();
		g.setColor(color);
		int w = getWidth();
		int h = getHeight();
		int wg = (int) (w * progressed);
		g.fillRect(0, 0, wg, h);
		g.setColor(c);
		super.paintComponent(g);
	}

	public void setProgressed(float progressed) {
		this.progressed = progressed;
		repaint();
	}

}
