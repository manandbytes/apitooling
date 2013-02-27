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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.pde.apitools.ant.util.IMemento;


public abstract class AbstractReport {

	protected String id;
	protected ArrayList<AbstractReport> children;
	public AbstractReport() {
		children = new ArrayList<AbstractReport>();
	}
	/**
	 * Add a child report to this report
	 * @param report a child report
	 */
	public void addChildReport(AbstractReport report) {
		children.add(report);
	}
	/**
	 * Iterate through our list of children, and let them
	 * fill the given memento
	 * 
	 * @param xml an xml memento to be filled with data
	 */
	protected void fillChildren(IMemento xml) {
		Iterator<AbstractReport> cIt = children.iterator();
		while(cIt.hasNext()) {
			AbstractReport next = cIt.next();
			next.fillMemento(xml);
		}
	}
	
	/**
	 * Ask children to fill themselves relative to a parent 
	 * Current implementation only passes the responsibility on 
	 * to child reports. 
	 * 
	 * Extenders should create child contexts to the IMemento and fill it
	 * with proper attributes, then pass on such a child memento 
	 * to fillChildren(...). 
	 */
	protected void fillMemento(IMemento parentContext) {
		// Fill with details here, then recurse by calling fillChildren
		fillChildren(parentContext);
	}
}