tmpDir=build

rm -rf *.jar
javac -d $tmpDir src/*.java
cd $tmpDir
jar -c --file ../server.jar --main-class=Main ./*
cd ..
rm -rf $tmpDir