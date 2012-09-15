OUTPUT=driftingdroids.jar

rm -f $OUTPUT

java -jar $HOME/Programme/proguard4.8/lib/proguard.jar @proguard_config -outjars $OUTPUT

chmod u+x $OUTPUT

