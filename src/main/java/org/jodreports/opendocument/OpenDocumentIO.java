package org.jodreports.opendocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class OpenDocumentIO {
	
	public static final Charset UTF_8 = Charset.forName("UTF-8");

	public static InputStreamReader toUtf8Reader(InputStream inputStream) {
		return new InputStreamReader(inputStream, UTF_8);
	}

	public static OutputStreamWriter toUtf8Writer(OutputStream outputStream) {
		return new OutputStreamWriter(outputStream, UTF_8);
	}

	public static OpenDocumentArchive readZip(InputStream inputStream) throws IOException {
		OpenDocumentArchive archive = new OpenDocumentArchive();
		ZipInputStream zipInputStream = new ZipInputStream(inputStream);
		while (true) {
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			if (zipEntry == null) {
				break;
			}
			OutputStream entryOutputStream = archive.getEntryOutputStream(zipEntry.getName());
			IOUtils.copy(zipInputStream, entryOutputStream);
			entryOutputStream.close();
			zipInputStream.closeEntry();
		}
		zipInputStream.close();
		return archive;
	}

	public static OpenDocumentArchive readDirectory(File directory) throws IOException {
		if (!(directory.isDirectory() && directory.canRead())) {
			throw new IllegalArgumentException("not a readable directory: " + directory);
		}
		OpenDocumentArchive archive = new OpenDocumentArchive();
		readSubDirectory(directory, "", archive);
		return archive;
	}

	private static void readSubDirectory(File subDirectory, String parentName, OpenDocumentArchive archive) throws IOException {
		String[] fileNames = subDirectory.list();
		for (int i = 0; i < fileNames.length; i++) {
			File file = new File(subDirectory, fileNames[i]);
			String relativeName = parentName + fileNames[i];
			if (file.isDirectory()) {
				readSubDirectory(file, relativeName + "/", archive);
			} else {
				InputStream fileInputStream = new FileInputStream(file);
				OutputStream entryOutputStream = archive.getEntryOutputStream(relativeName);
				IOUtils.copy(fileInputStream, entryOutputStream);
				entryOutputStream.close();
				fileInputStream.close();
			}
		}
	}

	public static void writeZip(OpenDocumentArchive archive, OutputStream outputStream) throws IOException {
		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
		Set entryNames = archive.getEntryNames();
		
		// OpenDocument spec requires 'mimetype' to be the first entry
		writeZipEntry(zipOutputStream, archive, OpenDocumentArchive.ENTRY_MIMETYPE, ZipEntry.STORED);
		
		for (Iterator it = entryNames.iterator(); it.hasNext();) {
			String entryName = (String) it.next();
			if (!entryName.equals(OpenDocumentArchive.ENTRY_MIMETYPE)) {
				writeZipEntry(zipOutputStream, archive, entryName, ZipEntry.DEFLATED);
			}
		}
		zipOutputStream.close();
	}
	
	private static InputStream removeEmptyTextNodes(InputStream inStream) throws IOException {
		byte[] output = null;
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(inStream);
			XPathFactory xpathFactory = XPathFactory.newInstance();
			XPathExpression xpathExp = xpathFactory.newXPath().compile(
			        "//text()[normalize-space(.) = '']");  
			NodeList emptyTextNodes = (NodeList) 
			        xpathExp.evaluate(document, XPathConstants.NODESET);
	
			for (int i = 0; i < emptyTextNodes.getLength(); i++) {
			    Node emptyTextNode = emptyTextNodes.item(i);
			    emptyTextNode.getParentNode().removeChild(emptyTextNode);
			}
			
			Transformer tr = TransformerFactory.newInstance().newTransformer();
	        tr.setOutputProperty(OutputKeys.INDENT, "no");
	        tr.setOutputProperty(OutputKeys.METHOD, "xml");
	        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//	        tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd");
	        tr.transform(new DOMSource(document), new StreamResult(outStream));
			
			output = outStream.toByteArray();

		} catch (ParserConfigurationException e) {
			throw new IOException("error removing empty text nodes: " + e, e);
		} catch (SAXException e) {
			throw new IOException("error removing empty text nodes: " + e, e);
		} catch (IOException e) {
			throw new IOException("error removing empty text nodes: " + e, e);
		} catch (XPathExpressionException e) {
			throw new IOException("error removing empty text nodes: " + e, e);
		} catch (TransformerConfigurationException e) {
			throw new IOException("error removing empty text nodes: " + e, e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new IOException("error removing empty text nodes: " + e, e);
		} catch (TransformerException e) {
			throw new IOException("error removing empty text nodes: " + e, e);
		}
		return new ByteArrayInputStream(output);
	}

	private static void writeZipEntry(ZipOutputStream zipOutputStream, OpenDocumentArchive archive, String entryName, int method) throws IOException {
		ZipEntry zipEntry = new ZipEntry(entryName);
		InputStream entryInputStream = archive.getEntryInputStream(entryName);
		if (OpenDocumentArchive.ENTRY_CONTENT.equals(entryName)
			|| OpenDocumentArchive.ENTRY_META.equals(entryName)
			|| OpenDocumentArchive.ENTRY_SETTINGS.equals(entryName)
			|| OpenDocumentArchive.ENTRY_STYLES.equals(entryName)) {
			entryInputStream = removeEmptyTextNodes(archive.getEntryInputStream(entryName));
		}
		zipEntry.setMethod(method);
		if (method == ZipEntry.STORED) {
			byte[] inputBytes = IOUtils.toByteArray(entryInputStream);
			CRC32 crc = new CRC32();
			crc.update(inputBytes);
			zipEntry.setCrc(crc.getValue());
			zipEntry.setSize(inputBytes.length);
			zipEntry.setCompressedSize(inputBytes.length);
			zipOutputStream.putNextEntry(zipEntry);
			IOUtils.write(inputBytes, zipOutputStream);
		} else {
			zipOutputStream.putNextEntry(zipEntry);
			IOUtils.copy(entryInputStream, zipOutputStream);
		}
		IOUtils.closeQuietly(entryInputStream);
		zipOutputStream.closeEntry();
	}
}
