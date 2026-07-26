package com.meetingiq.platform.llm.openai;

import com.meetingiq.platform.config.LlmProperties;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Constructs the shared {@link OpenAIClient} used by both
 * {@link OpenAiLlmProvider} and {@link OpenAiEmbeddingProvider} — one client,
 * one connection pool, for both capabilities.
 *
 * <p>Returns {@code null} (registering no bean) when no API key is
 * configured, rather than throwing at startup: the app must start and serve
 * every non-AI endpoint with no key present, and {@code isAvailable()} on
 * each provider is how callers find out AI features aren't configured.
 */
@Configuration
public class OpenAiClientConfig {

    @Bean
    public OpenAIClient openAiClient(LlmProperties properties) {
        if (!properties.openai().hasApiKey()) {
            return null;
        }
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.openai().apiKey())
                .timeout(Duration.ofSeconds(properties.openai().timeoutSeconds()))
                .build();
    }
}
