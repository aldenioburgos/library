#!/bin/bash

check_logs() {
  nodes=(cliente0 cliente1 cliente2 cliente3 replica3 replica2 replica1 replica0)
  for n in "${nodes[@]}" ; do
    echo ${n}
    ssh  ${n}  "l /local/logs"
  done;
}

check_logs


