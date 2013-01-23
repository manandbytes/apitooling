Headless Eclipse API Tools
==========================

Purpose
--------

This contain tools to generate api reports for eclipse plugins, from Ant or Maven. It relies on [PDE API Tools](http://www.eclipse.org/pde/pde-api-tools/)
This effort is done on the context of [JBoss Tools](http://www.jboss.org/tools) and [Eclipse Common Build Infrastructure](http://wiki.eclipse.org/CBI). You might want to read the following bug reports: [for JBoss Tools](https://issues.jboss.org/browse/JBIDE-12190) and [for Eclipse](https://bugs.eclipse.org/bugs/show_bug.cgi?id=381057).


Contents
---------

These tools are divided in several parts:
* _ecliipse-project_ contains a twaeaked version of Ant tasks from _org.eclipse.pde.ui_. The package named net.oxbeef.apitools.core.ant is a clone of the ant tasks in pde.ui, and are essentially (I think?) unchanged. All other work is original or possibly broken out of those classes into utility classes. Many other classes have been brought in from other eclipse plugins (such as IMemento and XMLMemento) to help clean up the xml handling and report generation. 
* _binaries_ folder contains a jar to use if you want to use these Ant tasks
* _antSetup_ folder contains sample Ant build.xml files to consume the tasks
* _maven-plugins_ folder contains a maven plugin, that can be used to run the API analysis with Maven.

Usage
------

### With Maven

1. git clone the repo
2. From an Eclipse 4.3.0.M4, install the necessary bundles in your local Maven repo. Just run the list of installations as specified in _eclipse-plugins/apitools-plugin/bundlesToInstall.sh_
3. Run _mvn install_ on the whole repository, this will build the _eclipse-projects_, and the Maven plugin which depends on _net.oxbeef.apitools.core_.

#### Enable reports on a Tycho project

Just add this to your pom.xml:
```xml
<plugin>
	<groupId>org.eclipse.cbi</groupId>
	<artifactId>apitools-plugin</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<executions>
		<execution>
			<id>api-reports</id>
			<goals>
				<goal>generate-reports</goal>
			</goals>
			<phase>verify</phase>
			<configuration>
				<baseline>/home/mistria/jbdevstudio/studio/plugins</baseline>
			</configuration>
		</execution>
	</executions>
</plugin>
```
Then run _mvn verify_ or _maven reports_ depending on the chosen phase, and see your reports in _target/reports/api_

#### Generate API description file

API Tools can also generate an API description file for your bundle (based on @since annotions and so on), that can be used after in your reports. You can enable generation of such file on any _eclipse-plugin_ 

```xml
<plugin>
        <groupId>org.eclipse.cbi</groupId>
        <artifactId>apitools-plugin</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <executions>
                <execution>
                        <id>api-reports</id>
                        <goals> 
                                <goal>generate-description</goal>
                        </goals>
                        <phase>compile</phase>
                </execution>
        </executions>
</plugin>
```

### With Ant

1. Cone the repository
2. modify antSetup/build.xml
   1. Modify eclipse.install.dir to point to a base installation of eclipse, no custom plugins necessary
   2. Modify baseline to point to a directory full of eclipse plugins, including your custom plugin
   3. Modify nightly to point to a directory full of eclipse plugins, including a slightly modified version of your custom plugin
   4. Modify apitooling.root to point to the folder you git cloned, containing this README.md
   5. Modify includesFile to contain a list of the symbolic names of the bundles you want reports run on. No commas necessary.
3. Run the build by typing "ant"
4. verify output/20121219/your.plugin/report.xml exists
5. cp antSetup/web/analysis.xsl antSetup/output/20121219/your.plugin/    (move xsl into same folder as your report)
6. View in web browser, see the ugly progress


Origin
-------

Ant tasks were originally part of the git repo found at git://git.eclipse.org/gitroot/pde/eclipse.pde.ui.git, however the ant tasks were bundled in the same plugin when clearly they should be broken out. 

The package named net.oxbeef.apitools.core.ant is a clone of the ant tasks in pde.ui, and are essentially (I think?) unchanged. All other work is original or possibly broken out of those classes into utility classes. Many other classes have been brought in from other eclipse plugins (such as IMemento and XMLMemento) to help clean up the xml handling and report generation. 


License
--------
Necessarily, this project is an EPL project. 


Modify & contribute
--------------------

To modify or add to the ant tasks, you'll want to do the following:
1) Import the project (projects/net.oxbeef.apitools.core) into an eclipse suitable for plugin development
2) Modify the source as you see fit
3) Right-click the project, export as -> jar,  destination file = ${apitooling.root}/binaries/apitooling-ant.jar
4) re-run the ant script to verify your changes have been accepted
