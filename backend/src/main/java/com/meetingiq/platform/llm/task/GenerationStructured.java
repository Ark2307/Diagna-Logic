package com.meetingiq.platform.llm.task;

import java.util.List;

/** The structured half of a {@link GenerationResult} — present for every {@link com.meetingiq.platform.domain.enums.GenerationTask}, fields simply empty when not applicable. */
public record GenerationStructured(
        String overview,
        List<String> keyPoints,
        List<String> decisions,
        List<String> actionItems,
        List<String> topics,
        List<String> participants
) {
    private static final GenerationStructured EMPTY = new GenerationStructured("", List.of(), List.of(), List.of(), List.of(), List.of());

    /**
     * A real model's JSON-mode output is only guaranteed to be syntactically valid JSON, not
     * schema-compliant — it can (and occasionally does) omit {@code structured} entirely, or
     * null out individual fields, even though the system prompt asks for every field every time.
     * Callers (the {@code /ai/generate} response contract, and the frontend's non-optional
     * {@code GenerationStructured} type) rely on every field always being present, so this
     * backfills whatever the model left out rather than propagating nulls downstream.
     */
    public static GenerationStructured normalize(GenerationStructured raw) {
        if (raw == null) {
            return EMPTY;
        }
        return new GenerationStructured(
                raw.overview() != null ? raw.overview() : "",
                raw.keyPoints() != null ? raw.keyPoints() : List.of(),
                raw.decisions() != null ? raw.decisions() : List.of(),
                raw.actionItems() != null ? raw.actionItems() : List.of(),
                raw.topics() != null ? raw.topics() : List.of(),
                raw.participants() != null ? raw.participants() : List.of()
        );
    }
}
