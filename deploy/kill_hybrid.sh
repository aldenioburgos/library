#!/bin/bash

kill_hybrid() {
  nodes=(cliente0 cliente1 cliente2 cliente3 replica3 replica2 replica1 replica0)
  for n in "${nodes[@]}" ; do
    ssh  ${n}  "pkill -f java"
    echo ${n} killed
  done;
  echo 'finished killing all'
}

