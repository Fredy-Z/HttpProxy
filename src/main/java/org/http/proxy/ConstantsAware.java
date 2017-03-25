package org.http.proxy;

public interface ConstantsAware {
    String DEFAULT_ENCODING = "UTF-8";
    String CRLF = "\r\n";

    String GET = "GET";
    String POST = "POST";
    String HOST = "HOST";
    String CONTENT_LENGTH = "Content-Length";
    String CONTENT_TYPE = "Content-Type";
    String CONTENT_ENCODING = "Content-Encoding";
    String TRANSFER_ENCODING = "Transfer-Encoding";

    int DEFAULT_PORT = 80;

    String DEFAULT_SCHEMA = "http://";
    String SECURE_SCHEMA = "https://";

    String ERROR_RESPONSE = "<html><head><title>Proxy Error</title><style type=\"text/css\">body{font-family: Arial,Helvetica,Sans-serif;font-size: 12px;color: #333333;background-color: #ffffff;}h1 {font-size: 24px;font-weight: bold;}h2 {font-size: 18px;font-weight: bold;}</style></head><body><h1>Proxy Error</h1><h2>Failed to connect to remote host</h2><pre>%s</pre></body></html>";

    String Server = "Server";
    String TextHtml = "text/html";
    String ProxyServerName = "java-proxy";
    String[] SupportedContentTypes = {"text", "xml", "json", "javascript"};
}
