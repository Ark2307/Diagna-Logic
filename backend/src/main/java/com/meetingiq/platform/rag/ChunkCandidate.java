package com.meetingiq.platform.rag;

import java.util.List;

/** One packed window of consecutive transcript segments, before embedding — see {@link ChunkBuilder}. */
public record ChunkCandidate(
        int chunkIndex,
        int startIndex,
        int endIndex,
        String text,
        List<String> speakers,
        int tokenEstimate
) {
}
