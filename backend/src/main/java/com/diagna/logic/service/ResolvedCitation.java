package com.diagna.logic.service;

import com.diagna.logic.domain.TranscriptSegment;

import java.util.List;

/**
 * One merged, in-bounds attribution span, with its transcript segments
 * resolved — the output of {@link AttributionResolver}. Grouping is
 * preserved (rather than flattening straight to a segment list) so the UI
 * can render each cited span as its own highlight instead of one undifferentiated
 * blob when a turn cites several separate passages.
 */
public record ResolvedCitation(int startIndex, int endIndex, List<TranscriptSegment> segments) {
}
