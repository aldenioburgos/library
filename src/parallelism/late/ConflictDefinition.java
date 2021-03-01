/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism.late;

/**
 * @author eduardo
 */
public interface ConflictDefinition {

    boolean isDependent(Object r1, Object r2);

}
