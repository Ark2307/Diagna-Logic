package com.diagna.logic.llm.core;

import com.diagna.logic.domain.TokenUsage;
import com.diagna.logic.llm.spi.FinishReason;
import com.diagna.logic.llm.spi.LlmResult;

import java.time.Duration;

/**
 * The only concrete {@link LlmResult}. Package-private: every caller sees
 * results through the abstract {@code LlmResult<T>} type, constructed
 * exclusively by {@link AbstractLlmProvider}.
 */
final class SimpleLlmResult<T> extends LlmResult<T> {

    private final T payload;
    private final String rawText;
    private final TokenUsage usage;
    private final String providerId;
    private final String model;
    private final Duration latency;
    private final FinishReason finishReason;
    private final boolean cached;

    SimpleLlmResult(
            T payload,
            String rawText,
            TokenUsage usage,
            String providerId,
            String model,
            Duration latency,
            FinishReason finishReason,
            boolean cached
    ) {
        this.payload = payload;
        this.rawText = rawText;
        this.usage = usage;
        this.providerId = providerId;
        this.model = model;
        this.latency = latency;
        this.finishReason = finishReason;
        this.cached = cached;
    }

    @Override
    public T payload() {
        return payload;
    }

    @Override
    public String rawText() {
        return rawText;
    }

    @Override
    public TokenUsage usage() {
        return usage;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public Duration latency() {
        return latency;
    }

    @Override
    public FinishReason finishReason() {
        return finishReason;
    }

    @Override
    public boolean cached() {
        return cached;
    }
}
