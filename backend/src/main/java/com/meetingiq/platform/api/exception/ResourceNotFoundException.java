package com.meetingiq.platform.api.exception;

/** Thrown when a requested meeting/dialog/conversation id does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException meeting(String id) {
        return new ResourceNotFoundException("No meeting found with id '" + id + "'");
    }

    public static ResourceNotFoundException dialog(String id) {
        return new ResourceNotFoundException("No dialog found with id '" + id + "'");
    }
}
