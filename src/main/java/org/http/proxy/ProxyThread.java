package org.http.proxy;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.http.proxy.aspects.IProxyPreprocessor;
import org.http.proxy.aspects.IProxyInterceptor;
import org.http.proxy.aspects.IProxyPostprocessor;
import org.http.proxy.models.HttpResponse;
import org.http.proxy.models.KeyValuePair;
import org.http.proxy.utils.GarUtils;
import org.http.proxy.utils.HttpHeaderUtil;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class ProxyThread implements Callable, ConstantsAware {
    private static final Logger logger = Logger.getLogger(ProxyThread.class);
    public String requestTime;
    // method whole_path version
    public String[] requestFirstLine;
    // version status_code status_message
    public String[] responseFirstLine;
    // schema host port path
    public Object[] uriToks;
    public List<KeyValuePair<String, String>> requestHeaders;
    public List<KeyValuePair<String, String>> responseHeaders;
    public Socket clientSocket;
    private IProxyPreprocessor preprocessor;
    private IProxyInterceptor interceptor;
    private IProxyPostprocessor postprocessor;
    private String requestBody;
    private String responseBody;

    public ProxyThread(HttpProxy proxy, Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.preprocessor = proxy.getPreprocessor();
        this.interceptor = proxy.getInterceptor();
        this.postprocessor = proxy.getPostprocessor();

        uriToks = new Object[4];
        requestFirstLine = new String[3];
        responseFirstLine = new String[3];
        requestHeaders = new LinkedList<>();
        responseHeaders = new LinkedList<>();
        requestBody = "";
        responseBody = "";
    }

    public String getRequestBody() {
        return requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public Object call() throws Exception {
        while (clientSocket.isConnected()) {
            parseRequestHeaders(clientSocket.getInputStream());
            if (uriToks[0].toString().equalsIgnoreCase(SECURE_SCHEMA)) {
                doResponse(new HttpResponse(StatusCode.MethodNotAllowed, "https not supported".getBytes(DEFAULT_ENCODING), TextHtml));
                skipUnusedBody(requestHeaders, clientSocket.getInputStream());
                continue;
            }

            if (preprocessor != null) {
                preprocessor.on(this);
            }
            Socket proxyClientSocket = null;
            try {
                requestTime = new Timestamp(System.currentTimeMillis()).toString();
                proxyClientSocket = new Socket(uriToks[1].toString(), ((Number) uriToks[2]).intValue());
                sendRequest(proxyClientSocket);
                parseResponseHeaders(proxyClientSocket.getInputStream());
                HttpResponse tempResponse = buildTempResponse(proxyClientSocket);
                if (interceptor != null) {
                    interceptor.on(this, tempResponse);
                }
                doResponse(tempResponse);
                if (postprocessor != null) {
                    postprocessor.on(this);
                }
            } catch (Throwable throwable) {
                String response = String.format(ERROR_RESPONSE, throwable.getMessage());
                doResponse(new HttpResponse(StatusCode.ServiceUnavailable, response.getBytes(DEFAULT_ENCODING)));
            } finally {
                if (proxyClientSocket != null) {
                    proxyClientSocket.close();
                }
            }
        }
        return this;
    }

    private void parseRequestHeaders(InputStream requestInput) throws IOException {
        List<String> requestHeaderLines = HttpHeaderUtil.readHeaders(requestInput);
        if (requestHeaderLines.size() == 0)
            return;
        String firstLine = requestHeaderLines.get(0);
        int index1 = firstLine.indexOf(' ');
        int index2 = firstLine.lastIndexOf(' ');
        if (index1 > 0 && index2 > index1) {
            String method = firstLine.substring(0, index1).toUpperCase();
            String requestURL = firstLine.substring(index1 + 1, index2).trim();
            String version = firstLine.substring(index2 + 1).toUpperCase();
            requestFirstLine[0] = method;
            requestFirstLine[1] = requestURL;
            requestFirstLine[2] = version;
            if (!method.matches("^[A-Z]+$")) {
                throw new ParseException("bad request method");
            }
            if (!version.matches("^HTTP.+$")) {
                throw new ParseException("bad request version");
            }
            HttpHeaderUtil.parseHeaders(requestHeaders, requestHeaderLines);
            if (requestURL.startsWith("/")) {
                logger.info("request URL starts with / : " + requestURL);
                String host = HttpHeaderUtil.getHeaderValue(this.requestHeaders, HOST);
                if (host == null || host.length() < 1) {
                    throw new ParseException("Parse request headers error: no host");
                }
                uriToks[0] = DEFAULT_SCHEMA;
                uriToks[1] = host;
                uriToks[3] = requestURL;
                requestFirstLine[1] = DEFAULT_SCHEMA + host + requestURL;
            } else {
                logger.info("request URL does not starts with / : " + requestURL);
                String s;
                if (requestURL.length() > 8) {
                    s = requestURL.substring(0, 8).toLowerCase();
                } else {
                    s = requestURL.toLowerCase();
                }
                if (s.startsWith(DEFAULT_SCHEMA)) {
                    uriToks[0] = DEFAULT_SCHEMA;
                } else if (s.startsWith(SECURE_SCHEMA)) {
                    uriToks[0] = SECURE_SCHEMA;
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
                HttpHeaderUtil.setHeader(requestHeaders, HOST, uriToks[1]);
            }
            int port;
            String host = uriToks[1].toString();
            int index = host.lastIndexOf(':');
            if (index > 0) {
                port = Integer.parseInt(host.substring(index + 1).trim());
                host = host.substring(0, index);
            } else {
                port = DEFAULT_PORT;
            }
            uriToks[1] = host;
            uriToks[2] = port;
        } else {
            throw new ParseException("bad request first line");
        }
    }

    private void skipUnusedBody(List<KeyValuePair<String, String>> requestHeaders, InputStream input) throws IOException {
        String _length = HttpHeaderUtil.getHeaderValue(requestHeaders, CONTENT_LENGTH);
        if (_length != null && _length.length() > 0) {
            long length = Long.parseLong(_length);
            input.skip(length);
        }
    }

    private void sendRequest(Socket proxyClientSocket) throws IOException {
        OutputStream output = proxyClientSocket.getOutputStream();
        DataOutputStream requestOutput = new DataOutputStream(new BufferedOutputStream(output));
        transferRequestHeaders(requestOutput);
        transferRequestBody(requestOutput);
        requestOutput.flush();
    }

    private void transferRequestHeaders(OutputStream output) throws IOException {
        output.write(requestFirstLine[0].getBytes());
        output.write(' ');
        output.write(requestFirstLine[1].getBytes());
        output.write(' ');
        output.write(requestFirstLine[2].getBytes());
        output.write(CRLF.getBytes());
        HttpHeaderUtil.sendHeaders(output, requestHeaders);
        output.write(CRLF.getBytes());
    }

    private void transferRequestBody(OutputStream output) throws IOException {
        String contentLength = HttpHeaderUtil.getHeaderValue(this.requestHeaders, CONTENT_LENGTH);
        if ((contentLength != null && contentLength.length() > 0)) {
            InputStream requestInput = clientSocket.getInputStream();
            byte[] requestBodyBytes = GarUtils.transfer(requestInput, output, Long.parseLong(contentLength));
            if (requestBodyBytes != null && requestBodyBytes.length > 0) {
                requestBody = new String(requestBodyBytes, DEFAULT_ENCODING).trim();
            }
        }
    }

    private void parseResponseHeaders(InputStream responseInput) throws IOException {
        List<String> responseHeaderLines = HttpHeaderUtil.readHeaders(responseInput);
        if (responseHeaderLines != null && responseHeaderLines.size() != 0) {
            String firstLine = responseHeaderLines.get(0);
            int index1 = firstLine.indexOf(' ');
            int index2 = firstLine.indexOf(' ', index1 + 1);
            if (index1 > 0 && index2 > index1) {
                String version = firstLine.substring(0, index1).toUpperCase();
                String statusCode = firstLine.substring(index1 + 1, index2).trim();
                String reasonPhrase = firstLine.substring(index2 + 1).trim();
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
                HttpHeaderUtil.parseHeaders(responseHeaders, responseHeaderLines);
            } else {
                throw new ParseException("bad request first line");
            }
        }
    }

    private HttpResponse buildTempResponse(Socket proxyClientSocket) throws IOException {
        String statusCodeStr = responseFirstLine[1];
        String message = responseFirstLine[2];

        HttpResponse response = new HttpResponse(Integer.parseInt(statusCodeStr), message);
        response.setVersion(responseFirstLine[0]);
        response.setHeaders(responseHeaders);
        response.setInput(proxyClientSocket.getInputStream());

        return response;
    }

    private void doResponse(HttpResponse response) throws IOException {
        DataOutputStream responseOutput = new DataOutputStream(clientSocket.getOutputStream());
        responseOutput.write(response.getVersion().getBytes());
        responseOutput.write(' ');
        responseOutput.write(String.valueOf(response.getStatusCode()).getBytes());
        responseOutput.write(' ');
        responseOutput.write(response.getStatusMessage().getBytes());
        responseOutput.write(CRLF.getBytes());

        List<KeyValuePair<String, String>> headers = response.getHeaders();
        HttpHeaderUtil.sendHeaders(responseOutput, headers);
        responseOutput.write(CRLF.getBytes());

        byte[] body = response.getBody();
        // is null when sending the real response with mock body
        if (body == null || body.length == 0) {
            InputStream responseInput = response.getInput();
            if (responseInput != null) {
                GarUtils.transfer(responseInput, responseOutput);
            }
        } else {
            responseOutput.write(body);
        }
        setResponseBody(body, HttpHeaderUtil.getHeaderValue(responseHeaders, CONTENT_TYPE));

        responseOutput.flush();
    }

    private void setResponseBody(byte[] responseBodyBytes, String contentType) throws UnsupportedEncodingException {
        if (responseBodyBytes != null && responseBodyBytes.length > 0) {
            if (contentType == null || StringUtils.indexOfAny(contentType, SupportedContentTypes) != -1) {
                responseBody = new String(responseBodyBytes, DEFAULT_ENCODING).trim();
            } else {
                responseBody = "XXXXXX  Support of this kind of content type has not been implemented.";
            }
        } else {
            responseBody = "";
        }
    }
}
