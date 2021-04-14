#!/bin/bash

NUM_THREADS_CLIENTE=1
particoes=(2 4 6 8)
threads=(2 4 6)
workloads=('0 0' '5 10')  #percGlobal #percWrite
Server="CoinHybridServiceReplica"
Client="CoinClient"
contadorDeWorkload=0
contadorDeExecucao=0

for w in "${workloads[@]}" ; do
  for p in "${particoes[@]}" ; do
    for LATE_WORKERS_PER_PARTITION in "${threads[@]}" ; do
      mkdir ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}
      # executa experimento hybrid:
      ssh  replica0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 0 ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin >& ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_replica0-execucao${contadorDeExecucao}.txt" &
      sleep 3s
      echo created replica0
      ssh  replica1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 1 ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin >& ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_replica1-execucao${contadorDeExecucao}.txt" &
      sleep 3s
      echo created replica1
      ssh  replica2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 2 ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin >& ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_replica2-execucao${contadorDeExecucao}.txt" &
      sleep 3s
      echo created replica2
      sleep 30s
      echo starting clients
      ssh  cliente0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 0 7001 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin >& ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente0-execucao${contadorDeExecucao}.txt" &
      sleep 3s
      echo created cliente0
#      ssh  cliente1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 1 5001 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin >& ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente1-execucao${contadorDeExecucao}.txt" &
#      sleep 3s
#      ssh  cliente2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 2 6001 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin >& ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente2-execucao${contadorDeExecucao}.txt" &
#      sleep 3s
#      ssh  cliente3  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 3 7001 ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin >& ./logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente3-execucao${contadorDeExecucao}.txt" &
      sleep 4m
      contadorDeExecucao=$((contadorDeExecucao + 1))
      ssh  cliente0  "pkill -f java" &
      echo 'cliente0 killed'
#      ssh  cliente1  "pkill -f java" &
#      echo 'cliente1 killed'
#      ssh  cliente2  "pkill -f java" &
#      echo 'cliente2 killed'
#      ssh  cliente3  "pkill -f java" &
#      echo 'cliente3 killed'
#      ssh  replica2 "pkill -f java" &
#      echo 'replica2 killed'
#      ssh  replica1 "pkill -f java" &
#      echo 'replica1 killed'
#      ssh  replica0 "pkill -f java" &
#      echo 'replica0 killed'
      echo
      sleep 3s
      echo 'finished hibrid'

    done;
  done;

  mkdir "hybrid_coin_${contadorDeWorkload}"
  mv results* "hybrid_coin_${contadorDeWorkload}"
  contadorDeWorkload=$((contadorDeWorkload + 1))

done;



echo 'finished all'


