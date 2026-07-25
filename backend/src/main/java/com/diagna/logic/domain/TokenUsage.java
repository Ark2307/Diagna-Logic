package com.diagna.logic.domain;

/**
 * Token accounting for one LLM/embedding call, normalised across providers.
 * Shared by {@link com.diagna.logic.llm.spi.LlmResult}, {@code ChatMessage}
 * and {@code LlmInvocation} so usage is reported identically everywhere it
 * appears, regardless of which provider produced it.
 */
public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {

    public static final TokenUsage ZERO = new TokenUsage(0, 0, 0);

    public static TokenUsage of(int promptTokens, int completionTokens) {
        return new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);
    }
}
