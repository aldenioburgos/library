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

#workloads=('0-0-0' '15-0-0')
#particoes=(1 2 4 6 8)
#threads=(1 2 4 6 8 10 12 14 16 18 20 24 32 56 64)
particoes=(2 4 6)
threads=(1 2 4 6 8 10 12 14 16 18 20 24 32 56 64)
sizes=(1000)
globais=(0 1 5)
locais=(0 10 20)


for t in "${threads[@]}" ; do
   for p in "${particoes[@]}" ; do
      for s in "${sizes[@]}" ; do
        for g in "${globais[@]}" ; do
           for l in "${locais[@]}" ; do
    	     java -jar dist/BFT-Hibrid.jar ${s} ${t} ${p} ${g} ${l} 10000000 150
	     echo 'finished'
	
done;
done;
done;
done;
done;


#mkdir 0_conflict
#mv results* 0_conflict

echo 'finished all'


