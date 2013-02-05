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
import java.io.IOException;
import java.util.Properties;

public class IOUtil {
	public static Properties readPropertiesFile(String preferencesLocation) {
		File preferencesFile = new File(preferencesLocation);
		if (!preferencesFile.exists()) {
			return null;
		}
		BufferedInputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(preferencesFile));
			Properties temp = new Properties();
			temp.load(inputStream);
			return temp; 
		} catch (IOException e) {
			// ignore
			return null;
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch(IOException e) {
					// ignore
				}
			}
		}
	}
	
	public static boolean isZipJarFile(String fileName) {
		String normalizedFileName = fileName.toLowerCase();
		return normalizedFileName.endsWith(".zip") //$NON-NLS-1$
			|| normalizedFileName.endsWith(".jar"); //$NON-NLS-1$
	}

}
