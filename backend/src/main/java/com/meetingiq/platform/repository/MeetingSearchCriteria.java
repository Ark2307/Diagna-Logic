package com.meetingiq.platform.repository;

import com.meetingiq.platform.domain.enums.Corpus;
import com.meetingiq.platform.domain.enums.DatasetSplit;
import com.meetingiq.platform.domain.enums.MeetingDomain;

/**
 * Filters for {@code GET /api/v1/meetings}. Every field is nullable —
 * {@code null} means "no filter on this dimension". Use {@link #empty()}
 * as the base case rather than null-checking six parameters everywhere.
 */
public record MeetingSearchCriteria(
        Corpus corpus,
        MeetingDomain domain,
        DatasetSplit split,
        /** Exact speaker name; matches meetings where this speaker appears at all. */
        String speaker,
        /** Free-text search over transcript content, via the {@code transcriptSegments.text} text index. */
        String q,
        Integer minSegments
) {
    public static MeetingSearchCriteria empty() {
        return new MeetingSearchCriteria(null, null, null, null, null, null);
    }
}
