#!/bin/bash

check_java() {
  nodes=(cliente0 cliente1 cliente2 cliente3 replica3 replica2 replica1 replica0)
  for n in "${nodes[@]}" ; do
    echo ${n}
    ssh  ${n}  "java -version"
  done;
}

check_java


