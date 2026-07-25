package com.diagna.logic.llm.core;

import com.diagna.logic.domain.LlmInvocation;
import com.diagna.logic.domain.TokenUsage;
import com.diagna.logic.llm.spi.FinishReason;
import com.diagna.logic.llm.spi.LlmCompletion;
import com.diagna.logic.llm.spi.LlmOptions;
import com.diagna.logic.llm.spi.LlmParseException;
import com.diagna.logic.llm.spi.LlmQuery;
import com.diagna.logic.llm.spi.LlmResult;
import com.diagna.logic.llm.spi.ProviderCallException;
import com.diagna.logic.llm.spi.ProviderDescriptor;
import com.diagna.logic.llm.spi.ProviderUnavailableException;
import com.diagna.logic.llm.spi.ResponseParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies the fixed flow every {@link AbstractLlmProvider} shares:
 * availability check -> cache lookup -> provider call -> parse -> cache
 * write -> result — using a minimal test double so the ordering and
 * exception-translation rules are tested independent of any real vendor.
 */
@ExtendWith(MockitoExtension.class)
class AbstractLlmProviderTest {

    @Mock
    private LlmResponseCache cache;

    private TestProvider provider;

    @BeforeEach
    void setUp() {
        provider = new TestProvider(cache);
    }

    @Test
    void unavailableProviderThrowsWithoutTouchingCacheOrCallingDoComplete() {
        provider.available = false;
        TestQuery query = new TestQuery(raw -> raw);

        assertThatThrownBy(() -> provider.execute(query)).isInstanceOf(ProviderUnavailableException.class);

        assertThat(provider.doCompleteCalls).isZero();
        verifyNoInteractions(cache);
    }

    @Test
    void cacheMissCallsProviderThenSaves() {
        when(cache.isEnabled()).thenReturn(true);
        when(cache.find(anyString())).thenReturn(Optional.empty());
        TestQuery query = new TestQuery(raw -> raw);

        LlmResult<String> result = provider.execute(query);

        assertThat(provider.doCompleteCalls).isEqualTo(1);
        assertThat(result.cached()).isFalse();
        assertThat(result.payload()).isEqualTo(provider.completionToReturn.text());
        verify(cache).save(anyString(), eq("test"), eq("test-task"), isNull(), eq(provider.completionToReturn), any(Duration.class));
    }

    @Test
    void cacheHitSkipsTheProviderCallEntirely() {
        LlmInvocation cached = new LlmInvocation(
                "some-key", "test", "test-model", "test-task", null,
                new TokenUsage(1, 1, 2), 5L, "{\"cached\":true}", Instant.now()
        );
        when(cache.isEnabled()).thenReturn(true);
        when(cache.find(anyString())).thenReturn(Optional.of(cached));
        TestQuery query = new TestQuery(raw -> raw);

        LlmResult<String> result = provider.execute(query);

        assertThat(provider.doCompleteCalls).isZero();
        assertThat(result.cached()).isTrue();
        assertThat(result.payload()).isEqualTo("{\"cached\":true}");
        verify(cache, never()).save(any(), any(), any(), any(), any(), any());
    }

    @Test
    void disabledCacheSkipsBothLookupAndSave() {
        when(cache.isEnabled()).thenReturn(false);
        TestQuery query = new TestQuery(raw -> raw);

        provider.execute(query);

        verify(cache, never()).find(any());
        verify(cache, never()).save(any(), any(), any(), any(), any(), any());
    }

    @Test
    void parseExceptionFromTheParserIsRethrownUnwrapped() {
        when(cache.isEnabled()).thenReturn(false);
        TestQuery query = new TestQuery(raw -> {
            throw new LlmParseException("deliberately bad payload");
        });

        assertThatThrownBy(() -> provider.execute(query))
                .isInstanceOf(LlmParseException.class)
                .hasMessage("deliberately bad payload");
    }

    @Test
    void genericParserFailureIsWrappedAsLlmParseException() {
        when(cache.isEnabled()).thenReturn(false);
        TestQuery query = new TestQuery(raw -> {
            throw new RuntimeException("not json at all");
        });

        assertThatThrownBy(() -> provider.execute(query)).isInstanceOf(LlmParseException.class);
    }

    @Test
    void genericDoCompleteFailureIsWrappedAsProviderCallException() {
        when(cache.isEnabled()).thenReturn(false);
        provider.doCompleteThrows = new RuntimeException("connection reset");
        TestQuery query = new TestQuery(raw -> raw);

        assertThatThrownBy(() -> provider.execute(query)).isInstanceOf(ProviderCallException.class);
    }

    @Test
    void llmExceptionFromDoCompleteIsRethrownUnwrapped() {
        when(cache.isEnabled()).thenReturn(false);
        provider.doCompleteThrows = new ProviderUnavailableException("nested failure");
        TestQuery query = new TestQuery(raw -> raw);

        assertThatThrownBy(() -> provider.execute(query))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessage("nested failure");
    }

    @Test
    void successfulCallPopulatesUsageModelProviderAndLatency() {
        when(cache.isEnabled()).thenReturn(false);
        TestQuery query = new TestQuery(raw -> raw);

        LlmResult<String> result = provider.execute(query);

        assertThat(result.usage()).isEqualTo(provider.completionToReturn.usage());
        assertThat(result.model()).isEqualTo(provider.completionToReturn.model());
        assertThat(result.providerId()).isEqualTo("test");
        assertThat(result.latency()).isNotNull();
        assertThat(result.finishReason()).isEqualTo(FinishReason.STOP);
    }

    // --- test doubles ---------------------------------------------------

    private static class TestProvider extends AbstractLlmProvider {
        boolean available = true;
        int doCompleteCalls = 0;
        RuntimeException doCompleteThrows = null;
        final LlmCompletion completionToReturn =
                new LlmCompletion("{\"x\":1}", "test-model", new TokenUsage(3, 4, 7), FinishReason.STOP);

        TestProvider(LlmResponseCache cache) {
            super(cache);
        }

        @Override
        public ProviderDescriptor descriptor() {
            return new ProviderDescriptor("test", "Test Provider", true, true, "test-model", "test-model");
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        protected String resolveModel(LlmOptions options) {
            return "test-model";
        }

        @Override
        protected LlmCompletion doComplete(LlmQuery<?> query, String resolvedModel) {
            doCompleteCalls++;
            if (doCompleteThrows != null) {
                throw doCompleteThrows;
            }
            return completionToReturn;
        }
    }

    private static class TestQuery extends LlmQuery<String> {
        private final ResponseParser<String> parser;

        TestQuery(ResponseParser<String> parser) {
            super(LlmOptions.jsonDefaults());
            this.parser = parser;
        }

        @Override
        public String taskName() {
            return "test-task";
        }

        @Override
        public String systemPrompt() {
            return "system prompt";
        }

        @Override
        public String userPrompt() {
            return "user prompt";
        }

        @Override
        public ResponseParser<String> parser() {
            return parser;
        }
    }
}
