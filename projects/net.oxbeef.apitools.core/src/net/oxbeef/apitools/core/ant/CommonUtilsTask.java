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
package net.oxbeef.apitools.core.ant;

import java.io.File;
import java.util.Set;

import net.oxbeef.apitools.core.util.BaselineUtils;
import net.oxbeef.apitools.core.util.StringUtils;

import org.eclipse.ant.core.Task;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.Util;

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
	
	protected String[] parsePatterns(String patterns) {
		return StringUtils.parsePatterns(patterns);
	}

	public static String convertToHtml(String s) {
		return StringUtils.convertToHtml(s);
	}
}
