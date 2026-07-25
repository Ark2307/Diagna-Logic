package com.diagna.logic.rag;

import com.diagna.logic.config.RagProperties;
import com.diagna.logic.domain.AttributionRange;
import com.diagna.logic.domain.TranscriptSegment;
import com.diagna.logic.domain.enums.UnanswerableReason;
import com.diagna.logic.service.AttributionResolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Enforces that this app's RAG chat never answers out of scope or
 * generically — in four layers, none of which trust the model to police
 * itself:
 *
 * <ol>
 *   <li><b>Hard metadata filter</b> — {@code MeetingRetriever.retrieve} requires
 *       a {@code meetingId} as a non-optional first argument, and a
 *       conversation's {@code meetingId} is immutable once set
 *       ({@code MeetingChatService} always reads it from the stored
 *       conversation, never from client input on a follow-up). Enforced
 *       structurally elsewhere, not by this class.</li>
 *   <li>{@link #checkRelevanceFloor} — before any LLM call, reject
 *       questions that aren't about this meeting at all.</li>
 *   <li>The prompt contract given to the model (see {@code PromptLibrary}) —
 *       answer only from the supplied passages, cite every claim, or set
 *       {@code unanswerable}.</li>
 *   <li>{@link #verify} — after the LLM call, re-check every citation
 *       against what the model was actually shown; drop hallucinated ones;
 *       force {@code unanswerable} if nothing valid survives.</li>
 * </ol>
 */
@Component
public class ScopeGuard {

    private final RagProperties ragProperties;
    private final AttributionResolver attributionResolver;

    public ScopeGuard(RagProperties ragProperties, AttributionResolver attributionResolver) {
        this.ragProperties = ragProperties;
        this.attributionResolver = attributionResolver;
    }

    /**
     * Layer 2. If the best cosine score across every chunk in this meeting
     * is below {@code diagna.rag.min-relevance}, the question is out of
     * scope — returns that answer directly, with the LLM never called.
     * Empty means "proceed to the LLM."
     */
    public Optional<GuardedAnswer> checkRelevanceFloor(double topCosineScore) {
        if (topCosineScore < ragProperties.minRelevance()) {
            return Optional.of(GuardedAnswer.outOfScope());
        }
        return Optional.empty();
    }

    /**
     * Layer 4. {@code allowedSegmentIndices} is the set of segments the
     * model was actually shown (the retrieved chunks' ranges, or every
     * segment when the full transcript was used) — a citation to anything
     * outside that set is hallucinated and dropped, even if it happens to
     * be a valid index somewhere else in the meeting.
     */
    public GuardedAnswer verify(GroundedAnswer raw, Set<Integer> allowedSegmentIndices, List<TranscriptSegment> segments, int segmentCount) {
        if (raw.unanswerable()) {
            return new GuardedAnswer(raw.answer(), true, UnanswerableReason.NOT_IN_TRANSCRIPT, List.of());
        }

        List<Integer> validIndices = raw.citedSegmentIndices() == null
                ? List.of()
                : raw.citedSegmentIndices().stream().filter(allowedSegmentIndices::contains).distinct().toList();

        if (validIndices.isEmpty()) {
            return new GuardedAnswer(raw.answer(), true, UnanswerableReason.NO_VALID_CITATIONS, List.of());
        }

        List<AttributionRange> ranges = validIndices.stream().map(i -> new AttributionRange(i, i)).toList();
        return new GuardedAnswer(raw.answer(), false, null, attributionResolver.resolveFlat(ranges, segments, segmentCount));
    }
}
