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
package org.jodreports.templates.xmlfilters.tags;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.ParsingException;

import org.jodreports.templates.InjectableElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO docs
 * 
 * @author Enrico Manzini
 *
 */
public class IncludeTag implements JooScriptTag {

	private static final Logger log = LoggerFactory.getLogger(IncludeTag.class);
	
	public void process(Element scriptElement, Element tagElement) {
		InputStream includeInput = null;
		try {
			if (tagElement.getAttribute("resource") != null) {
				String resourcePath  = tagElement.getAttribute("resource").getValue();
				includeInput = getClass().getClassLoader().getResourceAsStream(resourcePath);
				if (includeInput == null) {
					log.warn("include resource " + resourcePath + " could not be found or loaded");
				}
			} else if (tagElement.getAttribute("file") != null) {
				String filePath  = tagElement.getAttribute("file").getValue();
				File file = new File(filePath);
				try {
					includeInput = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					log.warn("include file " + filePath + " does not exist");
				}
			}
			if (includeInput != null) {
				Node replaceElement = scriptElement.getParent();
				
				try {
					BufferedReader br = null;
					StringBuilder sb = new StringBuilder();
					String line;
					try {
						br = new BufferedReader(new InputStreamReader(includeInput));
						while ((line = br.readLine()) != null) {
							sb.append(line);
							sb.append('\n');
						}			 
					} finally {
						if (br != null) {
							try {
								br.close();
							} catch (IOException e) {
								log.error("could not close include input buffer: " + e, e);
							}
						}
					}
					String includeText = sb.toString();
					((InjectableElement)replaceElement).injectXML(includeText);
					if (tagElement.getAttribute("replace").getValue().equals("parent")) {
						ParentNode parentElement = replaceElement.getParent();
						int replaceElementPosition = parentElement.indexOf(replaceElement); 
						for(int i=replaceElement.getChildCount() - 1; i>=0; i--) {
							Node toPlaceNode = replaceElement.getChild(i);
							if (!toPlaceNode.equals(scriptElement)) {
								toPlaceNode.detach();
								parentElement.insertChild(toPlaceNode, replaceElementPosition);
							}
						}
						replaceElement.detach();
					} 
				} catch (ParsingException e) {
					log.error("error opening include document: " + e, e);
				} catch (IOException e) {
					log.error("error opening include document: " + e, e);
				} 
			}
		} finally {
			if (includeInput != null) {
				try {
					includeInput.close();
				} catch (IOException e) {
					log.error("could not close include input stream: " + e, e);
				}
			}
		}
	}

}
