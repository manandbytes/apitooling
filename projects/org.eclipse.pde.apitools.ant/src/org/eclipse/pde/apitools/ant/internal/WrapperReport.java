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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.eclipse.pde.apitools.ant.util.IMemento;


public class WrapperReport extends RootReport {

	private int[] problemTypes;
	public WrapperReport(String entityName, int[] problemTypes) {
		super(entityName);
		this.problemTypes = problemTypes;
	}
	protected void fillChildren(IMemento xml) {
		ArrayList<AbstractReport> tmp = new ArrayList<AbstractReport>();
		tmp.addAll(children);
		Collections.sort(tmp, new Comparator<AbstractReport>(){
			public int compare(AbstractReport o1, AbstractReport o2) {
				boolean b1, b2;
				b1 = o1 instanceof ApiAnalysisReport;
				b2 = o2 instanceof ApiAnalysisReport;
				if( !b1 && !b2)
					return 0;
				if( !b1 )
					return -1;
				if( !b2 )
					return 1;
				return ((ApiAnalysisReport)o1).countProblems(problemTypes) - ((ApiAnalysisReport)o2).countProblems(problemTypes);
			}
			
		});
		Collections.reverse(tmp);
		Iterator<AbstractReport> cIt = tmp.iterator();
		while(cIt.hasNext()) {
			IMemento bundle = xml.createChild("bundle");
			ApiAnalysisReport next = (ApiAnalysisReport)cIt.next();
			next.fillMemento(bundle, problemTypes);
		}
	}

	
}
