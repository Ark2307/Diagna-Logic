package com.meetingiq.platform.api.dto;

import java.time.Instant;

/** RFC-7807-flavoured error body returned by every failure path in this API. */
public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, Instant.now());
    }
}
