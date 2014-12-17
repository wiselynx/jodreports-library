package org.jodreports.opendocument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenDocumentArchive {
	
	private static final Logger log = LoggerFactory.getLogger(OpenDocumentArchive.class);
	
	public static final String ENTRY_MIMETYPE = "mimetype";
	public static final String ENTRY_STYLES = "styles.xml";
	public static final String ENTRY_CONTENT = "content.xml";
	public static final String ENTRY_MANIFEST = "META-INF/manifest.xml";
	public static final String ENTRY_SETTINGS = "settings.xml";
	public static final String ENTRY_META = "meta.xml";

	private Map/*<String,byte[]>*/ entries = new LinkedHashMap();

	/**
	 * A {@link ByteArrayOutputStream} that updates the entry data when it get close()d
	 */
	private class EntryByteArrayOutputStream extends ByteArrayOutputStream {
		
		private String entryName;
		
		public EntryByteArrayOutputStream(String entryName) {
			this.entryName = entryName;
		}
		
		public void close() throws IOException {
			entries.put(entryName, toByteArray());
		}
	}

	public Set getEntryNames() {
		return entries.keySet();
	}

	public InputStream getEntryInputStream(String entryName) {
		return new ByteArrayInputStream((byte[]) entries.get(entryName));
	}

	public Reader getEntryReader(String entryName) {
		return OpenDocumentIO.toUtf8Reader(getEntryInputStream(entryName));
	}

	/**
	 * Returns an {@link OutputStream} for writing the content of the given entry
	 * 
	 * @param entryName
	 * @return an {@link OutputStream}
	 */
	public OutputStream getEntryOutputStream(String entryName) {
		return new EntryByteArrayOutputStream(entryName);
	}

	/**
	 * Returns a {@link Writer} for writing the content of the given entry
	 * 
	 * @param entryName
	 * @return a {@link Writer}
	 */
	public Writer getEntryWriter(String entryName) {
		return OpenDocumentIO.toUtf8Writer(getEntryOutputStream(entryName));
	}

	public OpenDocumentArchive createCopy() {
		OpenDocumentArchive archiveCopy = new OpenDocumentArchive();
		for (Iterator it = entries.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			String name = (String) entry.getKey();
			byte[] entryData = (byte[]) entry.getValue();
			byte[] entryDataCopy = new byte[entryData.length];
			System.arraycopy(entryData, 0, entryDataCopy, 0, entryData.length);
			archiveCopy.entries.put(name, entryDataCopy);
		}
		return archiveCopy;
	}
	
	/**
	 * debug method to be used to dump a single entry in the archive (e.g. 
	 * "content.xml" or "styles.xml") to a temporary file.
	 * 
	 * @param entryName		the entry to be dumped
	 * @return the file the entry has been dumped to, or null in case of error
	 */
    public File dumpEntry(String entryName) {
    	File dumpFile = null;
    	try {
	    	dumpFile = File.createTempFile(entryName, ".xml");
			OutputStream dumpOutput = new FileOutputStream(dumpFile);
	    	InputStream dumpInput = getEntryInputStream(entryName);
	    	byte[] buffer = new byte[1024];
			int len = dumpInput.read(buffer);
	    	while (len != -1) {
	    	    dumpOutput.write(buffer, 0, len);
	    	    len = dumpInput.read(buffer);
	    	}
	    	dumpOutput.close();
	    	log.debug(entryName + " dumped to " + dumpFile.getAbsolutePath());
    	} catch (IOException e) {
    		log.debug("error making debug dump for entry " + entryName + ": " + e, e);
    	}
    	return dumpFile;
    }
}
