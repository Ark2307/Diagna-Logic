package com.meetingiq.platform.api.dto;

import java.util.Map;

/** Per-meeting rollup for {@code GET /api/v1/stats/meetings/{id}}, derived from that meeting's dialogs. */
public record MeetingStatsDto(
        String meetingId,
        String corpus,
        String domain,
        String split,
        int segmentCount,
        int speakerCount,
        int dialogCount,
        int totalTurns,
        int unanswerableTurns,
        int attributedTurns,
        Map<String, Integer> queryTypeCounts
) {
}
