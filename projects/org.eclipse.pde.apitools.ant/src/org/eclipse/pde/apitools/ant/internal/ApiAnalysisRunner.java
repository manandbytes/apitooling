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

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.eclipse.pde.api.tools.internal.model.StubApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.apitools.ant.util.BaselineUtils;

public class ApiAnalysisRunner extends AbstractAnalysisRunner {
	public static final String SUMMARY_REPORT_NAME = "ANALYSIS_SUMMARY";
	private static final String REFERENCE_BASE = "referenceBase";
	private static final String CURRENT_BASE = "currentBase";
	
	
	private String referenceBaseline;
	private String profileBaselineLocation = null;
	private File[] profileBaselineFiles = null;
	private String includeListLocation;
	private String excludeListLocation;
	
	
	private IApiBaseline refBaseline, profileBaseline;
	private IApiComponent[] refIncluded, profileIncluded;
	
	public ApiAnalysisRunner(String referenceBaseline, String profileBaselineLocation,
			String reports, String filters, Properties properties,
			boolean skipNonApi, String xslSheet,
			String includeListLocation, String excludeListLocation, boolean debug) {
		super(reports, filters, properties, skipNonApi, xslSheet, debug);
		this.referenceBaseline = referenceBaseline;
		this.profileBaselineLocation = profileBaselineLocation;
		this.includeListLocation = includeListLocation;
		this.excludeListLocation = excludeListLocation;
	}
	
	public void disposeBaselines() {
		long time = System.currentTimeMillis();
		if( refBaseline != null )
			refBaseline.dispose();
		if( profileBaseline != null )
			profileBaseline.dispose();
		StubApiComponent.disposeAllCaches();
		if (this.debug) {
			System.out.println("Cleanup : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	public IApiBaseline getReferenceBaseline() {
		return refBaseline;
	}
	
	public IApiBaseline getProfileBaseline() {
		return profileBaseline;
	}

	public IApiComponent[] getReferenceComponents() {
		return refIncluded;
	}
	
	public IApiComponent[] getProfileComponents() {
		return profileIncluded;
	}

	public ApiAnalysisRunner(String referenceBaseline, 
			File[] profileBaselineFiles,
			String reports, String filters, Properties properties,
			boolean skipNonApi, String xslSheet,
			String includeListLocation, String excludeListLocation, boolean debug) {
		super(reports, filters, properties, skipNonApi, xslSheet, debug);
		this.referenceBaseline = referenceBaseline;
		this.profileBaselineFiles = profileBaselineFiles;
		this.includeListLocation = includeListLocation;
		this.excludeListLocation = excludeListLocation;
	}

	public HashMap<String, ApiAnalysisReport> generateReports() throws BuildException {
		createBaselines();
		createInclusionArrays();
		return generateReports(refBaseline, refIncluded, profileIncluded, properties);
	}
	
	public void createInclusionArrays() {
		long time = System.currentTimeMillis();
		if( debug )
			System.out.println("Introspecting Inclusion and Exclusion patterns... ");

		// Get all included elements AFTER the filters are applied
		refIncluded = BaselineUtils.getFilteredElements(
				refBaseline, includeListLocation, excludeListLocation);
		profileIncluded = BaselineUtils.getFilteredElements(
				profileBaseline, includeListLocation, excludeListLocation);
		if( debug ) {
			System.out.println("Filtering Api Elements Complete in " + (System.currentTimeMillis() - time) + "ms");
		}
	}
	
	public void createBaselines() {
		long time = System.currentTimeMillis();

		if( debug )
			System.out.println("Creating Reference Baseline...");

		// Create two baselines
		refBaseline = BaselineUtils.createBaseline(
				REFERENCE_BASE, referenceBaseline, null);
		
		if( debug )
			System.out.println("Creating Profile Baseline...");

		// The profile baseline can be set either through a folder
		// Or a java.io.File array
		profileBaseline = null;
		if( profileBaselineFiles == null ) {
			profileBaseline = BaselineUtils.createBaseline(
				CURRENT_BASE, profileBaselineLocation, null);
		} else {
			profileBaseline = BaselineUtils.createBaseline(CURRENT_BASE, profileBaselineFiles);
		}
		
		if( debug )
			System.out.println("Finished Loading Baselines in " + (System.currentTimeMillis() - time) + "ms");
	}
}
