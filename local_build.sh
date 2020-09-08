
javac  --release 11  -sourcepath ./src -cp ./lib/bft-smart.jar;./dist/BFT-SMaRt-Parallel.jar -d ./build ./src/demo/parallelism/LocalHibridExecution.java
cp ./lib/*.jar ./build
cd ./build
tar xf *.jar
cd ..
jar --create --file ./dist/BFT-SMaRt-Hibrid.jar --verbose --main-class demo/parallelism/LocalHibridExecution -C ./build .

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
