package com.diagna.logic.llm.mock;

import com.diagna.logic.llm.core.LlmResponseCache;
import com.diagna.logic.llm.spi.LlmOptions;
import com.diagna.logic.llm.spi.LlmQuery;
import com.diagna.logic.llm.spi.ResponseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * {@link MockLlmProvider} is the whole app's ability to run and be tested
 * with no API key, so its determinism and its grounding-from-the-prompt
 * behaviour are directly tested here rather than only exercised indirectly.
 */
@ExtendWith(MockitoExtension.class)
class MockLlmProviderTest {

    @Mock
    private LlmResponseCache cache;

    private MockLlmProvider provider;

    @BeforeEach
    void setUp() {
        lenient().when(cache.isEnabled()).thenReturn(false);
        provider = new MockLlmProvider(cache);
    }

    private static TestQuery queryWithPrompt(String userPrompt) {
        return new TestQuery(userPrompt);
    }

    @Test
    void isAlwaysAvailableAndNeedsNoConfiguration() {
        assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    void extractsCitedSegmentIndicesFromNumberedTranscriptMarkers() {
        String prompt = "[108] Professor B: some text\n[118] Professor B: more text\nQuestion: what was recommended?";
        var result = provider.execute(queryWithPrompt(prompt));

        assertThat(result.payload().citedSegmentIndices()).containsExactly(108, 118);
        assertThat(result.payload().unanswerable()).isFalse();
    }

    @Test
    void marksUnanswerableWhenPromptHasNoSegmentMarkers() {
        var result = provider.execute(queryWithPrompt("What's the weather like today?"));

        assertThat(result.payload().unanswerable()).isTrue();
        assertThat(result.payload().citedSegmentIndices()).isEmpty();
    }

    @Test
    void capsCitedSegmentsAtThreeAndDedupes() {
        String prompt = "[1] a\n[2] b\n[3] c\n[4] d\n[2] b again";
        var result = provider.execute(queryWithPrompt(prompt));

        assertThat(result.payload().citedSegmentIndices()).containsExactly(1, 2, 3);
    }

    @Test
    void isDeterministicForTheSamePrompt() {
        String prompt = "[42] Speaker: some transcript content";
        var first = provider.execute(queryWithPrompt(prompt));
        var second = provider.execute(queryWithPrompt(prompt));

        assertThat(first.payload()).isEqualTo(second.payload());
        assertThat(first.rawText()).isEqualTo(second.rawText());
    }

    @Test
    void neverCallsRealNetworkAndReportsSyntheticUsage() {
        var result = provider.execute(queryWithPrompt("[7] Speaker: hello"));
        assertThat(result.usage().totalTokens()).isGreaterThan(0);
        assertThat(result.providerId()).isEqualTo("mock");
        assertThat(result.cached()).isFalse();
    }

    /**
     * Regression test for a real bug caught while curl-testing {@code /ai/generate}'s map-reduce
     * path: the reduce step's input is prior partial summaries (real prose), not a numbered
     * transcript, so it has no {@code [N]} markers at all. The chat-specific "unanswerable"
     * concept must not blank out the generation ({@code text}/{@code structured}) shape just
     * because no segments were cited — generation should still produce real content from
     * whatever it was given.
     */
    @Test
    void generationShapeProducesRealContentEvenWithNoSegmentMarkersInThePrompt() {
        String reduceStepInput = "Section 1:\nThe team discussed digit recognition and adaptation strategies.\n\n"
                + "Section 2:\nThey compared results against the Switchboard baseline.";
        var query = new GenerationTestQuery(reduceStepInput);
        var result = provider.execute(query);

        assertThat(result.payload().text()).isNotBlank();
        assertThat(result.payload().text()).doesNotContain("doesn't appear to cover that");
        assertThat(result.payload().structured().overview()).isEqualTo(result.payload().text());
    }

    /** Minimal payload record mirroring the {@code answer/unanswerable/citedSegmentIndices} envelope. */
    private record Payload(String answer, boolean unanswerable, java.util.List<Integer> citedSegmentIndices) {
    }

    /** Minimal payload record mirroring the {@code text/structured} generation envelope. */
    private record GenerationPayload(String text, StructuredPayload structured) {
    }

    private record StructuredPayload(String overview) {
    }

    private static class GenerationTestQuery extends LlmQuery<GenerationPayload> {
        private final String userPrompt;

        GenerationTestQuery(String userPrompt) {
            super(LlmOptions.jsonDefaults());
            this.userPrompt = userPrompt;
        }

        @Override
        public String taskName() {
            return "generate:summary";
        }

        @Override
        public String systemPrompt() {
            return "You are analyzing a meeting transcript.";
        }

        @Override
        public String userPrompt() {
            return userPrompt;
        }

        @Override
        public ResponseParser<GenerationPayload> parser() {
            return raw -> {
                var mapper = com.diagna.logic.llm.core.LlmJsonMapper.create();
                try {
                    return mapper.readValue(raw, GenerationPayload.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }

    private static class TestQuery extends LlmQuery<Payload> {
        private final String userPrompt;

        TestQuery(String userPrompt) {
            super(LlmOptions.jsonDefaults());
            this.userPrompt = userPrompt;
        }

        @Override
        public String taskName() {
            return "test-task";
        }

        @Override
        public String systemPrompt() {
            return "You are a helpful assistant.";
        }

        @Override
        public String userPrompt() {
            return userPrompt;
        }

        @Override
        public ResponseParser<Payload> parser() {
            return raw -> {
                var mapper = com.diagna.logic.llm.core.LlmJsonMapper.create();
                try {
                    return mapper.readValue(raw, Payload.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }
}
