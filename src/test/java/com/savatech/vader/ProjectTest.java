package com.savatech.vader;

import java.nio.file.Paths;

import org.junit.Test;

public class ProjectTest {
	
	@Test
	public void testName() throws Exception {
		TestObserver ui=new TestObserver();
		Project p = new Project(Paths.get("./data"), ui);
		System.out.println(ui.getSlices());
		
	}

}
