package com.meetingiq.platform.rag;

import com.meetingiq.platform.domain.MeetingChunk;
import com.meetingiq.platform.domain.TranscriptSegment;
import com.meetingiq.platform.service.TranscriptFormatter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turns a ranked chunk list (or a full transcript) into the actual prompt
 * text for one chat/QA turn.
 *
 * <p>{@link #packChunks} fills the budget greedily in RANK order (best
 * chunks first — the caller's list is expected pre-sorted, e.g. by
 * {@code MeetingRetriever}), then renders the chunks it kept in TRANSCRIPT
 * order so the model reads a coherent, chronological excerpt rather than a
 * relevance-shuffled one. Simplification worth noting: this does not expand
 * included chunks with extra neighbouring segments for readability — the
 * 1-segment overlap {@code ChunkBuilder} already bakes into adjacent chunks
 * covers the common case, and adding a second, independent expansion step
 * was judged not worth the complexity it would add here.
 */
@Component
public class ContextPacker {

    private final TranscriptFormatter formatter;

    public ContextPacker(TranscriptFormatter formatter) {
        this.formatter = formatter;
    }

    /** @param rankedChunks best-first; the returned context text renders whichever of these fit the budget in transcript order */
    public PackedContext packChunks(List<MeetingChunk> rankedChunks, int tokenBudget) {
        List<MeetingChunk> included = new ArrayList<>();
        int used = 0;
        for (MeetingChunk chunk : rankedChunks) {
            if (!included.isEmpty() && used + chunk.tokenEstimate() > tokenBudget) {
                break;
            }
            included.add(chunk);
            used += chunk.tokenEstimate();
        }

        List<MeetingChunk> inTranscriptOrder = included.stream()
                .sorted(Comparator.comparingInt(MeetingChunk::startIndex))
                .toList();
        String text = String.join("\n", inTranscriptOrder.stream().map(MeetingChunk::text).toList());
        return new PackedContext(text, inTranscriptOrder, false);
    }

    public PackedContext packFullTranscript(List<TranscriptSegment> segments) {
        return new PackedContext(formatter.format(segments), List.of(), true);
    }
}
