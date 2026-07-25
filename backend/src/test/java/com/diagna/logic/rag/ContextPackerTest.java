package com.diagna.logic.rag;

import com.diagna.logic.domain.MeetingChunk;
import com.diagna.logic.domain.TranscriptSegment;
import com.diagna.logic.service.TranscriptFormatter;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ContextPackerTest {

    private final ContextPacker packer = new ContextPacker(new TranscriptFormatter());

    private static MeetingChunk chunk(int startIndex, int endIndex, int tokenEstimate) {
        return new MeetingChunk(
                "m#" + startIndex, "m", startIndex, startIndex, endIndex,
                "[" + startIndex + "] Speaker: text", List.of("Speaker"), tokenEstimate,
                "hash" + startIndex, new byte[0], "model", "provider", 0, Instant.now()
        );
    }

    @Test
    void includesChunksUntilBudgetExceeded() {
        // Rank order: chunk at 100 is best, then 200, then 300 — each ~100 tokens, budget 250.
        List<MeetingChunk> ranked = List.of(chunk(100, 100, 100), chunk(200, 200, 100), chunk(300, 300, 100));
        PackedContext result = packer.packChunks(ranked, 250);

        assertThat(result.includedChunks()).hasSize(2);
        assertThat(result.usedFullTranscript()).isFalse();
    }

    @Test
    void alwaysIncludesAtLeastTheTopRankedChunkEvenIfItAloneExceedsBudget() {
        List<MeetingChunk> ranked = List.of(chunk(50, 50, 1000));
        PackedContext result = packer.packChunks(ranked, 10);

        assertThat(result.includedChunks()).hasSize(1);
    }

    @Test
    void rendersIncludedChunksInTranscriptOrderRegardlessOfRankOrder() {
        // Rank order deliberately out of transcript order: 300 is top-ranked, 100 is third.
        List<MeetingChunk> ranked = List.of(chunk(300, 300, 50), chunk(200, 200, 50), chunk(100, 100, 50));
        PackedContext result = packer.packChunks(ranked, 1000);

        assertThat(result.includedChunks()).extracting(MeetingChunk::startIndex).containsExactly(100, 200, 300);
    }

    @Test
    void emptyRankedListProducesEmptyContext() {
        PackedContext result = packer.packChunks(List.of(), 1000);
        assertThat(result.includedChunks()).isEmpty();
        assertThat(result.text()).isEmpty();
    }

    @Test
    void fullTranscriptModeFormatsEverySegmentAndReportsNoIncludedChunks() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0, "A", "hello"),
                new TranscriptSegment(1, "B", "world")
        );
        PackedContext result = packer.packFullTranscript(segments);

        assertThat(result.usedFullTranscript()).isTrue();
        assertThat(result.includedChunks()).isEmpty();
        assertThat(result.text()).isEqualTo("[0] A: hello\n[1] B: world");
    }
}
