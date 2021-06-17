#!/bin/bash
source ./kill_hybrid.sh


wait_replicas_end(){
   counter=0
   until [ -f ~/finishedReplicas/replica0 ] && [ -f ~/finishedReplicas/replica1 ] && [ -f ~/finishedReplicas/replica2 ] && [ -f ~/finishedReplicas/replica3 ] ;
    do
      sleep 10s 
      echo waiting replicas end.
      if [[ $counter -eq 36 ]];
	then
	break;
      fi
      ((counter++))
    done;
    rm  ~/finishedReplicas/replica0  ~/finishedReplicas/replica1  ~/finishedReplicas/replica2  ~/finishedReplicas/replica3
}

sleep_4min() {
  # tempo de execução
  sleep 1m
  echo 1 minute
  sleep 1m
  echo 2 minutes
  sleep 1m
  echo 3 minutes
  sleep 30s
  echo 3:30  encerrou!
}


zip_logs() {
  local wdir="$1"
  # tratando os logs
  echo zipando os logs para ${wdir}
  for i in {0..3} ; do
      ssh  cliente${i}  "cd /local/logs ; tar -czf ${wdir}/log_c${i}.tar.gz ./*; rm * ; exit" &
      ssh  replica${i}  "cd /local/logs ; tar -czf ${wdir}/log_r${i}.tar.gz ./*; rm * ; exit" &
  done
}

zip_results() {
  local wdir="$1"
  # tratando os resultados
  echo zipando os resultados para ${wdir}
  cd  ~/hybridpsmr/deploy || exit
  tar -czf ${wdir}/results.tar.gz  ./resultsCoin*
  rm ./resultsCoin*
}




agora=`date +"%y-%m-%d-%H-%M-%S"`
execDir=~/execution_${agora}
echo criando a pasta de execução "${execDir}"
mkdir ${execDir}

mkdir ~/finishedReplicas

NUM_THREADS_CLIENTE=600
contadorDeWorkload=0
contadorDeExecucao=0
workloads=('0 0' '0 5' '0 10' '5 10')  #percGlobal percWrite
for w in "${workloads[@]}" ; do
  particoes=(2 4 6 8)
  for p in "${particoes[@]}" ; do
    threads=(1 2 3 4)
    for LATE_WORKERS_PER_PARTITION in "${threads[@]}" ; do
      # servidores
      echo starting replicas
      for i in {0..3} ; do
        ssh  replica${i}  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.CoinHybridServiceReplica ${i} ${LATE_WORKERS_PER_PARTITION} ./warmup/warmup_p${p}.bin >& /local/logs/log_r${i}-p${p}-t${LATE_WORKERS_PER_PARTITION}-e${contadorDeExecucao}.txt || true; touch ~/finishedReplicas/replica${i} ; echo acabou replica${i}" &
        echo created replica${i}
      done
      sleep 30s
      echo starting clients
      for i in {0..3} ; do
        ssh  cliente${i}  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.CoinClient ${i} $((4001 + (i*1000))) ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin >& /local/logs/log_c${i}-p${p}-t${LATE_WORKERS_PER_PARTITION}-e${contadorDeExecucao}.txt" &
        echo created cliente${i}
      done
      # aguarda a execução por 3 minutos
      sleep_4min
      # mata tudo para começar denovo
      kill_clientes
      wait_replicas_end

      echo finished hibrid execution ${contadorDeExecucao}
      contadorDeExecucao=$((contadorDeExecucao + 1))
      echo
    done;
  done;

  #--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  if (( contadorDeWorkload < 3 ))
  then
    echo Realizando o teste do mesmo workload para o SMR SEQUENCIAL
    echo starting replicas
    for i in {0..3} ; do
      ssh  replica${i}  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.CoinSequentialServiceReplica ${i} ./warmup/warmup_p${p}.bin >& /local/logs/log_r${i}-p1-t1-e${contadorDeExecucao}.txt || true ; touch ~/finishedReplicas/replica${i} ; echo acabou a replica${i}" &
      echo created replica${i}
    done
    sleep 30s
    echo starting clients
    for i in {0..3} ; do
      ssh  cliente${i}  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.CoinClient ${i} $((4001 + (i*1000))) ${NUM_THREADS_CLIENTE} ${w} ./warmup/warmup_p${p}.bin >& /local/logs/log_c${i}-p1-t1-e${contadorDeExecucao}.txt" &
      echo created cliente${i}
      sleep 3s
    done
    # aguarda a execução por 3 minutos
    sleep_4min
    # mata tudo para começar denovo
    wait_replicas_end
    kill_hybrid

  fi
  #--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  # criando pasta do workload
  workDir=${execDir}/w${contadorDeWorkload}
  mkdir ${workDir}

  # zipando os logs
  zip_logs ${workDir}

  # zipando os resultados
  zip_results ${workDir}

  # entrando no proximo workload
  contadorDeWorkload=$((contadorDeWorkload + 1))
done;
echo 'finished all'



