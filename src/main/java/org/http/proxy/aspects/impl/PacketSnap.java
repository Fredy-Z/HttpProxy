package org.http.proxy.aspects.impl;

import org.apache.log4j.Logger;
import org.http.proxy.ConstantsAware;
import org.http.proxy.models.KeyValuePair;
import org.http.proxy.ProxyThread;
import org.http.proxy.aspects.IProxyPostprocessor;

import java.util.List;

public class PacketSnap implements IProxyPostprocessor, ConstantsAware {
    Logger logger = Logger.getLogger(PacketSnap.class);

    @Override
    public void on(ProxyThread thread) throws Exception {
        // System.out.println(thread.getResponseBody());
        String[] requestFirstLineArr = thread.requestFirstLine;
        StringBuilder requestFirstLine = new StringBuilder();
        for (int i = 0; i < requestFirstLineArr.length; i++) {
            requestFirstLine.append(requestFirstLineArr[i]);
            requestFirstLine.append(" ");
        }

        System.out.println(requestFirstLine.toString());
        logger.info(requestFirstLine.toString());
        // List<KeyValuePair<String, String>> requestHeaders =
        // thread.requestHeaders;
        // for (KeyValuePair<String, String> header : requestHeaders) {
        // logger.debug(header.getKey() + ": " + header.getValue());
        // }
        // logger.debug(thread.getRequestBody());
        //
        // logger.debug("######################");

        String[] responseFirstLineArr = thread.responseFirstLine;
        StringBuilder responseFirstLine = new StringBuilder();
        for (int i = 0; i < responseFirstLineArr.length; i++) {
            responseFirstLine.append(responseFirstLineArr[i]);
            responseFirstLine.append(" ");
        }
        logger.info(responseFirstLine.toString());
        List<KeyValuePair<String, String>> responseHeaders = thread.responseHeaders;
        for (KeyValuePair<String, String> header : responseHeaders) {
            logger.info(header.getKey() + ": " + header.getValue());
        }
        logger.info("\r\n");
        logger.info(thread.getResponseBody());

        // logger.debug("-----------------------------------------");
    }
}
