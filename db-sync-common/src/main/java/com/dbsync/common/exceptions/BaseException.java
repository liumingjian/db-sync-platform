package com.dbsync.common.exceptions;

import lombok.Getter;

/**
 * Base Exception
 *
 * @author DB Sync Platform
 */
@Getter
public class BaseException extends RuntimeException {

    private final Integer errorCode;
    private final String errorMessage;

    public BaseException(Integer errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public BaseException(Integer errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
