Headless Eclipse API Tools
==========================

Purpose
--------

This contain tools to generate api reports for eclipse plugins, from Ant or Maven. It relies on [PDE API Tools](http://www.eclipse.org/pde/pde-api-tools/)
This effort is done on the context of [JBoss Tools](http://www.jboss.org/tools) and [Eclipse Common Build Infrastructure](http://wiki.eclipse.org/CBI). You might want to read the following bug reports: [for JBoss Tools](https://issues.jboss.org/browse/JBIDE-12190) and [for Eclipse](https://bugs.eclipse.org/bugs/show_bug.cgi?id=381057).


Contents
---------

These tools are divided in several parts:
* _org.eclipse.pde.apitools.ant_ contains extracted Ant tasks related to apitools for from _org.eclipse.pde.apitools/src-ant_. The tasks are essentially (I think?) unchanged. All other work is original or possibly broken out of those classes into utility classes. Many other classes have been brought in from other eclipse plugins (such as IMemento and XMLMemento) to help clean up the xml handling and report generation. 
* _binaries_ folder contains a jar to use if you want to use these Ant tasks
* _antSetup_ folder contains sample Ant build.xml files to consume the tasks
* _maven-plugins_ folder contains a maven plugin, that can be used to run the API analysis with Maven.

Usage
------

### With Maven

#### Add repositories

Add the following repositories to your pom:

```xml
<repositories>
  <repository>
    <id>jbosstools-experiments</id>
    <url>https://repository.jboss.org/nexus/content/repositories/jbosstools-experiments/</url>
  </repository>
  <repository>
    <id>jbosstools-experiments-snapshots</id>
    <url>https://repository.jboss.org/nexus/content/repositories/jbosstools-experiments-snapshots/</url>
  </repository>
</repositories>
```

Those repositories contain:
* Eclipse dependencies (PDE, Resources...) as Maven artifacts
* org.eclipse.pde.apitooling.ant bundles available as a Maven artifact
* The Maven plugin that uses tasks from org.eclipse.pde.apitooling.ant

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
Then run `mvn verify` or `maven reports` depending on the chosen phase, and see your reports in `target/reports/api`

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

1. Clone the repository
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
-------------------

### Modify intenal plugin and Ant tasks
1. Import the project _org.eclipse.pde.apitools.ant_ as an Eclipse plugin project into an Eclipse suitable for plugin development
2. Modify sources locally
3. To use it with Maven: Install it into your local Maven repository with `mvn install`
4. To use it with Ant: Right-click the project, export as -> jar,  destination file = ${apitooling.root}/binaries/apitooling-ant.jar

### Modify the Maven Mojo
1. Import the project _maven-plugins/apitools-plugin_ into your workspace
2. Make your changes
3. Run `mvn install` to install it locally.
