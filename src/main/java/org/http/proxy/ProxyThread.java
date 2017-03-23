package org.http.proxy;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.http.proxy.utils.GarUtils;
import org.http.proxy.utils.HttpHeaderUtil;
import org.http.proxy.utils.MyByteArrayOutputStream;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class ProxyThread implements Callable, ConstantsAware {
    private static final Logger logger = Logger.getLogger(ProxyThread.class);

    private JavaHttpProxy httpProxy;
    private Socket clientSocket;
    private Socket proxyClientSocket;

    private List<String> headerLineList = new ArrayList<String>();

    public InputStream requestInput;
    public InputStream responseInput;

    public String requestTime;
    public MyByteArrayOutputStream outputBuf;
    // method whole_path version
    public String[] requestFirstLine;
    // version status_code status_message
    public String[] responseFirstLine;
    // schema host port path
    public Object[] uriToks;
    public List<KeyValuePair<String, String>> requestHeaders;
    public List<KeyValuePair<String, String>> responseHeaders;

    private String requestBody;
    private String responseBody;

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public ProxyThread(JavaHttpProxy proxy, Socket clientSocket) throws IOException {
        outputBuf = new MyByteArrayOutputStream(1024 * 10);

        this.httpProxy = proxy;
        this.clientSocket = clientSocket;

        uriToks = new Object[4];
        requestFirstLine = new String[3];
        responseFirstLine = new String[3];
        requestHeaders = new LinkedList<KeyValuePair<String, String>>();
        responseHeaders = new LinkedList<KeyValuePair<String, String>>();
        requestBody = "";
        responseBody = "";
    }

    @Override
    public Object call() throws Exception {
        // int i = 0;
        while (clientSocket.isConnected()) {
            // System.out.println("read content from this socket for " + i++ +
            // " times");

            requestInput = clientSocket.getInputStream();
            HttpHeaderUtil.readHeadersIntoList(requestInput, DefaultEncoding, outputBuf, headerLineList);
            parseRequestHeaders(headerLineList);
            // ###################request log#############
            if ("https://".equalsIgnoreCase(uriToks[0].toString())) {
                doResponse(new HttpResponse(StatusCode.MethodNotAllowed, "https not supported".getBytes(DefaultEncoding), TextHtml));
                skipUnusedBody(requestHeaders, requestInput);
                continue;
            }
            httpProxy.preprocessOnProxyThread(this);
            try {
                requestTime = new Timestamp(System.currentTimeMillis()).toString();
                sendRequest();
                getResponse();
            } catch (Throwable throwable) {
                closeProxyClientSocket();
                StringBuilder buffer = new StringBuilder();
                String errorResponse = "<html><head><title>Proxy Error</title><style type=\"text/css\">body{font-family: Arial,Helvetica,Sans-serif;font-size: 12px;color: #333333;background-color: #ffffff;}h1 {font-size: 24px;font-weight: bold;}h2 {font-size: 18px;font-weight: bold;}</style></head><body><h1>Proxy Error</h1><h2>Failed to connect to remote host</h2><pre>%s</pre></body></html>";
                buffer.append(String.format(errorResponse, throwable.getMessage()));
                doResponse(new HttpResponse(StatusCode.ServiceUnavailable, buffer.toString().getBytes(DefaultEncoding), TextHtml));
            }
            httpProxy.interceptOnProxyThread(this);
            sendResponse();
            httpProxy.callbackOnProxyThread(this);
            closeProxyClientSocket();
        }
        return this;
    }

    private void parseRequestHeaders(List<String> requestHeaderLineList) throws IOException {
        if (requestHeaderLineList.size() == 0)
            return;
        String first = requestHeaderLineList.get(0);
        int index1 = first.indexOf(' ');
        int index2 = first.lastIndexOf(' ');
        if (index1 > 0 && index2 > index1) {
            String method = first.substring(0, index1).toUpperCase();
            String requestURL = first.substring(index1 + 1, index2).trim();
            String version = first.substring(index2 + 1).toUpperCase();
            requestFirstLine[0] = method;
            requestFirstLine[1] = requestURL;
            requestFirstLine[2] = version;
            if (!method.matches("^[A-Z]+$")) {
                throw new ParseException("bad request verb");
            }
            if (!version.matches("^HTTP.+$")) {
                throw new ParseException("bad request version");
            }
            HttpHeaderUtil.parseHeaders(requestHeaders, requestHeaderLineList);
            if (requestURL.startsWith("/")) {
                logger.info("request URL starts with / : " + requestURL);
                String host = HttpHeaderUtil.getHeaderValue(this.requestHeaders, Host);
                if (host == null || host.length() < 1) {
                    throw new ParseException("Parse request headers error: no host");
                }
                uriToks[0] = DefaultSchema;
                uriToks[1] = host;
                uriToks[3] = requestURL;
                requestFirstLine[1] = DefaultSchema + host + requestURL;
            } else {
                logger.info("request URL does not starts with / : " + requestURL);
                String s;
                if (requestURL.length() > 8) {
                    s = requestURL.substring(0, 8).toLowerCase();
                } else {
                    s = requestURL.toLowerCase();
                }
                if (s.startsWith(DefaultSchema)) {
                    uriToks[0] = DefaultSchema;
                } else if (s.startsWith(SecureSchema)) {
                    uriToks[0] = SecureSchema;
                } else {
                    throw new ParseException("bad request path,bad schema");
                }
                requestURL = requestURL.substring(uriToks[0].toString().length());
                int index = requestURL.indexOf('/');
                if (index < 1) {
                    throw new ParseException("bad request path,no host or path");
                }
                uriToks[1] = requestURL.substring(0, index);
                uriToks[3] = requestURL.substring(index);
                HttpHeaderUtil.setHeader(requestHeaders, Host, uriToks[1]);
            }
            int port;
            String host = uriToks[1].toString();
            int index = host.lastIndexOf(':');
            if (index > 0) {
                port = Integer.parseInt(host.substring(index + 1).trim());
                host = host.substring(0, index);
            } else {
                port = DefaultPort;
            }
            uriToks[1] = host;
            uriToks[2] = port;
        } else {
            throw new ParseException("bad request first line");
        }
    }

    private void skipUnusedBody(List<KeyValuePair<String, String>> requestHeaders, InputStream input) throws IOException {
        String _length = HttpHeaderUtil.getHeaderValue(requestHeaders, ContentLength);
        if (_length != null && _length.length() > 0) {
            long length = Long.parseLong(_length);
            input.skip(length);
        }
    }

    public synchronized void closeProxyClientSocket() {
        try {
            if (proxyClientSocket != null) {
                proxyClientSocket.close();
                proxyClientSocket = null;
            }
        } catch (Throwable t) {
            logger.error("close proxy client socket error");
        }
        this.responseInput = null;
    }

    private void sendRequest() throws IOException {
        proxyClientSocket = new Socket(uriToks[1].toString(), ((Number) uriToks[2]).intValue());
        DataOutputStream requestOutput = new DataOutputStream(new BufferedOutputStream(proxyClientSocket.getOutputStream()));
        transferRequestHeaders(requestOutput, DefaultEncoding);
        transferRequestBody(requestOutput);
        requestOutput.flush();
    }

    private void transferRequestHeaders(OutputStream output, String encoding) throws IOException {
        output.write(requestFirstLine[0].getBytes(encoding));
        output.write(' ');
        output.write(uriToks[3].toString().getBytes(encoding));
        output.write(' ');
        output.write(requestFirstLine[2].getBytes(encoding));
        output.write(CRLF.getBytes(encoding));
        HttpHeaderUtil.sendHeaders(output, requestHeaders, encoding);
        output.write(CRLF.getBytes(encoding));
    }

    private void transferRequestBody(OutputStream output) throws IOException {
        String _length = HttpHeaderUtil.getHeaderValue(this.requestHeaders, ContentLength);
        if (_length != null && _length.length() > 0) {
            long length = Long.parseLong(_length);
            byte[] requestBodyBytes = GarUtils.transfer(requestInput, output, length);
            if (requestBodyBytes != null && requestBodyBytes.length > 0) {
                requestBody = new String(requestBodyBytes, DefaultEncoding).trim();
            }
        }
    }

    private void getResponse() throws IOException {
        responseInput = proxyClientSocket.getInputStream();
        HttpHeaderUtil.readHeadersIntoList(responseInput, DefaultEncoding, outputBuf, headerLineList);
        parseResponseHeaders(headerLineList);
    }

    private void parseResponseHeaders(List<String> responseHeaderLineList) throws IOException {
        int size = responseHeaderLineList.size();
        assert (size > 0);
        String first = responseHeaderLineList.get(0);
        int index1 = first.indexOf(' ');
        int index2 = first.indexOf(' ', index1 + 1);
        if (index1 > 0 && index2 > index1) {
            String version = first.substring(0, index1).toUpperCase();
            String statusCode = first.substring(index1 + 1, index2).trim();
            String reasonPhrase = first.substring(index2 + 1).trim();
            responseFirstLine[0] = version;
            responseFirstLine[1] = statusCode;
            responseFirstLine[2] = reasonPhrase;
            if (!version.matches("^HTTP.+$")) {
                throw new ParseException("bad response version");
            }
            if (!statusCode.matches("^\\d+$")) {
                throw new ParseException("bad response status code");
            }
            if (reasonPhrase.length() < 1) {
                throw new ParseException("no response status message");
            }
            HttpHeaderUtil.parseHeaders(responseHeaders, responseHeaderLineList);
        } else {
            throw new ParseException("bad request first line");
        }
    }

    private void sendResponse() throws IOException {
        String statusCodeStr = responseFirstLine[1];
        String message = responseFirstLine[2];

        HttpResponse response = new HttpResponse(Integer.parseInt(statusCodeStr), message);
        response.setVersionTok(responseFirstLine[0]);
        response.setHeaders(responseHeaders);
        response.setSendEncoding(DefaultEncoding);
        response.setInput(responseInput);
        response.setBody(MockBody);

        doResponse(response);
    }

    private void doResponse(HttpResponse response) throws IOException {
        String encoding = response.getSendEncoding();
        DataOutputStream responseOutputStream = new DataOutputStream(clientSocket.getOutputStream());
        responseOutputStream.write(response.getVersionTok().getBytes(encoding));
        responseOutputStream.write(' ');
        responseOutputStream.write(String.valueOf(response.getStatucCode()).getBytes(encoding));
        responseOutputStream.write(' ');
        responseOutputStream.write(response.getStatusMessage().getBytes(encoding));
        responseOutputStream.write(CRLF.getBytes(encoding));
        List<KeyValuePair<String, String>> headers = response.getHeaders();
        byte[] body = response.getBody();

        byte[] bodyBuf;
        InputStream responseInput = null;
        MyByteArrayOutputStream outputStreamBuffer = null;
        String _length = null;
        int size = 0;

        // is null when sending the real response with mock body
        if (body == null || body.length < 1) {
            if ((responseInput = response.getInput()) == null) {
                HttpHeaderUtil.setHeader(headers, ContentLength, 0);
            } else {
                _length = HttpHeaderUtil.getHeaderValue(headers, ContentLength);
                // sometimes the response doesn't contain the content-length
                // header
                if (_length == null || _length.length() < 1) {
                    outputStreamBuffer = outputBuf;
                    outputStreamBuffer.reset();
                    bodyBuf = GarUtils.transfer(responseInput, outputStreamBuffer, MaxCaptureBodyLength);
                    setResponseBody(bodyBuf, HttpHeaderUtil.getHeaderValue(headers, ContentType), _length);
                    // size = bodyBuf.length;
                    // if (size < MaxCaptureBodyLength) {
                    HttpHeaderUtil.setHeader(headers, ContentLength, bodyBuf.length);
                    // }
                }
            }
        } else {
            HttpHeaderUtil.setHeader(headers, ContentLength, body.length);
        }

        HttpHeaderUtil.sendHeaders(responseOutputStream, headers, encoding);
        responseOutputStream.write(CRLF.getBytes(encoding));

        try {
            if ((body == null || body.length < 1) && responseInput != null) {
                if (_length == null || _length.length() < 1) {
                    if (outputStreamBuffer != null) {
                        responseOutputStream.write(outputStreamBuffer.getBuffer(), 0, outputStreamBuffer.size());
                        if (size < MaxCaptureBodyLength) {
                            // return false;
                        }
                    }
                    // return false;
                } else {
                    bodyBuf = GarUtils.transfer(responseInput, responseOutputStream, Integer.parseInt(_length));
                    setResponseBody(bodyBuf, HttpHeaderUtil.getHeaderValue(headers, ContentType), _length);
                }
            } else {
                responseOutputStream.write(body);
            }
            // return false;
        } finally {
            responseOutputStream.flush();
        }
    }

    private void setResponseBody(byte[] responseBodyBytes, String contentType, String contentLength) throws UnsupportedEncodingException {
        if (responseBodyBytes != null && responseBodyBytes.length > 0) {
            if (contentType == null || StringUtils.indexOfAny(contentType, SupportedContentTypes) != -1) {
                responseBody = new String(responseBodyBytes, DefaultEncoding).trim();
            } else {
                responseBody = "XXXXXX  Support of this kind of content type has not been implemented.";
            }
        } else {
            responseBody = "";
        }
    }
}
