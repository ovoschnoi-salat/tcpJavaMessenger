tmpDir=build

cd $(dirname $0)
rm -rf *.jar
cd src
javac -d $tmpDir */*.java
cd $tmpDir
jar -c --file ../../client.jar --main-class=client.Main client/*
jar -c --file ../../server.jar --main-class=client.Main server/*
cd ..
rm -rf $tmpDir
cd ..