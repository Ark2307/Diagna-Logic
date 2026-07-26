package com.meetingiq.platform.domain;

import java.util.Map;

/**
 * Precomputed rollup over a dialog's turns, embedded as {@link Dialog#stats()}
 * so {@code /api/v1/stats} and dialog-list filters never need to scan
 * {@code turns} at query time.
 *
 * <p>{@code queryTypeCounts} keys are {@link com.meetingiq.platform.domain.enums.QueryType}
 * names as plain strings (e.g. {@code "SPECIFIC"}) rather than the enum
 * itself — Mongo document keys are always strings, and keeping this as
 * {@code Map<String, Integer>} avoids relying on Spring Data's enum-as-map-key
 * conversion, which is a less common code path. Use
 * {@link com.meetingiq.platform.domain.enums.QueryType#name()} to look up a count.
 */
public record DialogStats(
        int unanswerableCount,
        int attributedTurnCount,
        Map<String, Integer> queryTypeCounts
) {
}
