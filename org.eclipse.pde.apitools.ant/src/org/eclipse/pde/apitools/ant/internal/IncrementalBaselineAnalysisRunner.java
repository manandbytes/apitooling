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

import java.util.HashMap;
import java.util.Properties;


import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.apitools.ant.util.BaselineUtils;

/*
 * This class is not fully tested at this time and is not advised
 * to be used until further notice. 
 */
public class IncrementalBaselineAnalysisRunner extends AbstractAnalysisRunner {
	public static final String SUMMARY_REPORT_NAME = "ANALYSIS_SUMMARY";
	private static final String REFERENCE_BASE = "referenceBase";
	private static final String CURRENT_BASE = "currentBase";
	
	
	private String referenceBaseline;
	private String currentBaselineLocation;
	private String includeListLocation;
	private String excludeListLocation;
	
	public IncrementalBaselineAnalysisRunner(String referenceBaseline, String currentBaseline,
			String reports, String filters, Properties properties,
			boolean debug) {
		super(reports, filters, properties, false, null, debug);
		this.referenceBaseline = referenceBaseline;
		this.currentBaselineLocation = currentBaseline;
	}
	
	public HashMap<String, ApiAnalysisReport> generateReports() throws BuildException {
		IApiBaseline refBase = BaselineUtils.createBaseline("name555", referenceBaseline, null, null);
		IApiBaseline currentBase =  BaselineUtils.createBaseline("name556", referenceBaseline, currentBaselineLocation, null);

		if( debug )
			System.out.println("Loading Baselines...");
		try {
			IApiComponent[] fromUpdates = BaselineUtils.getComponentsFromLocation(refBase, currentBaselineLocation);
			
			IApiComponent[] refIncluded = BaselineUtils.getFilteredElements(
					refBase, fromUpdates);
			IApiComponent[] curIncluded = BaselineUtils.getFilteredElements(
					currentBase, fromUpdates);
			if( debug )
				System.out.println("Finished Loading Baselines.");
			return generateReports(refBase, refIncluded, curIncluded, properties);
		} catch(CoreException ce) {
			ce.printStackTrace();
			throw new BuildException(ce);
		}
	}
}
