package org.http.proxy.aspects.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.http.proxy.ConstantsAware;
import org.http.proxy.ProxyThread;
import org.http.proxy.aspects.IProxyInterceptor;
import org.http.proxy.models.HttpResponse;
import org.http.proxy.utils.GarUtils;
import org.http.proxy.utils.HttpHeaderUtil;

public class GZIPInterceptor implements IProxyInterceptor, ConstantsAware {
    private static final int MaxSize = 1024 * 1024 * 2;
    Logger logger = Logger.getLogger(GZIPInterceptor.class);

    @Override
    public void on(ProxyThread thread, HttpResponse tempResponse) {
        try {
            String responseCodeStr = thread.responseFirstLine[1];
            if (responseCodeStr != null && !responseCodeStr.equals("") && StringUtils.isNumeric(responseCodeStr)) {
                int code = Integer.parseInt(responseCodeStr);
                if (code == 200) {
                    String contentLength = HttpHeaderUtil.getHeaderValue(thread.responseHeaders, CONTENT_LENGTH);
                    String contentEncoding = HttpHeaderUtil.getHeaderValue(thread.responseHeaders, CONTENT_ENCODING);
                    String transferEncoding = HttpHeaderUtil.getHeaderValue(thread.responseHeaders, TRANSFER_ENCODING);
                    if (tempResponse != null && tempResponse.getInput() != null) {
                        // is normal gzip
                        if ("gzip".equalsIgnoreCase(contentEncoding) && !(transferEncoding != null && transferEncoding.equals("chunked"))) {
                            // int length = Integer.parseInt(contentLength);
                            // if (length <= MaxSize) {
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            int size = 0;
                            try {
                                InputStream input = tempResponse.getInput();
                                size = GarUtils.transfer(input, buffer).length;
                            } catch (Exception e) {
                                logger.error("Wrong gzip format data.");
                                logger.error(e.getMessage());
                                logger.error(thread.uriToks[1]);
                                logger.error(thread.requestFirstLine[1]);
                            }
                            tempResponse.setBody(buffer.toByteArray());
                            HttpHeaderUtil.removeHeader(thread.responseHeaders, CONTENT_ENCODING);
                            HttpHeaderUtil.setHeader(thread.responseHeaders, CONTENT_LENGTH, size);

                            // is chunked
                        } else if (transferEncoding != null && transferEncoding.equals("chunked")) {
                            int totalSize = 0;
                            byte[] unchunkedDataBuffer = new byte[MaxSize];
                            int offset = 0;
                            int chunkSize = 0;

                            InputStream chunkedInput = tempResponse.getInput();

                            chunkSize = readChunkSize(chunkedInput);
                            while (chunkSize != 0 && (chunkSize != -1)) {
                                totalSize += chunkSize;
                                readChunkData(chunkedInput, unchunkedDataBuffer, offset, chunkSize);
                                offset += chunkSize;
                                // read CRLF
                                int cr = chunkedInput.read();
                                int lf = chunkedInput.read();

                                chunkSize = readChunkSize(chunkedInput);
                            }
                            ByteArrayInputStream unchunkedDataBufferStream = new ByteArrayInputStream(unchunkedDataBuffer, 0, totalSize);

                            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                                transferGZIPInputStream(unchunkedDataBufferStream, thread, tempResponse);
                            } else {
                                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                                int size = GarUtils.transfer(unchunkedDataBufferStream, buffer).length;
                                tempResponse.setBody(buffer.toByteArray());
                                HttpHeaderUtil.removeHeader(thread.responseHeaders, TRANSFER_ENCODING);
                                HttpHeaderUtil.setHeader(thread.responseHeaders, CONTENT_LENGTH, size);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void transferGZIPInputStream(InputStream gZIPInputStream, ProxyThread proxyThread, HttpResponse tempResponse) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        GZIPInputStream input = new GZIPInputStream(gZIPInputStream);
        int size = GarUtils.transfer(input, buffer).length;
        tempResponse.setBody(buffer.toByteArray());
        HttpHeaderUtil.removeHeader(proxyThread.responseHeaders, CONTENT_ENCODING);
        HttpHeaderUtil.removeHeader(proxyThread.responseHeaders, TRANSFER_ENCODING);
        HttpHeaderUtil.setHeader(proxyThread.responseHeaders, CONTENT_LENGTH, size);
    }

    private int readChunkSize(InputStream input) throws IOException {
        byte[] chunkSizeBuffer = new byte[1024];
        int b;
        int index = 0;
        while ((b = input.read()) != -1) {
            // end of chunk
            if (b == 0)
                return 0;

            if (b != '\r') {
                chunkSizeBuffer[index] = (byte) b;
                index++;
            } else {
                // read the extra LF character
                input.read();
                String chunkSizeStr = new String(chunkSizeBuffer, 0, index, "US-ASCII");
                int chunkSize = Integer.parseInt(chunkSizeStr.trim(), 16);
                return chunkSize;
            }
        }
        return -1;
    }

    private void readChunkData(InputStream input, byte[] unchunkedDataBuffer, int offset, int length) throws IOException {
        int readSize = 0;
        while (readSize < length) {
            readSize += input.read(unchunkedDataBuffer, offset + readSize, length - readSize);
        }
    }
}
