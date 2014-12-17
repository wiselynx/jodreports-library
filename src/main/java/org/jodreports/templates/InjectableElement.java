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

import java.io.IOException;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;

/**
 * TODO docs 
 * 
 * @author Enrico Manzini
 *
 */
public class InjectableElement extends Element {

	private static ThreadLocal builders = new ThreadLocal() {

		protected synchronized Object initialValue() {
			return new Builder(new InjectableFactory());
		} 

	};

	public InjectableElement(String name) {
		super(name);
	}

	public InjectableElement(String namespace, String name) {
		super(namespace, name);
	}

	public InjectableElement(Element element) {
		super(element);
	}

	public void injectXML(String xml) throws ParsingException {
		xml = "<fakeRoot "
				+ "xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" "
				+ "xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" "
				+ "xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" "
				+ "xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" "
				+ "xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" "
				+ "xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" "
				+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
				+ "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" "
				+ "xmlns:meta=\"urn:oasis:names:tc:opendocument:xmlns:meta:1.0\" "
				+ "xmlns:number=\"urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0\" "
				+ "xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\" "
				+ "xmlns:chart=\"urn:oasis:names:tc:opendocument:xmlns:chart:1.0\" "
				+ "xmlns:dr3d=\"urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0\" "
				+ "xmlns:math=\"http://www.w3.org/1998/Math/MathML\" "
				+ "xmlns:form=\"urn:oasis:names:tc:opendocument:xmlns:form:1.0\" "
				+ "xmlns:script=\"urn:oasis:names:tc:opendocument:xmlns:script:1.0\" "
				+ "xmlns:config=\"urn:oasis:names:tc:opendocument:xmlns:config:1.0\" "
				+ "xmlns:ooo=\"http://openoffice.org/2004/office\" "
				+ "xmlns:ooow=\"http://openoffice.org/2004/writer\" "
				+ "xmlns:oooc=\"http://openoffice.org/2004/calc\" "
				+ "xmlns:dom=\"http://www.w3.org/2001/xml-events\" "
				+ "xmlns:xforms=\"http://www.w3.org/2002/xforms\" "
				+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" "
				+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xmlns:rpt=\"http://openoffice.org/2005/report\" "
				+ "xmlns:of=\"urn:oasis:names:tc:opendocument:xmlns:of:1.2\" "
				+ "xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" "
				+ "xmlns:grddl=\"http://www.w3.org/2003/g/data-view#\" "
				+ "xmlns:officeooo=\"http://openoffice.org/2009/office\" "
				+ "xmlns:tableooo=\"http://openoffice.org/2009/table\" "
				+ "xmlns:drawooo=\"http://openoffice.org/2010/draw\" "
				+ "xmlns:calcext=\"urn:org:documentfoundation:names:experimental:calc:xmlns:calcext:1.0\" "
				+ "xmlns:loext=\"urn:org:documentfoundation:names:experimental:office:xmlns:loext:1.0\" "
				+ "xmlns:field=\"urn:openoffice:names:experimental:ooo-ms-interop:xmlns:field:1.0\" "
				+ "xmlns:formx=\"urn:openoffice:names:experimental:ooxml-odf-interop:xmlns:form:1.0\" "
				+ "xmlns:css3t=\"http://www.w3.org/TR/css3-text/\" >" 
				+ xml + "</fakeRoot>";
		Document doc;
		try {
			doc = ((Builder) builders.get()).build(xml, null);
		} catch (IOException ex) {
			throw new ParsingException(ex.getMessage(), ex);
		}
		Nodes children = doc.getRootElement().removeChildren();
		for (int i = 0; i < children.size(); i++) {
			this.appendChild(children.get(i));
		}

	}

	public Node copy() {
		return new InjectableElement(this);
	}

}
