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
package org.eclipse.pde.apitools.ant.tasks.old;

import java.io.File;
import java.util.Set;

import org.apache.tools.ant.Task;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.apitools.ant.util.BaselineUtils;
import org.eclipse.pde.apitools.ant.util.ReportUtils;
import org.eclipse.pde.apitools.ant.util.ToolingException;

/**
 * Common code for API Tools Ant tasks.
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be sub-classed by clients.
 */
public abstract class CommonUtilsTask extends Task {
	private static final String ECLIPSE_FOLDER_NAME = "eclipse"; //$NON-NLS-1$

	protected static final String CURRENT = "currentBaseline"; //$NON-NLS-1$
	protected static final String CURRENT_BASELINE_NAME = "current_baseline"; //$NON-NLS-1$
	protected static final String REFERENCE = "referenceBaseline"; //$NON-NLS-1$
	protected static final String REFERENCE_BASELINE_NAME = "reference_baseline"; //$NON-NLS-1$

	protected boolean debug;
	protected String eeFileLocation;
	protected String currentBaselineLocation;
	protected String referenceBaselineLocation;
	protected String excludeListLocation;
	protected String includeListLocation;
	
	protected String reportLocation;
	
	/**
	 * Creates a baseline with the given name and EE file location in the given directory.  The installLocation
	 * will be searched for bundles to add as API components.
	 * 
	 * @param baselineName Name to use for the new baseline
	 * @param installLocation Location of an installation or directory of bundles to add as API components
	 * @param eeFileLocation execution environment location or <code>null</code> to have the EE determined from API components
	 * @return a new {@link IApiBaseline}
	 */
	protected IApiBaseline createBaseline(String baselineName, String installLocation, String eeFileLocation) {
		return BaselineUtils.createBaseline(baselineName, installLocation, eeFileLocation);
	}
	
	/**
	 * Deletes an {@link IApiBaseline} from the given folder
	 * @param referenceLocation
	 * @param folder
	 */
	protected void deleteBaseline(String referenceLocation, File folder) {
		BaselineUtils.deleteBaseline(referenceLocation, folder);
	}
	
	/**
	 * Extract extracts the SDK from the given location to the given directory name
	 * @param installDirName
	 * @param location
	 * @return the {@link File} handle to the extracted SDK
	 */
	protected File extractSDK(String installDirName, String location) {
		return BaselineUtils.extractSDK(installDirName, location);
	}
	
	/**
	 * Initializes the include/exclude list from the given file location, and returns
	 * a {@link Set} of project names that should be include/excluded.
	 * 
	 * @param excludeListLocation
	 * @return the set of project names to be excluded
	 */
	protected static FilteredElements initializeFilteredElements(String filterListLocation, IApiBaseline baseline, boolean debug) {
		return Util.initializeRegexFilterList(filterListLocation, baseline, debug);
	}

	/**
	 * Saves the report with the given name in the report location.  If a componentID is provided, a child
	 * directory using that name will be created to put the report in.
	 * @param componentID Name of the component to create a child directory for or <code>null<code> to put the report in the XML root
	 * @param contents contents to output to the report
	 * @param reportname name of the file to output to
	 */
	protected void saveReport(String componentID, String contents, String reportname) {
		try {
			ReportUtils.saveReport(componentID, contents, reportname, reportname);
		} catch(ToolingException te) {
			ApiPlugin.log(te);
		}
	}
	
	/**
	 * Parses and returns patterns as an array of Strings or <code>null</code> if none.
	 * 
	 * @param patterns comma separated list or <code>null</code>
	 * @return individual patterns or <code>null</code>
	 */
	protected String[] parsePatterns(String patterns) {
		return org.eclipse.pde.apitools.ant.util.StringUtils.parsePatterns(patterns);
	}

	public static String convertToHtml(String s) {
		return org.eclipse.pde.apitools.ant.util.StringUtils.convertToHtml(s);
	}
}
