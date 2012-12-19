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
