particoes=(1 2 4 6 8)
for p in "${particoes[@]}" ; do
#  if [ "$1" = "c" ]
#  then
#     NUM_THREADS_CLIENTE=200
#     NUM_OPERACOES_PER_CLIENTE=1000
#     NUM_OPS_PER_REQ=50
#     PERC_GLOBAL=0
#     PERC_WRITE=0
#    java -classpath psmr.jar  demo.coin.CoinClient $NUM_THREADS_CLIENTE $PERC_GLOBAL $PERC_WRITE $WARM_UP_FILE
#  fi
#
#  if [ "$1" = "r" ]  ||  [ "$1" = "s" ]
#  then
#    ID=$2
#    LATE_WORKERS_PER_PARTITION=4
#    java -classpath psmr.jar  demo.coin.CoinHybridServiceReplica $ID $LATE_WORKERS_PER_PARTITION $WARM_UP_FILE
#  fi
#
#  if [ "$1" = "w" ]
#  then
     NUM_USUARIOS=1000
     NUM_TOKENS_USUARIO=10
     NUM_PARTICOES=${p}
     OUTPUT_FILE_PATH=$5
    echo java -classpath psmr.jar  demo.coin.WarmUp $NUM_USUARIOS $NUM_TOKENS_USUARIO $NUM_PARTICOES warmup_p${p}.bin
    java -classpath psmr.jar  demo.coin.WarmUp $NUM_USUARIOS $NUM_TOKENS_USUARIO $NUM_PARTICOES warmup_p${p}.bin
#  fi
done;

echo fim.
