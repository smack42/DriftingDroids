## how to set up a development environment for DriftingDroids

DriftingDroids is a pure Java program. Its sources are stored in "src" folder
and they consist of "*.java" and "*.properties" files.

When you import the sources into your favourite IDE then make sure that you set
the compiler compliance level to "1.6" (or higher) and that you add the included
library "designgridlayout-*.jar" to the Java build path.
The application can be compiled, debugged and run in the IDE as usual.

As an alternative to using an IDE you can compile and run the program using the
included shell-script "compilerun.sh".

The included shell-script "buildjar.sh" calls ProGuard to read the class files
from "bin" folder (compiled by IDE or "compilerun.sh") and to create a complete
application jar file in folder "lib" with the name "driftingdroids.jar", which
also contains all necessary classes from library "designgridlayout-*.jar".
This script is used for creation of DriftingDroids release builds.


libraries and tools used:

Designgridlayout is an easy and powerful Swing LayoutManager
https://designgridlayout.java.net/

Appstart is a cross-platform application launcher
https://code.google.com/p/appstart/

ProGuard is a free Java class file shrinker, optimizer, obfuscator, preverifier
http://proguard.sourceforge.net/

