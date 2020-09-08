rm -rf build
mkdir build
cp ./lib/*.jar ./build
cd ./build
unzip -o bft-smart.jar
unzip -o commons-codec-1.5.jar
unzip -o core-0.1.4.jar
unzip -o netty-all-4.0.36.Final.jar
unzip -o netty-3.1.1.GA.jar
unzip -o slf4j-api-1.5.8.jar
unzip -o slf4j-jdk14-1.5.8.jar
rm *.jar
rm -rf META-INF
cd ..
javac  --release 11  -sourcepath ./src -cp ./lib/bft-smart.jar -d ./build ./src/demo/parallelism/LocalHibridExecution.java
jar cvfe BFT-Hibrid-0.1.jar demo.parallelism.LocalHibridExecution -C build ./dist
