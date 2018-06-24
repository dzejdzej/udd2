package com.crkomi.udd2.tools.indexer.handler;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.util.PDFTextStripper;

public class TxtHandler extends DocumentHandler {

	@Override
	public Document getDocument(File file) throws IncompleteIndexDocumentException {
		Document doc = new Document();
		TextField fileNameField = new TextField("fileName",	file.getName(), Store.YES);
		doc.add(fileNameField);
		try {
			StringField locationField = new StringField("location", file.getCanonicalPath(), Store.YES);
			doc.add(locationField);
			FileInputStream fisTargetFile = new FileInputStream(file);

			String targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");
			
			doc.add(new TextField("text", targetFileStr, Store.NO));
			// imported-document-1-1529867745594.txt
//			String id = file.getName().split("-")[2];
//			if(id!=null && !id.trim().equals("")) {
//				doc.add(new TextField("id", id, Store.YES));
//			}

		} catch (Exception e) {
			System.out.println("Greska pri konvertovanju pdf dokumenta");
		}
		
		return doc;
	}

}
