package org.http.proxy.interceptors.impl;


import org.http.proxy.ProxyThread;
import org.http.proxy.interceptors.IProxyInterceptor;

public class ComboInterceptor implements IProxyInterceptor {
    
    private IProxyInterceptor[] interceptors;

    public ComboInterceptor(IProxyInterceptor... interceptors) {
        this.interceptors=interceptors;
    }

    @Override
    public void on(ProxyThread thread) throws Exception {
        for (IProxyInterceptor interceptor : interceptors) {
            interceptor.on(thread);
        }
    }
}
