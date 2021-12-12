# To start DriftingDroids from the bash shell,
# run this script in the root folder of the repository

DESTDIR=bin
SRCDIR=src
LIBDIR=lib

echo compile...
rm -rf $DESTDIR
mkdir $DESTDIR
javac -sourcepath $SRCDIR -d $DESTDIR -cp $LIBDIR/designgridlayout-1.11.jar -source 1.8 -target 1.8 $SRCDIR/driftingdroids/ui/*.java $SRCDIR/driftingdroids/model/*.java
cp -rp $SRCDIR/META-INF $SRCDIR/*.properties $DESTDIR

echo
echo run...
java -Xmx800M -cp $DESTDIR:$LIBDIR/designgridlayout-1.11.jar driftingdroids.ui.Starter

