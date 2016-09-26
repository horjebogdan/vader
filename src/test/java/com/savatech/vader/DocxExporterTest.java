package com.savatech.vader;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import org.junit.Test;

public class DocxExporterTest {

	@Test
	public void testExport() throws Exception {
		DocxExporter e = new DocxExporter();
		URL r = DocxExporterTest.class.getResource("/test.txt");
		byte[] allBytes = Files.readAllBytes(new File(r.toURI()).toPath());
		String text=new String(allBytes);
		

		e.export(new File("./target/test.docx"), text);
	}

}
