package org.jodreports.templates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.jodreports.opendocument.OpenDocumentIO;

import freemarker.template.Configuration;

public class DocumentTemplateFactory {

	private final Configuration freemarkerConfiguration;

	public DocumentTemplateFactory() {
		freemarkerConfiguration = new Configuration();		
		freemarkerConfiguration.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
		freemarkerConfiguration.setDefaultEncoding(OpenDocumentIO.UTF_8.name());
		freemarkerConfiguration.setOutputEncoding(OpenDocumentIO.UTF_8.name());
	}

	/**
	 * Retrieves the FreeMarker {@link Configuration} for this factory.
	 * <p>
	 * You can use this method to set custom FreeMarker options on the returned
	 * {@link Configuration}, and they will apply to all templates returned by
	 * this factory.
	 * <p>
	 * Any such customizations should be used carefully. 
	 * Only use this method if you know what you are doing. 
	 * Limitation: Do not change the default square-bracket Tag Syntax.  
	 * 
	 * 
	 * @return the FreeMarker {@link Configuration} 
	 */
	public Configuration getFreemarkerConfiguration() {
		return freemarkerConfiguration;
	}

	public DocumentTemplate getTemplate(File file) throws IOException, DocumentTemplateException {
		if (file.isDirectory()) {
			return new UnzippedDocumentTemplate(file, freemarkerConfiguration);
		} else {
			return getTemplate(new FileInputStream(file));
		}
	}

	public DocumentTemplate getTemplate(InputStream inputStream) throws IOException, DocumentTemplateException {
		InputStream inputStreamCopy = copyInputStream(inputStream);
		if (isXmlFile(inputStreamCopy)) {
			return new FlatDocumentTemplate(inputStreamCopy, freemarkerConfiguration);
		} else {
			return new ZippedDocumentTemplate(inputStreamCopy, freemarkerConfiguration);
		}
	}
	
	private InputStream copyInputStream(final InputStream input) throws FileNotFoundException, IOException {
		ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[1024];
		int len;
		while ((len = input.read(buffer)) > -1 ) {
		    bufferStream.write(buffer, 0, len);
		}
		bufferStream.flush();
		
		return new ByteArrayInputStream(bufferStream.toByteArray());
	}
	
	private boolean isXmlFile(final InputStream inputStream) throws FileNotFoundException, IOException {
		byte[] firstBytes = new byte[5];
		inputStream.read(firstBytes);
		inputStream.reset();
		return new String(firstBytes).startsWith("<?xml");
	}

}
