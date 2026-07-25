package com.diagna.logic.api.dto;

import java.util.List;

/**
 * {@code resolvedCitations} is {@code null} unless the request opted in via
 * {@code ?resolveAttributions=true} — resolving means loading the meeting's
 * transcript, which a plain dialog read should not pay for by default.
 */
public record DialogTurnDto(
        int turnIndex,
        String query,
        String response,
        String queryType,
        boolean unanswerable,
        boolean contextDependent,
        List<AttributionRangeDto> attributionRanges,
        int attributedSegmentCount,
        List<CitationDto> resolvedCitations
) {
}
