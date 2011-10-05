OUTPUT=driftingdroids.jar

rm -f $OUTPUT

java -jar $HOME/Programme/proguard4.6/lib/proguard.jar @proguard_config -outjars $OUTPUT

#java -jar $HOME/Programme/autojar-2.1/autojar.jar  -o $OUTPUT  -m ./resource/META-INF/MANIFEST.MF  -c ./designgridlayout-1.8.jar -c ./fastutil-6.4.jar  -c ./bin  -v

chmod u+x $OUTPUT

