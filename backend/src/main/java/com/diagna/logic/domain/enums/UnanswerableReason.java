package com.diagna.logic.domain.enums;

/**
 * Why a chat/QA answer was marked {@code unanswerable}. Surfaced in the API
 * and rendered distinctly in the UI so an off-topic question ("what's the
 * weather in Paris?") reads differently from a genuinely-covered-but-unclear
 * question — mirroring the dataset's own 1,274 unanswerable gold turns,
 * which this reason set lets us evaluate against.
 *
 * <p>See {@code ScopeGuard} for exactly where each reason is assigned.
 */
public enum UnanswerableReason {
    /**
     * The best retrieved chunk's fused relevance score fell below the
     * configured floor ({@code diagna.rag.min-relevance}) — the question is
     * not about this meeting at all. Assigned BEFORE any LLM call.
     */
    OUT_OF_SCOPE,
    /**
     * Retrieval found relevant-looking passages, but the model determined
     * the transcript doesn't actually contain an answer.
     */
    NOT_IN_TRANSCRIPT,
    /**
     * The model returned an "answerable" response, but every cited segment
     * index failed server-side verification against the retrieved/supplied
     * passages — so the answer is treated as ungrounded and suppressed.
     */
    NO_VALID_CITATIONS
}
