package com.diagna.logic.domain;

/**
 * Per-speaker rollup for a meeting, embedded in {@link Meeting#speakers()} and
 * precomputed once at ingest time (see {@code mised_transform.speaker_stats})
 * rather than aggregated on every read.
 */
public record SpeakerStat(
        String name,
        int segmentCount,
        int charCount
) {
}
