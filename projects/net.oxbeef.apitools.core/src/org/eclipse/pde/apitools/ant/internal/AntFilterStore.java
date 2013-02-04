package org.eclipse.pde.apitools.ant.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AntFilterStore implements IApiFilterStore {
	private static final String GLOBAL = "!global!"; //$NON-NLS-1$
	private static int loadIntegerAttribute(Element element, String name) {
		String value = element.getAttribute(name);
		if(value.length() == 0) {
			return -1;
		}
		try {
			int number = Integer.parseInt(value);
			return number;
		}
		catch(NumberFormatException nfe) {}
		return -1;
	}
	private boolean debug;

	private Map fFilterMap;

	public AntFilterStore(boolean debug, String filtersRoot, String componentID) {
		this.initialize(filtersRoot, componentID);
	}

	public void addFiltersFor(IApiProblem[] problems) {
		// do nothing
	}

	public void addFilters(IApiProblemFilter[] filters) {
		// do nothing
	}

	private boolean argumentsEquals(String[] problemMessageArguments,
			String[] filterProblemMessageArguments) {
		// filter problems message arguments are always simple name
		// problem message arguments are fully qualified name outside the IDE
		int length = problemMessageArguments.length;
		if (length == filterProblemMessageArguments.length) {
			for (int i = 0; i < length; i++) {
				String problemMessageArgument = problemMessageArguments[i];
				String filterProblemMessageArgument = filterProblemMessageArguments[i];
				if (problemMessageArgument.equals(filterProblemMessageArgument)) {
					continue;
				}
				int index = problemMessageArgument.lastIndexOf('.');
				int filterProblemIndex = filterProblemMessageArgument.lastIndexOf('.');
				if (index == -1) {
					if (filterProblemIndex == -1) {
						return false; // simple names should match
					}
					if (filterProblemMessageArgument.substring(filterProblemIndex + 1).equals(problemMessageArgument)) {
						continue;
					} else {
						return false;
					}
				} else if (filterProblemIndex != -1) {
					return false; // fully qualified name should match
				} else {
					if (problemMessageArgument.substring(index + 1).equals(filterProblemMessageArgument)) {
						continue;
					} else {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	public void dispose() {
		// do nothing
	}

	public IApiProblemFilter[] getFilters(IResource resource) {
		return null;
	}

	public IResource[] getResources() {
		return null;
	}

	/**
	 * Initialize the filter store using the given component id
	 */
	private void initialize(String filtersRoot, String componentID) {
		if(fFilterMap != null) {
			return;
		}
		if(this.debug) {
			System.out.println("null filter map, creating a new one"); //$NON-NLS-1$
		}
		fFilterMap = new HashMap(5);
		String xml = null;
		InputStream contents = null;
		try {
			File filterFileParent = new File(filtersRoot, componentID);
			if (!filterFileParent.exists()) {
				return;
			}
			contents = new BufferedInputStream(new FileInputStream(new File(filterFileParent, IApiCoreConstants.API_FILTERS_XML_NAME)));
			xml = new String(Util.getInputStreamAsCharArray(contents, -1, IApiCoreConstants.UTF_8));
		}
		catch(IOException ioe) {}
		finally {
			if (contents != null) {
				try {
					contents.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
		if(xml == null) {
			return;
		}
		Element root = null;
		try {
			root = Util.parseDocument(xml);
		}
		catch(CoreException ce) {
			ApiPlugin.log(ce);
		}
		if (root == null) {
			return;
		}
		if (!root.getNodeName().equals(IApiXmlConstants.ELEMENT_COMPONENT)) {
			return;
		}
		String component = root.getAttribute(IApiXmlConstants.ATTR_ID);
		if(component.length() == 0) {
			return;
		}
		String versionValue = root.getAttribute(IApiXmlConstants.ATTR_VERSION);
		int version = 0;
		if(versionValue.length() != 0) {
			try {
				version = Integer.parseInt(versionValue);
			} catch (NumberFormatException e) {
				// ignore
			}
		}
		if (version < 2) {
			// we discard all filters since there is no way to retrieve the type name
			return;
		}
		NodeList resources = root.getElementsByTagName(IApiXmlConstants.ELEMENT_RESOURCE);
		ArrayList newfilters = new ArrayList();
		ArrayList comments = new ArrayList();
		for(int i = 0; i < resources.getLength(); i++) {
			Element element = (Element) resources.item(i);
			String typeName = element.getAttribute(IApiXmlConstants.ATTR_TYPE);
			if (typeName.length() == 0) {
				// if there is no type attribute, an empty string is returned
				typeName = null;
			}
			String path = element.getAttribute(IApiXmlConstants.ATTR_PATH);
			NodeList filters = element.getElementsByTagName(IApiXmlConstants.ELEMENT_FILTER);
			for(int j = 0; j < filters.getLength(); j++) {
				element = (Element) filters.item(j);
				int id = loadIntegerAttribute(element, IApiXmlConstants.ATTR_ID);
				if(id <= 0) {
					continue;
				}
				String[] messageargs = null;
				NodeList elements = element.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENTS);
				if (elements.getLength() != 1) continue;
				Element messageArguments = (Element) elements.item(0);
				NodeList arguments = messageArguments.getElementsByTagName(IApiXmlConstants.ELEMENT_PROBLEM_MESSAGE_ARGUMENT);
				int length = arguments.getLength();
				messageargs = new String[length];
				String comment = element.getAttribute(IApiXmlConstants.ATTR_COMMENT);
				comments.add((comment.length() < 1 ? null : comment));
				for (int k = 0; k < length; k++) {
					Element messageArgument = (Element) arguments.item(k);
					messageargs[k] = messageArgument.getAttribute(IApiXmlConstants.ATTR_VALUE);
				}
				newfilters.add(ApiProblemFactory.newApiProblem(path, typeName, messageargs, null, null, -1, -1, -1, id));
			}
		}
		internalAddFilters(componentID, (IApiProblem[]) newfilters.toArray(new IApiProblem[newfilters.size()]),
				(String[]) comments.toArray(new String[comments.size()]));
		newfilters.clear();
	}

	/**
	 * Internal use method that allows auto-persisting of the filter file to be turned on or off
	 * @param problems the problems to add the the store
	 * @param persist if the filters should be auto-persisted after they are added
	 */
	private void internalAddFilters(String componentID, IApiProblem[] problems, String[] comments) {
		if(problems == null) {
			if(this.debug) {
				System.out.println("null problems array not addding filters"); //$NON-NLS-1$
			}
			return;
		}
		for(int i = 0; i < problems.length; i++) {
			IApiProblem problem = problems[i];
			IApiProblemFilter filter = new ApiProblemFilter(componentID, problem, comments[i]);
			String typeName = problem.getTypeName();
			if (typeName == null) {
				typeName = GLOBAL;
			}
			Set filters = (Set) fFilterMap.get(typeName);
			if(filters == null) {
				filters = new HashSet();
				fFilterMap.put(typeName, filters);
			}
			filters.add(filter);
		}
	}

	public boolean isFiltered(IApiProblem problem) {
		if (this.fFilterMap == null || this.fFilterMap.isEmpty()) return false;
		String typeName = problem.getTypeName();
		if (typeName == null || typeName.length() == 0) {
			typeName = GLOBAL;
		}
		Set filters = (Set) this.fFilterMap.get(typeName);
		if (filters == null) {
			return false;
		}
		for (Iterator iterator = filters.iterator(); iterator.hasNext();) {
			IApiProblemFilter filter = (IApiProblemFilter) iterator.next();
			if (problem.getCategory() == IApiProblem.CATEGORY_USAGE) {
				// write our own matching implementation
				return matchUsageProblem(filter.getUnderlyingProblem(), problem);
			} else if (matchFilters(filter.getUnderlyingProblem(), problem)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchFilters(IApiProblem filterProblem, IApiProblem problem) {
		if (problem.getId() == filterProblem.getId() && argumentsEquals(problem.getMessageArguments(), filterProblem.getMessageArguments())) {
			String typeName = problem.getTypeName();
			String filteredProblemTypeName = filterProblem.getTypeName();
			if (typeName == null) {
				if (filteredProblemTypeName != null) {
					return false;
				}
				return true;
			} else if (filteredProblemTypeName == null) {
				return false;
			}
			return typeName.equals(filteredProblemTypeName);
		}
		return false;
	}

	private boolean matchUsageProblem(IApiProblem filterProblem, IApiProblem problem) {
		if (problem.getId() == filterProblem.getId()) {
			// check arguments
			String problemPath = problem.getResourcePath();
			String filterProblemPath = filterProblem.getResourcePath();
			if (problemPath == null) {
				if (filterProblemPath != null) {
					return false;
				}
			} else if (filterProblemPath == null) {
				return false;
			} else if (!new Path(problemPath).equals(new Path(filterProblemPath))) {
				return false;
			}
			String problemTypeName = problem.getTypeName();
			String filterProblemTypeName = filterProblem.getTypeName();
			if (problemTypeName == null) {
				if (filterProblemTypeName != null) {
					return false;
				}
			} else if (filterProblemTypeName == null) {
				return false;
			} else if (!problemTypeName.equals(filterProblemTypeName)) {
				return false;
			}
			return argumentsEquals(problem.getMessageArguments(), filterProblem.getMessageArguments());
		}
		return false;
	}
	public boolean removeFilters(IApiProblemFilter[] filters) {
		return false;
	}

}
