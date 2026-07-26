package com.meetingiq.platform.rag;

import com.meetingiq.platform.domain.MeetingChunk;

/** One retrieved chunk with its raw cosine similarity against the query (independent of fused rank position). */
public record ScoredChunk(MeetingChunk chunk, double cosineScore) {
}
