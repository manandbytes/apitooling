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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.TarException;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.apitools.ant.tasks.old.Messages;

public class BaselineUtils {
	private static final String ECLIPSE_FOLDER_NAME = "eclipse"; //$NON-NLS-1$

	
	/**
	 * Creates a baseline with the given name and EE file location in the given directory.  The installLocation
	 * will be searched for bundles to add as API components.
	 * 
	 * @param baselineName Name to use for the new baseline
	 * @param installLocation Location of an installation or directory of bundles to add as API components
	 * @param eeFileLocation execution environment location or <code>null</code> to have the EE determined from API components
	 * @return a new {@link IApiBaseline}
	 */
	public static IApiBaseline createBaseline(String baselineName, String installLocation, String eeFileLocation) {
		return createBaseline(baselineName, installLocation, null, eeFileLocation);
	}
	
	public static IApiBaseline createBaseline(String baselineName, String installLocation, String updates, String eeFileLocation) {

		try {
			IApiBaseline baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = ApiModelFactory.newApiBaseline(baselineName);
			} else if (eeFileLocation != null) {
				baseline = ApiModelFactory.newApiBaseline(baselineName, new File(eeFileLocation));
			} else {
				baseline = ApiModelFactory.newApiBaseline(baselineName, Util.getEEDescriptionFile());
			}
			
			IApiComponent[] components = null;
			if(updates == null) {
				components = ApiModelFactory.addComponents(baseline, installLocation, null);
			} else {
				components = addComponents(baseline, installLocation, updates, null);
			}
			if (components.length == 0){			
				throw new BuildException(NLS.bind(Messages.directoryIsEmpty, installLocation));
			}
			return baseline;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static IApiBaseline createBaseline(String baselineName, File[] files) {

		try {
			IApiBaseline baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = ApiModelFactory.newApiBaseline(baselineName);
			} else {
				baseline = ApiModelFactory.newApiBaseline(baselineName, Util.getEEDescriptionFile());
			}
			
			IApiComponent[] components = null;
			components = addComponents(baseline, files, null);
			if (components.length == 0){			
				throw new BuildException("No bundles found");
			}
			return baseline;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	
	public static IApiBaseline createMaintenanceBaseline(String baselineName, String installLocation, String updates) {
		return createBaseline(baselineName, installLocation, updates, null);
	}
	
	
	public static IApiComponent[] addComponents(IApiBaseline baseline, File[] files, IProgressMonitor monitor) throws CoreException {
		IApiComponent[] result = getApiComponentsFromFiles(baseline, files, false);
		if(result != null && result.length > 0) {
			baseline.addApiComponents(result);
			return result;
		}
		return ApiModelFactory.NO_COMPONENTS;
	}
	
	private static final String CVS_FOLDER_NAME = "CVS"; //$NON-NLS-1$
	static class CVSNameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return !name.equalsIgnoreCase(CVS_FOLDER_NAME);
		}
	}

	/**
	 * Collects API components for the bundles part of the specified installation and adds them to the baseline. The
	 * components that were added to the baseline are returned.
	 * 
	 * This is only intended to be called from ant tasks, where
	 * osgi is NOT running. 
	 * 
	 * @param baseline The baseline to add the components to
	 * @param installLocation location of an installation that components are collected from
	 * @param updatesLocation location of an installation of newer bundles to override those from installLocation
	 * @param monitor progress monitor or <code>null</code>, the caller is responsible for calling {@link IProgressMonitor#done()} 
	 * @return List of API components that were added to the baseline, possibly empty, never <code>null</code>
	 * @throws CoreException If problems occur getting components or modifying the baseline
	 */
	public static IApiComponent[] addComponents(IApiBaseline baseline, String installLocation, 
			String updatesLocation, IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, "Configuring Baseline", 50);
		ArrayList<IApiComponent> all = new ArrayList<IApiComponent>();
		try {
			// Load our two baselines
			IApiComponent[] fromBaseline = getComponentsFromLocation(baseline, installLocation);
			IApiComponent[] fromUpdates = getComponentsFromLocation(baseline, updatesLocation);
			
			// Cache in a map all the bundles from the updates location
			HashMap<String, IApiComponent> fromUpdatesMap = new HashMap<String, IApiComponent>();
			for( int i = 0; i < fromUpdates.length; i++ ) {
				if(!fromUpdates[i].isSourceComponent()) {
					fromUpdatesMap.put(fromUpdates[i].getSymbolicName(), fromUpdates[i]);
				}
			}
			
			// Add from original baseline if updates does not have a replacement
			for( int i = 0; i < fromBaseline.length; i++ ) {
				if( fromUpdatesMap.get(fromBaseline[i].getSymbolicName()) == null ) {
					all.add(fromBaseline[i]);
				}
			}
			
			all.addAll(fromUpdatesMap.values());
			IApiComponent[] result = (IApiComponent[]) all.toArray(new IApiComponent[all.size()]);
			if(result != null) {
				baseline.addApiComponents(result);
				return result;
			}
			return ApiModelFactory.NO_COMPONENTS;
		}
		finally {
			subMonitor.done();
		}
	}
	
	public static IApiComponent[] getComponentsFromLocation(IApiBaseline baseline, String installLocation) throws CoreException {
		return getComponentsFromLocation(baseline, installLocation,false);
	}
	
	public static IApiComponent[] getApiComponentsFromFiles(IApiBaseline baseline, 
			File[] files, boolean ignoreSourceBundles) throws CoreException {
		if(files == null) {
			return ApiModelFactory.NO_COMPONENTS;
		}
		List components = new ArrayList();
		for (int i = 0; i < files.length; i++) {
			File bundle = files[i];
			IApiComponent component = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
			if(component != null) {
				if( !ignoreSourceBundles || !component.isSourceComponent())
					components.add(component);
			}
		}
		return (IApiComponent[]) components.toArray(new IApiComponent[components.size()]);
	}
	
	public static IApiComponent[] getComponentsFromLocation(IApiBaseline baseline, String installLocation, boolean ignoreSourceBundles) throws CoreException {
		// The target platform service is unavailable (OSGi isn't running), add components by searching the plug-ins directory
		File dir = new File(installLocation);
		if(dir.exists()) {
			File[] files = dir.listFiles(new CVSNameFilter());
			return getApiComponentsFromFiles(baseline, files, ignoreSourceBundles);
		}
		return ApiModelFactory.NO_COMPONENTS;
	}
	
	/**
	 * Deletes an {@link IApiBaseline} from the given folder
	 * @param referenceLocation
	 * @param folder
	 */
	public static void deleteBaseline(String referenceLocation, File folder) {
		if (Util.isArchive(referenceLocation)) {
			Util.delete(folder.getParentFile());
		}
	}
	
	public static IApiComponent[] getFilteredElements(IApiBaseline baseline, 
			String includeListLocation, String excludeListLocation) {
		FilteredElements excludedElements=null;
		FilteredElements includedElements=null;
		
		if (excludeListLocation != null) {
			excludedElements = initializeFilteredElements(
					excludeListLocation, baseline, false);
		}
		if (includeListLocation != null) {
			includedElements = initializeFilteredElements(
					includeListLocation, baseline, false);
		}
		return getFilteredElements(baseline, includedElements, excludedElements);
	}

	/*
	 * Get a list of filtered elements from this baseline
	 * based on the symbolic names of the included api components.
	 * 
	 */
	public static IApiComponent[] getFilteredElements(
			IApiBaseline baseline, IApiComponent[] included) {
		FilteredElements includedElements=
				initializeRegexFilterList(baseline, 
				StringUtils.getSymbolicNamesAsString(included, "\n"), false);
		return getFilteredElements(baseline, includedElements, null);
	}

	public static IApiComponent[] getFilteredElements(IApiBaseline baseline, 
			FilteredElements includedElements,
			FilteredElements excludedElements ) {
		IApiComponent[] all = baseline.getApiComponents();
		ArrayList<IApiComponent> ret = new ArrayList<IApiComponent>();
		for( int i = 0; i < all.length; i++ ) {
			String componentID = all[i].getSymbolicName();
			if (excludedElements != null
					&& (excludedElements.containsExactMatch(componentID)
						|| excludedElements.containsPartialMatch(componentID))) {
				continue;
			}
			if (includedElements != null && !includedElements.isEmpty()
					&& !(includedElements.containsExactMatch(componentID)
						|| includedElements.containsPartialMatch(componentID))) {
				continue;
			}
			ret.add(all[i]);
		}
		return (IApiComponent[]) ret.toArray(new IApiComponent[ret.size()]);
	}
	

	protected static FilteredElements initializeFilteredElements(String filterListLocation, IApiBaseline baseline, boolean debug) {
		char[] contents2 = null;
		if (filterListLocation != null) {
			File file = new File(filterListLocation);
			if (file.exists()) {
				InputStream stream = null;
				try {
					stream = new BufferedInputStream(new FileInputStream(file));
					contents2 = Util.getInputStreamAsCharArray(stream, -1, Util.ISO_8859_1);
				} 
				catch (FileNotFoundException e) {} 
				catch (IOException e) {} 
				finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {}
					}
				}
			}
		}
		return initializeRegexFilterList(baseline, new String(contents2), debug);
	}

	
	/**
	 * Initializes the exclude set with regex support. The API baseline is used to determine which
	 * bundles should be added to the list when processing regex expressions.
	 * 
	 * This is an override of the Util.initializeRegexFilterList method
	 * which allows a String instead of a file location
	 * 
	 * @param location
	 * @param baseline
	 * @return the list of bundles to be excluded
	 */
	public static FilteredElements initializeRegexFilterList(IApiBaseline baseline, String contents, boolean debug) {
		FilteredElements matchedElements = new FilteredElements();
		if (contents != null) {
			LineNumberReader reader = new LineNumberReader(new StringReader(contents));
			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("#") || line.length() == 0) { //$NON-NLS-1$
						continue; 
					}
					if(line.startsWith(Util.REGULAR_EXPRESSION_START)) {
						if(baseline != null) {
							Util.collectRegexIds(line, matchedElements, baseline.getApiComponents(), debug);
						}
					} else {
						matchedElements.addExactMatch(line);
					}
				}
			} 
			catch (IOException e) {} 
			catch (Exception e) {} 
			finally {
				try {
					reader.close();
				} catch (IOException e) {}
			}
		}
		return matchedElements;
	}
	
	public static File extractSDK(String installDirName, String location) {
		File file = new File(location);
		File locationFile = file;
		if (!locationFile.exists()) {
			throw new BuildException(NLS.bind(Messages.fileDoesnotExist, location));
		}
		if (Util.isArchive(location)) {
			File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
			File installDir = new File(tempDir, installDirName);
			if (installDir.exists()) {
				// delete existing folder
				if (!Util.delete(installDir)) {
					throw new BuildException(
						NLS.bind(
							Messages.couldNotDelete,
							installDir.getAbsolutePath()));
				}
			}
			if (!installDir.mkdirs()) {
				throw new BuildException(
						NLS.bind(
								Messages.couldNotCreate,
								installDir.getAbsolutePath()));
			}
			try {
				if (Util.isZipJarFile(location)) {
					Util.unzip(location, installDir.getAbsolutePath());
				} else if (Util.isTGZFile(location)) {
					Util.guntar(location, installDir.getAbsolutePath());
				}
			} catch (IOException e) {
				throw new BuildException(
					NLS.bind(
						Messages.couldNotUnzip,
						new String[] {
								location,
								installDir.getAbsolutePath()
						}));
			} catch (TarException e) {
				throw new BuildException(
						NLS.bind(
								Messages.couldNotUntar,
								new String[] {
										location,
										installDir.getAbsolutePath()
								}));
			}
			return new File(installDir, ECLIPSE_FOLDER_NAME);
		} else {
			return locationFile;
		}
	}
}
