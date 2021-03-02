/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package parallelism.late;

/**
 *
 * @author eduardo
 */
public class DefaultConflictDefinition implements ConflictDefinition {

    public boolean isDependent(Object r1, Object r2) {
        return true;
    }
    
    
    
}
