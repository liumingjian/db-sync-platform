package com.dbsync.common.exceptions;

/**
 * Business Logic Exception
 *
 * @author DB Sync Platform
 */
public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(400, message);
    }

    public BusinessException(Integer errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(Integer errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
