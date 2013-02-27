/*******************************************************************************
 * Copyright (c) 2013 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.apitools.ant.internal;

import org.eclipse.pde.apitools.ant.util.IStyleSheetProvider;
import org.eclipse.pde.apitools.ant.util.XMLMemento;

public abstract class RootReport extends AbstractReport implements IStyleSheetProvider {
	private String entityName;
	private String sheetPath = null;
	
	public RootReport(String entityName) {
		this.entityName = entityName;
	}
	/*
	 * This should only be called on root elements.
	 * Root elements should override this and provide their own 
	 * root name
	 */
	public XMLMemento generateMemento() {
		XMLMemento xml = XMLMemento.createWriteRoot(entityName);
		addStylesheetInstructions(xml);
		fillMemento(xml);
		return xml;
	}
	
	protected void addStylesheetInstructions(XMLMemento memento) {
		if( this instanceof IStyleSheetProvider) {
			String s = ((IStyleSheetProvider)this).getStyleSheetPath();
			if( s != null ) {
				memento.addProcessingInstruction("xml-stylesheet",
						"type=\"text/xsl\" href=\"" + s + "\"");
			}
		}
	}
	
	public String getStyleSheetPath() {
		return sheetPath;
	}
	
	public void setStyleSheetPath(String path) {
		this.sheetPath = path;
	}
}
