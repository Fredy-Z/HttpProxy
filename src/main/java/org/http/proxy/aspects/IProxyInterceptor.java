package org.http.proxy.aspects;

import org.http.proxy.ProxyThread;
import org.http.proxy.models.HttpResponse;

public interface IProxyInterceptor {
    void on(ProxyThread thread, HttpResponse response) throws Exception;
}
