/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bftsmart.tom.core;

import bftsmart.reconfiguration.ServerViewController;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.core.messages.TOMMessageType;
import bftsmart.tom.server.Recoverable;
import parallelism.ParallelServiceReplica;

import java.util.concurrent.BrokenBarrierException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a thread which will deliver totally ordered requests to
 * the application
 */
public final class ParallelDeliveryThread extends DeliveryThread {

    /**
     * Creates a new instance of DeliveryThread
     *
     * @param tomLayer TOM layer
     * @param receiver Object that receives requests from clients
     */
    public ParallelDeliveryThread(TOMLayer tomLayer, ServiceReplica receiver, Recoverable recoverer, ServerViewController controller) {
        super(tomLayer, receiver, recoverer, controller);
        System.out.println("BRTSmart Delivery Thread Created: "+toString());
    }

    private void processReconfigMessages(int consId) {
        awaitOnReceiverReconfBarrier();

        byte[] response = controller.executeUpdates(consId);
        TOMMessage[] dests = controller.clearUpdates();

        if (controller.getCurrentView().isMember(receiver.getId())) {
            for (TOMMessage dest : dests) {
                var targets = new int[]{dest.getSender()};
                var sender = controller.getStaticConf().getProcessId();
                var view = controller.getCurrentViewId();
                tomLayer.getCommunication().send(targets, new TOMMessage(sender, dest.getSession(), dest.getSequence(), response, view, TOMMessageType.RECONFIG));
            }
            tomLayer.getCommunication().updateServersConnections();
        } else {
            receiver.restart();
        }
        awaitOnReceiverReconfBarrier();
    }

    private void awaitOnReceiverReconfBarrier() {
        try {
            ((ParallelServiceReplica) this.receiver).getReconfBarrier().await();
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelDeliveryThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BrokenBarrierException ex) {
            Logger.getLogger(ParallelDeliveryThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
