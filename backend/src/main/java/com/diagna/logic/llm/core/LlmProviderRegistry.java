package com.diagna.logic.llm.core;

import com.diagna.logic.config.LlmProperties;
import com.diagna.logic.llm.spi.EmbeddingProvider;
import com.diagna.logic.llm.spi.LlmProvider;
import com.diagna.logic.llm.spi.ProviderUnavailableException;
import com.diagna.logic.llm.spi.UnknownProviderException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Resolves an {@link LlmProvider}/{@link EmbeddingProvider} by request-supplied
 * id, falling back to {@code diagna.llm.default-provider}. This is the one
 * place in the app that turns a provider-id string into an actual provider —
 * everything else (services, controllers) calls through here rather than
 * ever holding a reference to a specific provider bean, which is what keeps
 * adding a vendor a one-class change: register the new bean and it's
 * immediately resolvable by id, with zero other code touched.
 */
@Component
public class LlmProviderRegistry {

    private final Map<String, LlmProvider> llmProviders;
    private final Map<String, EmbeddingProvider> embeddingProviders;
    private final LlmProperties properties;

    public LlmProviderRegistry(List<LlmProvider> llmProviders, List<EmbeddingProvider> embeddingProviders, LlmProperties properties) {
        this.llmProviders = index(llmProviders, p -> p.descriptor().id());
        this.embeddingProviders = index(embeddingProviders, p -> p.descriptor().id());
        this.properties = properties;
    }

    /** Resolves a chat provider by id, or the configured default when {@code requestedId} is null/blank. */
    public LlmProvider resolveLlm(String requestedId) {
        LlmProvider provider = llmProviders.get(effectiveId(requestedId));
        if (provider == null) {
            throw new UnknownProviderException(
                    "Unknown LLM provider '" + effectiveId(requestedId) + "'. Registered providers: " + llmProviders.keySet());
        }
        if (!provider.isAvailable()) {
            throw new ProviderUnavailableException(
                    "Provider '" + provider.descriptor().id() + "' is not available — check its configuration (e.g. API key)");
        }
        return provider;
    }

    /** Resolves an embedding provider by id, or the configured default when {@code requestedId} is null/blank. */
    public EmbeddingProvider resolveEmbedding(String requestedId) {
        EmbeddingProvider provider = embeddingProviders.get(effectiveId(requestedId));
        if (provider == null) {
            throw new UnknownProviderException(
                    "Unknown embedding provider '" + effectiveId(requestedId) + "'. Registered providers: " + embeddingProviders.keySet());
        }
        if (!provider.isAvailable()) {
            throw new ProviderUnavailableException(
                    "Embedding provider '" + provider.descriptor().id() + "' is not available — check its configuration (e.g. API key)");
        }
        return provider;
    }

    private String effectiveId(String requestedId) {
        return (requestedId == null || requestedId.isBlank()) ? properties.defaultProvider() : requestedId;
    }

    private static <T> Map<String, T> index(List<T> items, Function<T, String> idOf) {
        Map<String, T> map = new LinkedHashMap<>();
        for (T item : items) {
            map.put(idOf.apply(item), item);
        }
        return map;
    }
}
