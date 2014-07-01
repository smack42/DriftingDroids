#!/bin/bash
# To start DriftingDroids from the bash shell
# run this script in the root folder of the repository with
# ./run.sh

cd src/

echo compiling...
javac -cp ../designgridlayout-1.11.jar driftingdroids/ui/*.java driftingdroids/model/*.java

echo
echo run DriftingDroids ...
java -Xmx800M -cp .:../designgridlayout-1.11.jar driftingdroids.ui.Starter

