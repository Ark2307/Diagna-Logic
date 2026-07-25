package com.diagna.logic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code diagna.rag.*} from application.yml — see that file for behavioural notes on each field. */
@ConfigurationProperties(prefix = "diagna.rag")
public record RagProperties(
        int chunkTargetTokens,
        int chunkOverlapSegments,
        int topK,
        double minRelevance,
        ContextMode contextMode,
        int maxFullTranscriptTokens,
        int generationChunkBudgetTokens
) {
    public enum ContextMode {
        /** Send the full transcript alongside retrieved chunks when it fits {@link #maxFullTranscriptTokens}; retrieval-only otherwise. */
        AUTO,
        RETRIEVAL_ONLY,
        FULL_WHEN_FITS
    }
}
