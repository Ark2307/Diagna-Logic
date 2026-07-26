package com.meetingiq.platform.service;

import com.meetingiq.platform.domain.TranscriptSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptFormatterTest {

    private final TranscriptFormatter formatter = new TranscriptFormatter();

    @Test
    void formatsEachSegmentAsOneNumberedLine() {
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0, "Professor B", "Let's discuss the digits task."),
                new TranscriptSegment(1, "PhD A", "I ran the SRI system on it.")
        );
        String result = formatter.format(segments);
        assertThat(result).isEqualTo(
                "[0] Professor B: Let's discuss the digits task.\n" +
                "[1] PhD A: I ran the SRI system on it."
        );
    }

    @Test
    void emptyListFormatsToEmptyString() {
        assertThat(formatter.format(List.of())).isEmpty();
    }

    @Test
    void formatOneMatchesTheCorrespondingLineInFormat() {
        TranscriptSegment segment = new TranscriptSegment(108, "Professor B", "recommendation text");
        assertThat(formatter.formatOne(segment)).isEqualTo("[108] Professor B: recommendation text");
        assertThat(formatter.format(List.of(segment))).isEqualTo(formatter.formatOne(segment));
    }

    @Test
    void preservesNonSequentialIndices() {
        // A RAG chunk's segments needn't be contiguous from zero — the index shown must be
        // the segment's real transcript index, not its position in this list.
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(108, "A", "first"),
                new TranscriptSegment(327, "B", "second")
        );
        assertThat(formatter.format(segments)).isEqualTo("[108] A: first\n[327] B: second");
    }
}
