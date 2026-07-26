package com.meetingiq.platform.domain;

import com.meetingiq.platform.domain.enums.QueryType;

import java.util.List;

/**
 * One query/response turn of a MISeD dialog, embedded in {@link Dialog#turns()}.
 *
 * <p>{@code unanswerable} and {@code contextDependent} are plain
 * {@code boolean}s here, not {@code Boolean}, precisely because the ETL has
 * already resolved the source data's trickiest quirk for us: in the raw
 * proto3-JSON these keys are OMITTED when false, and
 * {@code mised_transform.build_dialog_doc} defaults an absent key to
 * {@code false} before this document is ever written. By the time a
 * {@code DialogTurn} reaches this class, "false" and "absent" are already
 * the same thing — there is deliberately no third, nullable state to model.
 */
public record DialogTurn(
        int turnIndex,
        String query,
        String response,
        QueryType queryType,
        boolean unanswerable,
        boolean contextDependent,
        /** Empty (not null) when this turn's response has no attribution. */
        List<AttributionRange> attributionRanges,
        /** Sum of {@code segmentCount()} across {@link #attributionRanges()}. */
        int attributedSegmentCount
) {
}
