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

import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport.AnalysisSkippedReport;
import org.eclipse.pde.apitools.ant.util.IMemento;

public class IgnoredReport extends RootReport {
	public static final String ROOT_ELEMENT = "report";
	public static final String BUNDLE_ELEMENT = "bundle";
	public static final String BUNDLE_NAME_ATTR = "name";
	public static final String BUNDLE_CAUSE_ATTR = "cause";
	
	
	ArrayList<AnalysisSkippedReport> list;
	public IgnoredReport(ArrayList<AnalysisSkippedReport> ignored) {
		super("report");
		this.list = ignored;
	}
	protected void fillMemento(IMemento parentContext) {
		Iterator<AnalysisSkippedReport> i = list.iterator();
		while(i.hasNext()) {
			AnalysisSkippedReport asr = i.next();
			IMemento next = parentContext.createChild(BUNDLE_ELEMENT);
			next.putString(BUNDLE_NAME_ATTR, asr.getId());
			next.putString(BUNDLE_CAUSE_ATTR, asr.getCause());
		}
	}

}
