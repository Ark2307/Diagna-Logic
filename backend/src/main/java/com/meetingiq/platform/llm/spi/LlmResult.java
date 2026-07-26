package com.meetingiq.platform.llm.spi;

import com.meetingiq.platform.domain.TokenUsage;

import java.time.Duration;

/**
 * A model-agnostic response: the task's parsed payload plus everything
 * about how it was produced (provider, model, timing, usage, cache status)
 * — normalised identically regardless of which vendor answered the call.
 * Concrete instances are built by {@code AbstractLlmProvider}, never by
 * a task or a controller.
 */
public abstract class LlmResult<T> {

    /** The parsed payload — what {@link LlmQuery#parser()} produced from {@link #rawText()}. */
    public abstract T payload();

    /** The provider's unparsed completion text, kept for debugging and audit. */
    public abstract String rawText();

    public abstract TokenUsage usage();

    public abstract String providerId();

    public abstract String model();

    public abstract Duration latency();

    public abstract FinishReason finishReason();

    /** True if this result came from {@code LlmResponseCache} rather than a live provider call. */
    public abstract boolean cached();
}
