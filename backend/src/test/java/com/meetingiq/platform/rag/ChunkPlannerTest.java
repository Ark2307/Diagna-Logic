package com.meetingiq.platform.rag;

import com.meetingiq.platform.domain.TranscriptSegment;
import com.meetingiq.platform.service.TranscriptFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkPlannerTest {

    private final ChunkPlanner planner = new ChunkPlanner(new ChunkBuilder(new TranscriptFormatter()));

    private static List<TranscriptSegment> segments(int count) {
        return IntStream.range(0, count).mapToObj(i -> new TranscriptSegment(i, "Speaker", "text-" + i)).toList();
    }

    @Test
    void meetingWithinBudgetUsesSinglePassAndProducesNoMapChunks() {
        GenerationPlan plan = planner.plan(5000, segments(10), 6000);
        assertThat(plan.singlePass()).isTrue();
        assertThat(plan.mapChunks()).isEmpty();
    }

    @Test
    void meetingExactlyAtBudgetUsesSinglePass() {
        GenerationPlan plan = planner.plan(6000, segments(10), 6000);
        assertThat(plan.singlePass()).isTrue();
    }

    @Test
    void meetingOneTokenOverBudgetTriggersMapReduce() {
        GenerationPlan plan = planner.plan(6001, segments(10), 6000);
        assertThat(plan.singlePass()).isFalse();
    }

    @Test
    void mapReducePlanProducesNonEmptyChunksCoveringTheWholeTranscript() {
        List<TranscriptSegment> segs = segments(2000); // large enough to force multiple chunks at a small budget
        GenerationPlan plan = planner.plan(999999, segs, 400);

        assertThat(plan.singlePass()).isFalse();
        assertThat(plan.mapChunks()).isNotEmpty();
        assertThat(plan.mapChunks().get(0).startIndex()).isZero();
        assertThat(plan.mapChunks().get(plan.mapChunks().size() - 1).endIndex()).isEqualTo(1999);
    }
}
