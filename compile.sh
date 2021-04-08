echo um
rm -rf ./bin
mkdir ./bin
cd ./bin
jar -xvf ../lib/commons-codec-1.5.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/core-0.1.4.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/netty-3.1.1.GA.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/netty-all-4.1.60.Final.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/queues.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/slf4j-api-1.5.8.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/slf4j-jdk14-1.5.8.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/bft-smart.jar
rm -rf ./bin/META-INF
jar -xvf ../lib/BFT-SMaRt-Parallel.jar
rm -rf ./bin/META-INF
cd ..
javac -classpath ./bin -d ./bin -sourcepath ./src/main/java ./src/main/java/demo/coin/*.java
cd bin

jar cf psmr.jar  .
cd ..

mv ./bin/psmr.jar ./deploy/psmr.jar


