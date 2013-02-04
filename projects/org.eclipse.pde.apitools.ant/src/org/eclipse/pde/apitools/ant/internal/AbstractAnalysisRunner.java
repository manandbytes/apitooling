package org.eclipse.pde.apitools.ant.internal;

import java.util.HashMap;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.BuildContext;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.apitools.ant.util.ApiToolsUtils;

public abstract class AbstractAnalysisRunner {
	protected String reports;
	protected String filters;
	protected Properties properties;
	protected boolean debug;
	protected boolean skipNonApi;
	protected String xslLoc;

	public AbstractAnalysisRunner(String reports, String filters, Properties properties, 
			boolean skipNonApi, String xslLoc, boolean debug) {
		this.reports = reports;
		this.filters = filters;
		this.properties = properties;
		this.debug = debug;
	}
	
	public abstract HashMap<String, ApiAnalysisReport> generateReports() throws BuildException;

	
	public HashMap<String, ApiAnalysisReport> generateReports(
			IApiBaseline refBase,
			IApiComponent[] refIncluded, IApiComponent[] curIncluded,
			Properties properties) throws BuildException {
		
		// Get the removed items between ref_baseline to current baseline
		IApiComponent[] removedBundles = ApiToolsUtils.getRemovedBundles(refIncluded, curIncluded);
		
		if( debug )
			System.out.println("Calculating Missing Bundles...");

		// Create our report mapping
		HashMap<String, ApiAnalysisReport> reports = new HashMap<String, ApiAnalysisReport>();
		for( int i = 0; i < removedBundles.length; i++ ) {
			IApiProblem problem = ApiToolsUtils.createRemovedComponentProblem(removedBundles[i].getSymbolicName());
			ApiAnalysisReport report = new ApiAnalysisReport(removedBundles[i].getSymbolicName(), 
					new IApiProblem[]{ problem }, properties, xslLoc);

			reports.put(removedBundles[i].getSymbolicName(), report);
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
			
			if (skipNonApi && !Util.isApiToolsComponent(apiComponent)) {
				reports.put(name, new ApiAnalysisReport.AnalysisSkippedReport(
						name, "nonAPI"));
				continue;
			}
			
			BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
			try {
				analyzer.analyzeComponent(null, ApiToolsUtils.getFilterStore(filters, name), properties, 
						refBase, apiComponent, new BuildContext(), new NullProgressMonitor());
				IApiProblem[] problems = analyzer.getProblems();
				// remove duplicates
				problems = ApiToolsUtils.removeDuplicates(problems);
				
				// Show even if empty. Otherwise users get confused
				reports.put(name, new ApiAnalysisReport(
						name, problems, properties));
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
