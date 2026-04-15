package com.sparkleshop.common.web.util;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.log.filter.TraceContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class Results {

    private Results() {
    }

    public static ResponseEntity<Result> ok() {
        return status(HttpStatus.OK, Result.success());
    }

    public static ResponseEntity<Result> ok(Object data) {
        return status(HttpStatus.OK, Result.success(data));
    }

    public static ResponseEntity<Result> created(Object data) {
        return status(HttpStatus.CREATED, Result.success(data));
    }

    public static ResponseEntity<Result> failure(Integer code, String msg) {
        return failure(resolveHttpStatus(code), code, msg);
    }

    public static ResponseEntity<Result> failure(HttpStatus status, Integer code, String msg) {
        return status(status, Result.error(code, msg));
    }

    public static ResponseEntity<Result> status(HttpStatus status, Result result) {
        return ResponseEntity.status(status).body(result.traceId(TraceContext.getTraceId()));
    }

    private static HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        int prefix = code / 100;
        return switch (prefix) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
