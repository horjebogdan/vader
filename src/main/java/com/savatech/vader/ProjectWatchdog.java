package com.savatech.vader;

import java.lang.ref.SoftReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * Triggers relevant timely backups.
 *  
 * @author bogdanhorje
 *
 */
public class ProjectWatchdog {
	private static final Logger logger = LoggerFactory.getLogger(ProjectWatchdog.class);
	private static final long DEF_SLEEP_TIME = 60 * 1000;

	private SoftReference<Project> projectRef;
	private Thread thread;
	private long sleepTime = DEF_SLEEP_TIME;

	public ProjectWatchdog(Project project) {
		projectRef = new SoftReference<>(project);
		this.thread = new Thread(this::run);
		this.thread.start();
	}

	private void run() {
		Project p = projectRef.get();
		while (p != null) {
			try {
				Thread.sleep(sleepTime);
				if (p.isOpen()) {
					p.backup();
				} else {
					break;
				}
			} catch (InterruptedException e) {
				logger.error("WD sleep intr.", e);
			}
		}
	}

}
