package com.diagna.logic.llm.core;

import com.diagna.logic.llm.spi.EmbeddingProvider;
import com.diagna.logic.llm.spi.EmbeddingQuery;
import com.diagna.logic.llm.spi.EmbeddingResult;
import com.diagna.logic.llm.spi.LlmException;
import com.diagna.logic.llm.spi.ProviderCallException;
import com.diagna.logic.llm.spi.ProviderUnavailableException;

/**
 * The embedding-side counterpart to {@link AbstractLlmProvider}: validates
 * availability and translates unexpected exceptions, leaving only model
 * resolution and the actual SDK call to the concrete provider.
 *
 * <p>No response cache here (unlike the chat path) — embeddings are already
 * made idempotent one layer up, by {@code EmbeddingIndexService} comparing
 * each chunk's {@code contentHash} + {@code embeddingModel} before ever
 * calling {@link #embed}, so a second cache at this layer would only add
 * complexity without avoiding any real work.
 */
public abstract class AbstractEmbeddingProvider implements EmbeddingProvider {

    @Override
    public final EmbeddingResult embed(EmbeddingQuery query) {
        if (!isAvailable()) {
            throw new ProviderUnavailableException(
                    "Provider '" + descriptor().id() + "' is not available — check its configuration (e.g. API key)");
        }
        String model = resolveModel(query.model());
        try {
            return doEmbed(query, model);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderCallException("Embedding call to provider '" + descriptor().id() + "' failed", e);
        }
    }

    /** Resolves the model to call: {@code requestedModel} when set, otherwise this provider's configured default. */
    protected abstract String resolveModel(String requestedModel);

    protected abstract EmbeddingResult doEmbed(EmbeddingQuery query, String resolvedModel);
}
