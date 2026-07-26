package com.meetingiq.platform.api.dto;

import com.meetingiq.platform.domain.Citation;
import com.meetingiq.platform.domain.TokenUsage;

import java.util.List;

/**
 * Response body for {@code POST /api/v1/ai/chat}. {@code unanswerableReason}
 * is {@code null} for an answerable turn. When the question was rejected by
 * {@code ScopeGuard}'s relevance floor, {@code provider}/{@code model} are
 * {@code null} and {@code usage} is {@link TokenUsage#ZERO} — the LLM was
 * never called.
 */
public record ChatResponseDto(
        String conversationId,
        String answer,
        boolean unanswerable,
        String unanswerableReason,
        List<Citation> citations,
        RetrievalInfoDto retrieval,
        String provider,
        String model,
        TokenUsage usage,
        long latencyMs
) {
}
