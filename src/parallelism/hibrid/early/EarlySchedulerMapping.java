package parallelism.hibrid.early;

import parallelism.ClassToThreads;

import java.util.*;

/**
 * @author eduardo
 */
public class EarlySchedulerMapping {

    public HibridClassToThreads[] CtoT = null;
    public int[] partitions;

    public HibridClassToThreads[] generateMappings(int numPartitions) {
        partitions = new int[numPartitions];
        for (int i = 0; i < numPartitions; i++) {
            partitions[i] = i;
        }
        return generateMappings(partitions);
    }

    public HibridClassToThreads[] generateMappings(int... partitions) {
        this.partitions = partitions;
        generate(partitions);
        return CtoT;
    }

    public int getClassId(int... partitions) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < partitions.length; j++) {
            sb.append(partitions[j]);
        }
        return sb.toString().hashCode();
    }


    public void generate(int[] status) {
        List<SortedSet<Comparable>> allCombList = new ArrayList<SortedSet<Comparable>>(); //aqui vai ficar a resposta

        for (int nstatus : status) {
            allCombList.add(new TreeSet<Comparable>(Arrays.asList(nstatus))); //insiro a combinação "1 a 1" de cada item
        }

        for (int nivel = 1; nivel < status.length; nivel++) {
            List<SortedSet<Comparable>> statusAntes = new ArrayList<SortedSet<Comparable>>(allCombList); //crio uma cópia para poder não iterar sobre o que já foi
            for (Set<Comparable> antes : statusAntes) {
                SortedSet<Comparable> novo = new TreeSet<Comparable>(antes); //para manter ordenado os objetos dentro do set
                novo.add(status[nivel]);
                if (!allCombList.contains(novo)) { //testo para ver se não está repetido
                    allCombList.add(novo);
                }
            }
        }

        Collections.sort(allCombList, new Comparator<SortedSet<Comparable>>() { //aqui só para organizar a saída de modo "bonitinho"

            @Override
            public int compare(SortedSet<Comparable> o1, SortedSet<Comparable> o2) {
                int sizeComp = o1.size() - o2.size();
                if (sizeComp == 0) {
                    Iterator<Comparable> o1iIterator = o1.iterator();
                    Iterator<Comparable> o2iIterator = o2.iterator();
                    while (sizeComp == 0 && o1iIterator.hasNext()) {
                        sizeComp = o1iIterator.next().compareTo(o2iIterator.next());
                    }
                }
                return sizeComp;

            }
        });


        this.CtoT = new HibridClassToThreads[allCombList.size()];
        for (int i = 0; i < this.CtoT.length; i++) {
            Object[] ar = ((TreeSet) allCombList.get(i)).toArray();
            int[] ids = new int[ar.length];
            for (int j = 0; j < ids.length; j++) {
                ids[j] = Integer.parseInt(ar[j].toString());
            }
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < ids.length; j++) {
                sb.append(ids[j]);
            }
            int type = ClassToThreads.CONC;
            if (ids.length > 1) {
                type = ClassToThreads.SYNC;
            }

            this.CtoT[i] = new HibridClassToThreads(sb.toString().hashCode(), type, ids);
        }
    }

}
