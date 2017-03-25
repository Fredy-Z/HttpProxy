package org.http.proxy.aspects.impl;

import java.io.*;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.http.proxy.ConstantsAware;
import org.http.proxy.ProxyThread;
import org.http.proxy.aspects.IProxyInterceptor;
import org.http.proxy.models.HttpResponse;
import org.http.proxy.utils.GarUtils;
import org.http.proxy.utils.HttpHeaderUtil;

public class LocalMapInterceptor implements IProxyInterceptor, ConstantsAware {

    private Map<String, String> mapLocalMap;

    public LocalMapInterceptor(Map<String, String> mapLocalMap) {
        this.mapLocalMap = mapLocalMap;
    }

    @Override
    public void on(ProxyThread thread, HttpResponse tempResponse) throws Exception {
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


                int size;
                InputStream input = null;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try {
                    input = new FileInputStream(localFile);
                    size = GarUtils.transfer(input, buffer).length;
                } finally {
                    if (input != null)
                        input.close();
                }
                tempResponse.setBody(buffer.toByteArray());
                HttpHeaderUtil.removeHeader(thread.responseHeaders, CONTENT_ENCODING);
                HttpHeaderUtil.setHeader(thread.responseHeaders, CONTENT_LENGTH, size);
                thread.responseFirstLine[1] = "200";
                thread.responseFirstLine[2] = "OK";

                break;
            }
        }
    }
}
