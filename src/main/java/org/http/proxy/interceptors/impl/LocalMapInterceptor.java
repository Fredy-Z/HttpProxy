package org.http.proxy.interceptors.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.http.proxy.ConstantsAware;
import org.http.proxy.ProxyThread;
import org.http.proxy.interceptors.IProxyInterceptor;
import org.http.proxy.utils.GarUtils;
import org.http.proxy.utils.HttpHeaderUtil;
import org.http.proxy.utils.MyByteArrayOutputStream;

public class LocalMapInterceptor implements IProxyInterceptor, ConstantsAware {

    private Map<String, String> mapLocalMap;

    public LocalMapInterceptor(Map<String, String> mapLocalMap) {
        this.mapLocalMap = mapLocalMap;
    }

    @Override
    public void on(ProxyThread thread) throws Exception {
        String currentUri = thread.requestFirstLine[1];

        for (String uri : mapLocalMap.keySet()) {
            String localPath = mapLocalMap.get(uri);
            uri = StringUtils.trim(uri);
            if (StringUtils.endsWith(uri, "*")) {
                uri = StringUtils.removeEnd(uri, "*");
            }

            if (StringUtils.startsWith(currentUri, uri)) {
                File localFile = new File(localPath);
                if (!localFile.exists()) {
                    break;
                }

                MyByteArrayOutputStream buffer = thread.outputBuf;
                buffer.reset();
                int size;
                InputStream input = null;
                try {
                    input = new FileInputStream(localFile);
                    size = GarUtils.transfer(input, buffer).length;
                } finally {
                    if (input != null)
                        input.close();
                }
                thread.responseInput = new ByteArrayInputStream(buffer.getBuffer(), 0, size);
                HttpHeaderUtil.removeHeader(thread.responseHeaders, ContentEncoding);
                HttpHeaderUtil.setHeader(thread.responseHeaders, ContentLength, size);
                thread.responseFirstLine[1] = "200";
                thread.responseFirstLine[2] = "OK";

                break;
            }
        }
    }
}
