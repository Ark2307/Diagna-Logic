package com.meetingiq.platform.domain;

import com.meetingiq.platform.domain.enums.UnanswerableReason;

import java.time.Instant;
import java.util.List;

/**
 * One message in a {@link ChatConversation}. User messages carry only
 * {@code index}/{@code role}/{@code content}/{@code createdAt}; the
 * remaining fields are populated on assistant messages, where {@code null}
 * simply means "not applicable to a user message" rather than "unknown".
 */
public record ChatMessage(
        int index,
        ChatRole role,
        String content,
        List<Citation> citations,
        boolean unanswerable,
        UnanswerableReason unanswerableReason,
        /** Ids of the {@code meeting_chunks} documents retrieved for this turn, for audit/debugging. */
        List<String> retrievedChunkIds,
        String provider,
        String model,
        TokenUsage usage,
        long latencyMs,
        Instant createdAt
) {

    /** Convenience factory for a user message — the fields an assistant reply owns are all empty/zero. */
    public static ChatMessage user(int index, String content, Instant createdAt) {
        return new ChatMessage(
                index, ChatRole.USER, content,
                List.of(), false, null, List.of(),
                null, null, TokenUsage.ZERO, 0L, createdAt
        );
    }
}
