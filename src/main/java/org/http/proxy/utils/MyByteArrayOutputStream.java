package org.http.proxy.utils;

import java.io.ByteArrayOutputStream;

public class MyByteArrayOutputStream extends ByteArrayOutputStream {

    public MyByteArrayOutputStream() {
        super();
    }

    public MyByteArrayOutputStream(int size) {
        super(size);
    }
    
    public byte[] getBuffer() {
        return super.buf;
    }

    public static void main(String[] args) {
        ByteArrayOutputStream baot = new ByteArrayOutputStream(10);
        baot.toByteArray();
    }
}
