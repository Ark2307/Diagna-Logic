package com.meetingiq.platform.api.dto;

/**
 * One search result. {@code type} is {@code "meeting"} or {@code "dialog"};
 * {@code segmentIndex} is set only for meeting/transcript hits, {@code null}
 * for dialog hits (which cite a turn, not a segment).
 */
public record SearchHitDto(
        String type,
        String id,
        String meetingId,
        Integer segmentIndex,
        String snippet,
        Double score
) {
}
