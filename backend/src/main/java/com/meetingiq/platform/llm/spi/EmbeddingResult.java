package com.meetingiq.platform.llm.spi;

import com.meetingiq.platform.domain.TokenUsage;

/** One embedding vector per input text, in the same order as {@link EmbeddingQuery#texts()}. */
public record EmbeddingResult(
        float[][] vectors,
        int dims,
        String model,
        String providerId,
        TokenUsage usage
) {
}
