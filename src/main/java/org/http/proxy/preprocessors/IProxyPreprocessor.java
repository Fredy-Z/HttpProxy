package org.http.proxy.preprocessors;

import org.http.proxy.ProxyThread;

public interface IProxyPreprocessor {

    public void on(ProxyThread thread);
}
