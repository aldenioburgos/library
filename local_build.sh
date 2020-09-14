nome=BFT-Hibrid-with-lists-executable.jar

rm -rf build
cp ../build ./build
javac  -sourcepath ./src -cp ./lib/bft-smart.jar -d ./build ./src/demo/parallelism/LocalHibridExecution.java
jar cvfe $nome demo.parallelism.LocalHibridExecution -C build .
mv  $nome ./dist/

