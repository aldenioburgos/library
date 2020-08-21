package demo.hibrid.server;

import bftsmart.tom.ServiceProxy;
import demo.hibrid.request.Request;


public class ServiceProxyMock extends ServiceProxy  {

    private final HibridServiceReplica replica;

    public ServiceProxyMock(int processId, HibridServiceReplica replica) {
        super(processId);
        this.replica = replica;
    }

    @Override
    public byte[] invokeOrdered(byte[] requestBytes) {
        Request request = new Request().fromBytes(requestBytes);
        replica.processRequest(request);
        return new byte[0];
    }
}
