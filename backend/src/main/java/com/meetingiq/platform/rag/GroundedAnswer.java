package com.meetingiq.platform.rag;

import java.util.List;

/**
 * The raw JSON payload a grounded-answer {@code LlmQuery} parses its
 * response into — exactly {@code {answer, unanswerable, citedSegmentIndices}}
 * per the prompt contract every provider is given: answer only from the
 * supplied passages, cite every claim by segment index, and set
 * {@code unanswerable} rather than guess. Nothing in this record is trusted
 * as-is; {@link ScopeGuard#verify} re-checks every citation before this
 * becomes a real answer.
 */
public record GroundedAnswer(String answer, boolean unanswerable, List<Integer> citedSegmentIndices) {
}
