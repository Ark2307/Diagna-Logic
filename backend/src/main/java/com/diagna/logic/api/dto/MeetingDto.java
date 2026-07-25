package com.diagna.logic.api.dto;

import java.util.List;

/**
 * One meeting, for both list and detail views. {@code transcriptSegments}
 * is {@code null} on list responses and on a detail response fetched
 * without {@code ?includeTranscript=true} — Jackson omits null fields
 * (see {@code spring.jackson.default-property-inclusion=non_null}), so a
 * list response body simply never contains the key.
 */
public record MeetingDto(
        String id,
        String corpus,
        String domain,
        String split,
        int segmentCount,
        int charCount,
        int estimatedTokens,
        int speakerCount,
        int dialogCount,
        List<SpeakerStatDto> speakers,
        List<TranscriptSegmentDto> transcriptSegments,
        String sourceFile,
        String ingestedAt
) {
}
