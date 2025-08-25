package org.jodreports.templates.image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class VectorImageSource implements ImageSource {

	private int width;
	private int height;
	private byte[] bytes;
	
	public VectorImageSource(final byte[] bytes) throws ValidityException, ParsingException, IOException {
		this.bytes = bytes;
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		try {
			Builder builder = new Builder();
			Document document = null;
			document = builder.build(stream);
			Element rootNode = document.getRootElement();
			Attribute widthAttr = rootNode.getAttribute("width");
			if (widthAttr != null) {
				width = Integer.parseInt(widthAttr.getValue());
			}
			Attribute heightAttr = rootNode.getAttribute("height");
			if (heightAttr != null) {
				height = Integer.parseInt(heightAttr.getValue());
			} 
		} finally {
			stream.close();
		}
	}
	
	@Override
	public void write(OutputStream paramOutputStream) throws IOException {
		paramOutputStream.write(bytes);
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public String getFileExtension() {
		return "svg";
	}

	@Override
	public String getMimeType() {
		return "image/svg+xml";
	}
	
}