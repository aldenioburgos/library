/**
 * Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and
 * the authors indicated in the @author tags
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package demo.list;

import bftsmart.tom.MessageContext;
import bftsmart.tom.server.SingleExecutable;
import parallelism.MessageContextPair;
import parallelism.ParallelMapping;
import parallelism.ParallelServiceReplica;
import parallelism.SequentialServiceReplica;
import parallelism.late.CBASEServiceReplica;
import parallelism.late.COSType;
import parallelism.late.ConflictDefinition;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


public final class ListServer implements SingleExecutable {

    private List<Integer> l = new LinkedList<>();


    public ListServer(int id, int initThreads, int entries, boolean late, String gType) {
        if (initThreads <= 0) {
            System.out.println("Replica in sequential execution model.");
            new SequentialServiceReplica(id, this, null);
        } else if (late) {
            System.out.println("Replica in parallel execution model (late scheduling).");
            ConflictDefinition conflictDef = getConflictDefinition();
            switch (gType) {
                case "coarseLock":
                    new CBASEServiceReplica(id, this, null, initThreads, conflictDef, COSType.coarseLockGraph);
                    break;
                case "fineLock":
                    new CBASEServiceReplica(id, this, null, initThreads, conflictDef, COSType.fineLockGraph);
                    break;
                case "lockFree":
                    new CBASEServiceReplica(id, this, null, initThreads, conflictDef, COSType.lockFreeGraph);
                    break;
                default:
                    new CBASEServiceReplica(id, this, null, initThreads, conflictDef, null);
                    break;
            }
        } else {
            System.out.println("Replica in parallel execution model (early scheduling).");
            new ParallelServiceReplica(id, this, null, initThreads);
        }
        for (int i = 0; i < entries; i++) {
            l.add(i);
        }

        System.out.println("Server initialization complete!");
    }

    private ConflictDefinition getConflictDefinition() {
        return new ConflictDefinition() {
            @Override
            public boolean isDependent(MessageContextPair r1, MessageContextPair r2) {
                return (r1.classId == ParallelMapping.SYNC_ALL || r2.classId == ParallelMapping.SYNC_ALL);
            }
        };
    }


    public byte[] executeOrdered(byte[] command, MessageContext msgCtx) {
        return execute(command, msgCtx);
    }

    public byte[] executeUnordered(byte[] command, MessageContext msgCtx) {
        return execute(command, msgCtx);
    }

    private byte[] execute(byte[] command, MessageContext msgCtx) {
        System.out.println("ListServer.execute requested in " + Thread.currentThread());

        try (ByteArrayInputStream bais = new ByteArrayInputStream(command);
             ObjectInputStream input = new ObjectInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(baos)) {

            switch (input.readInt()) {
                case BFTList.ADD: {
                    Integer value = (Integer) input.readObject();
                    boolean ret = false;
                    if (!l.contains(value)) {
                        ret = l.add(value);
                    }
                    output.writeBoolean(ret);
                    break;
                }
                case BFTList.REMOVE: {
                    Integer value = (Integer) input.readObject();
                    boolean ret = l.remove(value);
                    output.writeBoolean(ret);
                    break;
                }
                case BFTList.SIZE:{
                    output.writeInt(l.size());
                    break;
                }
                case BFTList.CONTAINS: {
                    Integer value = (Integer) input.readObject();
                    output.writeBoolean(l.contains(value));
                    break;
                }
                case BFTList.GET: {
                    int index = input.readInt();
                    Integer r = (index > l.size()) ? Integer.valueOf(-1) : l.get(index);
                    output.writeObject(r);
                    break;
                }
            }
            output.flush();
            baos.flush();
            return baos.toByteArray();
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ListServer.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }


    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("Usage: ... ListServer <processId> <num threads> <initial entries> <late scheduling?> <graph type>");
            System.exit(-1);
        }
        int processId = Integer.parseInt(args[0]);
        int initialNT = Integer.parseInt(args[1]);
        int entries = Integer.parseInt(args[2]);
        boolean late = Boolean.parseBoolean(args[3]);
        String gType = args[4];

//        int processId = 0;
//        int initialNT = 10;
//        int entries = 10000;
//        boolean late = true;
//        String gType = "multipleLockFree";
        System.out.println("Main is " + Thread.currentThread());

        new ListServer(processId, initialNT, entries, late, gType);
    }

}
