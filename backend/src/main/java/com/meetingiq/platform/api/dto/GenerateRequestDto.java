package com.meetingiq.platform.api.dto;

import com.meetingiq.platform.domain.enums.GenerationTask;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/v1/ai/generate}. Exactly one of
 * {@code meetingId}/{@code dialogId} should be supplied — a dialog id is
 * resolved to its meeting before generation (validated in the service, not
 * here, since it's a cross-field business rule rather than a shape check).
 */
public record GenerateRequestDto(
        String meetingId,
        String dialogId,
        @NotNull GenerationTask task,
        String instructions,
        Integer maxWords,
        String provider,
        String model
) {
}
