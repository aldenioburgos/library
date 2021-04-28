#!/bin/bash

sudo rm /usr/bin/java;
if [[ ! -d /local/jdk-15.0.2 ]]
then
  cp -r ~/jdk-15.0.2 /local
fi
sudo ln -s /local/jdk-15.0.2/bin/java /usr/bin/java;
java -version

commands=`cat <<EOF
sudo rm /usr/bin/java;
[ ! -d /local/jdk-15.0.2 ] &&  cp -r ~/jdk-15.0.2 /local
sudo ln -s /local/jdk-15.0.2/bin/java /usr/bin/java;
java -version
EOF
`


nodes=(replica0 replica1 replica2 replica3 cliente0 cliente1 cliente2 cliente3)
for n in "${nodes[@]}" ; do
  echo  ${n}
  ssh  ${n}  "${commands}" &
done;
echo 'finished setup'
