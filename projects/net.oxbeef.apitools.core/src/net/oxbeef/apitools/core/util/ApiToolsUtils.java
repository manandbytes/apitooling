package net.oxbeef.apitools.core.util;

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

import net.oxbeef.apitools.core.internal.AntFilterStore;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

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
	
}
