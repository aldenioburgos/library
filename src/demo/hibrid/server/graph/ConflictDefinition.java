package demo.hibrid.server.graph;

/**
 * @author aldenio
 */
public interface  ConflictDefinition<T> {

      boolean isDependent(T r1, T r2);

}
