package com.diagna.logic.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/ai/chat}. {@code conversationId} is
 * omitted to start a new thread; when supplied, {@code meetingId} is still
 * required but is cross-checked against the stored conversation's own
 * (immutable) meeting id — a follow-up cannot redirect an existing thread
 * to a different meeting.
 */
public record ChatRequestDto(
        @NotBlank String meetingId,
        @NotBlank String message,
        String conversationId,
        String provider,
        String model
) {
}
