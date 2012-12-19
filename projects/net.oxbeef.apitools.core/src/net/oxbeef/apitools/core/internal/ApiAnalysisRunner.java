package net.oxbeef.apitools.core.internal;

import java.util.HashMap;
import java.util.Properties;

import net.oxbeef.apitools.core.util.ApiToolsUtils;
import net.oxbeef.apitools.core.util.BaselineUtils;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.BuildContext;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;

public class ApiAnalysisRunner {
	public static final String SUMMARY_REPORT_NAME = "ANALYSIS_SUMMARY";
	private static final String REFERENCE_BASE = "referenceBase";
	private static final String CURRENT_BASE = "currentBase";
	
	
	private String referenceBaseline;
	private String currentBaselineLocation;
	private String reports;
	private String filters;
	private Properties properties;
	private String includeListLocation;
	private String excludeListLocation;
	private boolean debug;
	
	public ApiAnalysisRunner(String referenceBaseline, String currentBaseline,
			String reports, String filters, Properties properties,
			String includeListLocation, String excludeListLocation, boolean debug) {
		this.referenceBaseline = referenceBaseline;
		this.currentBaselineLocation = currentBaseline;
		this.reports = reports;
		this.filters = filters;
		this.properties = properties;
		this.includeListLocation = includeListLocation;
		this.excludeListLocation = excludeListLocation;
		this.debug = debug;
	}
	
	public HashMap<String, ApiAnalysisReport> generateReports() throws BuildException {
		
		// Create two baselines
		IApiBaseline refBase = BaselineUtils.createBaseline(
				REFERENCE_BASE, referenceBaseline, null);
		IApiBaseline currentBase = BaselineUtils.createBaseline(
				CURRENT_BASE, currentBaselineLocation, null);
		
		if( debug )
			System.out.println("Loading Baselines...");
		// Get all included elements AFTER the filters are applied
		IApiComponent[] refIncluded = BaselineUtils.getFilteredElements(
				refBase, includeListLocation, excludeListLocation);
		IApiComponent[] curIncluded = BaselineUtils.getFilteredElements(
				currentBase, includeListLocation, excludeListLocation);
		if( debug )
			System.out.println("Finished Loading Baselines.");

		// Get the removed items between ref_baseline to current baseline
		IApiComponent[] removedBundles = ApiToolsUtils.getRemovedBundles(refIncluded, curIncluded);
		
		if( debug )
			System.out.println("Calculating Missing Bundles...");

		// Create our report mapping
		HashMap<String, ApiAnalysisReport> reports = new HashMap<String, ApiAnalysisReport>();
		for( int i = 0; i < removedBundles.length; i++ ) {
			IApiProblem problem = ApiToolsUtils.createRemovedComponentProblem(removedBundles[i].getSymbolicName());
			reports.put(removedBundles[i].getSymbolicName(), 
					new ApiAnalysisReport(removedBundles[i].getSymbolicName(), 
							new IApiProblem[]{ problem }, properties));
		}
		if( debug )
			System.out.println("Finished Calculating Missing Bundles.\n\n");

		
		
		IApiComponent[] bundlesToCompare = curIncluded;
		for( int i = 0; i < bundlesToCompare.length; i++ ) {
			IApiComponent apiComponent = bundlesToCompare[i];
			String name = apiComponent.getSymbolicName();
			if( debug )
				System.out.println("Analyzing Bundle " + (i+1) + " of " + bundlesToCompare.length + ": " + name);
			
			if (apiComponent.isSystemComponent()) {
				reports.put(name, new ApiAnalysisReport.AnalysisSkippedReport(
						name, "systemComponent"));
				continue;
			}
			
//			if (!Util.isApiToolsComponent(apiComponent)) {
//				reports.put(name, new ApiAnalysisReport.AnalysisSkippedReport(
//						name, "nonAPI"));
//				continue;
//			}
			
			BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
			try {
				analyzer.analyzeComponent(null, ApiToolsUtils.getFilterStore(filters, name), this.properties, 
						refBase, apiComponent, new BuildContext(), new NullProgressMonitor());
				IApiProblem[] problems = analyzer.getProblems();
				// remove duplicates
				problems = ApiToolsUtils.removeDuplicates(problems);
				
				// Show even if empty. Otherwise users get confused
				reports.put(name, new ApiAnalysisReport(
						name, problems, this.properties));
			} catch(RuntimeException e) {
				ApiPlugin.log(e);
				throw e;
			} finally {
				analyzer.dispose();
			}
		}
		return reports;
	}
}
