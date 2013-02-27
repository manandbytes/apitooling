/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.apitools.ant.tasks;

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.apitools.ant.util.StringUtils;

/**
 * This class is used to exclude some deltas from the generated report.
 */
public class FilterListDeltaVisitor extends AbstractFilterListDeltaVisitor {
	private FilteredElements excludedElements;
	private FilteredElements includedElements;
	private ArrayList<String> nonExcludedElements;

	private int flags;
	
	public FilterListDeltaVisitor(FilteredElements excludedElements,FilteredElements includedElements, int flags) throws CoreException {
		super(flags);
		this.excludedElements = excludedElements;
		this.includedElements = includedElements;
		this.nonExcludedElements = new ArrayList<String>();
		this.flags = flags;
	}
	
	/* 
	 * Get a String representation, one component per line, of 
	 * not explicitly excluded (but potentially excluded) components 
	 */
	public String getPotentialExcludeList() {
		if (this.nonExcludedElements != null) { 
			Collections.sort(this.nonExcludedElements);
			return StringUtils.toLineSeparatedString(nonExcludedElements);
		}
		return Util.EMPTY_STRING;
	}
	
	protected boolean isExcluded(IDelta delta) {
		String listKey = generateListKey(delta);
		String componentId = delta.getComponentId();
		if( componentId == null)
			return false;
		if (this.excludedElements.containsExactMatch(componentId)
				|| this.excludedElements.containsPartialMatch(componentId)) {
			return true;
		}
		if (!this.includedElements.isEmpty() && !(this.includedElements.containsExactMatch(componentId)
				|| this.includedElements.containsPartialMatch(componentId))) {
			return true;
		}
		if (this.excludedElements.containsExactMatch(listKey)) {
			return true;
		}
		if (!this.includedElements.isEmpty() && !(this.includedElements.containsExactMatch(delta.getKey())
				|| this.includedElements.containsPartialMatch(delta.getKey()))) {
			return true;
		}
		
		this.nonExcludedElements.add(listKey);
		return false;
	}
}