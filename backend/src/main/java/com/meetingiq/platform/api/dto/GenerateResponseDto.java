package com.meetingiq.platform.api.dto;

import com.meetingiq.platform.domain.TokenUsage;
import com.meetingiq.platform.llm.task.GenerationStructured;

/** Response body for {@code POST /api/v1/ai/generate}. */
public record GenerateResponseDto(
        String text,
        GenerationStructured structured,
        String provider,
        String model,
        TokenUsage usage,
        long latencyMs,
        boolean cached
) {
}
