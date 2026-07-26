package com.meetingiq.platform.rag;

import com.meetingiq.platform.domain.TranscriptSegment;
import com.meetingiq.platform.service.TranscriptFormatter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Packs consecutive transcript segments into ~token-budgeted windows for
 * RAG retrieval, with one segment of overlap between consecutive chunks so
 * a passage split across a chunk boundary still appears whole in at least
 * one chunk.
 *
 * <p><strong>Never splits a segment</strong> — even a segment far longer
 * than the target budget becomes its own (oversized) chunk rather than
 * being cut mid-utterance. Chunks therefore share {@link com.meetingiq.platform.domain.AttributionRange}'s
 * exact inclusive {@code [startIndex, endIndex]} contract, which is what
 * lets {@code AttributionResolver} resolve a retrieved chunk's citation the
 * same way it resolves a dataset gold attribution.
 */
@Component
public class ChunkBuilder {

    /** Rough chars-per-token estimate, matching the convention used everywhere else in this app (ETL, TranscriptFormatter callers). */
    private static final int CHARS_PER_TOKEN = 4;

    private final TranscriptFormatter formatter;

    public ChunkBuilder(TranscriptFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @param segments        the meeting's transcript, in order
     * @param targetTokens    approximate token budget per chunk (packing decisions use raw segment
     *                        text length against this budget; the returned chunk's own
     *                        {@code tokenEstimate} is measured from its final formatted text)
     * @param overlapSegments how many trailing segments of a finished chunk to carry into the start
     *                        of the next one (0 disables overlap)
     */
    public List<ChunkCandidate> build(List<TranscriptSegment> segments, int targetTokens, int overlapSegments) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }
        int targetChars = targetTokens * CHARS_PER_TOKEN;

        List<ChunkCandidate> chunks = new ArrayList<>();
        List<TranscriptSegment> window = new ArrayList<>();
        int windowCharCount = 0;
        int chunkIndex = 0;

        for (TranscriptSegment segment : segments) {
            if (!window.isEmpty() && windowCharCount + segment.text().length() > targetChars) {
                chunks.add(finalizeChunk(window, chunkIndex++));
                window = overlapTail(window, overlapSegments);
                windowCharCount = charCount(window);
            }
            window.add(segment);
            windowCharCount += segment.text().length();
        }
        if (!window.isEmpty()) {
            chunks.add(finalizeChunk(window, chunkIndex));
        }
        return chunks;
    }

    private static List<TranscriptSegment> overlapTail(List<TranscriptSegment> window, int overlapSegments) {
        if (overlapSegments <= 0 || window.isEmpty()) {
            return new ArrayList<>();
        }
        int from = Math.max(0, window.size() - overlapSegments);
        return new ArrayList<>(window.subList(from, window.size()));
    }

    private static int charCount(List<TranscriptSegment> segments) {
        return segments.stream().mapToInt(s -> s.text().length()).sum();
    }

    private ChunkCandidate finalizeChunk(List<TranscriptSegment> window, int chunkIndex) {
        int start = window.get(0).index();
        int end = window.get(window.size() - 1).index();
        String text = formatter.format(window);
        List<String> speakers = window.stream().map(TranscriptSegment::speakerName).distinct().toList();
        return new ChunkCandidate(chunkIndex, start, end, text, speakers, text.length() / CHARS_PER_TOKEN);
    }
}
