#!/bin/bash

NUM_THREADS_CLIENTE=250
particoes=(2 4 6 8)
threads=(2 4 6)
workloads=('0 0' '5 10')  #percGlobal #percWrite
Server="CoinHybridServiceReplica"
Client="CoinClient"
i=0

for w in "${workloads[@]}" ; do
  for p in "${particoes[@]}" ; do
    for LATE_WORKERS_PER_PARTITION in "${threads[@]}" ; do
      mkdir ./logs_${i}
      # executa experimento hybrid:
     echo ssh  replica0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 0 ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin &> ./logs_${i}/log_replica0.txt" &
     echo sleep 3s
     echo ssh  replica1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 1 ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin &> ./logs_${i}/log_replica1.txt" &
     echo sleep 3s
     echo ssh  replica2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 2 ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin &> ./logs_${i}/log_replica2.txt" &
     echo sleep 30s
     echo ssh  cliente0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 0 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin &> ./logs_${i}/log_cliente0.txt" &
     echo sleep 3s
     echo ssh  cliente1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 1 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin &> ./logs_${i}/log_cliente1.txt" &
     echo sleep 3s
     echo ssh  cliente2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 2 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin &> ./logs_${i}/log_cliente2.txt" &
     echo sleep 3s
     echo ssh  cliente3  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 3 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin &> ./logs_${i}/log_cliente3.txt" &
     echo sleep 4m
     echo ssh  cliente0  "pkill -f java" &
     echo echo 'cliente0 killed'
     echo ssh  cliente1  "pkill -f java" &
     echo echo 'cliente1 killed'
     echo ssh  cliente2  "pkill -f java" &
     echo echo 'cliente2 killed'
     echo ssh  cliente3  "pkill -f java" &
     echo echo 'cliente3 killed'
     echo ssh  replica2 "pkill -f java" &
     echo echo 'replica2 killed'
     echo ssh  replica1 "pkill -f java" &
     echo echo 'replica1 killed'
     echo ssh  replica0 "pkill -f java" &
     echo echo 'replica0 killed'
     echo echo
     echo sleep 3s
     echo echo 'finished hibrid'

    done;
  done;

  echo mkdir "hybrid_coin_${i}"
  echo mv results* "hybrid_coin_${i}"
  i=$((i + 1))

done;



echo 'finished all'


