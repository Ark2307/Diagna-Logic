package com.meetingiq.platform.rag;

import java.util.List;

/**
 * The outcome of one {@code MeetingRetriever.retrieve} call: the top-K fused
 * chunks to show the model, and separately the best raw cosine score across
 * every chunk in the meeting — the latter is what {@code ScopeGuard}'s
 * relevance floor checks, independent of which chunks made the top-K cut.
 */
public record RetrievalResult(List<ScoredChunk> topChunks, double topCosineScore) {

    public static RetrievalResult empty() {
        return new RetrievalResult(List.of(), 0.0);
    }
}
