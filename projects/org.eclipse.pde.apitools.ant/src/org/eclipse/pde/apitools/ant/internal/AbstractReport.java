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
	public void addChildReport(AbstractReport report) {
		children.add(report);
	}
	protected void fillChildren(IMemento xml) {
		Iterator<AbstractReport> cIt = children.iterator();
		while(cIt.hasNext()) {
			AbstractReport next = cIt.next();
			next.fillMemento(xml);
		}
	}
	/*
	 * Ask children to fill themselves relative to a parent 
	 */
	protected void fillMemento(IMemento parentContext) {
		// Fill with details here, then recurse by calling fillChildren
		
		fillChildren(parentContext);
	}
}