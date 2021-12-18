DESTDIR=DriftingDroids_release

rm -rf $DESTDIR
mkdir $DESTDIR

mkdir $DESTDIR/lib
jar --create --file $DESTDIR/lib/driftingdroids.jar --no-compress --manifest src/META-INF/MANIFEST.MF -C bin/ driftingdroids -C src/ resource
cp -p  lib/designgridlayout-1.11.jar  $DESTDIR/lib
cp -p  lib/flatlaf*.jar               $DESTDIR/lib

cp -p  lib/appstart.properties        $DESTDIR
cp -p  lib/appstart.jar               $DESTDIR/start.jar

cp -p  CHANGES.txt                    $DESTDIR
cp -p  LICENSE.txt                    $DESTDIR
cp -p  LIESMICH.txt                   $DESTDIR
cp -p  README.txt                     $DESTDIR

cp -rp src                            $DESTDIR

mkdir $DESTDIR/doc
cp -p  doc/*.txt                      $DESTDIR/doc
cp -p  doc/*.pdf                      $DESTDIR/doc
cp -p  doc/*.jpg                      $DESTDIR/doc

