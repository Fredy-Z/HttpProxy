package org.http.proxy.aspects.impl;


import org.http.proxy.ProxyThread;
import org.http.proxy.aspects.IProxyInterceptor;
import org.http.proxy.models.HttpResponse;

public class ComboInterceptor implements IProxyInterceptor {
    
    private IProxyInterceptor[] interceptors;

    public ComboInterceptor(IProxyInterceptor... interceptors) {
        this.interceptors=interceptors;
    }

    @Override
    public void on(ProxyThread thread, HttpResponse tempResponse) throws Exception {
        for (IProxyInterceptor interceptor : interceptors) {
            interceptor.on(thread, tempResponse);
        }
    }
}
