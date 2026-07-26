package com.meetingiq.platform.llm.spi;

/**
 * Turns a provider's raw completion text into a task's actual payload type.
 * This is where task-specific meaning lives — the vendor is irrelevant here,
 * only the task (summarize, answer-with-citations, etc.) matters, which is
 * exactly the orthogonality the whole {@code llm} package is built around:
 * {@link LlmQuery} varies by task via its parser, {@link LlmProvider} varies
 * by vendor via {@code doComplete}, and neither depends on the other.
 */
@FunctionalInterface
public interface ResponseParser<T> {

    /** @throws LlmParseException if {@code rawText} cannot be parsed into {@code T} */
    T parse(String rawText);
}
