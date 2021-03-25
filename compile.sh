javac -cp ./lib/bft-smart.jar:./lib/BFT-SMaRt-Parallel.jar:./lib/commons-codec-1.5.jar:./lib/core-0.1.4.jar:./lib/junit-jupiter-api-5.7.1.jar:./lib/netty-3.1.1.GA.jar:./lib/netty-all-4.0.36.Final.jar:./lib/queues.jar:./lib/slf4j-api-1.5.8.jar:./lib/slf4j-jdk14-1.5.8.jar \
 -d ./bin/server -sourcepath ./src/main/java ./src/main/java/demo/coin/CoinHibridServiceReplica.java
javac -cp ./lib/bft-smart.jar:./lib/BFT-SMaRt-Parallel.jar:./lib/commons-codec-1.5.jar:./lib/core-0.1.4.jar:./lib/junit-jupiter-api-5.7.1.jar:./lib/netty-3.1.1.GA.jar:./lib/netty-all-4.0.36.Final.jar:./lib/queues.jar:./lib/slf4j-api-1.5.8.jar:./lib/slf4j-jdk14-1.5.8.jar \
 -d ./bin/client -sourcepath ./src/main/java ./src/main/java/demo/coin/CoinClient.java
cd bin
jar cfe client.jar demo.coin.CoinClient -C client .
jar cfe server.jar demo.coin.CoinHibridServiceReplica -C server .
cd ..


