
javac -verbose  --release 11  -sourcepath ./src -cp ./lib/bft-smart.jar;./dist/BFT-SMaRt-Parallel.jar -d ./build ./src/demo/parallelism/LocalHibridExecution2.java
jar --create --file ./dist/BFT-SMaRt-Hibrid.jar --verbose --main-class demo/parallelism/LocalHibridExecution2 -C ./build .



