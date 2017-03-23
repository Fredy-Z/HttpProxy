package org.http.proxy.preprocessors.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.http.proxy.ConstantsAware;
import org.http.proxy.ProxyThread;
import org.http.proxy.preprocessors.IProxyPreprocessor;
import org.http.proxy.utils.HttpHeaderUtil;

public class RemoteMapPreprocessor implements IProxyPreprocessor, ConstantsAware {
    private Map<String, String> mapRemoteUriMap;

    public RemoteMapPreprocessor(Map<String, String> remoteUriMap) {
        this.mapRemoteUriMap = remoteUriMap;
    }

    @Override
    public void on(ProxyThread thread) {
        String currentUri = thread.requestFirstLine[1];
        boolean regexIsUsed = false;

        for (String uri : mapRemoteUriMap.keySet()) {
            String remoteUri = mapRemoteUriMap.get(uri);
            uri = StringUtils.trim(uri);
            if (StringUtils.endsWith(uri, "*")) {
                regexIsUsed = true;
                uri = StringUtils.removeEnd(uri, "*");
            }

            if (StringUtils.startsWith(currentUri, uri)) {
                if (regexIsUsed) {
                    String regexContent = StringUtils.remove(currentUri, uri);
                    remoteUri = remoteUri + regexContent;
                }
                thread.requestFirstLine[1] = remoteUri;

                remoteUri = StringUtils.removeStartIgnoreCase(remoteUri, "http://");
                String hostAndPort = StringUtils.substringBefore(remoteUri, "/");
                String path = "/" + StringUtils.substringAfter(remoteUri, "/");
                String host = StringUtils.substringBefore(hostAndPort, ":");
                Integer port = Integer.parseInt((StringUtils.contains(hostAndPort, ":") ? StringUtils.substringAfter(
                        hostAndPort, ":") : "80"));
                thread.uriToks[1] = host;
                thread.uriToks[2] = port;
                thread.uriToks[3] = path;
                HttpHeaderUtil.setHeader(thread.requestHeaders, Host, hostAndPort);

                break;
            }
        }
    }
}
