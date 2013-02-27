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
package org.eclipse.pde.apitools.ant.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport;
import org.eclipse.pde.apitools.ant.internal.RootReport;
import org.eclipse.pde.apitools.ant.tasks.Messages;

public class ReportUtils {
	
	/**
	 * Saves the report with the given name in the report location.  If a componentID is provided, a child
	 * directory using that name will be created to put the report in.
	 * @param componentID Name of the component to create a child directory for or <code>null<code> to put the report in the XML root
	 * @param contents contents to output to the report
	 * @param reportname name of the file to output to
	 */
	public static void saveReport(String componentID, String contents, String reportname, String reportLocation) throws ToolingException {
		File dir = new File(reportLocation);
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				throw new ToolingException(NLS.bind(Messages.errorCreatingReportDirectory, reportLocation));
			}
		}
		// If the caller has provided a component id, create a child directory
		if (componentID != null){
			dir = new File(dir, componentID);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					throw new ToolingException(NLS.bind(Messages.errorCreatingReportDirectory, dir));
				}
			}
		}
		File reportFile = new File(dir, reportname);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(reportFile));
			writer.write(contents);
			writer.flush();
		} catch (IOException e) {
			throw new ToolingException(e.getMessage(), e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	public static void saveReport(RootReport report, File location ) throws ToolingException {
		XMLMemento reportMemento = report.generateMemento();
		saveMemento(reportMemento, location);
	} 

	public static void saveAnalysisReport(ApiAnalysisReport report, File location, int[] categories ) throws ToolingException {
		XMLMemento reportMemento = report.generateMemento(categories);
		saveMemento(reportMemento, location);
	} 

	public static void saveMemento(XMLMemento reportMemento, File location ) throws ToolingException {
		if( !location.getParentFile().exists()) {
			if( !location.getParentFile().mkdirs() ) {
				throw new ToolingException(NLS.bind(Messages.errorCreatingParentReportFile, location.getAbsolutePath()));
			}
		}
		try {
			reportMemento.saveToFile(location.getAbsolutePath());
		} catch(IOException ioe) {
			throw new ToolingException("Cannot save report to file " + location.getAbsolutePath(), ioe);
		}
	}

}
