package com.meetingiq.platform.llm.mock;

import com.meetingiq.platform.llm.spi.EmbeddingQuery;
import com.meetingiq.platform.llm.spi.EmbeddingResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * {@link MockEmbeddingProvider} has to satisfy two properties for the RAG
 * pipeline to be meaningfully testable offline: determinism (same text ->
 * same vector, always) and a real lexical-similarity signal (shared
 * vocabulary -> higher cosine similarity than unrelated text) — both are
 * exercised directly here.
 */
class MockEmbeddingProviderTest {

    private final MockEmbeddingProvider provider = new MockEmbeddingProvider();

    private static double cosine(float[] a, float[] b) {
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    @Test
    void isAlwaysAvailable() {
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    void sameTextAlwaysProducesTheSameVector() {
        EmbeddingResult first = provider.embed(EmbeddingQuery.of(List.of("the digits task and adaptation")));
        EmbeddingResult second = provider.embed(EmbeddingQuery.of(List.of("the digits task and adaptation")));

        assertThat(first.vectors()[0]).isEqualTo(second.vectors()[0]);
    }

    @Test
    void vectorsAreL2Normalized() {
        EmbeddingResult result = provider.embed(EmbeddingQuery.of(List.of("some meeting transcript text here")));
        double normSquared = 0;
        for (float v : result.vectors()[0]) {
            normSquared += (double) v * v;
        }
        assertThat(Math.sqrt(normSquared)).isCloseTo(1.0, offset(1e-5));
    }

    @Test
    void sharedVocabularyScoresHigherThanUnrelatedText() {
        EmbeddingResult result = provider.embed(EmbeddingQuery.of(List.of(
                "Professor B recommended running a test on the TI digits task",
                "we should run the SRI system on the digits task as well",
                "the weather forecast for tomorrow shows heavy rain and wind"
        )));
        float[] question = result.vectors()[0];
        float[] related = result.vectors()[1];
        float[] unrelated = result.vectors()[2];

        assertThat(cosine(question, related)).isGreaterThan(cosine(question, unrelated));
    }

    /**
     * Regression test for a real bug caught while curl-testing the RAG chat endpoint: before
     * stop words were filtered, an ordinary English out-of-scope question like "What is the
     * weather today?" scored ABOVE the 0.35 relevance floor against a real meeting transcript
     * purely from hash collisions on function words ("what", "is", "the") — meaning ScopeGuard's
     * out-of-scope path was untestable under the mock provider for realistic queries. A query
     * built entirely from stop words must now carry no similarity signal at all.
     */
    @Test
    void stopWordOnlyQueryHasNoSimilarityToContentBearingText() {
        EmbeddingResult result = provider.embed(EmbeddingQuery.of(List.of(
                "What is the weather today?",
                "Professor B recommended running a test on the TI digits task"
        )));
        assertThat(cosine(result.vectors()[0], result.vectors()[1])).isZero();
    }

    @Test
    void identicalTextHasCosineSimilarityOfOne() {
        EmbeddingResult result = provider.embed(EmbeddingQuery.of(List.of("identical text", "identical text")));
        assertThat(cosine(result.vectors()[0], result.vectors()[1])).isCloseTo(1.0, offset(1e-5));
    }

    @Test
    void reportsConfiguredDimensionsAndProviderId() {
        EmbeddingResult result = provider.embed(EmbeddingQuery.of(List.of("hello world")));
        assertThat(result.dims()).isEqualTo(result.vectors()[0].length);
        assertThat(result.providerId()).isEqualTo("mock");
    }

    @Test
    void emptyTextProducesAZeroVectorRatherThanFailing() {
        EmbeddingResult result = provider.embed(EmbeddingQuery.of(List.of("")));
        for (float v : result.vectors()[0]) {
            assertThat(v).isZero();
        }
    }
}
