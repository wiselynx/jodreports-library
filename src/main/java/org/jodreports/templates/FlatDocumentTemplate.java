//
// JOOReports - The Open Source Java/OpenOffice Report Engine
// Copyright (C) 2004-2006 - Mirko Nasato <mirko@artofsolving.com>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// http://www.gnu.org/copyleft/lesser.html
//
package org.jodreports.templates;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jodreports.opendocument.OpenDocumentArchive;
import org.jodreports.opendocument.OpenDocumentNamespaces;

import freemarker.template.Configuration;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;

public class FlatDocumentTemplate extends AbstractDocumentTemplate {

	private OpenDocumentArchive archive;

	public FlatDocumentTemplate(InputStream inputStream, Configuration freemarkerConfiguration) 
	throws IOException, DocumentTemplateException {
		super(freemarkerConfiguration);
		archive = readXml(inputStream);
	}
	
	public FlatDocumentTemplate(File xml, Configuration freemarkerConfiguration) 
	throws IOException, DocumentTemplateException {
		super(freemarkerConfiguration);
		archive = readXml(new FileInputStream(xml));
	}
	
	public FlatDocumentTemplate(OpenDocumentArchive archive, Configuration freemarkerConfiguration) 
	throws IOException, DocumentTemplateException {
		super(freemarkerConfiguration);
		this.archive = archive;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private OpenDocumentArchive readXml(final InputStream inputStream) 
	throws IOException, DocumentTemplateException {
		archive = new OpenDocumentArchive();
		Builder builder = new Builder();
		Document document = null;
		try {
			document = builder.build(inputStream);
		} catch (ParsingException parsingException) {
			throw new DocumentTemplateException(parsingException);
		}
		Element rootNode = document.getRootElement();
		Map declaredNamespaces = new HashMap(rootNode.getNamespaceDeclarationCount());
		for (int i=0; i<rootNode.getNamespaceDeclarationCount(); i++) {
			String nsPrefix = rootNode.getNamespacePrefix(i);
			declaredNamespaces.put(nsPrefix, rootNode.getNamespaceURI(nsPrefix));
		}
		
		byte[] mimeType = rootNode
				.getAttribute("mimetype", OpenDocumentNamespaces.URI_OFFICE)
				.getValue()
				.getBytes();
		OutputStream mimeOutputStream = archive.getEntryOutputStream(OpenDocumentArchive.ENTRY_MIMETYPE);
		mimeOutputStream.write(mimeType, 0, mimeType.length);
		mimeOutputStream.close();
		
		Nodes metaNodes = rootNode.query("office:meta", 
				OpenDocumentNamespaces.XPATH_CONTEXT);
		readElement("office:document-meta", OpenDocumentArchive.ENTRY_META, 
				declaredNamespaces, metaNodes);
		
		Nodes settingsNodes = rootNode.query("office:settings", 
				OpenDocumentNamespaces.XPATH_CONTEXT);
		readElement("office:document-settings", OpenDocumentArchive.ENTRY_SETTINGS, 
				declaredNamespaces, settingsNodes);
		
		Nodes stylesNodes = rootNode.query("office:styles", 
				OpenDocumentNamespaces.XPATH_CONTEXT);
		addAllNodes(stylesNodes, rootNode.query("office:automatic-styles", 
				OpenDocumentNamespaces.XPATH_CONTEXT));
		addAllNodes(stylesNodes, rootNode.query("office:master-styles", 
				OpenDocumentNamespaces.XPATH_CONTEXT));
		addAllNodes(stylesNodes, rootNode.query("office:font-face-decls", 
				OpenDocumentNamespaces.XPATH_CONTEXT));
		readElement("office:document-styles", OpenDocumentArchive.ENTRY_STYLES, 
				declaredNamespaces, stylesNodes);
		
		Nodes contentNodes = rootNode.query("office:scripts", 
				OpenDocumentNamespaces.XPATH_CONTEXT);
		addAllNodes(contentNodes, rootNode.query("office:font-face-decls", 
				OpenDocumentNamespaces.XPATH_CONTEXT));
		addAllNodes(contentNodes, rootNode.query("office:automatic-styles", 
				OpenDocumentNamespaces.XPATH_CONTEXT));
		addAllNodes(contentNodes, rootNode.query("office:body", 
				OpenDocumentNamespaces.XPATH_CONTEXT));
		readElement("office:document-content", OpenDocumentArchive.ENTRY_CONTENT, 
				declaredNamespaces, contentNodes);
		
		String version = rootNode
				.getAttribute("version", OpenDocumentNamespaces.URI_OFFICE)
				.getValue();
		Element manifestRoot =  new Element("manifest:manifest", OpenDocumentNamespaces.URI_MANIFEST);
		manifestRoot.addAttribute(new Attribute("manifest:version", OpenDocumentNamespaces.URI_MANIFEST, version));
		Element manifestFirst = addManifestElement("/", "application/vnd.oasis.opendocument.text");
		manifestFirst.addAttribute(new Attribute("manifest:version", OpenDocumentNamespaces.URI_MANIFEST, version));
		manifestRoot.appendChild(manifestFirst);
		manifestRoot.appendChild(addManifestElement(OpenDocumentArchive.ENTRY_CONTENT, "text/xml"));
		manifestRoot.appendChild(addManifestElement(OpenDocumentArchive.ENTRY_STYLES, "text/xml"));
		manifestRoot.appendChild(addManifestElement(OpenDocumentArchive.ENTRY_SETTINGS, "text/xml"));
		manifestRoot.appendChild(addManifestElement(OpenDocumentArchive.ENTRY_META, "text/xml"));
		Document manifest = new Document(manifestRoot);
		addEntry(OpenDocumentArchive.ENTRY_MANIFEST, manifest);
	
		return archive;
	}
	
	private void addAllNodes(final Nodes dest, final Nodes toAdd) {
		for (int nodeIndex = 0; nodeIndex < toAdd.size(); nodeIndex++) {
			dest.append(toAdd.get(nodeIndex));
		}	
	}
	
	private void readElement(final String rootName, final String entryName,
			final Map namespaces, final Nodes nodes) 
	throws IOException {
		Document doc = copyElement(rootName, namespaces, nodes);
		addEntry(entryName, doc);
	}
	
	private Document copyElement(final String rootName, final Map namespaces, final Nodes nodes) {
		Element rootElement = new Element(rootName, OpenDocumentNamespaces.URI_OFFICE);
		Iterator rootChildren = namespaces.keySet().iterator();
		while (rootChildren.hasNext()) {
			String nsPrefix = (String)rootChildren.next();
			rootElement.addNamespaceDeclaration(nsPrefix, (String)namespaces.get(nsPrefix));
		}
		for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
			Node clonedNode = nodes.get(nodeIndex).copy();
			rootElement.appendChild(clonedNode);
		}
		return new Document(rootElement);
	}
	
	private void addEntry(final String entryName, final Document doc) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		Serializer serializer = new Serializer(byteArrayOutputStream, TemplatePreProcessor.UTF_8);
		serializer.write(doc);
		OutputStream outputStream = archive.getEntryOutputStream(entryName);
		outputStream.write(byteArrayOutputStream.toByteArray());
		outputStream.close();
	}
	
	private Element addManifestElement(final String fullPath, final String mediaType) {
		Element newElement =  new Element("manifest:file-entry", OpenDocumentNamespaces.URI_MANIFEST);
		newElement.addAttribute(new Attribute("manifest:full-path", OpenDocumentNamespaces.URI_MANIFEST, fullPath));
		newElement.addAttribute(new Attribute("manifest:media-type", OpenDocumentNamespaces.URI_MANIFEST, mediaType));
		return newElement;
	}

    public OpenDocumentArchive getOpenDocumentArchive() {
    	return archive;
    }
    
	private Node dumpEntry(final OpenDocumentArchive outputArchive, final String entryName) 
	throws IOException, DocumentTemplateException {
		InputStream in = outputArchive.getEntryInputStream(entryName);
		Node dump = null;
		try {
			Document document = new Builder().build(in);
			dump = document.getRootElement();
		} catch (Exception e) {
			throw new DocumentTemplateException(e);
		} finally {
			in.close();
		}
		return dump.copy();
	}
	
    public void createDocument(Object model, OutputStream output) throws IOException, DocumentTemplateException {
    	OpenDocumentArchive outputArchive = processDocument(model);
    	
    	Element root = new Element("office:document", OpenDocumentNamespaces.URI_OFFICE);
    	
    	ByteArrayOutputStream result = new ByteArrayOutputStream();
    	byte[] buffer = new byte[1024];
    	int length;
    	InputStream in = outputArchive.getEntryInputStream(OpenDocumentArchive.ENTRY_MIMETYPE);
    	while ((length = in.read(buffer)) != -1) {
    	    result.write(buffer, 0, length);
    	}
    	in.close();
    	String mimeType = result.toString("UTF-8");
    	root.addAttribute(new Attribute("office:mimetype", OpenDocumentNamespaces.URI_OFFICE, mimeType));
    	
    	Element meta = new Element("office:meta", OpenDocumentNamespaces.URI_OFFICE);
    	meta.appendChild(dumpEntry(outputArchive, OpenDocumentArchive.ENTRY_META));
    	root.appendChild(meta);
    	
    	Element settings = new Element("office:settings", OpenDocumentNamespaces.URI_OFFICE);
    	settings.appendChild(dumpEntry(outputArchive, OpenDocumentArchive.ENTRY_SETTINGS));
    	root.appendChild(settings);
    	
    	Element styles = new Element("office:styles", OpenDocumentNamespaces.URI_OFFICE);
    	styles.appendChild(dumpEntry(outputArchive, OpenDocumentArchive.ENTRY_STYLES));
    	root.appendChild(styles);
    	
    	Element content = new Element("office:scripts", OpenDocumentNamespaces.URI_OFFICE);
    	content.appendChild(dumpEntry(outputArchive, OpenDocumentArchive.ENTRY_CONTENT));
    	root.appendChild(content);
//    	
//    	Element manifest = new Element("manifest:manifest", OpenDocumentNamespaces.URI_MANIFEST);
//    	manifest.appendChild(dumpEntry(outputArchive, OpenDocumentArchive.ENTRY_MANIFEST));
//    	root.appendChild(manifest);
    	
    	Document doc = new Document(root);
    	Serializer serializer = new Serializer(output, TemplatePreProcessor.UTF_8);
		serializer.write(doc);
    }
    
}
