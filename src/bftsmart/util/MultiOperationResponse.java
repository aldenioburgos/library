/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bftsmart.util;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author eduardo
 */
public class MultiOperationResponse {

    public Response[] operations;

    public MultiOperationResponse(int number) {
        operations = new Response[number];
        for (int i = 0; i < operations.length; i++) {
            operations[i] = new Response();
        }
    }

    public MultiOperationResponse(byte[] buffer) {
        try (var in = new ByteArrayInputStream(buffer);
             var dis = new DataInputStream(in)) {
            this.operations = new Response[dis.readInt()];
            for (int i = 0; i < this.operations.length; i++) {
                this.operations[i] = new Response();
                this.operations[i].data = new byte[dis.readInt()];
                dis.readFully(this.operations[i].data);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isComplete() {
        for (Response operation : operations) {
            if (operation.data == null) {
                return false;
            }
        }
        return true;
    }

    public void add(int index, byte[] data) {
        this.operations[index].data = data;
    }

    public byte[] serialize() {
        try (var baos = new ByteArrayOutputStream();
             var oos = new DataOutputStream(baos)) {
            oos.writeInt(operations.length);
            for (int i = 0; i < operations.length; i++) {
                oos.writeInt(this.operations[i].data.length);
                oos.write(this.operations[i].data);
            }
            oos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public class Response {

        public volatile byte[] data;

        public Response() {
        }
        public Response(byte[] data) {
            this.data = data;
        }

    }

}
