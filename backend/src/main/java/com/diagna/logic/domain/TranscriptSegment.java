package com.diagna.logic.domain;

/**
 * One utterance in a meeting transcript, embedded in {@link Meeting#transcriptSegments()}.
 *
 * <p><strong>{@code index} is the load-bearing field of this entire application.</strong>
 * It equals the segment's position in the transcript array, and every
 * attribution — both the dataset's gold {@code AttributionRange}s and this
 * app's own RAG-retrieved chunks — cites segments purely by this index. It
 * is stored explicitly (rather than relying on array position alone) so
 * that any partial/projected read of the transcript ({@code $slice}, RAG
 * chunk text, an API response) remains self-describing.
 */
public record TranscriptSegment(
        int index,
        String speakerName,
        String text
) {
}
