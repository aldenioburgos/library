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
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.SingleExecutable;
import parallelism.MessageContextPair;
import parallelism.ParallelServiceReplica;
import parallelism.SequentialServiceReplica;
import parallelism.late.CBASEServiceReplica;
import parallelism.late.COSType;
import parallelism.late.ConflictDefinition;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


public final class ListServerMP implements SingleExecutable {

    private List<Integer> l1 = new LinkedList<Integer>();
    private List<Integer> l2 = new LinkedList<Integer>();
    private List<Integer> l3 = new LinkedList<Integer>();
    private List<Integer> l4 = new LinkedList<Integer>();
    private List<Integer> l5 = new LinkedList<Integer>();
    private List<Integer> l6 = new LinkedList<Integer>();
    private List<Integer> l7 = new LinkedList<Integer>();
    private List<Integer> l8 = new LinkedList<Integer>();
    private int numberpartitions;
    private ServiceReplica replica;

    public ListServerMP(int id, int initThreads, int entries, int numberPartitions, boolean cbase) {

        this.numberpartitions = numberPartitions;

        if (initThreads <= 0) {
            System.out.println("Replica in sequential execution model.");

            replica = new SequentialServiceReplica(id, this, null);
        } else if (cbase) {
            System.out.println("Replica in parallel execution model (CBASE).");
            ConflictDefinition cd = new ConflictDefinition() {
                @Override
                public boolean isDependent(MessageContextPair r1, MessageContextPair r2) {

                    switch (r1.classId) {
                        case MultipartitionMapping.GR:
                            if (r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.W1
                                    || r2.classId == MultipartitionMapping.W2
                                    || r2.classId == MultipartitionMapping.W3
                                    || r2.classId == MultipartitionMapping.W4
                                    || r2.classId == MultipartitionMapping.W5
                                    || r2.classId == MultipartitionMapping.W6
                                    || r2.classId == MultipartitionMapping.W7
                                    || r2.classId == MultipartitionMapping.W8) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R1:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W1) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R2:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W2) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R3:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W3) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R4:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W4) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R5:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W5) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R6:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W6) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R7:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W7) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.R8:
                            if (r2.classId == MultipartitionMapping.GW || r2.classId == MultipartitionMapping.W8) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.GW:
                            return true;
                        case MultipartitionMapping.W1:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R1
                                    || r2.classId == MultipartitionMapping.W1) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.W2:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R2
                                    || r2.classId == MultipartitionMapping.W2) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.W3:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R3
                                    || r2.classId == MultipartitionMapping.W3) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.W4:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R4
                                    || r2.classId == MultipartitionMapping.W4) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.W5:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R5
                                    || r2.classId == MultipartitionMapping.W5) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.W6:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R6
                                    || r2.classId == MultipartitionMapping.W6) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.W7:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R7
                                    || r2.classId == MultipartitionMapping.W7) {
                                return true;
                            }
                            break;
                        case MultipartitionMapping.W8:
                            if (r2.classId == MultipartitionMapping.GR
                                    || r2.classId == MultipartitionMapping.GW
                                    || r2.classId == MultipartitionMapping.R8
                                    || r2.classId == MultipartitionMapping.W8) {
                                return true;
                            }
                            break;
                        default:
                            break;
                    }
                    return false;
                }
            };
            replica = new CBASEServiceReplica(id, this, null, initThreads, cd, COSType.coarseLockGraph);
        } else {
            System.out.println("Replica in parallel execution model.");

            if (numberPartitions == 1) {

            } else if (numberPartitions == 2) {
                if (initThreads == 2) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P2T2());
                } else if (initThreads == 4) {
                    System.out.println("4T-2S normal");
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P2T4());
                } else if (initThreads == 8) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P2T8TunnedR1());
                } else if (initThreads == 10) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getP2T10());
                } else {
                    initThreads = 12;
                    System.out.println("Vai testar RW");
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P2T12RW());
                }
            } else if (numberPartitions == 4) {
                if (initThreads == 2) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P4T2());
                } else if (initThreads == 4) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P4T4());
                } else if (initThreads == 8) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P4T8());
                } else if (initThreads == 10) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getP4T10());
                } else {
                    initThreads = 12;
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getM2P4T12());
                }
            } else if (numberPartitions == 6) {
                if (initThreads == 6) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getP6T6());
                } else if (initThreads == 12) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getNaiveP6T12());
                } else if (initThreads == 10) {
                    replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getP6T10());
                } else {
                    System.exit(0);
                }
            } else if (initThreads == 8) {
                replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getP8T8());
            } else if (initThreads == 10) {
                replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getP8T10());
            } else if (initThreads == 16) {
                System.out.println("Naive 16T-8S");
                replica = new ParallelServiceReplica(id, this, null, initThreads, MultipartitionMapping.getNaiveP8T16());
            } else {
                System.exit(0);
            }

        }

        for (int i = 0; i < entries; i++) {
            l1.add(i);
            l2.add(i);
            l3.add(i);
            l4.add(i);
            l5.add(i);
            l6.add(i);
            l7.add(i);
            l8.add(i);
        }

        System.out.println("Server initialization complete!");
    }

    public byte[] executeOrdered(byte[] command, MessageContext msgCtx) {
        return execute(command, msgCtx);
    }

    public byte[] executeUnordered(byte[] command, MessageContext msgCtx) {
        return execute(command, msgCtx);
    }

    public boolean add(Integer value, int pId) {
        boolean ret = false;
        if (pId == MultipartitionMapping.W1) {
            if (!l1.contains(value)) {
                ret = l1.add(value);
            }
            return ret;

        } else if (pId == MultipartitionMapping.W2) {
            if (!l2.contains(value)) {
                ret = l2.add(value);
            }
            return ret;
        } else if (pId == MultipartitionMapping.W3) {
            if (!l3.contains(value)) {
                ret = l3.add(value);
            }
            return ret;

        } else if (pId == MultipartitionMapping.W4) {
            if (!l4.contains(value)) {
                ret = l4.add(value);
            }
            return ret;

        } else if (pId == MultipartitionMapping.W5) {
            if (!l5.contains(value)) {
                ret = l5.add(value);
            }
            return ret;
        } else if (pId == MultipartitionMapping.W6) {
            if (!l6.contains(value)) {
                ret = l6.add(value);
            }
            return ret;
        } else if (pId == MultipartitionMapping.W7) {
            if (!l7.contains(value)) {
                ret = l7.add(value);
            }
            return ret;
        } else if (pId == MultipartitionMapping.W8) {
            if (!l8.contains(value)) {
                ret = l8.add(value);
            }
            return ret;
        } else if (pId == MultipartitionMapping.GW) {

            if (!l1.contains(value)) {
                ret = l1.add(value);
            }
            if (!l2.contains(value)) {
                ret = l2.add(value);
            }
            if (numberpartitions >= 4) {
                if (!l3.contains(value)) {
                    ret = l3.add(value);
                }
                if (!l4.contains(value)) {
                    ret = l4.add(value);
                }

                if (numberpartitions >= 6) {

                    if (!l5.contains(value)) {
                        ret = l5.add(value);
                    }
                    if (!l6.contains(value)) {
                        ret = l6.add(value);
                    }

                    if (numberpartitions >= 8) {

                        if (!l7.contains(value)) {
                            ret = l7.add(value);
                        }
                        if (!l8.contains(value)) {
                            ret = l8.add(value);
                        }
                    }
                }
            }

            return ret;

        }
        return ret;
    }

    public byte[] execute(byte[] command, MessageContext msgCtx) {

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(command);
            ByteArrayOutputStream out = null;
            byte[] reply = null;
            int cmd = new DataInputStream(in).readInt();

            switch (cmd) {
                case MultipartitionMapping.W1:
                    Integer value = (Integer) new ObjectInputStream(in).readObject();
                    boolean ret = add(value, MultipartitionMapping.W1);
                    out = new ByteArrayOutputStream();
                    ObjectOutputStream out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.W2:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.W2);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.W3:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.W3);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.W4:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.W4);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.W5:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.W5);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.W6:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.W6);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.W7:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.W7);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.W8:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.W8);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.GW:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    ret = add(value, MultipartitionMapping.GW);
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(ret);
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.R1:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l1.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.R2:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l2.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.R3:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l3.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
                case MultipartitionMapping.R4:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l4.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.R5:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l5.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.R6:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l6.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.R7:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l7.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.R8:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    out1.writeBoolean(l8.contains(value));
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;

                case MultipartitionMapping.GR:
                    value = (Integer) new ObjectInputStream(in).readObject();
                    out = new ByteArrayOutputStream();
                    out1 = new ObjectOutputStream(out);
                    if (this.numberpartitions == 2) {
                        if (l1.contains(value) && l2.contains(value)) {
                            out1.writeBoolean(true);
                        } else {
                            out1.writeBoolean(false);
                        }
                    } else if (this.numberpartitions == 4) {
                        if (l1.contains(value) && l2.contains(value) && l3.contains(value) && l4.contains(value)) {
                            out1.writeBoolean(true);
                        } else {
                            out1.writeBoolean(false);
                        }

                    } else if (this.numberpartitions == 6) {
                        if (l1.contains(value) && l2.contains(value) && l3.contains(value) && l4.contains(value)
                                && l5.contains(value) && l6.contains(value)) {
                            out1.writeBoolean(true);
                        } else {
                            out1.writeBoolean(false);
                        }
                    } else { // 8 partitions 
                        if (l1.contains(value) && l2.contains(value) && l3.contains(value) && l4.contains(value)
                                && l5.contains(value) && l6.contains(value) && l7.contains(value) && l8.contains(value)) {
                            out1.writeBoolean(true);
                        } else {
                            out1.writeBoolean(false);
                        }
                    }
                    out.flush();
                    out1.flush();
                    reply = out.toByteArray();
                    break;
            }
            return reply;
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(ListServerMP.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }

    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: ... ListServer <processId> <number partitions> <initialNum threads> <initial entries> <CBASE?>");
            System.exit(-1);
        }

        int processId = Integer.parseInt(args[0]);
        int part = Integer.parseInt(args[1]);
        int initialNT = Integer.parseInt(args[2]);
        int entries = Integer.parseInt(args[3]);
        boolean cbase = Boolean.parseBoolean(args[4]);

        new ListServerMP(processId, initialNT, entries, part, cbase);
    }

}
