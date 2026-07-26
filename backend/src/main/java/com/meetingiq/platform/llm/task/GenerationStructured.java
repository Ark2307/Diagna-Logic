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
}
