package com.diagna.logic.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A persisted RAG chat thread, scoped to exactly one meeting for its entire
 * lifetime. {@code meetingId} is set once at creation and never changed —
 * this is layer 1 of {@code ScopeGuard}: a conversation cannot be redirected
 * to answer questions about a different meeting by any sequence of
 * follow-ups, because retrieval always reads {@code meetingId} from the
 * stored conversation, never from client input on a follow-up turn.
 */
@Document(collection = "chat_conversations")
public record ChatConversation(
        @Id String id,
        String meetingId,
        /** Derived from the first user message, truncated; shown in a thread list. */
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<ChatMessage> messages
) {

    /** Returns a copy with one more message appended and {@code updatedAt} refreshed. */
    public ChatConversation withMessageAppended(ChatMessage message, Instant now) {
        List<ChatMessage> updated = new java.util.ArrayList<>(messages);
        updated.add(message);
        return new ChatConversation(id, meetingId, title, createdAt, now, List.copyOf(updated));
    }
}
