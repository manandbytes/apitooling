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
import java.util.Map;

import net.oxbeef.apitools.core.ant.slim.ApiAnalysisTask;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.PluginDescription;
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

    public void execute() throws MojoExecutionException, MojoFailureException {
    	ApiAnalysisTask task = new ApiAnalysisTask();
    	task.setBaseline(this.baseline.getAbsolutePath());
    	task.setDebug(getLog().isDebugEnabled());
    	task.setReport(this.outputDirectory.getAbsolutePath());
    	prepareProfile();
    	task.setProfile(this.profileLocation.getAbsolutePath());
    	// TODO includes & excludes
    	task.execute();
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
		tychoProject.getDependencyWalker(this.project).walk(new ArtifactDependencyVisitor() {
			@Override
			public void visitPlugin(PluginDescription pluginRef) {
				try {
					File location = pluginRef.getLocation();
					if (location.isFile()) {
						FileUtils.copyFileToDirectory(pluginRef.getLocation(), APIReportsMojo.this.profileLocation);
					} else if (location.isDirectory()) {
						if (pluginRef.getMavenProject() != null) {
							getLog().warn("Reactor projects not yet supported: " + pluginRef.getMavenProject());
						} else {
							getLog().warn("Directory-shaped bundles not yet supported: " + pluginRef.getLocation());
						}
					}
				} catch (Exception ex) {
					errorBuilder.append("Couldn't copy " + pluginRef.getLocation() + " to " + APIReportsMojo.this.profileLocation);
					errorBuilder.append("\n");
				}
			}
		});
		if (errorBuilder.length() > 0) {
			throw new MojoFailureException(errorBuilder.toString());
		}
	}
}
