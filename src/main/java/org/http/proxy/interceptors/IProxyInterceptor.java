package org.http.proxy.interceptors;

import org.http.proxy.ProxyThread;

public interface IProxyInterceptor {

    public void on(ProxyThread thread) throws Exception;

}
