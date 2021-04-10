
nodes=('replica0' 'replica1' 'replica2' 'cliente0' 'cliente1' 'cliente2' 'cliente3') #perc_global perc_write



for n in "${nodes[@]}" ; do
# executa experimento hybrid:
ssh  ${n}  "sudo ln -s /users/aldenio/jdk-15.0.2/bin/jar /usr/bin/jar; sudo ln -s /users/aldenio/jdk-15.0.2/bin/java /usr/bin/java; sudo ln -s /users/aldenio/jdk-15.0.2/bin/javac /usr/bin/javac"
ssh  ${n}  "java -version > ${n}.txt"

done;

echo 'finished setup'


