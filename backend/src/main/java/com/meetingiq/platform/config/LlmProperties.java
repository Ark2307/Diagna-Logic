package com.meetingiq.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code meetingiq.llm.*} from application.yml — see that file for behavioural notes on each field. */
@ConfigurationProperties(prefix = "meetingiq.llm")
public record LlmProperties(
        String defaultProvider,
        OpenAi openai,
        Cache cache
) {
    public record OpenAi(
            String apiKey,
            String chatModel,
            String embeddingModel,
            int embeddingDimensions,
            int timeoutSeconds
    ) {
        /** True once a real key is configured — the provider reports itself unavailable otherwise, never fails loudly at startup. */
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Cache(boolean enabled, int ttlDays) {
    }
}
