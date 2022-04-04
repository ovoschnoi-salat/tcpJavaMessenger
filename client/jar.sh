tmpDir=build

rm -rf *.jar
javac -d $tmpDir src/*.java
cd $tmpDir
jar -c --file ../client.jar --main-class=Main ./*
cd ..
rm -rf $tmpDir