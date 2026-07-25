package com.diagna.logic.rag;

import com.diagna.logic.domain.MeetingChunk;
import com.diagna.logic.llm.core.LlmProviderRegistry;
import com.diagna.logic.llm.spi.EmbeddingProvider;
import com.diagna.logic.llm.spi.EmbeddingQuery;
import com.diagna.logic.repository.MeetingChunkRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Retrieves the most relevant passages from ONE meeting for a question —
 * {@code meetingId} is a required first parameter, not an optional filter,
 * so a query is structurally incapable of reaching another meeting's
 * content. This is layer 1 of {@code ScopeGuard}.
 *
 * <p>Combines two independent rankings via {@link ReciprocalRankFusion}:
 * exact cosine similarity (semantic recall) and MongoDB {@code $text}
 * search scoped to this meeting (catches jargon and proper nouns embeddings
 * tend to blur). The candidate set is always small (median 23, max ~90
 * chunks per meeting), so the cosine scan is a brute-force, exact
 * comparison — no ANN index, no approximation.
 */
@Component
public class MeetingRetriever {

    /** Cap on lexical candidates pulled per query — generous relative to a meeting's ~90-chunk ceiling. */
    private static final int LEXICAL_CANDIDATE_LIMIT = 50;

    private final MeetingChunkRepository chunkRepository;
    private final EmbeddingIndexService embeddingIndexService;
    private final LlmProviderRegistry providerRegistry;
    private final MongoTemplate mongoTemplate;

    public MeetingRetriever(
            MeetingChunkRepository chunkRepository,
            EmbeddingIndexService embeddingIndexService,
            LlmProviderRegistry providerRegistry,
            MongoTemplate mongoTemplate
    ) {
        this.chunkRepository = chunkRepository;
        this.embeddingIndexService = embeddingIndexService;
        this.providerRegistry = providerRegistry;
        this.mongoTemplate = mongoTemplate;
    }

    public RetrievalResult retrieve(String meetingId, String question, int topK, String embeddingProviderId) {
        embeddingIndexService.ensureIndexed(meetingId, embeddingProviderId);
        List<MeetingChunk> chunks = chunkRepository.findByMeetingId(meetingId);
        if (chunks.isEmpty()) {
            return RetrievalResult.empty();
        }

        EmbeddingProvider embeddingProvider = providerRegistry.resolveEmbedding(embeddingProviderId);
        float[] queryVector = embeddingProvider.embed(EmbeddingQuery.of(List.of(question))).vectors()[0];

        List<ScoredChunk> byCosine = chunks.stream()
                .map(c -> new ScoredChunk(c, CosineSimilarity.of(queryVector, EmbeddingCodec.decode(c.embedding()))))
                .sorted(Comparator.comparingDouble(ScoredChunk::cosineScore).reversed())
                .toList();
        double topCosineScore = byCosine.isEmpty() ? 0.0 : byCosine.get(0).cosineScore();

        List<MeetingChunk> byLexical = lexicalSearch(meetingId, question);
        List<MeetingChunk> fused = ReciprocalRankFusion.fuse(
                byCosine.stream().map(ScoredChunk::chunk).toList(), byLexical, MeetingChunk::id);

        Map<String, Double> cosineById = byCosine.stream()
                .collect(Collectors.toMap(sc -> sc.chunk().id(), ScoredChunk::cosineScore));
        List<ScoredChunk> topChunks = fused.stream()
                .limit(topK)
                .map(c -> new ScoredChunk(c, cosineById.getOrDefault(c.id(), 0.0)))
                .toList();

        return new RetrievalResult(topChunks, topCosineScore);
    }

    private List<MeetingChunk> lexicalSearch(String meetingId, String question) {
        TextQuery query = TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(question)).sortByScore();
        query.addCriteria(Criteria.where("meetingId").is(meetingId));
        query.limit(LEXICAL_CANDIDATE_LIMIT);
        return mongoTemplate.find(query, MeetingChunk.class);
    }
}
