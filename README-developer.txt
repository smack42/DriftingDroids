## how to set up a development environment for DriftingDroids

DriftingDroids is a pure Java program. Its sources are stored in "src" folder
and they consist of "*.java" and "*.properties" files.

When you import the sources into your favourite IDE then make sure that you set
the compiler compliance level to "1.8" and that you add the included
library "designgridlayout-1.11.jar" to the Java build path.
The application can be compiled, debugged and run in the IDE as usual.

As an alternative to using an IDE you can compile and run the program using the
included shell-script "compilerun.sh".

The included shell-script "buildrelease.sh" calls "jar" to read the class files
from "bin" folder (compiled by IDE or "compilerun.sh") and to create a complete
application jar file in folder "lib" with the name "driftingdroids.jar".
This script also copies all files and folders, which are necessary for a release
build, into the newly created folder DriftingDroids_release.


libraries and tools used:

Designgridlayout is an easy and powerful Swing LayoutManager
https://web.archive.org/web/20170409233103/https://java.net/projects/designgridlayout/pages/Home
https://search.maven.org/artifact/net.java.dev.designgridlayout/designgridlayout

Appstart is a cross-platform application launcher
https://code.google.com/p/appstart/
https://github.com/smack42/appstart

