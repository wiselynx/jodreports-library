package org.jodreports.templates;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FlatDocumentTemplateTest extends AbstractTemplateTest {

	public void testCreateDocument() throws FileNotFoundException, IOException, DocumentTemplateException {
		File templateFile = getTestFile("hello-template.odt");
		File openDocumentFile = createTempFile("fodt");
        DocumentTemplateFactory factory = new DocumentTemplateFactory();
		DocumentTemplate template = documentTemplateFactory.getTemplate(templateFile);
        DocumentTemplate flatTemplate = new FlatDocumentTemplate(template.getOpenDocumentArchive(), factory.getFreemarkerConfiguration());
        Map configurations = flatTemplate.getConfigurations();
        configurations.put("process_jooscript_only", Boolean.FALSE);
        Map model = new HashMap();
        model.put("name", "Mirko");
        flatTemplate.createDocument(model, new FileOutputStream(openDocumentFile));
        System.out.println(openDocumentFile.getAbsolutePath());
	}

}
