package com.meetingiq.platform.rag;

import java.util.List;

/** Whether {@code /ai/generate} can use a single pass over the full transcript, or needs {@code ChunkPlanner}'s map-reduce fallback. */
public record GenerationPlan(boolean singlePass, List<ChunkCandidate> mapChunks) {
}
