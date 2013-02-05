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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.ant.core.Task;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport.AnalysisSkippedReport;
import org.eclipse.pde.apitools.ant.util.IOUtil;

/**
 * This abstract superclass is intended for any API Tools
 * Task which requires two baselines, specifically 
 * an original baseline (most likely the previous major release)
 * and a profile baseline (a nightly build or maintenance release).
 * 
 * It includes properties for:
 *     A reference baseline
 *     A profile baseline
 *     A report directory
 *     A properties file listing bundles to include in the analysis
 *     A properties file listing bundles to exclude in the analysis
 *     A file representing the api filters to be used during the analysis
 *     A style sheet, such that the generated reports can reference it
 *     A boolean flag indicating to skip non-api bundles
 *     A boolean flag indicating debug output is requested
 */
public abstract class AbstractComparisonTask extends Task {
	protected String referenceBaseline;
	protected String profileBaseline;
	protected String reports;
	protected String includeListLocation;
	protected String excludeListLocation;
	protected String filters;
	protected Properties properties;
	protected String styleSheet;
	protected boolean skipNonApi = false;
	protected boolean debug;
	
	protected ArrayList<AnalysisSkippedReport> findSkippedReports(HashMap<String, ApiAnalysisReport> reportMap) {
		ArrayList<AnalysisSkippedReport> ignored = new ArrayList<AnalysisSkippedReport>();
		Iterator<String> i = reportMap.keySet().iterator();
		while(i.hasNext()) {
			ApiAnalysisReport report = reportMap.get(i.next());
			if( report instanceof AnalysisSkippedReport) {
				ignored.add((AnalysisSkippedReport)report);
			}
		}
		return ignored;
	}


	/**
	 * Set the location of the reference baseline.
	 * 
	 * <p>It can be a .zip, .jar, .tgz, .tar.gz file, or a directory that corresponds to 
	 * the Eclipse installation folder. This is the directory is which you can find the 
	 * Eclipse executable.
	 * </p>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param baselineLocation the given location for the reference baseline to analyze
	 */
	public void setBaseline(String baselineLocation) {
		this.referenceBaseline = baselineLocation;
	}
	
	/**
	 * The alternate baseline or partial baseline to be combined
	 * with the reference baseline. 
	 * 
	 * @param nightly the given location for a complete or partial newer baseline
	 */
	public void setProfile(String nightly) {
		this.profileBaseline = nightly;
	}
	

	/**
	 * Set the output location where the reports will be generated.
	 * 
	 * <p>Once the task is completed, a report will be posted 
	 * in this folder by the name breakageReport.xml, 
	 * with another report named breakageSkippedBundles.xml</p> 
	 * <p>A special folder called "allNonApiBundles" is also created in this folder that contains a xml file called
	 * "report.xml". This file lists all the bundles that are not using the API Tools nature.</p>
	 * 
	 * @param baselineLocation the given location for the reference baseline to analyze
	 */
	public void setReport(String reportLocation) {
		this.reports = reportLocation;
	}
	

	/**
	 * Set the root directory of API filters to use during the analysis.
	 * 
	 * <p>The argument is the root directory of the .api_filters files that should be used to filter potential
	 * problems created by the API Tools analysis. The root needs to contain the following structure:</p>
	 * <pre>
	 * root
	 *  |
	 *  +-- component name (i.e. org.eclipse.jface)
	 *         |
	 *         +--- .api_filters
	 * </pre>
	 *
	 * @param filters the root of the .api_filters files
	 */
	public void setFilters(String filters) {
		this.filters = filters; 
	}
	/**
	 * Set the preferences for the task.
	 * 
	 * <p>The preferences are used to configure problem severities. Problem severities have
	 * three possible values: Ignore, Warning, or Error. The set of problems detected is defined
	 * by corresponding problem preference keys in API tools.</p>
	 * <p>If the given location doesn't exist, the preferences won't be set.</p>
	 * <p>Lines starting with '#' are ignored. The format of the preferences file looks like this:</p>
	 * <pre>
	 * #Thu Nov 20 17:35:06 EST 2008
	 * ANNOTATION_ELEMENT_TYPE_ADDED_METHOD_WITHOUT_DEFAULT_VALUE=Ignore
	 * ANNOTATION_ELEMENT_TYPE_CHANGED_TYPE_CONVERSION=Ignore
	 * ...
	 * </pre>
	 * <p>The keys can be found in {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes}.</p>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param preferencesLocation the location of the preference file
	 */
	public void setPreferences(String preferencesLocation) {
		this.properties = IOUtil.readPropertiesFile(preferencesLocation);
	}
	

	/**
	 * Set the exclude list location.
	 * 
	 * <p>The exclude list is used to know what bundles should excluded from the xml report generated by the task
	 * execution. Lines starting with '#' are ignored from the excluded elements.</p>
	 * <p>The format of the exclude list file looks like this:</p>
	 * <pre>
	 * # DOC BUNDLES
	 * org.eclipse.jdt.doc.isv
	 * org.eclipse.jdt.doc.user
	 * org.eclipse.pde.doc.user
	 * org.eclipse.platform.doc.isv
	 * org.eclipse.platform.doc.user
	 * # NON-ECLIPSE BUNDLES
	 * com.ibm.icu
	 * com.jcraft.jsch
	 * javax.servlet
	 * javax.servlet.jsp
	 * ...
	 * </pre>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param excludeListLocation the given location for the excluded list file
	 */
	public void setExcludeList(String excludeListLocation) {
		this.excludeListLocation = excludeListLocation;
	}
	
	/**
	 * Set the include list location.
	 * 
	 * <p>The include list is used to know what bundles should included from the xml report generated by the task
	 * execution. Lines starting with '#' are ignored from the included elements.</p>
	 * <p>The format of the include list file looks like this:</p>
	 * <pre>
	 * # DOC BUNDLES
	 * org.eclipse.jdt.doc.isv
	 * org.eclipse.jdt.doc.user
	 * org.eclipse.pde.doc.user
	 * org.eclipse.platform.doc.isv
	 * org.eclipse.platform.doc.user
	 * # NON-ECLIPSE BUNDLES
	 * com.ibm.icu
	 * com.jcraft.jsch
	 * javax.servlet
	 * javax.servlet.jsp
	 * ...
	 * </pre>
	 * <p>The location is set using an absolute path.</p>
	 *
	 * @param includeListLocation the given location for the included list file
	 */
	public void setIncludeList(String includeListLocation) {
		this.includeListLocation = includeListLocation;
	}

	/**
	 * Set whether this task is run in debug mode
	 * 
	 * @param b
	 */
	public void setDebug(boolean b) {
		debug = b;
	}
	
	
	/**
	 * Set the location of a stylesheet file which should be referenced by the reports
	 * @param styleSheet
	 */
	public void setStyleSheet(String styleSheet) {
		this.styleSheet = styleSheet;
	}
	
	/**
	 * Do you wish to generate empty reports for skipping 
	 * a non-api bundle?
	 * 
	 * @param b true if we should skip reports, false if generate all
	 */
	public void setSkipNonApi(boolean b) {
		this.skipNonApi = b;
	}
}
