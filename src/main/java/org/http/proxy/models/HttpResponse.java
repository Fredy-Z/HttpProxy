package org.http.proxy.models;

import org.http.proxy.ConstantsAware;
import org.http.proxy.StatusCode;
import org.http.proxy.utils.HttpHeaderUtil;
import org.http.proxy.utils.MyByteArrayOutputStream;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class HttpResponse implements ConstantsAware {

    MyByteArrayOutputStream baos;
    private String versionTok = "HTTP/1.1";
    private int statusCode;
    private String statusMessage;
    private List<KeyValuePair<String, String>> headers;
    private byte[] body;
    private String sendEncoding = DefaultEncoding;
    private InputStream input;

    private HttpResponse() {
        headers = new ArrayList<KeyValuePair<String, String>>();
        headers.add(new KeyValuePair<String, String>(Server, ProxyServerName));
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
        HttpHeaderUtil.setHeader(this.getHeaders(), ContentType, contentType);
    }

    public String getVersionTok() {
        return versionTok;
    }

    public void setVersionTok(String versionTok) {
        this.versionTok = versionTok;
    }

    public int getStatucCode() {
        return statusCode;
    }

    public void setStatucCode(int statucCode) {
        this.statusCode = statucCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
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

    public String getSendEncoding() {
        return sendEncoding;
    }

    public void setSendEncoding(String sendEncoding) {
        this.sendEncoding = sendEncoding;
    }

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

}
