package com.meetingiq.platform.service;

import com.meetingiq.platform.domain.AttributionRange;
import com.meetingiq.platform.domain.Citation;
import com.meetingiq.platform.domain.TranscriptSegment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns a set of {@link AttributionRange}s (either the dataset's gold
 * citations on a {@code DialogTurn}, or the segment ranges a RAG-retrieved
 * chunk covers — both share the same inclusive {@code [startIndex, endIndex]}
 * shape) into the actual transcript segments they point to.
 *
 * <p>This is the one place attribution ranges are ever turned into text, and
 * it is used identically by the dialog API, the RAG chat/QA answer path,
 * and (via the same JSON shape) the frontend's citation highlighter — so a
 * fix or a behavioural change here is guaranteed to apply everywhere at once.
 *
 * <p>Pipeline: clamp each range into {@code [0, segmentCount)}, drop any that
 * become empty/invalid after clamping, sort by start, merge overlapping or
 * adjacent ranges into single spans (this is also where exact duplicates
 * collapse), then resolve each merged span's segments by transcript index —
 * not by array position, so a partial or reordered segment list still
 * resolves correctly.
 */
@Component
public class AttributionResolver {

    /**
     * Resolves {@code ranges} against {@code segments}.
     *
     * @param ranges       attribution ranges to resolve; may be unsorted, overlapping,
     *                     duplicated, or partially out of bounds
     * @param segments     the transcript segments available to resolve against — need not
     *                     be the complete transcript or in index order; segments are looked
     *                     up by {@link TranscriptSegment#index()}, not array position
     * @param segmentCount the meeting's total segment count, used to clamp {@code endIndex}
     * @return merged, in-bounds citations in ascending order; segments whose index isn't
     *         present in {@code segments} are simply omitted from that citation's segment list
     */
    public List<ResolvedCitation> resolve(List<AttributionRange> ranges, List<TranscriptSegment> segments, int segmentCount) {
        if (ranges == null || ranges.isEmpty() || segmentCount <= 0) {
            return List.of();
        }

        Map<Integer, TranscriptSegment> byIndex = segments.stream()
                .collect(Collectors.toMap(TranscriptSegment::index, Function.identity(), (a, b) -> a));

        List<AttributionRange> clamped = ranges.stream()
                .map(r -> clamp(r, segmentCount))
                .filter(r -> r != null)
                .sorted(Comparator.comparingInt(AttributionRange::startIndex))
                .toList();

        List<AttributionRange> merged = merge(clamped);

        List<ResolvedCitation> result = new ArrayList<>(merged.size());
        for (AttributionRange range : merged) {
            List<TranscriptSegment> spanSegments = new ArrayList<>();
            for (int i = range.startIndex(); i <= range.endIndex(); i++) {
                TranscriptSegment segment = byIndex.get(i);
                if (segment != null) {
                    spanSegments.add(segment);
                }
            }
            result.add(new ResolvedCitation(range.startIndex(), range.endIndex(), spanSegments));
        }
        return result;
    }

    /** Convenience overload that flattens straight to {@link Citation}s, for callers that don't need span grouping. */
    public List<Citation> resolveFlat(List<AttributionRange> ranges, List<TranscriptSegment> segments, int segmentCount) {
        return resolve(ranges, segments, segmentCount).stream()
                .flatMap(c -> c.segments().stream())
                .map(s -> new Citation(s.index(), s.speakerName(), s.text()))
                .toList();
    }

    /**
     * Clamps a range into {@code [0, segmentCount)}. Returns null if the range
     * is entirely outside those bounds or inverted after clamping (dropped
     * rather than silently producing a nonsensical span).
     */
    private static AttributionRange clamp(AttributionRange range, int segmentCount) {
        int start = Math.max(0, range.startIndex());
        int end = Math.min(segmentCount - 1, range.endIndex());
        if (start > end) {
            return null;
        }
        return new AttributionRange(start, end);
    }

    /** Merges overlapping and adjacent (end + 1 == next start) ranges. Input must already be sorted by start. */
    private static List<AttributionRange> merge(List<AttributionRange> sorted) {
        List<AttributionRange> merged = new ArrayList<>();
        for (AttributionRange range : sorted) {
            if (merged.isEmpty()) {
                merged.add(range);
                continue;
            }
            AttributionRange last = merged.get(merged.size() - 1);
            if (range.startIndex() <= last.endIndex() + 1) {
                merged.set(merged.size() - 1, new AttributionRange(last.startIndex(), Math.max(last.endIndex(), range.endIndex())));
            } else {
                merged.add(range);
            }
        }
        return merged;
    }
}
