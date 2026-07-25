package com.diagna.logic.llm.core;

import com.diagna.logic.config.LlmProperties;
import com.diagna.logic.llm.spi.EmbeddingProvider;
import com.diagna.logic.llm.spi.EmbeddingQuery;
import com.diagna.logic.llm.spi.EmbeddingResult;
import com.diagna.logic.llm.spi.LlmProvider;
import com.diagna.logic.llm.spi.LlmQuery;
import com.diagna.logic.llm.spi.LlmResult;
import com.diagna.logic.llm.spi.ProviderDescriptor;
import com.diagna.logic.llm.spi.ProviderUnavailableException;
import com.diagna.logic.llm.spi.UnknownProviderException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmProviderRegistryTest {

    private static LlmProperties properties(String defaultProvider) {
        return new LlmProperties(
                defaultProvider,
                new LlmProperties.OpenAi("", "gpt-4.1-mini", "text-embedding-3-small", 1536, 60),
                new LlmProperties.Cache(true, 30)
        );
    }

    private static FakeLlmProvider llm(String id, boolean available) {
        return new FakeLlmProvider(id, available);
    }

    private static FakeEmbeddingProvider embed(String id, boolean available) {
        return new FakeEmbeddingProvider(id, available);
    }

    @Test
    void resolvesLlmByExplicitId() {
        var registry = new LlmProviderRegistry(List.of(llm("a", true), llm("b", true)), List.of(), properties("a"));
        assertThat(registry.resolveLlm("b").descriptor().id()).isEqualTo("b");
    }

    @Test
    void resolvesLlmDefaultWhenRequestedIdIsNullOrBlank() {
        var registry = new LlmProviderRegistry(List.of(llm("a", true), llm("b", true)), List.of(), properties("b"));
        assertThat(registry.resolveLlm(null).descriptor().id()).isEqualTo("b");
        assertThat(registry.resolveLlm("  ").descriptor().id()).isEqualTo("b");
    }

    @Test
    void unknownLlmIdThrowsUnknownProviderException() {
        var registry = new LlmProviderRegistry(List.of(llm("a", true)), List.of(), properties("a"));
        assertThatThrownBy(() -> registry.resolveLlm("nonexistent")).isInstanceOf(UnknownProviderException.class);
    }

    @Test
    void unavailableLlmProviderThrowsProviderUnavailableException() {
        var registry = new LlmProviderRegistry(List.of(llm("a", false)), List.of(), properties("a"));
        assertThatThrownBy(() -> registry.resolveLlm("a")).isInstanceOf(ProviderUnavailableException.class);
    }

    @Test
    void resolvesEmbeddingByExplicitIdAndDefault() {
        var registry = new LlmProviderRegistry(List.of(), List.of(embed("x", true), embed("y", true)), properties("y"));
        assertThat(registry.resolveEmbedding("x").descriptor().id()).isEqualTo("x");
        assertThat(registry.resolveEmbedding(null).descriptor().id()).isEqualTo("y");
    }

    @Test
    void unknownEmbeddingIdThrowsUnknownProviderException() {
        var registry = new LlmProviderRegistry(List.of(), List.of(embed("x", true)), properties("x"));
        assertThatThrownBy(() -> registry.resolveEmbedding("nope")).isInstanceOf(UnknownProviderException.class);
    }

    @Test
    void unavailableEmbeddingProviderThrowsProviderUnavailableException() {
        var registry = new LlmProviderRegistry(List.of(), List.of(embed("x", false)), properties("x"));
        assertThatThrownBy(() -> registry.resolveEmbedding("x")).isInstanceOf(ProviderUnavailableException.class);
    }

    private record FakeLlmProvider(String id, boolean available) implements LlmProvider {
        @Override
        public ProviderDescriptor descriptor() {
            return new ProviderDescriptor(id, id, true, false, "m", null);
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public <T> LlmResult<T> execute(LlmQuery<T> query) {
            throw new UnsupportedOperationException("not needed for registry tests");
        }
    }

    private record FakeEmbeddingProvider(String id, boolean available) implements EmbeddingProvider {
        @Override
        public ProviderDescriptor descriptor() {
            return new ProviderDescriptor(id, id, false, true, null, "m");
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public EmbeddingResult embed(EmbeddingQuery query) {
            throw new UnsupportedOperationException("not needed for registry tests");
        }
    }
}
