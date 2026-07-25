package com.diagna.logic.llm.core;

import com.diagna.logic.config.LlmProperties;
import com.diagna.logic.domain.LlmInvocation;
import com.diagna.logic.llm.spi.LlmCompletion;
import com.diagna.logic.repository.LlmInvocationRepository;
import com.diagna.logic.util.Sha256;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * The LLM response cache, backed by {@code llm_invocations}. An identical
 * request (same provider, model, task, target and prompt) against unchanged
 * input always hashes to the same key, so a repeat call is a pure Mongo read
 * with zero provider cost — this is also the observability trail: every
 * entry records provider/model/task/usage/latency regardless of whether it
 * was ever re-read as a cache hit.
 */
@Component
public class LlmResponseCache {

    private final LlmInvocationRepository repository;
    private final LlmProperties properties;

    public LlmResponseCache(LlmInvocationRepository repository, LlmProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.cache().enabled();
    }

    public Optional<LlmInvocation> find(String cacheKey) {
        return repository.findById(cacheKey);
    }

    public void save(String cacheKey, String providerId, String taskName, String targetId, LlmCompletion completion, Duration latency) {
        repository.save(new LlmInvocation(
                cacheKey, providerId, completion.model(), taskName, targetId,
                completion.usage(), latency.toMillis(), completion.text(), Instant.now()
        ));
    }

    /** {@code sha256(providerId|model|taskName|targetId|prompt)} — see the class-level note on why this makes repeats free. */
    public static String computeKey(String providerId, String model, String taskName, String targetId, String prompt) {
        String raw = providerId + "|" + model + "|" + taskName + "|" + (targetId == null ? "" : targetId) + "|" + prompt;
        return Sha256.hex(raw);
    }
}
