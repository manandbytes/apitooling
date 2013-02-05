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
package org.eclipse.pde.apitools.ant.tasks;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport.AnalysisSkippedReport;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisRunner;
import org.eclipse.pde.apitools.ant.internal.IgnoredReport;
import org.eclipse.pde.apitools.ant.tasks.old.Messages;
import org.eclipse.pde.apitools.ant.util.ReportUtils;
import org.eclipse.pde.apitools.ant.util.ToolingException;

public class ApiAnalysisTask extends AbstractComparisonTask {
	public static final String REPORT_NAME = "analysisReport.xml";
	public static final String ANALYSIS_SKIPPED_REPORT_NAME = "apiAnalysisSkippedBundles.xml";

	public void execute() throws BuildException {
		checkArgs();
		if( debug ) {
			printArgs();
			System.out.println("\nRunning API Analysis");
		}
		
		// Generate the reports
		ApiAnalysisRunner runner = 
				new ApiAnalysisRunner(referenceBaseline, referenceBaseline, 
						reports, filters,  properties, 
						skipNonApi, styleSheet,
						includeListLocation, excludeListLocation, debug);
		HashMap<String, ApiAnalysisReport> reports = runner.generateReports();
		
		if( debug )
			System.out.println("Generating API Analysis Reports");

		// Iterate and save the reports for each bundle
		Iterator<String> i = reports.keySet().iterator();
		while(i.hasNext()) {
			try {
				String id = i.next();
				ApiAnalysisReport report = reports.get(id);
				if( !(report instanceof AnalysisSkippedReport)) {
					File file = new File(this.reports, id);
					File file2 = new File(file, REPORT_NAME);
					if( debug ) 
						System.out.println("Saving report for bundle " + id + " to " + file2.getAbsolutePath());
					ReportUtils.saveReport(report, file2);
				}
			} catch(ToolingException ioe) {
				throw new BuildException(ioe);
			}
		}
		
		// Add any skipped / not-analyzed bundle to a file
		try {
			IgnoredReport report = new IgnoredReport(findSkippedReports(reports));
			ReportUtils.saveReport(report, new File(this.reports, ANALYSIS_SKIPPED_REPORT_NAME));
		} catch(ToolingException ioe) {
			throw new BuildException(ioe);
		}

	}
}
