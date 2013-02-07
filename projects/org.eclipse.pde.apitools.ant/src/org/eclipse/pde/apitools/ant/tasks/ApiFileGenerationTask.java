/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.apitools.ant.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.ApiDescriptionXmlCreator;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.model.ArchiveApiTypeContainer;
import org.eclipse.pde.api.tools.internal.model.CompositeApiTypeContainer;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.apitools.ant.tasks.slim.AbstractComparisonTask;
import org.eclipse.pde.apitools.ant.util.ApiToolsUtils;
import org.eclipse.pde.apitools.ant.util.IOUtil;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Ant task to generate the .api_description file during the Eclipse build.
 */
public class ApiFileGenerationTask extends Task {

	boolean debug;

	String projectName;
	String projectLocation;
	String targetFolder;
	String binaryLocations;
	String manifests;
	String sourceLocations;
	boolean allowNonApiProject = false;
	Set apiPackages = new HashSet(0);

	/**
	 * Set the project name.
	 * 
	 * @param projectName the given project name
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	/**
	 * Set the project location.
	 * 
	 * <br><br>This is the folder that contains all the source files for the given project.
	 * <br><br>The location is set using an absolute path.</p>
	 * 
	 * @param projectLocation the given project location
	 */
	public void setProject(String projectLocation) {
		this.projectLocation = projectLocation;
	}
	/**
	 * Set the target location.
	 * 
	 * <br><br>This is the folder in which the generated files are generated.
	 * <br><br>The location is set using an absolute path.</p>
	 *
	 * @param targetLocation the given target location
	 */
	public void setTarget(String targetLocation) {
		this.targetFolder = targetLocation;
	}
	/**
	 * Set the binary locations.
	 * 
	 * <br><br>This is a list of folders or jar files that contain all the .class files for the given project.
	 * They are separated by the platform path separator. Each entry must exist.
	 * <br><br>They should be specified using absolute paths.
	 *
	 * @param binaryLocations the given binary locations
	 */
	public void setBinary(String binaryLocations) {
		this.binaryLocations = binaryLocations;
	}
	
	/**
	 * Set if the task should scan the project even if it is not API tools enabled.
	 * <p>The possible values are: <code>true</code> or <code>false</code></p>
	 * <p>Default is: <code>false</code>.</p>
	 * @since 1.2
	 * @param allow
	 */
	public void setAllowNonApiProject(String allow) {
		this.allowNonApiProject = Boolean.valueOf(allow).booleanValue();
	}
	
	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	/**
	 * Set the extra manifest files' locations.
	 * 
	 * <p>This is a list of extra MANIFEST.MF files' locations that can be set to provide more api
	 * packages to scan. They are separated by the platform path separator. Each entry must exist.</p>
	 * <p>If the path is not absolute, it will be resolved relative to the current working directory.</p>
	 * <p>Jar files can be specified instead of MANIFEST.MF file. If a jar file is specified, its MANIFEST.MF file
	 * will be read if it exists.</p>
	 *
	 * @param manifests the given extra manifest files' locations
	 */
	public void setExtraManifests(String manifests) {
		this.manifests = manifests;
	}
	/**
	 * Set the extra source locations.
	 * 
	 * <br><br>This is a list of locations for source files that will be scanned.
	 * They are separated by the platform path separator. Each entry must exist.
	 * <br><br>They should be specified using absolute paths.
	 *
	 * @param manifests the given extra source locations
	 */
	public void setExtraSourceLocations(String sourceLocations) {
		this.sourceLocations = sourceLocations;
	}
	
	/* Check that the required arguments are present */
	private void checkArgs() throws BuildException {
		String[] required = new String[] {
				this.projectName, this.projectLocation,
				this.binaryLocations, this.targetFolder};
		AbstractComparisonTask.checkArgs(required, Messages.api_generation_printArguments);
	}
	
	/* Print some args */
	private void printArgs() {
		if (this.debug) {
			System.out.println("Project name : " + this.projectName); //$NON-NLS-1$
			System.out.println("Project location : " + this.projectLocation); //$NON-NLS-1$
			System.out.println("Binary locations : " + this.binaryLocations); //$NON-NLS-1$
			System.out.println("Target folder : " + this.targetFolder); //$NON-NLS-1$
			if (this.manifests != null) {
				System.out.println("Extra manifest entries : " + this.manifests); //$NON-NLS-1$
			}
			if (this.sourceLocations != null) {
				System.out.println("Extra source locations entries : " + this.sourceLocations); //$NON-NLS-1$
			}
		}
	}
	
	/* 
	 * Introspect some details to see whether we should perform any action, 
	 * or abort the task prematurely. 
	 */
	private boolean validateProject() throws BuildException {
		// collect all compilation units
		File root = new File(this.projectLocation);
		if (!root.exists() || !root.isDirectory()) {
			if (this.debug) {
				System.err.println("Must be a directory : " + this.projectLocation); //$NON-NLS-1$
			}
			throw new BuildException(
					NLS.bind(Messages.api_generation_projectLocationNotADirectory, this.projectLocation));
		}
		// check if the project contains the api tools nature
		File dotProjectFile = new File(root, ".project"); //$NON-NLS-1$
		
		if(!this.allowNonApiProject && !isAPIToolsNature(dotProjectFile)) {
			System.err.println("The project does not have an API Tools nature so a api_description file will not be generated"); //$NON-NLS-1$
			return false;
		}
		// check if the .api_description file exists
		File targetProjectFolder = new File(this.targetFolder);
		if (!targetProjectFolder.exists()) {
			targetProjectFolder.mkdirs();
		} else if (!targetProjectFolder.isDirectory()) {
			if (this.debug) {
				System.err.println("Must be a directory : " + this.targetFolder); //$NON-NLS-1$
			}
			throw new BuildException(
				NLS.bind(Messages.api_generation_targetFolderNotADirectory, this.targetFolder));
		}
		File apiDescriptionFile = new File(targetProjectFolder, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
		if (apiDescriptionFile.exists()) {
			System.out.println("An .api_description file already exists for this project.");
			// Already exists, so ignore, but print no error
			return false;
		}
		return true;
	}
	
	private boolean isForbiddenProject() {
		return this.projectLocation.endsWith(Util.ORG_ECLIPSE_SWT);
	}
	/**
	 * Execute the ant task
	 */
	public void execute() throws BuildException {
		// CHeck we have all required args
		checkArgs();
		// Print the args
		printArgs();
		// Validate teh project's main structure and pre-reqs
		boolean isValid = validateProject();
		if( !isValid )
			return;

		File root = new File(this.projectLocation);
		File targetProjectFolder = new File(this.targetFolder);
		File apiDescriptionFile = new File(targetProjectFolder, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
		
		
		ArrayList<File> allFiles = new ArrayList<File>();
		IApiTypeContainer classFileContainer = null;
		String complianceString = null;
		this.apiPackages = new HashSet<String>();

		// Why is this specifically hard-coded here? Shouldn't this be up to the build
		// to not call this task on this project?
		if (!isForbiddenProject()) {
			Map manifestMap = null;
			classFileContainer = createClassFileContainer();
			manifestMap = createRootManifestMap(root);
			complianceString = resolveCompliance(manifestMap);
			try {
				Set<String> packs = collectApiPackageNames(manifestMap);
				this.apiPackages.addAll(packs);
			} catch(BundleException be) {
				ApiPlugin.log(be);
			}
			
			if (this.manifests != null) {
				String[] allManifestFiles = this.manifests.split(File.pathSeparator);
				for (int i = 0, max = allManifestFiles.length; i < max; i++) {
					File currentManifest = new File(allManifestFiles[i]);
					manifestMap = createManifestMapForFile(currentManifest);
					Set currentApiPackages = null;
					if( manifestMap != null ) {
						try {
							currentApiPackages = collectApiPackageNames(manifestMap);
						} catch(BundleException be) {
							ApiPlugin.log(be);
						}
					}
					if (currentApiPackages != null) {
						this.apiPackages.addAll(currentApiPackages);
					}
				}
			}
			
			// Get the list of appropriate files to be analyzed for this project
			FileFilter fileFilter = new FileFilter() {
				public boolean accept(File path) {
					return (path.isFile() && Util.isJavaFileName(path.getName()) && isApi(path.getParent())) || path.isDirectory();
				}
			};
			File[] rootFiles = Util.getAllFiles(root, fileFilter);
			allFiles.addAll(Arrays.asList(rootFiles));
			
			/* Add all the appropriate files from the extra source locations */
			if (this.sourceLocations != null) {
				String[] allSourceLocations = this.sourceLocations.split(File.pathSeparator);
				for (int i = 0, max = allSourceLocations.length; i < max; i++) {
					String currentSourceLocation = allSourceLocations[i];
					File[] sourceLocFiles = Util.getAllFiles(new File(currentSourceLocation), fileFilter);
					if( sourceLocFiles != null ) {
						allFiles.addAll(Arrays.asList(sourceLocFiles));
					}
				}
			}
		}

		// Save the .api_description file
		try {
			ApiDescription apiDescription = new ApiDescription(this.projectName);
			if( allFiles.size() > 0 ) { 
				File[] allFileArray = (File[]) allFiles.toArray(new File[allFiles.size()]);
				fillApiDescription(apiDescription, allFileArray, classFileContainer, complianceString);
			}
			ApiDescriptionXmlCreator xmlVisitor = new ApiDescriptionXmlCreator(this.projectName, this.projectName);
			apiDescription.accept(xmlVisitor, null);
			String xml = xmlVisitor.getXML();
			Util.saveFile(apiDescriptionFile, xml);
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}
	
	private IApiTypeContainer createClassFileContainer() {
		// create the directory class file container used to resolve signatures during tag scanning
		String[] allBinaryLocations = this.binaryLocations.split(File.pathSeparator);
		List allContainers = new ArrayList();
		IApiTypeContainer container = null;
		for (int i = 0; i < allBinaryLocations.length; i++) {
			container = getContainer(allBinaryLocations[i]);
			if (container == null) {
				throw new BuildException(NLS.bind(Messages.api_generation_invalidBinaryLocation, allBinaryLocations[i]));
			}
			allContainers.add(container);
		}
		return new CompositeApiTypeContainer(null, allContainers);
	}
	
	
	private Map createRootManifestMap(File root) {
		File manifestFile = null;
		File manifestDir = new File(root, "META-INF"); //$NON-NLS-1$
		if (manifestDir.exists() && manifestDir.isDirectory()) {
			manifestFile = new File(manifestDir, "MANIFEST.MF"); //$NON-NLS-1$
		}
		return createManifestMapForFile(manifestFile);
	}
	
	private Map createManifestMapForFile(File manifestFile) {	
		if (manifestFile != null && manifestFile.exists()) {
			BufferedInputStream inputStream = null;
			ZipFile zipFile = null;
			try {
				if (IOUtil.isZipJarFile(manifestFile.getName())) {
					zipFile = new ZipFile(manifestFile);
					final ZipEntry entry = zipFile.getEntry("META-INF/MANIFEST.MF"); //$NON-NLS-1$
					if (entry != null) {
						inputStream = new BufferedInputStream(zipFile.getInputStream(entry));
					}
				} else {
					inputStream = new BufferedInputStream(new FileInputStream(manifestFile));
				}
				if( inputStream != null ) 
					return ManifestElement.parseBundleManifest(inputStream, null);
			} catch (FileNotFoundException e) {
				ApiPlugin.log(e);
			} catch (IOException e) {
				ApiPlugin.log(e);
			} catch (BundleException e) {
				ApiPlugin.log(e);
			} finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					} catch(IOException e) {
						// Ignore
					}
				}
				if (zipFile != null) {
					try {
						zipFile.close();
					}  catch(IOException e) {
						// ignore
					}
				}
			}
		}
		return null;
	}
	
	private void fillApiDescription(ApiDescription apiDescription, File[] allFiles, 
			IApiTypeContainer classFileContainer, String compilerCompliance) {
		TagScanner tagScanner = TagScanner.newScanner();
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, compilerCompliance);
		CompilationUnit unit = null;
		for (int i = 0, max = allFiles.length; i < max; i++) {
			unit = new CompilationUnit(allFiles[i].getAbsolutePath());
			if (this.debug) {
				System.out.println("Unit name[" + i + "] : " + unit.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			try {
				tagScanner.scan(unit, apiDescription, classFileContainer, options, null);
			} catch (CoreException e) {
				ApiPlugin.log(e);
			} finally {
				try {
					if (classFileContainer != null) {
						classFileContainer.close();
					}
				} 
				catch (CoreException e) {}
			}
		}
	}

	/**
	 * Returns if the given path ends with one of the collected API path names
	 * @param path
	 * @return true if the given path name ends with one of the collected API package names 
	 */
	boolean isApi(String path) {
		String pkg = null;
		for(Iterator iter = this.apiPackages.iterator(); iter.hasNext();) {
			pkg = (String) iter.next();
			if(path.endsWith(pkg.replace('.', File.separatorChar))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Collects the names of the packages that are API for the bundle the api description is being created for
	 * @param manifestmap
	 * @return the names of the packages that are API for the bundle the api description is being created for
	 * @throws BundleException if parsing the manifest map to get API package names fail for some reason
	 */
	private Set<String> collectApiPackageNames(Map manifestmap) throws BundleException {
		HashSet<String> set = new HashSet<String>();
		ManifestElement[] packages = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) manifestmap.get(Constants.EXPORT_PACKAGE));
		if (packages != null) {
			for (int i = 0; i < packages.length; i++) {
				ManifestElement packageName = packages[i];
				Enumeration directiveKeys = packageName.getDirectiveKeys();
				if(directiveKeys == null) {
					set.add(packageName.getValue());
				} else {
					boolean include = true;
					loop: for (; directiveKeys.hasMoreElements();) {
						Object directive = directiveKeys.nextElement();
						if ("x-internal".equals(directive)) { //$NON-NLS-1$
							String value = packageName.getDirective((String) directive);
							if (Boolean.valueOf(value).booleanValue()) {
								include = false;
								break loop;
							}
						}
						if ("x-friends".equals(directive)) { //$NON-NLS-1$
							include = false;
							break loop;
						}
					}
					if (include) {
						set.add(packageName.getValue());
					}
				}
			}
		}
		return set;
	}
	
	private IApiTypeContainer getContainer(String location) {
		boolean exists = new File(location).exists();
		if (exists) {
			if (IOUtil.isZipJarFile(location)) {
				return new ArchiveApiTypeContainer(null, location);
			} else {
				return new DirectoryApiTypeContainer(null, location);
			}
		}
		return null;
	}
	/**
	 * Resolves the compiler compliance based on the BREE entry in the MANIFEST.MF file
	 * @param manifestmap
	 * @return The derived {@link JavaCore#COMPILER_COMPLIANCE} from the BREE in the manifest map,
	 * or {@link JavaCore#VERSION_1_3} if there is no BREE entry in the map or if the BREE entry does not directly map
	 * to one of {"1.3", "1.4", "1.5", "1.6", "1.7"}.
	 */
	private String resolveCompliance(Map manifestmap) {
		if(manifestmap != null) {
			String eename = (String) manifestmap.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
			if(eename != null) {
				if("J2SE-1.4".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_4;
				}
				if("J2SE-1.5".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_5;
				}
				if("JavaSE-1.6".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_6;
				}
				if("JavaSE-1.7".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_7;
				}
			}
		}
		return JavaCore.VERSION_1_3;
	}
	
	/**
	 * Resolves if the '.project' file belongs to an API enabled project or not
	 * @param dotProjectFile
	 * @return true if the '.project' file is for an API enabled project, false otherwise
	 */
	private boolean isAPIToolsNature(File dotProjectFile) {
		if (!dotProjectFile.exists()) return false;
		BufferedInputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(dotProjectFile));
			String contents = new String(Util.getInputStreamAsCharArray(stream, -1, "UTF-8")); //$NON-NLS-1$
			return containsAPIToolsNature(contents);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return false;
	}

	/**
	 * Check if the given source contains an source extension point.
	 * 
	 * @param pluginXMLContents the given file contents
	 * @return true if it contains a source extension point, false otherwise
	 */
	private boolean containsAPIToolsNature(String pluginXMLContents) {
		return ApiToolsUtils.containsAPIToolsNature(pluginXMLContents);
	}
}
