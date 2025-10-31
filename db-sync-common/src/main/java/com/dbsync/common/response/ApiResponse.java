package com.dbsync.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified API Response
 *
 * @author DB Sync Platform
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String traceId;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
        this.traceId = UUID.randomUUID().toString();
    }

    public ApiResponse(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ApiResponse(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    /**
     * Success response
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(0, "Success");
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "Success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(0, message, data);
    }

    /**
     * Error response
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message);
    }

    /**
     * Common error responses
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return new ApiResponse<>(401, message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return new ApiResponse<>(403, message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(404, message);
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return new ApiResponse<>(409, message);
    }

    public static <T> ApiResponse<T> tooManyRequests(String message) {
        return new ApiResponse<>(429, message);
    }

    public static <T> ApiResponse<T> internalError(String message) {
        return new ApiResponse<>(500, message);
    }
}
