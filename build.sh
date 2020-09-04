set myLibs=./lib/bft-smart.jar;./lib/commons-codec-1.5.jar;./lib/core-0.1.4.jar;./lib/;./dist/BFT-SMaRt.Parallel.jar;./lib/netty-3.1.1.GA.jar;./lib/netty-all-4.0.36.Final.jar;./lib/slf4j-api-1.5.8.jar;./lib/slf4j-jdk14-1.5.8.jar

javac -verbose  --release 11  -sourcepath ./src -cp ./lib/bft-smart.jar;./dist/BFT-SMaRt-Parallel.jar -d ./build ./src/demo/parallelism/LocalHibridExecution.java
jar --create --file ./dist/BFT-SMaRt-Hibrid.jar --verbose --main-class demo/parallelism/LocalHibridExecution -C ./build .
java -cp %myLibs%;./dist/BFT-SMaRt-Hibrid.jar demo.parallelism.LocalHibridExecution 2 2 1000 10 10 10000000 150


