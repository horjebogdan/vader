package com.savatech.vader;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocument1;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocxExporter implements Exporter {

	private static final Logger logger = LoggerFactory.getLogger(DocxExporter.class);

	@Override
	public void export(File f, String text) {

		if (f == null) {
			logger.error("Null fle ", new RuntimeException("Null file!"));
			return;
		}

		try (FileOutputStream fos = new FileOutputStream(f)) {

			// Blank Document
			XWPFDocument xdocument = new XWPFDocument();

			CTDocument1 document = xdocument.getDocument();
			CTBody body = document.getBody();

			if (!body.isSetSectPr()) {
				body.addNewSectPr();
			}
			CTSectPr section = body.getSectPr();

			if (!section.isSetPgSz()) {
				section.addNewPgSz();
			}
			CTPageSz pageSize = section.getPgSz();

			pageSize.setW(BigInteger.valueOf(595 * 20));
			pageSize.setH(BigInteger.valueOf(842 * 20));

			// create Paragraph
			XWPFParagraph paragraph = xdocument.createParagraph();
			XWPFRun run = paragraph.createRun();
			run.setText(Timestamp.formatTimeStamps(text));
			
			xdocument.write(fos);
			logger.info("Sucessfuly exported text to " + f.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Could not export project " + f.getAbsolutePath(), e);
		}

	}

}
