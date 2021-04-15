#!/bin/bash

sudo ln -s /users/aldenio/jdk-15.0.2/bin/jar /usr/bin/jar;
sudo ln -s /users/aldenio/jdk-15.0.2/bin/java /usr/bin/java;
sudo ln -s /users/aldenio/jdk-15.0.2/bin/javac /usr/bin/javac; java -version

nodes=(replica0 replica1 replica2 replica3 cliente0 cliente1 cliente2 cliente3)
for n in "${nodes[@]}" ; do
  ssh  ${n}  "sudo ln -s /users/aldenio/jdk-15.0.2/bin/jar /usr/bin/jar; sudo ln -s /users/aldenio/jdk-15.0.2/bin/java /usr/bin/java; sudo ln -s /users/aldenio/jdk-15.0.2/bin/javac /usr/bin/javac; java -version"
done;
echo 'finished setup'


