package com.diagna.logic.rag;

import com.diagna.logic.domain.MeetingChunk;

/** One retrieved chunk with its raw cosine similarity against the query (independent of fused rank position). */
public record ScoredChunk(MeetingChunk chunk, double cosineScore) {
}
