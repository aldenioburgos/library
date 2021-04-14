#!/bin/bash

NUM_THREADS_CLIENTE=1000
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
      mkdir /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}
      # servidores
      ssh  replica0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 0 ${LATE_WORKERS_PER_PARTITION} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_replica0-e${contadorDeExecucao}.txt" &
      echo created replica0
      ssh  replica1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 1 ${LATE_WORKERS_PER_PARTITION} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_replica1-e${contadorDeExecucao}.txt" &
      echo created replica1
      ssh  replica2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 2 ${LATE_WORKERS_PER_PARTITION} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_replica2-e${contadorDeExecucao}.txt" &
      echo created replica2
      ssh  replica3  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 3 ${LATE_WORKERS_PER_PARTITION} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_replica3-e${contadorDeExecucao}.txt" &
      echo created replica3
      # espera as replicas sincronizarem
      sleep 30s
      # clientes
      echo starting clients
      ssh  cliente0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 0 4001 ${NUM_THREADS_CLIENTE} ${w} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente0-e${contadorDeExecucao}.txt" &
      echo created cliente0
      ssh  cliente1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 1 5001 ${NUM_THREADS_CLIENTE} ${w} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente1-e${contadorDeExecucao}.txt" &
      echo created cliente1
      ssh  cliente2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 2 6001 ${NUM_THREADS_CLIENTE} ${w} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente2-e${contadorDeExecucao}.txt" &
      echo created cliente2
      ssh  cliente3  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 3 7001 ${NUM_THREADS_CLIENTE} ${w} /local/warmup/warmup_p${p}.bin >& /local/logs_w${contadorDeWorkload}_e${contadorDeExecucao}/log_cliente3-e${contadorDeExecucao}.txt" &
      echo created cliente3
      # tempo de execução
      sleep 1m
      echo 1 minute
      sleep 1m
      echo 2 minutes
      sleep 1m
      echo 3 minutes
      sleep 1m
      echo encerrou
      # mata tudo para começar denovo
      ssh  cliente0  "pkill -f java" &
      echo 'cliente0 killed'
      ssh  cliente1  "pkill -f java" &
      echo 'cliente1 killed'
      ssh  cliente2  "pkill -f java" &
      echo 'cliente2 killed'
      ssh  cliente3  "pkill -f java" &
      echo 'cliente3 killed'
      ssh  replica2 "pkill -f java" &
      echo 'replica2 killed'
      ssh  replica1 "pkill -f java" &
      echo 'replica1 killed'
      ssh  replica0 "pkill -f java" &
      echo 'replica0 killed'
      echo
      sleep 3s
      echo finished hibrid execution ${contadorDeExecucao}
      contadorDeExecucao=$((contadorDeExecucao + 1))
    done;
  done;

  mkdir "results_hybrid_coin_${contadorDeWorkload}"
  mv results* "results_hybrid_coin_${contadorDeWorkload}"
  contadorDeWorkload=$((contadorDeWorkload + 1))

done;



echo 'finished all'


