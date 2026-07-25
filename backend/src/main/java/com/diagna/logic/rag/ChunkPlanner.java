package com.diagna.logic.rag;

import com.diagna.logic.domain.TranscriptSegment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides how {@code /ai/generate} should handle one meeting: almost all of
 * them fit in a single pass over the full transcript (median meeting is
 * ~8.3K tokens), but the small number of outliers (the largest is ~29K
 * tokens) exceed a single prompt's budget and need {@code TextGenerationService}
 * to run a map-reduce instead — summarize large sections independently,
 * then combine those section summaries into the final result.
 *
 * <p>Reuses {@link ChunkBuilder} for the map step's sections, sized to the
 * generation budget rather than the (much smaller) RAG chunk size, and with
 * no overlap — summarization doesn't need the per-segment attribution
 * robustness overlap exists for in retrieval.
 */
@Component
public class ChunkPlanner {

    private final ChunkBuilder chunkBuilder;

    public ChunkPlanner(ChunkBuilder chunkBuilder) {
        this.chunkBuilder = chunkBuilder;
    }

    public GenerationPlan plan(int meetingEstimatedTokens, List<TranscriptSegment> segments, int singlePassBudgetTokens) {
        if (meetingEstimatedTokens <= singlePassBudgetTokens) {
            return new GenerationPlan(true, List.of());
        }
        return new GenerationPlan(false, chunkBuilder.build(segments, singlePassBudgetTokens, 0));
    }
}
