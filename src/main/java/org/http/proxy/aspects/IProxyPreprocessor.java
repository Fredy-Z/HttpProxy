package org.http.proxy.aspects;

import org.http.proxy.ProxyThread;

public interface IProxyPreprocessor {
    void on(ProxyThread thread);
}
