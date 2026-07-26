package com.meetingiq.platform.llm.spi;

/**
 * An embeddings vendor, kept as its own SPI (separate from {@link LlmProvider})
 * because a vendor can implement one capability without the other, and
 * because RAG retrieval only ever needs this half of the abstraction.
 */
public interface EmbeddingProvider {

    ProviderDescriptor descriptor();

    boolean isAvailable();

    EmbeddingResult embed(EmbeddingQuery query);
}
