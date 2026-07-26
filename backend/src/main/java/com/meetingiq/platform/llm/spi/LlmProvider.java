package com.meetingiq.platform.llm.spi;

/**
 * A chat-completion vendor, registered as a Spring bean and resolved by
 * {@code LlmProviderRegistry}. To add a new vendor (Gemini, Claude, ...):
 * write one class implementing this (in practice, extending
 * {@code AbstractLlmProvider} and providing just {@code doComplete} +
 * {@link #descriptor()}) and register it as a bean — no other code in the
 * app changes. See {@code com.meetingiq.platform.llm.openai.OpenAiLlmProvider} and
 * {@code com.meetingiq.platform.llm.mock.MockLlmProvider} for the two shipped
 * implementations.
 */
public interface LlmProvider {

    ProviderDescriptor descriptor();

    /** False when misconfigured (e.g. no API key) — never throws to find out. */
    boolean isAvailable();

    <T> LlmResult<T> execute(LlmQuery<T> query);
}
