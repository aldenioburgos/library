#!/bin/bash
particoes=(1 2 4 6 8)
for p in "${particoes[@]}" ; do
     NUM_USUARIOS=1000
     NUM_TOKENS=10
     NUM_PARTICOES=${p}
    echo java -classpath psmr.jar  demo.coin.WarmUp $NUM_USUARIOS $NUM_TOKENS $NUM_PARTICOES warmup_p${p}.bin
    java -classpath psmr.jar  demo.coin.WarmUp $NUM_USUARIOS $NUM_TOKENS $NUM_PARTICOES warmup_p${p}.bin
done;
echo fim.