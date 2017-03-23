package org.http.proxy.callbacks;

import org.http.proxy.ProxyThread;

public interface IProxyCallback {

    public void on(ProxyThread thread) throws Exception;
}
