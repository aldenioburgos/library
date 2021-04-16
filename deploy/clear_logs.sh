#!/bin/bash

nodes=(replica0 replica1 replica2 replica3 cliente0 cliente1 cliente2 cliente3)

for n in "${nodes[@]}" ; do
  ssh  ${n}  "rm -rf /local/logs/*"
done;
echo 'finished clearing logs'


