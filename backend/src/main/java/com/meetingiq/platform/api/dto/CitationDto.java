package com.meetingiq.platform.api.dto;

/**
 * One cited transcript segment. Reused both flattened (a turn's
 * {@code resolvedCitations}) and nested inside a {@link ResolvedCitationDto}
 * (the standalone attribution-resolution endpoint) — the same shape either
 * way, so the frontend has one citation type to render regardless of which
 * endpoint it came from.
 */
public record CitationDto(int segmentIndex, String speakerName, String text) {
}
