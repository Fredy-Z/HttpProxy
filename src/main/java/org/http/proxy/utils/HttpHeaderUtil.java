package org.http.proxy.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.http.proxy.ConstantsAware;
import org.http.proxy.models.KeyValuePair;

public class HttpHeaderUtil implements ConstantsAware {

    public static void sendHeaders(OutputStream output, List<KeyValuePair<String, String>> pairs) throws IOException {
        for (KeyValuePair<String, String> pair : pairs) {
            String key = pair.getKey();
            String value = pair.getValue();
            output.write(key.getBytes());
            output.write(':');
            output.write(' ');
            output.write(value.getBytes());
            output.write(CRLF.getBytes());
        }
    }

    public static boolean setHeader(List<KeyValuePair<String, String>> headerList, String name, Object value) {
        boolean found = false;
        for (KeyValuePair<String, String> pair : headerList) {
            if (pair.getKey().equalsIgnoreCase(name)) {
                pair.setValue(value.toString());
                found = true;
            }
        }
        if (!found) {
            headerList.add(new KeyValuePair<>(name, value.toString()));
        }
        return found;
    }

    public static void parseHeaders(List<KeyValuePair<String, String>> headers, List<String> lines) {
        headers.clear();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            int index = line.indexOf(':');
            if (index > 0) {
                headers.add(new KeyValuePair<>(line.substring(0, index), line.substring(index + 1).trim()));
            }
        }
    }

    public static int removeHeader(List<KeyValuePair<String, String>> headerList, String name) {
        int count = 0;
        for (Iterator<KeyValuePair<String, String>> it = headerList.iterator(); it.hasNext(); ) {
            if (it.next().getKey().equalsIgnoreCase(name)) {
                it.remove();
                count++;
            }
        }
        return count;
    }

    public static String getHeaderValue(List<KeyValuePair<String, String>> headerList, String name) {
        for (KeyValuePair<String, String> pair : headerList) {
            if (pair.getKey().equalsIgnoreCase(name)) {
                String value = pair.getValue();
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    public static List<String> readHeaders(InputStream input)
            throws IOException {
        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        List<String> headerLines = new ArrayList<>();

        int n;
        int start = 0;
        while ((n = input.read()) != -1) {
            outputBuffer.write(n);
            if (n == '\r') {
                n = input.read();
                outputBuffer.write(n);
                if (n == '\n') {
                    int end = outputBuffer.size() - 1;
                    byte[] buffer = outputBuffer.toByteArray();
                    String line = new String(buffer, start, end - start).trim();
                    if (line.equals("")) {
                        break;
                    }
                    headerLines.add(line);
                    start = end;
                }
            }
        }

        return headerLines;
    }
}
