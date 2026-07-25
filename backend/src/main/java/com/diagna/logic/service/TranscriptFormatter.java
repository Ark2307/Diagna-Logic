package com.diagna.logic.service;

import com.diagna.logic.domain.TranscriptSegment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Renders transcript segments as numbered lines — {@code "[<index>] <speaker>: <text>"}
 * — the one text format every LLM-facing prompt in this app uses to show a
 * transcript. Numbering by segment index (not line position) is what lets a
 * model cite a segment and have that citation mean something: the same
 * index a {@code TranscriptSegment} carries, an {@code AttributionRange}
 * points at, and the RAG chunk store keys its ranges by.
 *
 * <p>Used for two different inputs: the full meeting transcript
 * ({@code /ai/generate}'s single-pass and map-reduce prompts) and a single
 * RAG chunk's segment window ({@code ChunkBuilder}) — both go through this
 * one formatter so the model always sees the same convention.
 */
@Component
public class TranscriptFormatter {

    /** Formats segments as one numbered line each, joined with newlines. */
    public String format(List<TranscriptSegment> segments) {
        StringBuilder sb = new StringBuilder();
        for (TranscriptSegment segment : segments) {
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append('[').append(segment.index()).append("] ")
                    .append(segment.speakerName()).append(": ")
                    .append(segment.text());
        }
        return sb.toString();
    }

    /** Formats a single segment as one numbered line, with no trailing newline. */
    public String formatOne(TranscriptSegment segment) {
        return "[" + segment.index() + "] " + segment.speakerName() + ": " + segment.text();
    }
}
