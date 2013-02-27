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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiScope;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiScope;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.apitools.ant.internal.AntFilterStore;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class ApiToolsUtils {
	/**
	 * By default, we return a warning severity.
	 * @param problem the given problem
	 * @return the problem's severity
	 */
	public static int getSeverity(Properties props, IApiProblem problem) {
		if (props!= null) {
			String key = ApiProblemFactory.getProblemSeverityId(problem);
			if (key != null) {
				String value = props.getProperty(key, null);
				if (value != null) {
					if (ApiPlugin.VALUE_ERROR.equals(value)) {
						return ApiPlugin.SEVERITY_ERROR;
					}
					if (ApiPlugin.VALUE_DISABLED.equals(value)) {
						return ApiPlugin.SEVERITY_IGNORE;
					}
					if (ApiPlugin.VALUE_IGNORE.equals(value)) {
						return ApiPlugin.SEVERITY_IGNORE;
					}
				}
			}
		}
		return ApiPlugin.SEVERITY_WARNING;
	}
	
	public static  IApiProblem[] removeDuplicates(IApiProblem[] problems) {
		int length = problems.length;
		if (length <= 1) return problems;
		Set uniqueProblems = new HashSet(length);
		List allProblems = null;
		for (int i = 0; i < length; i++) {
			IApiProblem apiProblem = problems[i];
			String message = apiProblem.getMessage();
			if (!uniqueProblems.contains(message)) {
				if (allProblems == null) {
					allProblems = new ArrayList(length);
				}
				uniqueProblems.add(message);
				allProblems.add(apiProblem);
			}
		}
		return (IApiProblem[]) allProblems.toArray(new IApiProblem[allProblems.size()]);
	}

	public static IApiFilterStore getFilterStore(String filters, String name) {
		if (filters == null) return null;
		return new AntFilterStore(false, filters, name);
	}

	
	public static  IApiComponent[] removeFromList(IApiComponent[] list, IApiComponent[] toRemove) {
		ArrayList<IApiComponent> list2 = new ArrayList<IApiComponent>();
		list2.addAll(Arrays.asList(list));
		list2.removeAll(Arrays.asList(toRemove));
		return (IApiComponent[]) list2.toArray(new IApiComponent[list2.size()]);
	}
	
	public static  IApiComponent[] getRemovedBundles(IApiComponent[] base, IApiComponent[] current) {
		HashMap<String, IApiComponent> map = new HashMap<String, IApiComponent>();
		for( int i = 0; i < base.length; i++ ) {
			map.put(base[i].getSymbolicName(), base[i]);
		}
		for( int i = 0; i < current.length; i++ ) {
			map.remove(current[i].getSymbolicName());
		}
		Collection<IApiComponent> c = map.values();
		return (IApiComponent[]) c.toArray(new IApiComponent[c.size()]);
	}
	
	public static IApiProblem createRemovedComponentProblem(String id) {
		IApiProblem problem = ApiProblemFactory.newApiProblem(id,
				null, new String[] { id },
				new String[] {
					IApiMarkerConstants.MARKER_ATTR_HANDLE_ID,
					IApiMarkerConstants.API_MARKER_ATTR_ID
				},
				new Object[] {
					id,
					new Integer(IApiMarkerConstants.COMPATIBILITY_MARKER_ID),
				},
				0, -1, -1,
				IApiProblem.CATEGORY_COMPATIBILITY,
				IDelta.API_BASELINE_ELEMENT_TYPE,
				IDelta.REMOVED,
				IDelta.API_COMPONENT);
		return problem;
	}

	public static IApiProblem[] sortProblems(IApiProblem[] original) {
		ArrayList<IApiProblem> ret = new ArrayList<IApiProblem>();
		ret.addAll(Arrays.asList(original));
		Collections.sort(ret, new Comparator() {
			public int compare(Object o1, Object o2) {
				IApiProblem p1 = (IApiProblem) o1;
				IApiProblem p2 = (IApiProblem) o2;
				return p1.getTypeName().compareTo(p2.getTypeName());
			}
		});
		return (IApiProblem[]) ret.toArray(new IApiProblem[ret.size()]);
	}
	
	public static boolean containsAPIToolsNature(String pluginXMLContents) {
		SAXParserFactory factory = null;
		try {
			factory = SAXParserFactory.newInstance();
		} catch (FactoryConfigurationError e) {
			return false;
		}
		SAXParser saxParser = null;
		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			// ignore
		} catch (SAXException e) {
			// ignore
		}

		if (saxParser == null) {
			return false;
		}

		// Parse
		InputSource inputSource = new InputSource(new BufferedReader(new StringReader(pluginXMLContents)));
		try {
			APIToolsNatureDefaultHandler defaultHandler = new APIToolsNatureDefaultHandler();
			saxParser.parse(inputSource, defaultHandler);
			return defaultHandler.isAPIToolsNature();
		} catch (SAXException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
		return false;
	}

	static class APIToolsNatureDefaultHandler extends DefaultHandler {
		private static final String NATURE_ELEMENT_NAME = "nature"; //$NON-NLS-1$
		boolean isAPIToolsNature = false;
		boolean insideNature = false;
		StringBuffer buffer;
		public void error(SAXParseException e) throws SAXException {
			e.printStackTrace();
		}
		public void startElement(String uri, String localName, String name, Attributes attributes)
				throws SAXException {
			if (this.isAPIToolsNature) return;
			this.insideNature = NATURE_ELEMENT_NAME.equals(name);
			if (this.insideNature) {
				this.buffer = new StringBuffer();
			}
		}
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (this.insideNature) {
				this.buffer.append(ch, start, length);
			}
		}
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			if (this.insideNature) {
				// check the contents of the characters
				String natureName = String.valueOf(this.buffer).trim();
				this.isAPIToolsNature = ApiPlugin.NATURE_ID.equals(natureName);
			}
			this.insideNature = false;
		}
		public boolean isAPIToolsNature() {
			return this.isAPIToolsNature;
		}
	}
	
	public static IApiScope getResolvableScope(IApiBaseline currentBaseline, boolean debug) {
		IApiComponent[] apiComponents = currentBaseline.getApiComponents();
		ApiScope scope = new ApiScope();
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			IApiComponent apiComponent = apiComponents[i];
			try {
				ResolverError[] errors = apiComponent.getErrors();
				if (errors != null) {
					if (debug) {
						System.out.println("Errors for component : " + apiComponent.getSymbolicName()); //$NON-NLS-1$
						for (int j = 0, max2 = errors.length; j < max2; j++) {
							System.out.println(errors[j]);
						}
					}
					continue;
				}
				scope.addElement(apiComponent);
			} catch (CoreException e) {
				// ignore
			}
		}
		return scope;

	}
}
