package com.diagna.logic.rag;

import com.diagna.logic.domain.TranscriptSegment;
import com.diagna.logic.service.TranscriptFormatter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkBuilderTest {

    private final ChunkBuilder builder = new ChunkBuilder(new TranscriptFormatter());

    /** Each segment's text is exactly {@code charsPerSegment} characters, for predictable packing math. */
    private static List<TranscriptSegment> uniformTranscript(int count, int charsPerSegment) {
        String text = "x".repeat(charsPerSegment);
        return IntStream.range(0, count).mapToObj(i -> new TranscriptSegment(i, "Speaker", text)).toList();
    }

    @Test
    void emptyTranscriptProducesNoChunks() {
        assertThat(builder.build(List.of(), 400, 1)).isEmpty();
    }

    @Test
    void singleSegmentMeetingProducesExactlyOneChunk() {
        List<TranscriptSegment> segments = List.of(new TranscriptSegment(0, "Speaker", "hello"));
        List<ChunkCandidate> chunks = builder.build(segments, 400, 1);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).startIndex()).isZero();
        assertThat(chunks.get(0).endIndex()).isZero();
    }

    @Test
    void neverSplitsASegmentEvenWhenLongerThanTheBudget() {
        // 1 token ~= 4 chars, so a 2000-char segment is ~500 tokens — bigger than a 400-token target.
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0, "Speaker", "y".repeat(2000)),
                new TranscriptSegment(1, "Speaker", "short")
        );
        List<ChunkCandidate> chunks = builder.build(segments, 400, 0);
        // The oversized segment must still form its own whole chunk, not be truncated.
        assertThat(chunks.get(0).startIndex()).isZero();
        assertThat(chunks.get(0).endIndex()).isZero();
        assertThat(chunks.get(0).text()).contains("y".repeat(2000));
    }

    @Test
    void packsMultipleSegmentsIntoOneChunkUntilBudgetExceeded() {
        // 10 segments x 50 chars = 500 chars ~= 125 tokens, well under a 400-token (1600-char) budget.
        List<TranscriptSegment> segments = uniformTranscript(10, 50);
        List<ChunkCandidate> chunks = builder.build(segments, 400, 0);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).startIndex()).isZero();
        assertThat(chunks.get(0).endIndex()).isEqualTo(9);
    }

    @Test
    void splitsIntoMultipleChunksWhenBudgetExceeded() {
        // 100 segments x 50 chars = 5000 chars; target 400 tokens = 1600 chars -> expect several chunks.
        List<TranscriptSegment> segments = uniformTranscript(100, 50);
        List<ChunkCandidate> chunks = builder.build(segments, 400, 0);
        assertThat(chunks.size()).isGreaterThan(1);
        // chunkIndex is sequential from 0
        assertThat(IntStream.range(0, chunks.size()).boxed().toList())
                .containsExactlyElementsOf(chunks.stream().map(ChunkCandidate::chunkIndex).toList());
        // every chunk is a valid, non-inverted, contiguous-with-the-next range
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).startIndex()).isLessThanOrEqualTo(chunks.get(i).endIndex());
        }
        // ranges cover the whole transcript with no gaps (no overlap requested here)
        assertThat(chunks.get(0).startIndex()).isZero();
        assertThat(chunks.get(chunks.size() - 1).endIndex()).isEqualTo(99);
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i + 1).startIndex()).isEqualTo(chunks.get(i).endIndex() + 1);
        }
    }

    @Test
    void consecutiveChunksShareExactlyOneOverlappingSegment() {
        List<TranscriptSegment> segments = uniformTranscript(100, 50);
        List<ChunkCandidate> chunks = builder.build(segments, 400, 1);
        assertThat(chunks.size()).isGreaterThan(1);
        for (int i = 0; i < chunks.size() - 1; i++) {
            // With 1-segment overlap, the next chunk's start IS the previous chunk's end.
            assertThat(chunks.get(i + 1).startIndex()).isEqualTo(chunks.get(i).endIndex());
        }
    }

    @Test
    void speakersAreDistinctAndPreserveTranscriptOrder() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0, "Alice", "hi"),
                new TranscriptSegment(1, "Bob", "hello"),
                new TranscriptSegment(2, "Alice", "hey again")
        );
        List<ChunkCandidate> chunks = builder.build(segments, 400, 0);
        assertThat(chunks.get(0).speakers()).containsExactly("Alice", "Bob");
    }

    @Test
    void tokenEstimateReflectsFormattedTextLength() {
        List<TranscriptSegment> segments = List.of(new TranscriptSegment(0, "Speaker", "hello world"));
        ChunkCandidate chunk = builder.build(segments, 400, 0).get(0);
        assertThat(chunk.tokenEstimate()).isEqualTo(chunk.text().length() / 4);
    }
}
