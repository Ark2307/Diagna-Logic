package com.meetingiq.platform.domain;

/**
 * An inclusive [startIndex, endIndex] range into a meeting's
 * {@code transcriptSegments} array, citing the segments that support a
 * dialog turn's response (or, for RAG, the segments a retrieved chunk
 * covers — {@code MeetingChunk} deliberately shares this exact shape).
 *
 * <p><strong>{@code endIndex} is INCLUSIVE</strong>, matching the source
 * MISeD data exactly (verified: no {@code endIndex} ever equals a meeting's
 * segment count, which it would if the convention were exclusive). A range
 * covering only segment 108 is {@code (108, 108)}, not {@code (108, 109)}.
 * See {@link #segmentCount()}.
 */
public record AttributionRange(int startIndex, int endIndex) {

    /** Number of segments this inclusive range covers. */
    public int segmentCount() {
        return endIndex - startIndex + 1;
    }
}
