# To start DriftingDroids from the bash shell
# run this script in the root folder of the repository with
# ./run.sh

DESTDIR=bin
SRCDIR=src

echo compile...
rm -rf $DESTDIR
mkdir $DESTDIR
javac -sourcepath $SRCDIR -d $DESTDIR -cp designgridlayout-1.11.jar -source 1.6 -target 1.6 $SRCDIR/driftingdroids/ui/*.java $SRCDIR/driftingdroids/model/*.java
cp -rp $SRCDIR/META-INF $SRCDIR/*.properties $DESTDIR

echo
echo run...
java -Xmx800M -cp $DESTDIR:designgridlayout-1.11.jar driftingdroids.ui.Starter

