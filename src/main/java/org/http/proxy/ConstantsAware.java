package org.http.proxy;

public interface ConstantsAware {
    String CRLF = "\r\n";
    String DefaultSchema = "http://";
    String SecureSchema = "https://";
    String Host = "Host";
    String Server = "Server";
    String DefaultEncoding = "UTF-8";
    String ContentLength = "Content-Length";
    String ContentType = "Content-Type";
    String ContentEncoding = "Content-Encoding";
    String TransferEncoding = "Transfer-Encoding";
    String TestHtml = "this is <b>test</b> html!";
    String TextHtml = "text/html";
    int DefaultPort = 80;
    String Connection = "Connection";
    String ProxyConnection = "Proxy-Connection";
    String ProxyServerName = "my-java-proxy";
    byte[] MockBody = null;
    int MaxCaptureBodyLength = 5000000;
    String[] SupportedContentTypes = {"text", "xml", "json", "javascript"};
}
