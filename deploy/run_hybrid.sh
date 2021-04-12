#!/bin/bash

NUM_THREADS_CLIENTE=250
particoes=(2 4 6 8)
threads=(2 4 6)
workloads=('0 0' '5 10')  #percGlobal #percWrite
Server="CoinHybridServiceReplica"
Client="CoinClient"


for w in "${workloads[@]}" ; do
  for p in "${particoes[@]}" ; do
    for LATE_WORKERS_PER_PARTITION in "${threads[@]}" ; do

      # executa experimento hybrid:
      ssh  replica0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 0 ${LATE_WORKERS_PER_PARTITION} warmup_p${p}.bin" &
      sleep 3s
      ssh  replica1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 1 ${LATE_WORKERS_PER_PARTITION} warmup_p${p}.bin" &
      sleep 3s
      ssh  replica2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 2 ${LATE_WORKERS_PER_PARTITION} warmup_p${p}.bin" &
      sleep 30s
      ssh  cliente0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 0 ${NUM_THREADS_CLIENTE} ${w} warmup_p${p}.bin" &
      sleep 3s
      ssh  cliente1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 1 ${NUM_THREADS_CLIENTE} ${w} warmup_p${p}.bin" &
      sleep 3s
      ssh  cliente2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 2 ${NUM_THREADS_CLIENTE} ${w} warmup_p${p}.bin" &
      sleep 3s
      ssh  cliente3  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 3 ${NUM_THREADS_CLIENTE} ${w} warmup_p${p}.bin" &
      sleep 4m
      ssh  cliente0  "pkill -f 'java.*bft-smart*'" &
      echo 'cliente0 killed'
      ssh  cliente1  "pkill -f 'java.*bft-smart*'" &
      echo 'cliente1 killed'
      ssh  cliente2  "pkill -f 'java.*bft-smart*'" &
      echo 'cliente2 killed'
      ssh  cliente3  "pkill -f 'java.*bft-smart*'" &
      echo 'cliente3 killed'
      ssh  replica2 "pkill -f 'java.*bft-smart*'" &
      echo 'replica2 killed'
      ssh  replica1 "pkill -f 'java.*bft-smart*'" &
      echo 'replica1 killed'
      ssh  replica0 "pkill -f 'java.*bft-smart*'"
      echo 'replica0 killed'
      echo
      sleep 3s
      echo 'finished hibrid'

    done;
  done;

  echo mkdir "hybrid_coin_${w}"
  mv results* "hybrid_coin_${w}"

done;



echo 'finished all'


