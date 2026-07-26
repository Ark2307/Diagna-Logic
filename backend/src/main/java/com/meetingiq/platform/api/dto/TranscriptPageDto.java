package com.meetingiq.platform.api.dto;

import java.util.List;

/** A windowed read of one meeting's transcript — the response for {@code GET /meetings/{id}/transcript}. */
public record TranscriptPageDto(
        String meetingId,
        int from,
        int to,
        int segmentCount,
        List<TranscriptSegmentDto> segments
) {
}
