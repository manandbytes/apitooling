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
package org.eclipse.pde.apitools.ant.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;


import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.apitools.ant.util.ApiToolsUtils;
import org.eclipse.pde.apitools.ant.util.IMemento;
import org.eclipse.pde.apitools.ant.util.IStyleSheetProvider;
import org.eclipse.pde.apitools.ant.util.XMLMemento;
import org.w3c.dom.Element;

public class ApiAnalysisReport extends RootReport implements IStyleSheetProvider {
	private static final int CATEGORIES[] = new int[] {
		IApiProblem.CATEGORY_VERSION,
		IApiProblem.CATEGORY_COMPATIBILITY, 
		IApiProblem.CATEGORY_SINCETAGS,
		IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION,
		IApiProblem.CATEGORY_USAGE, 
		IApiProblem.CATEGORY_API_BASELINE,
		IApiProblem.CATEGORY_API_USE_SCAN_PROBLEM,
		IApiProblem.CATEGORY_FATAL_PROBLEM,
	};
	private static final HashMap<Integer, String> categoryToName = new HashMap<Integer, String>();
	static {
		categoryToName.put(CATEGORIES[0], "bundleVersion");
		categoryToName.put(CATEGORIES[1], "compatibility");
		categoryToName.put(CATEGORIES[2], "sinceTags");
		categoryToName.put(CATEGORIES[3], "resolution");
		categoryToName.put(CATEGORIES[4], "usage");
		categoryToName.put(CATEGORIES[5], "baseline");
		categoryToName.put(CATEGORIES[6], "useScan");
		categoryToName.put(CATEGORIES[7], "fatal");
	}

	protected String id;
	protected IApiProblem[] problems;
	protected Properties preferences;
	protected String styleSheetLoc;
	public ApiAnalysisReport(String componentId, IApiProblem[] problems, Properties problemPreferences) {
		this(componentId, problems, problemPreferences, null);
	}
	public ApiAnalysisReport(String componentId, IApiProblem[] problems, Properties problemPreferences, String styleSheetLoc) {
		super("report");
		this.id = componentId;
		this.problems = problems;
		this.preferences = problemPreferences;
		this.styleSheetLoc = (styleSheetLoc == null ? "analysis.xsl" : styleSheetLoc);
	}
	
	private int[] mementoProblemTypes = null;
	public XMLMemento generateMemento(int[] problemTypes) {
		mementoProblemTypes = problemTypes;
		return super.generateMemento();
	}
	
	protected void fillMemento(IMemento parentContext) {
		fillMemento(parentContext, mementoProblemTypes == null ? CATEGORIES : mementoProblemTypes);
	}
	protected void fillMemento(IMemento parentContext, int[] problemTypes) {
		parentContext.putString(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_REPORT_CURRENT_VERSION);
		parentContext.putString(IApiXmlConstants.ATTR_COMPONENT_ID, id);
		
		for( int i = 0; i < problemTypes.length; i++ ) {
			addCategory(parentContext, problemTypes[i], categoryToName.get(problemTypes[i]));
		}
	}
	
	protected void addCategory(IMemento parentContext, int type, String categoryValue) {
		IMemento child = parentContext.createChild(IApiXmlConstants.ATTR_CATEGORY);
		child.putString(IApiXmlConstants.ATTR_KEY, Integer.toString(type));
		child.putString(IApiXmlConstants.ATTR_VALUE, categoryValue);
		insertProblems(child, getProblemsOfType(type));
	}
	
	protected void insertProblems(IMemento parent, IApiProblem[] problems) {
		IApiProblem[] sorted = ApiToolsUtils.sortProblems(problems);
		IMemento problemsElement = parent.createChild(IApiXmlConstants.ELEMENT_API_PROBLEMS);
		for( int i = 0; i < sorted.length; i++ ) {
			addSingleProblem(problemsElement, sorted[i]);
		}
	}
	protected void addSingleProblem(IMemento problems, IApiProblem problem) {
		IMemento problemEl = problems.createChild(IApiXmlConstants.ELEMENT_API_PROBLEM);
		int severity = ApiToolsUtils.getSeverity(preferences, problem);

		problemEl.putString(IApiXmlConstants.ATTR_TYPE_NAME, String.valueOf(problem.getTypeName()));
		problemEl.putString(IApiXmlConstants.ATTR_ID, Integer.toString(problem.getId()));
		problemEl.putString(IApiXmlConstants.ATTR_LINE_NUMBER, Integer.toString(problem.getLineNumber()));
		problemEl.putString(IApiXmlConstants.ATTR_CHAR_START, Integer.toString(problem.getCharStart()));
		problemEl.putString(IApiXmlConstants.ATTR_CHAR_END, Integer.toString(problem.getCharEnd()));
		problemEl.putString(IApiXmlConstants.ATTR_ELEMENT_KIND, Integer.toString(problem.getElementKind()));
		problemEl.putString(IApiXmlConstants.ATTR_SEVERITY, Integer.toString(severity));
		problemEl.putString(IApiXmlConstants.ATTR_KIND, Integer.toString(problem.getKind()));
		problemEl.putString(IApiXmlConstants.ATTR_FLAGS, Integer.toString(problem.getFlags()));
		problemEl.putString(IApiXmlConstants.ATTR_MESSAGE, problem.getMessage());
		
		// add the extra marker attributes
		String[] extraMarkerAttributeIds = problem.getExtraMarkerAttributeIds();
		if (extraMarkerAttributeIds != null && extraMarkerAttributeIds.length != 0) {
			int length = extraMarkerAttributeIds.length;
			Object[] extraMarkerAttributeValues = problem.getExtraMarkerAttributeValues();
			IMemento extraArgumentsElement = problemEl.createChild(IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENTS);
			for (int j = 0; j < length; j++) {
				IMemento extraArgumentElement = extraArgumentsElement.createChild(IApiXmlConstants.ELEMENT_PROBLEM_EXTRA_ARGUMENT);
				extraArgumentElement.putString(IApiXmlConstants.ATTR_ID, extraMarkerAttributeIds[j]);
				extraArgumentElement.putString(IApiXmlConstants.ATTR_VALUE, String.valueOf(extraMarkerAttributeValues[j]));
			}
		}
		String[] messageArguments = problem.getMessageArguments();
		if (messageArguments != null && messageArguments.length != 0) {
			int length = messageArguments.length;
			IMemento messageArgumentsElement = problemEl.createChild(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
			for (int j = 0; j < length; j++) {
				IMemento messageArgumentElement = messageArgumentsElement.createChild(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
				messageArgumentElement.putString(IApiXmlConstants.ATTR_VALUE, String.valueOf(messageArguments[j]));
			}
		}
	}
	
	public int countProblems(int[] types) {
		int count = 0;
		for( int i = 0; i < types.length; i++ ) {
			count += getProblemsOfType(types[i]).length;
		}
		return count;
	}
	
	protected IApiProblem[] getProblemsOfType(int type) {
		if( problems == null )
			return new IApiProblem[]{};
		ArrayList<IApiProblem> ret = new ArrayList<IApiProblem>();
		for( int i = 0; i < problems.length; i++ ) {
			if( problems[i].getCategory() == type)
				ret.add(problems[i]);
		}
		return (IApiProblem[]) ret.toArray(new IApiProblem[ret.size()]);
	}
	
	public static class AnalysisSkippedReport extends ApiAnalysisReport {
		private String cause;
		public AnalysisSkippedReport(String componentId, String cause) {
			super(componentId, null, null);
			this.cause = cause;
		}
		protected void fillMemento(IMemento parentContext) {
			parentContext.putString(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_REPORT_CURRENT_VERSION);
			parentContext.putString(IApiXmlConstants.ATTR_COMPONENT_ID, id);
			IMemento mem = parentContext.createChild("analysisSkipped");
			mem.putString("cause", cause);
		}
		public String getId() {
			return id;
		}
		public String getCause() {
			return cause;
		}
	}

	@Override
	public String getStyleSheetPath() {
		return styleSheetLoc;
	}
}
