package com.meetingiq.platform.llm.spi;

/**
 * What a provider is and can do — its registry key, the model(s) it's
 * configured with, and which of the two capabilities (chat completion,
 * embeddings) it implements. A provider need not implement both; e.g. a
 * hypothetical embeddings-only provider would report {@code supportsChat=false}.
 */
public record ProviderDescriptor(
        String id,
        String displayName,
        boolean supportsChat,
        boolean supportsEmbeddings,
        String chatModel,
        String embeddingModel
) {
}
