package org.http.proxy.aspects;

import org.http.proxy.ProxyThread;

public interface IProxyPostprocessor {
    void on(ProxyThread thread) throws Exception;
}
