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
      # servidores
      echo starting replicas
      for i in {0..3} ; do
        ssh  replica${i}  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 0 ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin >& /local/logs/log_r${i}-e${contadorDeExecucao}.txt" &
        echo created replica${i}
      done
      sleep 30s
      echo starting clients
      for i in {0..3} ; do
        ssh  cliente${i}  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} 0 $((4001 + (i*1000))) ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin >& /local/logs/log_c${i}-e${contadorDeExecucao}.txt" &
        echo created cliente${i}
        sleep 3s
      done
      # tempo de execução
      sleep 1m
      echo 1 minute
      sleep 1m
      echo 2 minutes
      sleep 1m
      echo 3 minutes encerrou
      # mata tudo para começar denovo
      for i in {0..3} ; do
        ssh  cliente${i}  "pkill -f java" &
        echo cliente${i} killed
        sleep 3s
      done
      for i in {0..3} ; do
        ssh  replica${i}  "pkill -f java" &
        echo replica${i} killed
        sleep 3s
      done
      sleep 10s
      echo finished hibrid execution ${contadorDeExecucao}
      contadorDeExecucao=$((contadorDeExecucao + 1))
      echo
    done;
  done;

  # criando pasta de execução
  agora=`date +"%y-%m-%d-%T"`
  execDir=execution_w${contadorDeWorkload}_${agora}
  echo criando a pasta de execução ~/hybridpsmr/deploy/${execDir}
  mkdir ~/hybridpsmr/deploy/${execDir}

  echo zipando os logs para ${execDir}
  for i in {0..3} ; do
      ssh  cliente${i}  "tar -czf ~/hybridpsmr/deploy/${execDir}/log_c${i}.tar.gz /local/logs/*" &
      ssh  replica${i}  "tar -czf ~/hybridpsmr/deploy/${execDir}/log_r${i}.tar.gz /local/logs/*" &
  done

  echo zipando os resultados para ${execDir}
  tar -czf ~/hybridpsmr/deploy/${execDir}/results.tar.gz  ~/hybridpsmr/deploy/resultsCoin* &
  sleep 3s
  # entrando no proximo workload
  contadorDeWorkload=$((contadorDeWorkload + 1))
done;
echo 'finished all'




