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

import nu.xom.Element;
import nu.xom.NodeFactory;

/**
 * TODO docs
 * 
 * @author Enrico Manzini
 *
 */
public class InjectableFactory extends NodeFactory {
	
	public Element startMakingElement(String namespaceURI, String name) {
        return new InjectableElement(namespaceURI, name);
    }

}
