#!/bin/bash

kill_hybrid() {
  nodes=(cliente0 cliente1 cliente2 cliente3)
  for n in "${nodes[@]}" ; do
    ssh  ${n}  "pkill -f java ; exit"  &
    echo ${n} killed
  done;
  sleep 10s
  echo 'finished killing all'
}

kill_hybrid
