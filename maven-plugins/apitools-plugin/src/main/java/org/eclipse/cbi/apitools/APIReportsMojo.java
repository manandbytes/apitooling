/**
 * Copyright (c) 2013, Red Hat Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.eclipse.cbi.apitools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import net.oxbeef.apitools.core.ant.slim.ApiAnalysisTask;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.core.TychoProject;

/**
 * Generates API reports
 *
 * @goal generate-reports
 * @phase report
 * @author Mickael Istria (JBoss, by Red Hat)
 */
public class APIReportsMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}/reports/api"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter
     * @required
     */
    private File baseline;


    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;

    private File profileLocation;
    private File includeList;

    public void execute() throws MojoExecutionException, MojoFailureException {
    	ApiAnalysisTask task = new ApiAnalysisTask();
    	task.setBaseline(this.baseline.getAbsolutePath());
    	task.setDebug(getLog().isDebugEnabled());
    	task.setReport(this.outputDirectory.getAbsolutePath());
    	populateIncludeList();
    	task.setIncludeList(this.includeList.getAbsolutePath());
    	task.setFilters(this.project.getArtifactId());
    	prepareProfile();
    	task.setProfile(this.profileLocation.getAbsolutePath());
    	// TODO includes & excludes
    	task.execute();
    }

	private void populateIncludeList() throws MojoExecutionException {
		StringBuilder content = new StringBuilder();
		if (this.project.getPackaging().equals("eclipse-plugin") || this.project.getPackaging().equals("eclipse-test-plugin")) {
			content.append(this.project.getArtifactId());
		}
		try {
			this.outputDirectory.mkdirs();
	    	this.includeList = new File(this.outputDirectory, "includeList");
	    	if (!this.includeList.exists()) {
	    		this.includeList.createNewFile();
	    	}
			FileOutputStream includeListStream = new FileOutputStream(this.includeList);
			includeListStream.write(content.toString().getBytes());
			includeListStream.close();
		} catch (IOException ex) {
			throw new MojoExecutionException("Could not write includeList", ex);
		}
	}

	private void prepareProfile() throws MojoExecutionException, MojoFailureException {
		this.profileLocation = new File(this.outputDirectory, "profile");
		TychoProject tychoProject = projectTypes.get(this.project.getPackaging());
		if (tychoProject == null) {
			throw new MojoExecutionException("This only applies to Tycho projects");
		}

		if (!this.profileLocation.exists()) {
			this.profileLocation.mkdirs();
		}
		final StringBuilder errorBuilder = new StringBuilder();
		for (ArtifactDescriptor dep : tychoProject.getDependencyArtifacts(project).getArtifacts()) {
			try {
				File location = dep.getLocation();
				if (location.isFile()) {
					FileUtils.copyFileToDirectory(dep.getLocation(), APIReportsMojo.this.profileLocation);
				} else if (location.isDirectory()) {
					if (dep.getMavenProject() != null) {
						FileUtils.copyFileToDirectory(dep.getMavenProject().getArtifact().getAbsoluteFile(), APIReportsMojo.this.profileLocation);
					} else {
						getLog().warn("Directory-shaped bundles not yet supported: " + dep.getLocation());
					}
				}
			} catch (Exception ex) {
				errorBuilder.append("Couldn't copy " + dep.getLocation() + " to " + APIReportsMojo.this.profileLocation);
				errorBuilder.append("\n");
			}
		}
		if (this.project.getPackaging().equals("eclipse-plugin") || this.project.equals("eclipse-test-plugin")) {
			try {
				FileUtils.copyFileToDirectory(this.project.getArtifact().getFile(), APIReportsMojo.this.profileLocation);
			} catch (Exception ex) {
				errorBuilder.append("Couldn't copy current project artifact (" + this.project.getArtifact().getFile() + ") to " + APIReportsMojo.this.profileLocation);
				errorBuilder.append("\n");
			}
		}
		if (errorBuilder.length() > 0) {
			throw new MojoFailureException(errorBuilder.toString());
		}
	}
}
