package com.diagna.logic.rag;

import com.diagna.logic.domain.ChatMessage;
import com.diagna.logic.domain.ChatRole;
import com.diagna.logic.domain.TokenUsage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationContextBuilderTest {

    private final ConversationContextBuilder builder = new ConversationContextBuilder();

    private static ChatMessage user(int index, String content) {
        return ChatMessage.user(index, content, Instant.now());
    }

    private static ChatMessage assistant(int index, String content) {
        return new ChatMessage(index, ChatRole.ASSISTANT, content, List.of(), false, null, List.of(), "mock", "mock-1", TokenUsage.ZERO, 1L, Instant.now());
    }

    @Test
    void emptyHistoryProducesEmptyText() {
        assertThat(builder.buildHistoryText(List.of(), 1000)).isEmpty();
    }

    @Test
    void singlePairWithinBudgetIsIncludedInFull() {
        List<ChatMessage> messages = List.of(user(0, "What was decided?"), assistant(1, "They decided to proceed."));
        String history = builder.buildHistoryText(messages, 1000);
        assertThat(history).contains("What was decided?").contains("They decided to proceed.");
    }

    @Test
    void dropsOldestPairsFirstWhenBudgetExceeded() {
        List<ChatMessage> messages = List.of(
                user(0, "First question far in the past"), assistant(1, "First answer far in the past"),
                user(2, "Second question"), assistant(3, "Second answer"),
                user(4, "Most recent question"), assistant(5, "Most recent answer")
        );
        // Budget tight enough for roughly one pair.
        String history = builder.buildHistoryText(messages, 15);

        assertThat(history).contains("Most recent question");
        assertThat(history).doesNotContain("First question far in the past");
    }

    @Test
    void alwaysKeepsAtLeastTheMostRecentPairEvenIfItAloneExceedsBudget() {
        List<ChatMessage> messages = List.of(
                user(0, "A very long question ".repeat(20)),
                assistant(1, "A very long answer ".repeat(20))
        );
        String history = builder.buildHistoryText(messages, 1);
        assertThat(history).isNotEmpty();
    }

    @Test
    void rewriteForRetrievalReturnsMessageUnchangedWithNoPriorHistory() {
        assertThat(builder.rewriteForRetrieval("What was decided?", List.of())).isEqualTo("What was decided?");
    }

    @Test
    void rewriteForRetrievalIncludesThePrecedingExchange() {
        List<ChatMessage> messages = List.of(
                user(0, "What did Professor B recommend?"),
                assistant(1, "Running a test on TI digits.")
        );
        String rewritten = builder.rewriteForRetrieval("Why did he suggest that?", messages);

        assertThat(rewritten).contains("What did Professor B recommend?");
        assertThat(rewritten).contains("Running a test on TI digits.");
        assertThat(rewritten).contains("Why did he suggest that?");
    }

    @Test
    void rewriteForRetrievalHandlesAPendingUserMessageWithNoAssistantReplyYet() {
        List<ChatMessage> messages = List.of(user(0, "What did Professor B recommend?"));
        String rewritten = builder.rewriteForRetrieval("Why?", messages);

        assertThat(rewritten).contains("What did Professor B recommend?");
        assertThat(rewritten).contains("Why?");
    }
}
