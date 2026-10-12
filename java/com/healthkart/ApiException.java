package com.healthkart;

import java.util.Map;

/** Thrown to short-circuit a request with a specific status/body. Aborts DB writes too. */
public class ApiException extends RuntimeException {
    public final int status;
    public final transient Map<String, Object> body;

    public ApiException(int status, Map<String, Object> body) {
        super(String.valueOf(body.get("error")));
        this.status = status;
        this.body = body;
    }

    public static ApiException of(int status, String error) {
        return new ApiException(status, Util.m("success", false, "error", error));
    }
}
