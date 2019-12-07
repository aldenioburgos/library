/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.server.graph;

/**
 * @author aldenio
 */
public interface  ConflictDefinition<T> {

      boolean isDependent(T r1, T r2);

}
