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

}
