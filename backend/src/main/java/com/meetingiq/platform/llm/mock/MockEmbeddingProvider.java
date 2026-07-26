package com.meetingiq.platform.llm.mock;

import com.meetingiq.platform.domain.TokenUsage;
import com.meetingiq.platform.llm.core.AbstractEmbeddingProvider;
import com.meetingiq.platform.llm.spi.EmbeddingQuery;
import com.meetingiq.platform.llm.spi.EmbeddingResult;
import com.meetingiq.platform.llm.spi.ProviderDescriptor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A deterministic, keyless stand-in for a real embedding provider, using the
 * classic "hashing trick": each lowercase, non-stop-word token in the text
 * is hashed into one of {@link #DIMS} buckets and counted, then the vector
 * is L2-normalized.
 *
 * <p>This is not a semantic embedding — it can't capture meaning the way a
 * trained model can — but it is a genuine bag-of-words representation:
 * texts sharing content vocabulary land in overlapping buckets and score
 * higher on cosine similarity than unrelated texts, which is exactly the
 * property {@code MeetingRetriever}'s ranking and {@code ScopeGuard}'s
 * relevance floor need to be meaningfully exercised without any external
 * dependency. {@code String.hashCode()} is specified by the JDK to be
 * stable across runs, so the same text always produces the same vector.
 *
 * <p>Stop words are filtered before hashing — without this, an ordinary
 * English question ("What is the weather today?") collides in enough
 * buckets with common filler words in ANY real transcript to score above a
 * typical relevance floor by accident, which would make the mock provider
 * unable to demonstrate the out-of-scope path convincingly. Filtering them
 * out is standard bag-of-words practice, not a workaround: only content
 * words should count as topical signal.
 */
@Component
public class MockEmbeddingProvider extends AbstractEmbeddingProvider {

    private static final String PROVIDER_ID = "mock";
    private static final String MODEL = "mock-embed-2";
    private static final int DIMS = 4096;
    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "if", "so", "of", "to", "in", "on", "at", "for", "with",
            "is", "are", "was", "were", "be", "been", "being", "am", "do", "does", "did", "doing",
            "this", "that", "these", "those", "it", "its", "as", "by", "from", "into", "about",
            "what", "when", "where", "who", "whom", "which", "why", "how",
            "i", "you", "he", "she", "we", "they", "me", "him", "her", "us", "them", "my", "your", "his", "their", "our",
            "will", "would", "can", "could", "should", "shall", "may", "might", "must",
            "not", "no", "yes", "there", "here", "than", "then", "too", "very", "just",
            "have", "has", "had", "having", "up", "down", "out", "over", "under", "again",
            "today", "tomorrow", "yesterday"
    );

    @Override
    public ProviderDescriptor descriptor() {
        return new ProviderDescriptor(PROVIDER_ID, "Mock (offline, deterministic)", true, true, MODEL, MODEL);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    protected String resolveModel(String requestedModel) {
        return (requestedModel != null && !requestedModel.isBlank()) ? requestedModel : MODEL;
    }

    @Override
    protected EmbeddingResult doEmbed(EmbeddingQuery query, String resolvedModel) {
        float[][] vectors = new float[query.texts().size()][];
        for (int i = 0; i < query.texts().size(); i++) {
            vectors[i] = embedOne(query.texts().get(i));
        }
        int totalChars = query.texts().stream().mapToInt(String::length).sum();
        TokenUsage usage = TokenUsage.of(totalChars / 4, 0);
        return new EmbeddingResult(vectors, DIMS, resolvedModel, PROVIDER_ID, usage);
    }

    private static float[] embedOne(String text) {
        float[] vector = new float[DIMS];
        Matcher matcher = TOKEN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group();
            if (STOP_WORDS.contains(token)) {
                continue;
            }
            int bucket = Math.floorMod(token.hashCode(), DIMS);
            vector[bucket] += 1f;
        }
        normalize(vector);
        return vector;
    }

    private static void normalize(float[] vector) {
        double sumSquares = 0;
        for (float v : vector) {
            sumSquares += (double) v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
    }
}
