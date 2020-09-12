package demo.hibrid.server;

import demo.hibrid.server.graph.LockFreeNode;

public interface HibridReplier {
    void manageReply(LockFreeNode node, boolean[] results) ;
}
