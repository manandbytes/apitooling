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
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisRunner;
import org.eclipse.pde.apitools.ant.internal.WrapperReport;
import org.eclipse.pde.apitools.ant.tasks.ApiAnalysisTask.IgnoredReport;
import org.eclipse.pde.apitools.ant.tasks.old.Messages;
import org.eclipse.pde.apitools.ant.util.IOUtil;
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
public class BreakageTask extends Task {
	private String referenceBaseline;
	private String profileBaseline;
	private String reports;
	private String includeListLocation;
	private String excludeListLocation;
	private String filters;
	private Properties properties;
	private String styleSheet;
	private boolean skipNonApi = false;
	private boolean debug;
	
	protected void checkArgs() throws BuildException {
		if (this.referenceBaseline == null
				|| this.profileBaseline == null 
				|| this.reports == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(
				NLS.bind(Messages.printArguments,
					new String[] {
						this.referenceBaseline,
						this.profileBaseline,
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
		if( debug ) {
			System.out.println("Running API Breakage analysis");
		}
		
		ApiAnalysisRunner runner = 
				new ApiAnalysisRunner(referenceBaseline, profileBaseline, 
						reports, filters,  properties, 
						skipNonApi, styleSheet,
						includeListLocation, excludeListLocation, debug);
		HashMap<String, ApiAnalysisReport> reports = runner.generateReports();
		
		if( debug ) {
			System.out.println("StyleSheet is " + styleSheet);
			System.out.println("Generating API Breakage report");
		}
		
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
		main.setStyleSheetPath(styleSheet);
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
			ReportUtils.saveReport(report, new File(this.reports, "breakageSkippedBundles.xml"));
		} catch(ToolingException ioe) {
			throw new BuildException(ioe);
		}

		if( debug ) {
			System.out.println("API Breakage Analysis Complete");
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
