package com.sparkleshop.common.core.model;

import com.sparkleshop.common.core.constant.CommonConstants;
import lombok.Data;

@Data
public class Result {

    public static final Integer BAD_REQUEST = 40000;
    public static final Integer UNAUTHORIZED = 40100;
    public static final Integer FORBIDDEN = 40300;
    public static final Integer NOT_FOUND = 40400;
    public static final Integer CONFLICT = 40900;
    public static final Integer SERVER_ERROR = 50000;

    private Integer code;
    private String msg;
    private Object data;
    private String traceId;

    public static Result success() {
        Result result = new Result();
        result.code = CommonConstants.SUCCESS_CODE;
        result.msg = CommonConstants.SUCCESS_MESSAGE;
        return result;
    }

    public static Result success(Object data) {
        Result result = success();
        result.data = data;
        return result;
    }

    public static Result error(Integer code, String msg) {
        Result result = new Result();
        result.code = code;
        result.msg = msg;
        return result;
    }

    public Result traceId(String traceId) {
        this.traceId = traceId;
        return this;
    }
}
