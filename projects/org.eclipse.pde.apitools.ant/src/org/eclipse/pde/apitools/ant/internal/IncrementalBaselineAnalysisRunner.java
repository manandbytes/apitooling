package org.eclipse.pde.apitools.ant.internal;

import java.util.HashMap;
import java.util.Properties;


import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.apitools.ant.util.BaselineUtils;

public class IncrementalBaselineAnalysisRunner extends AbstractAnalysisRunner {
	public static final String SUMMARY_REPORT_NAME = "ANALYSIS_SUMMARY";
	private static final String REFERENCE_BASE = "referenceBase";
	private static final String CURRENT_BASE = "currentBase";
	
	
	private String referenceBaseline;
	private String currentBaselineLocation;
	private String includeListLocation;
	private String excludeListLocation;
	
	public IncrementalBaselineAnalysisRunner(String referenceBaseline, String currentBaseline,
			String reports, String filters, Properties properties,
			boolean debug) {
		super(reports, filters, properties, debug);
		this.referenceBaseline = referenceBaseline;
		this.currentBaselineLocation = currentBaseline;
	}
	
	public HashMap<String, ApiAnalysisReport> generateReports() throws BuildException {
		IApiBaseline refBase = BaselineUtils.createBaseline("name555", referenceBaseline, null, null);
		IApiBaseline currentBase =  BaselineUtils.createBaseline("name556", referenceBaseline, currentBaselineLocation, null);

		if( debug )
			System.out.println("Loading Baselines...");
		try {
			IApiComponent[] fromUpdates = BaselineUtils.getComponentsFromLocation(refBase, currentBaselineLocation);
			
			IApiComponent[] refIncluded = BaselineUtils.getFilteredElements(
					refBase, fromUpdates);
			IApiComponent[] curIncluded = BaselineUtils.getFilteredElements(
					currentBase, fromUpdates);
			if( debug )
				System.out.println("Finished Loading Baselines.");
			return generateReports(refBase, refIncluded, curIncluded, properties);
		} catch(CoreException ce) {
			ce.printStackTrace();
			throw new BuildException(ce);
		}
	}
}
