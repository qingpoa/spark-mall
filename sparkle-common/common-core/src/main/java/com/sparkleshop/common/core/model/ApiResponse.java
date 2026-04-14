package com.sparkleshop.common.core.model;

import com.sparkleshop.common.core.constant.CommonConstants;

public class ApiResponse<T> {

    private int code;
    private String msg;
    private T data;
    private String traceId;

    public static <T> ApiResponse<T> success(T data, String traceId) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = CommonConstants.SUCCESS_CODE;
        response.msg = CommonConstants.SUCCESS_MESSAGE;
        response.data = data;
        response.traceId = traceId;
        return response;
    }

    public static <T> ApiResponse<T> fail(int code, String msg, String traceId) {
        ApiResponse<T> response = new ApiResponse<>();
        response.code = code;
        response.msg = msg;
        response.traceId = traceId;
        return response;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public String getTraceId() {
        return traceId;
    }
}
