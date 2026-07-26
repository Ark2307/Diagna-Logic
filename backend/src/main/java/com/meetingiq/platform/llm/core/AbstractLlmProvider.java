package com.meetingiq.platform.llm.core;

import com.meetingiq.platform.domain.LlmInvocation;
import com.meetingiq.platform.llm.spi.FinishReason;
import com.meetingiq.platform.llm.spi.LlmCompletion;
import com.meetingiq.platform.llm.spi.LlmException;
import com.meetingiq.platform.llm.spi.LlmOptions;
import com.meetingiq.platform.llm.spi.LlmParseException;
import com.meetingiq.platform.llm.spi.LlmProvider;
import com.meetingiq.platform.llm.spi.LlmQuery;
import com.meetingiq.platform.llm.spi.LlmResult;
import com.meetingiq.platform.llm.spi.ProviderCallException;
import com.meetingiq.platform.llm.spi.ProviderUnavailableException;

import java.time.Duration;

/**
 * The template method every {@link LlmProvider} shares: validate
 * availability, check the cache, call the vendor, parse the response, write
 * the cache/audit entry, and wrap the result — in that fixed order, every
 * time, regardless of vendor. A new provider (Gemini, Claude, ...) extends
 * this and writes exactly two things: {@link #resolveModel} (which model to
 * use) and {@link #doComplete} (call the SDK, map its response into the
 * neutral {@link LlmCompletion}). Nothing else in this class is
 * vendor-specific, and nothing outside this class ever needs to change to
 * add a vendor.
 */
public abstract class AbstractLlmProvider implements LlmProvider {

    private final LlmResponseCache cache;

    protected AbstractLlmProvider(LlmResponseCache cache) {
        this.cache = cache;
    }

    @Override
    public final <T> LlmResult<T> execute(LlmQuery<T> query) {
        if (!isAvailable()) {
            throw new ProviderUnavailableException(
                    "Provider '" + descriptor().id() + "' is not available — check its configuration (e.g. API key)");
        }

        String model = resolveModel(query.options());
        String cacheKey = LlmResponseCache.computeKey(descriptor().id(), model, query.taskName(), query.targetId(), query.userPrompt());

        if (cache.isEnabled()) {
            var hit = cache.find(cacheKey);
            if (hit.isPresent()) {
                return fromCacheHit(query, hit.get());
            }
        }

        long startNanos = System.nanoTime();
        LlmCompletion completion = callProvider(query, model);
        Duration latency = Duration.ofNanos(System.nanoTime() - startNanos);

        if (cache.isEnabled()) {
            cache.save(cacheKey, descriptor().id(), query.taskName(), query.targetId(), completion, latency);
        }

        T payload = parse(query, completion.text());
        return new SimpleLlmResult<>(payload, completion.text(), completion.usage(), descriptor().id(), completion.model(), latency, completion.finishReason(), false);
    }

    private <T> LlmCompletion callProvider(LlmQuery<T> query, String model) {
        try {
            return doComplete(query, model);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderCallException(
                    "Call to provider '" + descriptor().id() + "' failed for task '" + query.taskName() + "'", e);
        }
    }

    private <T> LlmResult<T> fromCacheHit(LlmQuery<T> query, LlmInvocation cached) {
        T payload = parse(query, cached.rawText());
        return new SimpleLlmResult<>(
                payload, cached.rawText(), cached.usage(), cached.provider(), cached.model(),
                Duration.ZERO, FinishReason.STOP, true
        );
    }

    private <T> T parse(LlmQuery<T> query, String rawText) {
        try {
            return query.parser().parse(rawText);
        } catch (LlmParseException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmParseException("Failed to parse response for task '" + query.taskName() + "'", e);
        }
    }

    /** Resolves the model to call: {@code options.model()} when the caller set one, otherwise this provider's configured default. */
    protected abstract String resolveModel(LlmOptions options);

    /**
     * The only vendor-specific code in a provider: call the SDK with
     * {@code query.systemPrompt()}/{@code query.userPrompt()}/{@code query.options()}
     * and map its response into the neutral {@link LlmCompletion}. Any
     * exception thrown here is wrapped as {@link ProviderCallException}
     * (unless it's already an {@link LlmException}).
     */
    protected abstract LlmCompletion doComplete(LlmQuery<?> query, String resolvedModel);
}
