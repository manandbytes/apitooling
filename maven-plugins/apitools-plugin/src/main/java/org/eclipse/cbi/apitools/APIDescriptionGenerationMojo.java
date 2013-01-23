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

import net.oxbeef.apitools.core.ant.ApiFileGenerationTask;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Generates API reports
 *
 * @goal generate-description
 * @phase compile
 * @author Mickael Istria (JBoss, by Red Hat)
 */
public class APIDescriptionGenerationMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
    	// API Tools can apply to any kind of Java code
    	if (! (this.project.getPackaging().equals("eclipse-plugin") || this.project.getPackaging().equals("jar")) ) {
    		getLog().info("API Analysis plugin only applies to Eclipse-plugin and jar project");
    		return;
    	}
    	ApiFileGenerationTask task = new ApiFileGenerationTask();
    	task.setProject(this.project.getBasedir().getAbsolutePath());
    	task.setProjectName(this.project.getName());
    	task.setBinary(this.project.getBuild().getDirectory());
    	task.setTarget(new File(this.project.getBuild().getDirectory(), "classes").getAbsolutePath());
    	task.setDebug(Boolean.toString(getLog().isDebugEnabled()));
    	task.execute();
    }
}
