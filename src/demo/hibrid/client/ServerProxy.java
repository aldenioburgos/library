/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo.hibrid.client;

import demo.hibrid.request.Command;
import demo.hibrid.request.CommandResult;


/**
 * @author aldenio
 */
public interface ServerProxy {


    CommandResult[] execute(int clientProcessId, int id, Command... commands) throws InterruptedException;

}
