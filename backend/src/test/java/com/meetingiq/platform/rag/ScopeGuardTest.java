package com.meetingiq.platform.rag;

import com.meetingiq.platform.config.RagProperties;
import com.meetingiq.platform.domain.Citation;
import com.meetingiq.platform.domain.TranscriptSegment;
import com.meetingiq.platform.domain.enums.UnanswerableReason;
import com.meetingiq.platform.service.AttributionResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code ScopeGuard} is the component that keeps this app's RAG chat from
 * ever answering out of scope or generically — these tests pin its two
 * enforcement layers directly, independent of any real LLM call.
 */
class ScopeGuardTest {

    private static final double MIN_RELEVANCE = 0.35;

    private final RagProperties ragProperties = new RagProperties(400, 1, 6, MIN_RELEVANCE, RagProperties.ContextMode.AUTO, 12000, 6000);
    private final ScopeGuard scopeGuard = new ScopeGuard(ragProperties, new AttributionResolver());

    private static List<TranscriptSegment> transcript(int count) {
        return IntStream.range(0, count).mapToObj(i -> new TranscriptSegment(i, "Speaker", "text-" + i)).toList();
    }

    // --- layer 2: relevance floor ---------------------------------------

    @Test
    void belowFloorScoreReturnsOutOfScopeAnswer() {
        Optional<GuardedAnswer> result = scopeGuard.checkRelevanceFloor(MIN_RELEVANCE - 0.01);

        assertThat(result).isPresent();
        assertThat(result.get().unanswerable()).isTrue();
        assertThat(result.get().reason()).isEqualTo(UnanswerableReason.OUT_OF_SCOPE);
        assertThat(result.get().citations()).isEmpty();
    }

    @Test
    void atOrAboveFloorScoreProceedsToTheLlm() {
        assertThat(scopeGuard.checkRelevanceFloor(MIN_RELEVANCE)).isEmpty();
        assertThat(scopeGuard.checkRelevanceFloor(MIN_RELEVANCE + 0.5)).isEmpty();
    }

    @Test
    void zeroScoreIsOutOfScope() {
        assertThat(scopeGuard.checkRelevanceFloor(0.0)).isPresent();
    }

    // --- layer 4: citation verification -----------------------------------

    @Test
    void modelDeclaringUnanswerableIsRespectedWithNotInTranscriptReason() {
        GroundedAnswer raw = new GroundedAnswer("I don't know", true, List.of());
        GuardedAnswer result = scopeGuard.verify(raw, Set.of(1, 2, 3), transcript(10), 10);

        assertThat(result.unanswerable()).isTrue();
        assertThat(result.reason()).isEqualTo(UnanswerableReason.NOT_IN_TRANSCRIPT);
        assertThat(result.citations()).isEmpty();
    }

    @Test
    void allCitationsOutsideAllowedSetForcesUnanswerableWithNoValidCitationsReason() {
        // The model claims answerable and cites segment 99, but 99 was never shown to it.
        GroundedAnswer raw = new GroundedAnswer("Some answer", false, List.of(99));
        GuardedAnswer result = scopeGuard.verify(raw, Set.of(1, 2, 3), transcript(100), 100);

        assertThat(result.unanswerable()).isTrue();
        assertThat(result.reason()).isEqualTo(UnanswerableReason.NO_VALID_CITATIONS);
        assertThat(result.citations()).isEmpty();
    }

    @Test
    void hallucinatedCitationIsDroppedEvenWhenItIsAValidTranscriptIndexElsewhere() {
        // Segment 50 is a perfectly real segment in this 100-segment meeting, but it's outside
        // what was actually shown to the model for this turn — it must not survive verification.
        GroundedAnswer raw = new GroundedAnswer("Answer citing real but unseen segment", false, List.of(2, 50));
        GuardedAnswer result = scopeGuard.verify(raw, Set.of(1, 2, 3), transcript(100), 100);

        assertThat(result.unanswerable()).isFalse();
        assertThat(result.citations()).extracting(Citation::segmentIndex).containsExactly(2);
    }

    @Test
    void validCitationsSurviveAndResolveToRealSegments() {
        GroundedAnswer raw = new GroundedAnswer("Answer citing valid segments", false, List.of(2, 3));
        GuardedAnswer result = scopeGuard.verify(raw, Set.of(1, 2, 3), transcript(10), 10);

        assertThat(result.unanswerable()).isFalse();
        assertThat(result.reason()).isNull();
        assertThat(result.citations()).extracting(Citation::segmentIndex).containsExactly(2, 3);
    }

    @Test
    void duplicateCitedIndicesAreDeduplicated() {
        GroundedAnswer raw = new GroundedAnswer("Answer", false, List.of(2, 2, 2));
        GuardedAnswer result = scopeGuard.verify(raw, Set.of(2), transcript(10), 10);

        assertThat(result.citations()).hasSize(1);
    }

    @Test
    void nullCitedIndicesListIsTreatedAsNoValidCitations() {
        GroundedAnswer raw = new GroundedAnswer("Answer", false, null);
        GuardedAnswer result = scopeGuard.verify(raw, Set.of(1, 2, 3), transcript(10), 10);

        assertThat(result.unanswerable()).isTrue();
        assertThat(result.reason()).isEqualTo(UnanswerableReason.NO_VALID_CITATIONS);
    }
}
