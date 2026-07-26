package com.meetingiq.platform.repository;

import java.util.List;
import java.util.Map;

/**
 * The full {@code GET /api/v1/stats} rollup, assembled by {@link StatsRepository}
 * from two separate aggregations (one over {@code meetings}, one over
 * {@code dialogs} — MongoDB's {@code $facet} runs against a single base
 * collection, so a genuinely cross-collection rollup means combining two
 * aggregations rather than writing one).
 */
public record StatsSummary(
        long totalMeetings,
        long totalDialogs,
        long totalTurns,
        long totalSegments,
        Map<String, Long> meetingsByCorpus,
        Map<String, Long> meetingsByDomain,
        Map<String, Long> dialogsBySplit,
        Map<String, Long> queryTypeCounts,
        long unanswerableTurns,
        double unanswerableRate,
        long attributedTurns,
        double attributionCoverage,
        double avgTurnsPerDialog,
        double avgSegmentsPerMeeting,
        List<SpeakerCount> topSpeakers,
        MeetingRef longestMeeting,
        MeetingRef shortestMeeting
) {
    public record SpeakerCount(String name, long segmentCount) {
    }

    public record MeetingRef(String meetingId, int segmentCount) {
    }
}
