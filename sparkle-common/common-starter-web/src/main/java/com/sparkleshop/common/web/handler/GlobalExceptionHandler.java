package com.sparkleshop.common.web.handler;

import jakarta.validation.ConstraintViolationException;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result> handleBusinessException(BusinessException exception) {
        return Results.failure(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        return Results.failure(Result.BAD_REQUEST, extractBindingMessage(exception));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result> handleBindException(BindException exception) {
        return Results.failure(Result.BAD_REQUEST, extractBindingMessage(exception));
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseEntity<Result> handleBadRequestException(Exception exception) {
        return Results.failure(Result.BAD_REQUEST, extractBadRequestMessage(exception));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result> handleDuplicateKeyException(DuplicateKeyException exception) {
        return Results.failure(Result.CONFLICT, "数据重复，操作冲突");
    }

    @ExceptionHandler({
            MaxUploadSizeExceededException.class,
            MultipartException.class
    })
    public ResponseEntity<Result> handleMultipartException(Exception exception) {
        return Results.failure(Result.BAD_REQUEST, "图片超过最大值 5MB");
    }

    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class
    })
    public ResponseEntity<Result> handleUnsupportedRequestException(Exception exception) {
        return Results.failure(Result.BAD_REQUEST, extractBadRequestMessage(exception));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception exception) {
        return Results.failure(Result.SERVER_ERROR, exception.getMessage());
    }

    private String extractBindingMessage(BindException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse("Request parameter validation failed");
        return message;
    }

    private String extractBadRequestMessage(Exception exception) {
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            return constraintViolationException.getConstraintViolations().stream()
                    .findFirst()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .orElse("Request parameter validation failed");
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Request parameter validation failed" : message;
    }
}
