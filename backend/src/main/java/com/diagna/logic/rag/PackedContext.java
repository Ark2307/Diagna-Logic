package com.diagna.logic.rag;

import com.diagna.logic.domain.MeetingChunk;

import java.util.List;

/**
 * The transcript context actually handed to the LLM for one chat/QA turn —
 * either a window of retrieved chunks or the full transcript (see
 * {@code MeetingChatService}'s {@code AUTO} context-mode decision).
 * {@code includedChunks} is empty when {@code usedFullTranscript} is true.
 */
public record PackedContext(String text, List<MeetingChunk> includedChunks, boolean usedFullTranscript) {
}
