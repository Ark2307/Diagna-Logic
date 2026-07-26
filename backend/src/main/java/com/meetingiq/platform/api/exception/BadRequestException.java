package com.meetingiq.platform.api.exception;

/** Thrown for a malformed or semantically invalid request (unknown provider id, invalid range, etc). Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
