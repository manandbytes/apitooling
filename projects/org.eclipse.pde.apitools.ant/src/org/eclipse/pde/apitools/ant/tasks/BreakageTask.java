package org.eclipse.pde.apitools.ant.tasks;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.eclipse.ant.core.Task;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisReport.AnalysisSkippedReport;
import org.eclipse.pde.apitools.ant.internal.IncrementalBaselineAnalysisRunner;
import org.eclipse.pde.apitools.ant.internal.WrapperReport;
import org.eclipse.pde.apitools.ant.tasks.ApiAnalysisTask.IgnoredReport;
import org.eclipse.pde.apitools.ant.tasks.old.Messages;
import org.eclipse.pde.apitools.ant.util.IOUtil;
import org.eclipse.pde.apitools.ant.util.ReportUtils;
import org.eclipse.pde.apitools.ant.util.ToolingException;

/**
 * This class is meant to take one complete baseline, 
 * and one folder full of replacement plugins. 
 * 
 * It will create a temporary folder of a complete second 
 * target platform, consisting of the original baseline 
 * PLUS the jars in the replacement folder 
 * MINUS any original duplicates.
 * 
 * It also requires a temporary folder it can use.
 * 
 *
 */
public class BreakageTask extends Task {
	private String referenceBaseline;
	private String nightly;
	private String reports;
	private String filters;
	private Properties properties;

	
	protected void checkArgs() throws BuildException {
		if (this.referenceBaseline == null
				|| this.nightly == null 
				|| this.reports == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(
				NLS.bind(Messages.printArguments,
					new String[] {
						this.referenceBaseline,
						this.nightly,
						this.reports,
					})
			);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
	}	

	
	public void execute() throws BuildException {
		checkArgs();
		// Generate the reports
		IncrementalBaselineAnalysisRunner runner = 
				new IncrementalBaselineAnalysisRunner(referenceBaseline, nightly, 
						reports, filters,  properties, true);
		HashMap<String, ApiAnalysisReport> reports = runner.generateReports();
		
		// Iterate and save the reports for each bundle
		Iterator<String> i = reports.keySet().iterator();
		ArrayList<AnalysisSkippedReport> ignored = new ArrayList<AnalysisSkippedReport>();
		int[] breakages = new int[] { 
				IApiProblem.CATEGORY_VERSION,
				IApiProblem.CATEGORY_COMPATIBILITY, 
				IApiProblem.CATEGORY_SINCETAGS,
				IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION
		};
		WrapperReport main = new WrapperReport("breakage", breakages);
		while(i.hasNext()) {
			String id = i.next();
			ApiAnalysisReport report = reports.get(id);
			if( report instanceof AnalysisSkippedReport) {
				ignored.add((AnalysisSkippedReport)report);
				continue;
			}
			main.addChildReport(report);
		}
		
		try {
			File file = new File(this.reports);
			File file2 = new File(file, "breakageReport.xml");
			ReportUtils.saveReport(main, file2);
		} catch(ToolingException ioe) {
			throw new BuildException(ioe);
		}

		
		// Add any skipped / not-analyzed bundle to a file
		try {
			IgnoredReport report = new IgnoredReport(ignored);
			ReportUtils.saveReport(report, new File(this.reports, "apiAnalysisSkippedBundles.xml"));
		} catch(ToolingException ioe) {
			throw new BuildException(ioe);
		}


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
	 * Set the output location where the reports will be generated.
	 * 
	 * <p>Once the task is completed, reports are available in this directory using a structure
	 * similar to the filter root. A sub-folder is created for each component that has problems
	 * to be reported. Each sub-folder contains a file called "report.xml". </p>
	 * 
	 * <p>A special folder called "allNonApiBundles" is also created in this folder that contains a xml file called
	 * "report.xml". This file lists all the bundles that are not using the API Tools nature.</p>
	 * 
	 * @param baselineLocation the given location for the reference baseline to analyze
	 */
	public void setReport(String reportLocation) {
		this.reports = reportLocation;
	}
	
	public void setNightly(String nightly) {
		this.nightly = nightly;
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
}
