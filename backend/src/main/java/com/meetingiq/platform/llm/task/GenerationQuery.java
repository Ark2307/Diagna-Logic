package com.meetingiq.platform.llm.task;

import com.meetingiq.platform.domain.enums.GenerationTask;
import com.meetingiq.platform.llm.core.JsonResponseParser;
import com.meetingiq.platform.llm.core.LlmJsonMapper;
import com.meetingiq.platform.llm.spi.JsonResponseSchema;
import com.meetingiq.platform.llm.spi.LlmOptions;
import com.meetingiq.platform.llm.spi.LlmQuery;
import com.meetingiq.platform.llm.spi.ResponseParser;

/**
 * The {@code /ai/generate} task: produce meeting-context text (summary,
 * minutes, decisions, action items, topics, or free-form custom
 * instructions). Deliberately not RAG-grounded — {@code contextText} is
 * either the full numbered transcript (single pass) or the concatenated
 * partial summaries from {@code ChunkPlanner}'s map-reduce fallback (the
 * same query class serves the reduce step too, since both are "produce
 * this task's output from this context text").
 */
public class GenerationQuery extends LlmQuery<GenerationResult> {

    private final GenerationTask task;
    private final String instructions;
    private final String contextText;
    private final String targetId;

    public GenerationQuery(GenerationTask task, String instructions, String contextText, String targetId, LlmOptions options) {
        super(options);
        this.task = task;
        this.instructions = instructions;
        this.contextText = contextText;
        this.targetId = targetId;
    }

    @Override
    public String taskName() {
        return "generate:" + task.name().toLowerCase();
    }

    @Override
    public String systemPrompt() {
        return PromptLibrary.generationSystemPrompt(task);
    }

    @Override
    public String userPrompt() {
        if (instructions == null || instructions.isBlank()) {
            return contextText;
        }
        return contextText + "\n\nAdditional instructions: " + instructions;
    }

    @Override
    public ResponseParser<GenerationResult> parser() {
        return new JsonResponseParser<>(LlmJsonMapper.create(), GenerationResult.class);
    }

    @Override
    public JsonResponseSchema responseSchema() {
        return JsonSchemas.GENERATION_RESULT;
    }

    @Override
    public String targetId() {
        return targetId;
    }
}
