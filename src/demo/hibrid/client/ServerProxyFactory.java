package demo.hibrid.client;

import java.lang.reflect.InvocationTargetException;

public class ServerProxyFactory {

    private Class<? extends ServerProxy> serverProxyClass;
    private ServerProxy serverProxy;

    public ServerProxyFactory(ServerProxy serverProxy) {
        this.serverProxy = serverProxy;
    }

    public ServerProxyFactory(Class<? extends ServerProxy> serverProxyClass) {
        this.serverProxyClass = serverProxyClass;
    }

    public ServerProxy getServerProxy(int id) {
        try {
            if (serverProxy != null) return serverProxy;
            else return serverProxyClass.getDeclaredConstructor(int.class).newInstance(id);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
