apitooling
==========

Tooling for generating api reports for eclipse plugins

Step 1: git clone the repo
Step 2: modify antSetup/build.xml
   a: Modify eclipse.install.dir to point to a base installation of eclipse, no custom plugins necessary
   b: Modify baseline to point to a directory full of eclipse plugins, including your custom plugin
   c: Modify nightly to point to a directory full of eclipse plugins, including a slightly modified version of your custom plugin
   d: Modify apitooling.root to point to the folder you git cloned, containing this README.md
   e: Modify includesFile to contain a list of the symbolic names of the bundles you want reports run on. No commas necessary.

Step 3: Run the build by typing "ant"
Step 4: verify output/20121219/your.plugin/report.xml exists
Step 5: cp antSetup/web/analysis.xsl antSetup/output/20121219/your.plugin/    (move xsl into same folder as your report)
Step 6: View in web browser, see the ugly progress

origin  

This was originally part of the git repo found at  git://git.eclipse.org/gitroot/pde/eclipse.pde.ui.git, however the ant tasks were bundled in the same plugin when clearly they should be broken out. 

The package named net.oxbeef.apitools.core.ant is a clone of the ant tasks in pde.ui, and are essentially (I think?) unchanged. All other work is original or possibly broken out of those classes into utility classes. Many other classes have been brought in from other eclipse plugins (such as IMemento and XMLMemento) to help clean up the xml handling and report generation. 

Necessarily, this project is an EPL project. 

To modify or add to the ant tasks, you'll want to do the following:
1) Import the project (projects/net.oxbeef.apitools.core) into an eclipse suitable for plugin development
2) Modify the source as you see fit
3) Right-click the project, export as -> jar,  destination file = ${apitooling.root}/binaries/apitooling-ant.jar
4) re-run the ant script to verify your changes have been accepted
