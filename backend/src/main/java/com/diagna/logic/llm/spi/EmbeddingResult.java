package com.diagna.logic.llm.spi;

import com.diagna.logic.domain.TokenUsage;

/** One embedding vector per input text, in the same order as {@link EmbeddingQuery#texts()}. */
public record EmbeddingResult(
        float[][] vectors,
        int dims,
        String model,
        String providerId,
        TokenUsage usage
) {
}
