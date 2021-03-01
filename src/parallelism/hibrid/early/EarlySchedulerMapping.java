package parallelism.hibrid.early;

import parallelism.ClassToThreads;

import java.util.*;

/**
 * @author eduardo
 */
public class EarlySchedulerMapping {

    public HibridClassToThreads[] CtoT = null;
    private int numPartitions;
    public int[] partitions;

    public HibridClassToThreads[] generateMappings(int numPartitions) {
        this.numPartitions = numPartitions;
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
        List<SortedSet<Integer>> allCombList = new ArrayList<>(); //aqui vai ficar a resposta

        for (int nstatus : status) {
            allCombList.add(new TreeSet<>(List.of(nstatus))); //insiro a combinação "1 a 1" de cada item
        }

        for (int nivel = 1; nivel < status.length; nivel++) {
            List<SortedSet<Integer>> statusAntes = new ArrayList<>(allCombList); //crio uma cópia para poder não iterar sobre o que já foi
            for (Set<Integer> antes : statusAntes) {
                SortedSet<Integer> novo = new TreeSet<>(antes); //para manter ordenado os objetos dentro do set
                novo.add(status[nivel]);
                if (!allCombList.contains(novo)) { //testo para ver se não está repetido
                    allCombList.add(novo);
                }
            }
        }

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
