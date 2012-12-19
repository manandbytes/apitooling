package net.oxbeef.apitools.core.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.oxbeef.apitools.core.ant.Messages;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.TarException;
import org.eclipse.pde.api.tools.internal.util.Util;

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
		try {
			IApiBaseline baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = ApiModelFactory.newApiBaseline(baselineName);
			} else if (eeFileLocation != null) {
				baseline = ApiModelFactory.newApiBaseline(baselineName, new File(eeFileLocation));
			} else {
				baseline = ApiModelFactory.newApiBaseline(baselineName, Util.getEEDescriptionFile());
			}
			
			IApiComponent[] components = ApiModelFactory.addComponents(baseline, installLocation, null);
			if (components.length == 0){			
				throw new BuildException(NLS.bind(Messages.directoryIsEmpty, installLocation));
			}
			return baseline;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
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
		return Util.initializeRegexFilterList(filterListLocation, baseline, debug);
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
