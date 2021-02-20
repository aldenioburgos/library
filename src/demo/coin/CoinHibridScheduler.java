package demo.coin;

import bftsmart.tom.core.messages.TOMMessage;
import parallelism.MultiOperationCtx;
import parallelism.hibrid.early.HibridClassToThreads;
import parallelism.hibrid.early.HibridScheduler;
import parallelism.hibrid.early.TOMMessageWrapper;
import parallelism.hibrid.late.HibridLockFreeNode;
import parallelism.late.graph.Vertex;

import java.util.Queue;

public class CoinHibridScheduler extends HibridScheduler {

    public CoinHibridScheduler(int numberOfPartitions, HibridClassToThreads[] cToT, int queuesCapacity) {
        super(numberOfPartitions, cToT, queuesCapacity);
    }

    @Override
    public void schedule(TOMMessage request) {
        HibridClassToThreads ct = this.getClass(request.groupId);
        if (ct == null) {
            System.err.println("CLASStoTHREADs MAPPING NOT FOUND");

        } else if (ct.type == HibridClassToThreads.CONC) {//conc (só tem uma thread, então vai ser sempre na posição 0)
            boolean inserted = false;
            while (!inserted) {
                inserted = ct.queues[0].offer(request);
            }

        } else { //sync (adicionar em todas as filas)... ja cria o node FAZER O BATCH EM UM UNICO NODE AQUI

            CoinMultiOperationRequest reqs = new CoinMultiOperationRequest(request.getContent()); //TODO examinar se a deserialização poderia ser feita depois.
            MultiOperationCtx ctx = new MultiOperationCtx(reqs.getNumOperations(), request);

            for (int i = 0; i < reqs.getNumOperations(); i++) {
                TOMMessageWrapper mw = new TOMMessageWrapper(new CoinMessageContextPair(request, request.groupId, i, reqs.getOperations()[i], reqs.opId, ctx));
                mw.msg.node = new HibridLockFreeNode(mw.msg, Vertex.MESSAGE, null, getAllQueues().length, ct.tIds.length);
                mw.msg.threadId = ct.tIds[ct.threadIndex];
                ct.threadIndex = (ct.threadIndex + 1) % ct.tIds.length;

                for (Queue q : ct.queues) {
                    boolean inserted = false;
                    while (!inserted) {
                        inserted = q.offer(mw);
                    }
                }
            }
        }
    }
}
