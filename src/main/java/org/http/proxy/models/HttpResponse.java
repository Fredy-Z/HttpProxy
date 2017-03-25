package org.http.proxy.models;

import org.http.proxy.ConstantsAware;
import org.http.proxy.StatusCode;
import org.http.proxy.utils.HttpHeaderUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class HttpResponse implements ConstantsAware {
    private String version = "HTTP/1.1";
    private int statusCode;
    private String statusMessage;
    private List<KeyValuePair<String, String>> headers;
    private byte[] body;

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    private InputStream input;

    private HttpResponse() {
        headers = new ArrayList<>();
        headers.add(new KeyValuePair<>(Server, ProxyServerName));
    }

    public HttpResponse(int code, String message) {
        this();
        this.statusCode = code;
        this.statusMessage = message;
    }

    public HttpResponse(StatusCode status) {
        this();
        this.statusCode = status.getCode();
        this.statusMessage = status.name();
    }

    public HttpResponse(StatusCode status, byte[] body) {
        this(status);
        this.setBody(body);
    }

    public HttpResponse(StatusCode status, byte[] body, String contentType) {
        this(status, body);
        HttpHeaderUtil.setHeader(this.getHeaders(), CONTENT_TYPE, contentType);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public List<KeyValuePair<String, String>> getHeaders() {
        return headers;
    }

    public void setHeaders(List<KeyValuePair<String, String>> _headers) {
        this.headers.clear();
        this.headers.addAll(_headers);
        String server = HttpHeaderUtil.getHeaderValue(this.headers, Server);
        if (server == null || server.length() < 1) {
            HttpHeaderUtil.setHeader(this.headers, Server, ProxyServerName);
        }
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

}
