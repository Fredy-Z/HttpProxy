package org.http.proxy;

public enum StatusCode {

    MethodNotAllowed(405),
    ServiceUnavailable(503),;

    private int code;

    StatusCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
