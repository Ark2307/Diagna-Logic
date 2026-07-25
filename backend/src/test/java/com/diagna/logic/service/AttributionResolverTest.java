package com.diagna.logic.service;

import com.diagna.logic.domain.AttributionRange;
import com.diagna.logic.domain.Citation;
import com.diagna.logic.domain.TranscriptSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AttributionResolver} — the shared range-to-segments
 * logic used by the dialog API, RAG citations, chat, and the UI highlighter.
 *
 * <p>{@link #groundTruthBmr019SegmentsResolveIndividually()} pins the
 * resolver against the real, verified gold attribution from the MISeD
 * dataset (meeting {@code Bmr019}, the turn "What did Professor B recommend
 * to do during the discussion of digits?"), which cites segments
 * 108, 118, 189, 224, 327 — the same cross-check used end-to-end by the
 * Playwright suite.
 */
class AttributionResolverTest {

    private final AttributionResolver resolver = new AttributionResolver();

    /** Builds a contiguous transcript of {@code count} segments, indices 0..count-1. */
    private static List<TranscriptSegment> transcript(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new TranscriptSegment(i, "Speaker", "text-" + i))
                .toList();
    }

    @Test
    void emptyRangesYieldNoCitations() {
        assertThat(resolver.resolve(List.of(), transcript(10), 10)).isEmpty();
        assertThat(resolver.resolve(null, transcript(10), 10)).isEmpty();
    }

    @Test
    void zeroOrNegativeSegmentCountYieldsNoCitations() {
        List<AttributionRange> ranges = List.of(new AttributionRange(0, 0));
        assertThat(resolver.resolve(ranges, transcript(10), 0)).isEmpty();
        assertThat(resolver.resolve(ranges, transcript(10), -1)).isEmpty();
    }

    @Test
    void singleInBoundsRangeResolvesItsSegments() {
        List<ResolvedCitation> result = resolver.resolve(List.of(new AttributionRange(2, 4)), transcript(10), 10);
        assertThat(result).hasSize(1);
        ResolvedCitation citation = result.get(0);
        assertThat(citation.startIndex()).isEqualTo(2);
        assertThat(citation.endIndex()).isEqualTo(4);
        assertThat(citation.segments()).extracting(TranscriptSegment::index).containsExactly(2, 3, 4);
    }

    @Test
    void overlappingRangesMerge() {
        List<AttributionRange> ranges = List.of(new AttributionRange(0, 5), new AttributionRange(3, 8));
        List<ResolvedCitation> result = resolver.resolve(ranges, transcript(10), 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startIndex()).isEqualTo(0);
        assertThat(result.get(0).endIndex()).isEqualTo(8);
    }

    @Test
    void adjacentRangesMerge() {
        // endIndex + 1 == next startIndex: contiguous, should merge into one span.
        List<AttributionRange> ranges = List.of(new AttributionRange(0, 3), new AttributionRange(4, 7));
        List<ResolvedCitation> result = resolver.resolve(ranges, transcript(10), 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startIndex()).isEqualTo(0);
        assertThat(result.get(0).endIndex()).isEqualTo(7);
    }

    @Test
    void nonAdjacentRangesStaySeparate() {
        List<AttributionRange> ranges = List.of(new AttributionRange(0, 2), new AttributionRange(10, 12));
        List<ResolvedCitation> result = resolver.resolve(ranges, transcript(20), 20);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).startIndex()).isEqualTo(0);
        assertThat(result.get(1).startIndex()).isEqualTo(10);
    }

    @Test
    void exactDuplicateRangesCollapseToOne() {
        List<AttributionRange> ranges = List.of(new AttributionRange(5, 5), new AttributionRange(5, 5));
        List<ResolvedCitation> result = resolver.resolve(ranges, transcript(10), 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startIndex()).isEqualTo(5);
        assertThat(result.get(0).endIndex()).isEqualTo(5);
    }

    @Test
    void unsortedInputIsSortedBeforeMerging() {
        List<AttributionRange> ranges = List.of(new AttributionRange(10, 12), new AttributionRange(0, 2));
        List<ResolvedCitation> result = resolver.resolve(ranges, transcript(20), 20);
        assertThat(result).hasSize(2);
        // ascending order regardless of input order
        assertThat(result.get(0).startIndex()).isEqualTo(0);
        assertThat(result.get(1).startIndex()).isEqualTo(10);
    }

    @Test
    void rangeEntirelyBeforeZeroAfterClampingIsDropped() {
        // startIndex=-3 clamps to 0, endIndex=-1 clamps to -1 (min(segmentCount-1,-1)) -> start > end -> dropped.
        List<AttributionRange> ranges = List.of(new AttributionRange(-3, -1));
        assertThat(resolver.resolve(ranges, transcript(10), 10)).isEmpty();
    }

    @Test
    void rangeEntirelyBeyondSegmentCountIsDropped() {
        // segmentCount=10 (valid indices 0..9): startIndex=20 clamps to 20, endIndex=25 clamps to 9 -> start > end -> dropped.
        List<AttributionRange> ranges = List.of(new AttributionRange(20, 25));
        assertThat(resolver.resolve(ranges, transcript(10), 10)).isEmpty();
    }

    @Test
    void partiallyOutOfBoundsRangeIsClampedNotDropped() {
        // segmentCount=10: endIndex=15 clamps down to 9, so (7,15) becomes (7,9) — still a valid, non-empty span.
        List<AttributionRange> ranges = List.of(new AttributionRange(7, 15));
        List<ResolvedCitation> result = resolver.resolve(ranges, transcript(10), 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startIndex()).isEqualTo(7);
        assertThat(result.get(0).endIndex()).isEqualTo(9);
        assertThat(result.get(0).segments()).extracting(TranscriptSegment::index).containsExactly(7, 8, 9);
    }

    @Test
    void missingSegmentIndexIsOmittedNotFabricated() {
        // Only a partial segment list is supplied (as if reading a $slice projection) —
        // segments outside it must simply be absent from the resolved span, not synthesized.
        List<TranscriptSegment> partial = List.of(new TranscriptSegment(2, "Speaker", "only this one"));
        List<ResolvedCitation> result = resolver.resolve(List.of(new AttributionRange(2, 4)), partial, 10);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).startIndex()).isEqualTo(2);
        assertThat(result.get(0).endIndex()).isEqualTo(4);
        assertThat(result.get(0).segments()).extracting(TranscriptSegment::index).containsExactly(2);
    }

    @Test
    void resolveFlatFlattensSpansInOrder() {
        List<AttributionRange> ranges = List.of(new AttributionRange(5, 6), new AttributionRange(0, 1));
        List<Citation> flat = resolver.resolveFlat(ranges, transcript(10), 10);
        assertThat(flat).extracting(Citation::segmentIndex).containsExactly(0, 1, 5, 6);
    }

    /**
     * Ground truth: dialog {@code 004ac02783ba442e8eeb307ea45ee97c} (meeting
     * {@code Bmr019}) cites exactly segments 108, 118, 189, 224, 327 for
     * "What did Professor B recommend to do during the discussion of digits?"
     * — verified directly against the ingested dataset. These five singleton
     * ranges are far enough apart that none should merge.
     */
    @Test
    void groundTruthBmr019SegmentsResolveIndividually() {
        List<AttributionRange> goldRanges = List.of(
                new AttributionRange(108, 108),
                new AttributionRange(118, 118),
                new AttributionRange(189, 189),
                new AttributionRange(224, 224),
                new AttributionRange(327, 327)
        );
        List<ResolvedCitation> result = resolver.resolve(goldRanges, transcript(1319), 1319);

        assertThat(result).hasSize(5);
        assertThat(result).extracting(ResolvedCitation::startIndex).containsExactly(108, 118, 189, 224, 327);
        assertThat(result).extracting(ResolvedCitation::endIndex).containsExactly(108, 118, 189, 224, 327);
        assertThat(result).allSatisfy(c -> assertThat(c.segments()).hasSize(1));
    }
}
