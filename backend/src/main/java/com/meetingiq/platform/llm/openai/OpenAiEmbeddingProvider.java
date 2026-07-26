package com.meetingiq.platform.llm.openai;

import com.meetingiq.platform.config.LlmProperties;
import com.meetingiq.platform.domain.TokenUsage;
import com.meetingiq.platform.llm.core.AbstractEmbeddingProvider;
import com.meetingiq.platform.llm.spi.EmbeddingQuery;
import com.meetingiq.platform.llm.spi.EmbeddingResult;
import com.meetingiq.platform.llm.spi.ProviderDescriptor;
import com.openai.client.OpenAIClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The OpenAI implementation of {@link com.meetingiq.platform.llm.spi.EmbeddingProvider}
 * ({@code text-embedding-3-small} by default). This is the only class that
 * imports the {@code com.openai.*} embeddings SDK; {@code MeetingRetriever}
 * and {@code EmbeddingIndexService} work purely against {@link EmbeddingQuery}/
 * {@link EmbeddingResult}.
 */
@Component
public class OpenAiEmbeddingProvider extends AbstractEmbeddingProvider {

    private static final String PROVIDER_ID = "openai";

    private final LlmProperties properties;
    private final OpenAIClient client;

    public OpenAiEmbeddingProvider(LlmProperties properties, ObjectProvider<OpenAIClient> clientProvider) {
        this.properties = properties;
        this.client = clientProvider.getIfAvailable();
    }

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(
                PROVIDER_ID, "OpenAI", true, true,
                properties.openai().chatModel(), properties.openai().embeddingModel()
        );
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    protected String resolveModel(String requestedModel) {
        return (requestedModel != null && !requestedModel.isBlank()) ? requestedModel : properties.openai().embeddingModel();
    }

    @Override
    protected EmbeddingResult doEmbed(EmbeddingQuery query, String resolvedModel) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .inputOfArrayOfStrings(query.texts())
                .model(resolvedModel)
                .dimensions((long) properties.openai().embeddingDimensions())
                .build();

        CreateEmbeddingResponse response = client.embeddings().create(params);
        List<Embedding> data = response.data();

        float[][] vectors = new float[data.size()][];
        for (Embedding embedding : data) {
            // Assign by the response's own declared index rather than iteration order —
            // the API does not guarantee response order matches input order.
            List<Float> values = embedding.embedding();
            float[] vector = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                vector[i] = values.get(i);
            }
            vectors[Math.toIntExact(embedding.index())] = vector;
        }
        int dims = vectors.length > 0 ? vectors[0].length : 0;

        TokenUsage usage = new TokenUsage(Math.toIntExact(response.usage().promptTokens()), 0, Math.toIntExact(response.usage().totalTokens()));
        return new EmbeddingResult(vectors, dims, resolvedModel, PROVIDER_ID, usage);
    }
}
