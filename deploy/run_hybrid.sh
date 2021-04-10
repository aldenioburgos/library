#!/bin/bash

particoes=(1 2 4 6 8)
threads=(2 4 6)
workloads=('0 0' '5 10')
Server="CoinHybridServiceReplica"
Client="CoinClient"
NUM_THREADS_CLIENTE=250
NUM_OPERACOES_PER_CLIENTE=1000000
NUM_OPS_PER_REQ=1
PERC_GLOBAL=0
PERC_WRITE=0


for w in "${workloads[@]}" ; do
  for p in "${particoes[@]}" ; do
    for LATE_WORKERS_PER_PARTITION in "${threads[@]}" ; do

      # executa experimento hybrid:
      ssh  replica0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 0 ${LATE_WORKERS_PER_PARTITION} warmup_p${p}_.bin" &
      sleep 3s
      ssh  replica1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 1 ${LATE_WORKERS_PER_PARTITION} warmup_p${p}_.bin" &
      sleep 3s
      ssh  replica2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 2 ${LATE_WORKERS_PER_PARTITION} warmup_p${p}_.bin" &
      sleep 30s
      ssh  cliente0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
      sleep 3s
      ssh  cliente1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
      sleep 3s
      ssh  cliente2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
      sleep 3s
      ssh  cliente3  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
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

  echo mkdir "hybrid_${w}"
  mv results* "hybrid_${w}"

done;



echo 'finished all'


