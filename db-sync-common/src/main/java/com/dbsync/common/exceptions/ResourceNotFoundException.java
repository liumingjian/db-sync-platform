package com.dbsync.common.exceptions;

/**
 * Resource Not Found Exception
 *
 * @author DB Sync Platform
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(404, message);
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(404, String.format("%s with ID '%s' not found", resourceType, resourceId));
    }
}
