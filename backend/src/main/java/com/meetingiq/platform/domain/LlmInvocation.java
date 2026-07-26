package com.meetingiq.platform.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A record of one LLM call, serving two purposes at once: the response
 * cache ({@code LlmResponseCache} reads/writes by {@code _id}) and an
 * observability trail (every call's provider, model, latency and token
 * usage, queryable for debugging/cost analysis).
 *
 * <p>{@code _id} IS the cache key: {@code sha256(providerId|model|taskName|
 * targetId|prompt)}. An identical request against an unchanged transcript
 * always hashes to the same id, so a repeat request is a pure read with
 * zero provider calls and zero cost.
 */
@Document(collection = "llm_invocations")
public record LlmInvocation(
        @Id String id,
        String provider,
        String model,
        String taskName,
        /** The meetingId or dialogId this invocation was about, for filtering/debugging. */
        String targetId,
        TokenUsage usage,
        long latencyMs,
        /** The raw text returned by the provider, before task-specific parsing. */
        String rawText,
        Instant createdAt
) {
}
