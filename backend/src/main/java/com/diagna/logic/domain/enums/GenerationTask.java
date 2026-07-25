package com.diagna.logic.domain.enums;

/**
 * The kind of meeting-context text generation requested via
 * {@code POST /api/v1/ai/generate}. Each value maps to a distinct prompt in
 * {@code PromptLibrary}, but all share the same {@code LlmQuery} contract —
 * adding a task means adding a prompt, not a new code path.
 */
public enum GenerationTask {
    /** A short prose overview of the meeting. */
    SUMMARY,
    /** Formal meeting-minutes style output: attendees, agenda, discussion, decisions. */
    MINUTES,
    /** Just the decisions reached, extracted as a list. */
    DECISIONS,
    /** Actionable follow-ups, each ideally with an owner if the transcript names one. */
    ACTION_ITEMS,
    /** The main topics discussed, as a short list. */
    TOPICS,
    /** Free-form instructions supplied by the caller instead of a fixed task shape. */
    CUSTOM
}
