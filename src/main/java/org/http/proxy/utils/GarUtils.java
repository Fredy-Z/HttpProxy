package org.http.proxy.utils;

import java.io.*;

import org.apache.commons.lang.ArrayUtils;

public class GarUtils {
    public static byte[] transfer(InputStream input, OutputStream outputStream) throws IOException {
        return transfer(input, outputStream, Long.MAX_VALUE);
    }

    public static byte[] transfer(InputStream input, OutputStream output, long size) throws IOException {
        byte[] total = {};
        byte[] buffer = new byte[204800];
        int bufferSize;
        int loopTimes = 0;

        BufferedInputStream bufferedInput = new BufferedInputStream(input);
        while (size > 0 && (bufferSize = bufferedInput.read(buffer, 0, buffer.length)) != -1) {
            buffer = ArrayUtils.subarray(buffer, 0, bufferSize);
            loopTimes++;
            if (loopTimes == 1) {
                total = buffer;
            } else {
                total = ArrayUtils.addAll(total, buffer);
            }

            output.write(buffer, 0, bufferSize);
            buffer = new byte[204800];
            size -= bufferSize;
        }
        return total;
    }
}
