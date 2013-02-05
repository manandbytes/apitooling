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
package org.eclipse.pde.apitools.ant.tasks.slim;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport.AnalysisSkippedReport;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisRunner;
import org.eclipse.pde.apitools.ant.internal.IgnoredReport;
import org.eclipse.pde.apitools.ant.internal.WrapperReport;
import org.eclipse.pde.apitools.ant.util.ReportUtils;
import org.eclipse.pde.apitools.ant.util.ToolingException;

/**
 * This class requires two folders:
 *    The first must be a full complete baseline
 *    The second may be either:
 *    	1) an alternate-version complete baseline, or
 *      2) a folder with only a few jars meant to replace
 *      	the corresponding bundles in the first baseline
 * 
 * It will create a virtual baseline consisting of these 
 * jars.
 * 
 * This task will generate reports printing out 
 * api changes that break binary compatability
 * in a bundle that has not provided a suitable 
 * change in version.
 * 
 * Example:  Removing an API method without incrementing the 
 * version's major segment.
 */
public class BreakageTask extends AbstractComparisonTask {
	public static final String BREAKAGE_REPORT = "breakageReport.xml";
	public static final String BREAKAGE_ROOT_ELEMENT = "breakage";
	public static final String BUNDLES_SKIPPED_REPORT = "breakageSkippedBundles.xml";

	public void execute() throws BuildException {
		checkArgs();
		printArgs();
		
		// Generate the reports
		if( debug ) {
			System.out.println("Running API Breakage analysis");
		}
		
		ApiAnalysisRunner runner = createAnalysisRunner();
		HashMap<String, ApiAnalysisReport> reports = runner.generateReports();
		
		if( debug ) {
			System.out.println("Generating API Breakage report for " + reports.size() + " bundles");
		}
		
		saveBreakageReport(reports);
		saveIgnoredBundlesReport(reports);
		
		if( debug ) {
			System.out.println("API Breakage Task Complete");
		}
	}

	protected void saveBreakageReport(HashMap<String, ApiAnalysisReport> reportMap) {
		// Iterate and save the reports for each bundle
		Iterator<String> i = reportMap.keySet().iterator();
		WrapperReport main = new WrapperReport(BREAKAGE_ROOT_ELEMENT, getApiBreakageKeys());
		main.setStyleSheetPath(styleSheet);
		while(i.hasNext()) {
			String id = i.next();
			ApiAnalysisReport report = reportMap.get(id);
			if( !(report instanceof AnalysisSkippedReport)) {
				main.addChildReport(report);
			}
		}
		
		try {
			File file = new File(this.reports);
			File file2 = new File(file, BREAKAGE_REPORT);
			ReportUtils.saveReport(main, file2);
		} catch(ToolingException ioe) {
			throw new BuildException(ioe);
		}
	}
	
	protected int[] getApiBreakageKeys() {
		int[] breakages = new int[] { 
				IApiProblem.CATEGORY_VERSION,
				IApiProblem.CATEGORY_COMPATIBILITY, 
				IApiProblem.CATEGORY_SINCETAGS,
				IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION
		};
		return breakages;
	}
		
	protected void saveIgnoredBundlesReport(HashMap<String, ApiAnalysisReport> reportMap) {
		try {
			IgnoredReport report = new IgnoredReport(findSkippedReports(reportMap));
			ReportUtils.saveReport(report, new File(this.reports, BUNDLES_SKIPPED_REPORT));
		} catch(ToolingException ioe) {
			throw new BuildException(ioe);
		}

	}
}
