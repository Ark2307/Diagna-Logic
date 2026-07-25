package com.diagna.logic.api.dto;

import com.diagna.logic.domain.TokenUsage;
import com.diagna.logic.llm.task.GenerationStructured;

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
