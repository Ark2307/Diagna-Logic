package com.diagna.logic.llm.spi;

import java.util.List;

/** A batch of texts to embed in one call. {@code model} is {@code null} to use the provider's configured default. */
public record EmbeddingQuery(List<String> texts, String model) {

    public static EmbeddingQuery of(List<String> texts) {
        return new EmbeddingQuery(texts, null);
    }
}
