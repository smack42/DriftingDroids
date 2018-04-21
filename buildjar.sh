OUTPUT=lib/driftingdroids.jar

rm -f $OUTPUT

java -jar $HOME/Programme/proguard6.0/lib/proguard.jar @proguard_config -outjars $OUTPUT

chmod u+x $OUTPUT

