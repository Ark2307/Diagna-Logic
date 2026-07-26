package com.meetingiq.platform.llm.task;

import com.meetingiq.platform.llm.spi.LlmOptions;
import com.meetingiq.platform.llm.spi.LlmQuery;
import com.meetingiq.platform.llm.spi.ResponseParser;

/**
 * The map step of {@code ChunkPlanner}'s map-reduce fallback: a plain-prose
 * (not JSON) partial summary of one large section of an oversized meeting.
 * {@code MockLlmProvider} always emits its shared JSON envelope regardless
 * of task, so under the mock provider a partial "summary" ends up being
 * that envelope's JSON as text — harmless (the reduce step still has real,
 * if odd-looking, content to work with) but worth knowing if you're reading
 * mock output directly for meetings large enough to trigger this path.
 */
public class MapSummaryQuery extends LlmQuery<String> {

    private final String chunkText;
    private final String targetId;

    public MapSummaryQuery(String chunkText, String targetId, LlmOptions options) {
        super(options);
        this.chunkText = chunkText;
        this.targetId = targetId;
    }

    @Override
    public String taskName() {
        return "generate:map-summary";
    }

    @Override
    public String systemPrompt() {
        return PromptLibrary.mapSummarySystemPrompt();
    }

    @Override
    public String userPrompt() {
        return chunkText;
    }

    @Override
    public ResponseParser<String> parser() {
        return raw -> raw == null ? "" : raw.trim();
    }

    @Override
    public String targetId() {
        return targetId;
    }
}
