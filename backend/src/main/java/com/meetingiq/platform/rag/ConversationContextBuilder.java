package com.meetingiq.platform.rag;

import com.meetingiq.platform.domain.ChatMessage;
import com.meetingiq.platform.domain.ChatRole;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Turns a chat conversation's prior turns into two things a follow-up
 * question needs: history text to include in the prompt (budgeted, oldest
 * pairs dropped first, most recent always kept), and a standalone retrieval
 * query for a context-dependent follow-up like "why did he suggest that?"
 * — supporting the dataset's 548 context-dependent gold queries.
 *
 * <p>Both operations are pure text transformations, deliberately not an LLM
 * call: retrieval and prompt assembly must not themselves depend on the
 * very provider they're preparing a request for.
 */
@Component
public class ConversationContextBuilder {

    private static final int CHARS_PER_TOKEN = 4;

    /**
     * Renders prior user/assistant pairs as {@code "User: …\nAssistant: …"}
     * blocks, most-recent-first for the budget decision (so a long-running
     * conversation trims its oldest history, never its newest), then
     * returned in chronological order.
     */
    public String buildHistoryText(List<ChatMessage> messages, int tokenBudget) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        int budgetChars = tokenBudget * CHARS_PER_TOKEN;
        List<String> pairs = pairTurns(messages);

        Deque<String> kept = new ArrayDeque<>();
        int used = 0;
        for (int i = pairs.size() - 1; i >= 0; i--) {
            String pair = pairs.get(i);
            if (!kept.isEmpty() && used + pair.length() > budgetChars) {
                break;
            }
            kept.addFirst(pair);
            used += pair.length();
        }
        return String.join("\n\n", kept);
    }

    /**
     * Prepends the immediately preceding exchange to a follow-up message,
     * so a pronoun-heavy question like "why did he suggest that?" has
     * enough lexical content of its own for {@code MeetingRetriever} to
     * embed meaningfully. Returns {@code message} unchanged when there is
     * no prior exchange to draw on.
     */
    public String rewriteForRetrieval(String message, List<ChatMessage> priorMessages) {
        ChatMessage lastUser = lastOfRole(priorMessages, ChatRole.USER);
        ChatMessage lastAssistant = lastOfRole(priorMessages, ChatRole.ASSISTANT);
        if (lastUser == null && lastAssistant == null) {
            return message;
        }

        StringBuilder sb = new StringBuilder();
        if (lastUser != null) {
            sb.append("Previous question: ").append(lastUser.content()).append('\n');
        }
        if (lastAssistant != null) {
            sb.append("Previous answer: ").append(lastAssistant.content()).append('\n');
        }
        sb.append("Follow-up question: ").append(message);
        return sb.toString();
    }

    private static List<String> pairTurns(List<ChatMessage> messages) {
        List<String> pairs = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            if (message.role() != ChatRole.USER) {
                continue;
            }
            StringBuilder pair = new StringBuilder("User: ").append(message.content());
            if (i + 1 < messages.size() && messages.get(i + 1).role() == ChatRole.ASSISTANT) {
                pair.append("\nAssistant: ").append(messages.get(i + 1).content());
            }
            pairs.add(pair.toString());
        }
        return pairs;
    }

    private static ChatMessage lastOfRole(List<ChatMessage> messages, ChatRole role) {
        if (messages == null) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).role() == role) {
                return messages.get(i);
            }
        }
        return null;
    }
}
