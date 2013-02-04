package org.eclipse.pde.apitools.ant.internal;

import java.util.HashMap;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.apitools.ant.util.BaselineUtils;

public class ApiAnalysisRunner extends AbstractAnalysisRunner {
	public static final String SUMMARY_REPORT_NAME = "ANALYSIS_SUMMARY";
	private static final String REFERENCE_BASE = "referenceBase";
	private static final String CURRENT_BASE = "currentBase";
	
	
	private String referenceBaseline;
	private String currentBaselineLocation;
	private String includeListLocation;
	private String excludeListLocation;
	
	public ApiAnalysisRunner(String referenceBaseline, String currentBaseline,
			String reports, String filters, Properties properties,
			boolean skipNonApi, String xslSheet,
			String includeListLocation, String excludeListLocation, boolean debug) {
		super(reports, filters, properties, skipNonApi, xslSheet, debug);
		this.referenceBaseline = referenceBaseline;
		this.currentBaselineLocation = currentBaseline;
		this.includeListLocation = includeListLocation;
		this.excludeListLocation = excludeListLocation;
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
		return generateReports(refBase, refIncluded, curIncluded, properties);
	}
}
