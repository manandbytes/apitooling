package org.eclipse.pde.apitools.ant.tasks.slim;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.apitools.ant.internal.ApiAnalysisRunner;
import org.eclipse.pde.apitools.ant.internal.RootReport;
import org.eclipse.pde.apitools.ant.tasks.Messages;
import org.eclipse.pde.apitools.ant.util.ReportUtils;
import org.eclipse.pde.apitools.ant.util.ToolingException;

public abstract class AbstractDeltaComparisonTask extends AbstractComparisonTask {

	public abstract String getReportFileName();
	
	public abstract IDelta createDelta(IApiBaseline referenceBaseline, IApiBaseline profileBaseline) throws CoreException;

	public abstract RootReport createReport(IDelta delta, IApiComponent[] components);
	
	public void execute() throws BuildException {
		checkArgs();
		if( debug ) {
			printArgs();
			System.out.println("\nRunning " + getTaskName() + " Analysis");
		}

		// Generate the reports
		ApiAnalysisRunner runner = createAnalysisRunner();
		runner.createBaselines();
		runner.createInclusionArrays();

		// run the comparison and get the baselines for the references
		IApiBaseline referenceBaseline = runner.getReferenceBaseline();
		IApiBaseline currentBaseline = runner.getProfileBaseline();
		
		/*
		 * Calculate our delta
		 */
		long time = System.currentTimeMillis();
		if (this.debug) {
			System.out.println("Beginning Delta Calculation");
		}
		IDelta delta = null;
		try {
			delta = createDelta(referenceBaseline, currentBaseline);
		} catch(CoreException ce) {
		}

		if (this.debug) {
			System.out.println("Delta Calculation Complete : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		runner.disposeBaselines();
		
		/* Verify delta */
		if (delta == null) {
			// an error occurred during the comparison
			throw new BuildException(Messages.errorInComparison);
		}
		
		/* Generate the report now */
		if (delta != ApiComparator.NO_DELTA) {
			// dump the report in the appropriate folder
			File outputFile = new File(this.reports, getReportFileName());
			RootReport report = createReport(delta, runner.getProfileComponents());
			report.setStyleSheetPath(styleSheet);
			try {
				ReportUtils.saveReport(report, outputFile);
			} catch(ToolingException ioe) {
				throw new BuildException(ioe);
			}
			
			if (this.debug) {
				System.out.println("Report generation : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}
}
