# Copyright (c) 2018-2019 Eliã Batista
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#particoes=(1 2 4 6 8)
#threads=(1 2 4 6 8 10 12 14 16 18 20 24 32 56 64)
particoes=(1 2 4 6 8)
threads=(2 4 6)
workloads=('0 0' '5 10') #perc_global perc_write
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
ssh  replica0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 0 ${LATE_WORKERS_PER_PARTITION}" &
sleep 3s
ssh  replica1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 1 ${LATE_WORKERS_PER_PARTITION}" &
sleep 3s
ssh  replica2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Server} 2 ${LATE_WORKERS_PER_PARTITION}" &
sleep 3s
ssh  cliente0  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
sleep 3s
ssh  cliente1  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
sleep 3s
ssh  cliente2  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
sleep 3s
ssh  cliente3  "cd ~/hybridpsmr/deploy; java -classpath psmr.jar demo.coin.${Client} ${NUM_THREADS_CLIENTE} ${NUM_OPERACOES_PER_CLIENTE} ${NUM_OPS_PER_REQ} ${w} warmup_p${p}_.bin" &
sleep 3m
ssh   replica2 "pkill -f 'java.*bft-smart*'"
echo 'replica2 killed'
ssh  replica1 "pkill -f 'java.*bft-smart*'"
echo 'replica1 killed'
ssh  replica0 "pkill -f 'java.*bft-smart*'"
echo 'replica0 killed'
ssh  cliente0  "pkill -f 'java.*bft-smart*'"
echo 'cliente0 killed'
ssh  cliente1  "pkill -f 'java.*bft-smart*'"
echo 'cliente1 killed'
ssh  cliente2  "pkill -f 'java.*bft-smart*'"
echo 'cliente2 killed'
ssh  cliente3  "pkill -f 'java.*bft-smart*'"
echo 'cliente3 killed'
echo 'finished hibrid'
sleep 3s

	#params: 'w-w-w-shards-threads-size-skewed-experiment-replicas-generateReport-totalExp-execDuration-clients-copyFiles generateConsolidatedFiles-warmup'
	#ssh  elia@replica0 "cd workstealing; java -classpath psmr.jar bftsmart.util.ConsolidateFiles '${w}-${p}-${t}-${s}-${skew}-1-3-false-1-${intervalMinutes}-${clients}-true-false-${warmup}'"
#sleep 1s
#echo 'files copied to experiment directory'

#ssh  elia@replica0 "pkill -f 'java.*bft-smart*'"
#echo 'replica0 killed'
#sleep 5s

#done;

done;
done;

echo mkdir "hybrid_${w}"
mv results* "hybrid_${w}"

done;



echo 'finished all'


