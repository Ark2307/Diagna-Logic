package com.meetingiq.platform.llm.task;

import com.meetingiq.platform.llm.core.JsonResponseParser;
import com.meetingiq.platform.llm.core.LlmJsonMapper;
import com.meetingiq.platform.llm.spi.LlmOptions;
import com.meetingiq.platform.llm.spi.LlmQuery;
import com.meetingiq.platform.llm.spi.ResponseParser;
import com.meetingiq.platform.rag.GroundedAnswer;

/**
 * The RAG chat/QA task: answer a question about one meeting using only the
 * supplied transcript passages, citing every claim by segment index. See
 * {@link PromptLibrary#groundedChatSystemPrompt()} for the exact contract —
 * {@code ScopeGuard} verifies the model actually followed it before any
 * answer reaches a caller.
 */
public class GroundedAnswerQuery extends LlmQuery<GroundedAnswer> {

    private final String meetingId;
    private final String historyText;
    private final String contextText;
    private final String question;

    public GroundedAnswerQuery(String meetingId, String historyText, String contextText, String question, LlmOptions options) {
        super(options);
        this.meetingId = meetingId;
        this.historyText = historyText;
        this.contextText = contextText;
        this.question = question;
    }

    @Override
    public String taskName() {
        return "chat";
    }

    @Override
    public String systemPrompt() {
        return PromptLibrary.groundedChatSystemPrompt();
    }

    @Override
    public String userPrompt() {
        StringBuilder sb = new StringBuilder();
        if (historyText != null && !historyText.isBlank()) {
            sb.append("Conversation so far:\n").append(historyText).append("\n\n");
        }
        sb.append("Transcript passages:\n").append(contextText).append("\n\nQuestion: ").append(question);
        return sb.toString();
    }

    @Override
    public ResponseParser<GroundedAnswer> parser() {
        return new JsonResponseParser<>(LlmJsonMapper.create(), GroundedAnswer.class);
    }

    @Override
    public String targetId() {
        return meetingId;
    }
}
