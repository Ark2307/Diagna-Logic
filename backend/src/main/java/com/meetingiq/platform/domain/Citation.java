package com.meetingiq.platform.domain;

/**
 * A single transcript segment offered as evidence for an answer — the
 * resolved, human-readable form of an {@link AttributionRange} entry,
 * produced by {@code AttributionResolver} for both the dataset's gold
 * attributions and this app's own RAG-grounded chat/QA answers.
 */
public record Citation(int segmentIndex, String speakerName, String text) {
}
