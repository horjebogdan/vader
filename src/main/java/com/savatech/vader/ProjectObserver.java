package com.savatech.vader;

import java.util.List;

public interface ProjectObserver {


	void updateInfo(Project project, String info);

	void playing(String name,long actualMicros,long micros);
}
