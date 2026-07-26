package com.meetingiq.platform.llm.spi;

/**
 * Generation parameters for one {@link LlmQuery}. {@code model} overrides
 * the provider's configured default when set; {@code null} means "use the
 * provider's default for this call type."
 */
public record LlmOptions(
        String model,
        double temperature,
        int maxOutputTokens,
        /** When true, the query expects (and the provider should request, if it supports doing so) strict JSON output. */
        boolean jsonMode
) {
    private static final double DEFAULT_TEMPERATURE = 0.2;
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 1024;

    /** Sensible defaults for a task that expects structured JSON back: low temperature, JSON mode on. */
    public static LlmOptions jsonDefaults() {
        return new LlmOptions(null, DEFAULT_TEMPERATURE, DEFAULT_MAX_OUTPUT_TOKENS, true);
    }

    public static LlmOptions jsonDefaults(int maxOutputTokens) {
        return new LlmOptions(null, DEFAULT_TEMPERATURE, maxOutputTokens, true);
    }

    public LlmOptions withModel(String model) {
        return new LlmOptions(model, temperature, maxOutputTokens, jsonMode);
    }
}
