package com.meetingiq.platform.rag;

import com.meetingiq.platform.api.exception.ResourceNotFoundException;
import com.meetingiq.platform.config.RagProperties;
import com.meetingiq.platform.domain.Meeting;
import com.meetingiq.platform.domain.MeetingChunk;
import com.meetingiq.platform.llm.core.LlmProviderRegistry;
import com.meetingiq.platform.llm.spi.EmbeddingProvider;
import com.meetingiq.platform.llm.spi.EmbeddingQuery;
import com.meetingiq.platform.llm.spi.EmbeddingResult;
import com.meetingiq.platform.repository.MeetingChunkRepository;
import com.meetingiq.platform.repository.MeetingRepository;
import com.meetingiq.platform.util.Sha256;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds and maintains {@code meeting_chunks} lazily — the first question
 * asked about a meeting embeds its ~23 (median) chunks in one batched call;
 * every later question against that meeting is a pure read. There is
 * deliberately no admin endpoint to trigger this ahead of time: it costs
 * nothing to be lazy at this scale (a few cents corpus-wide with a real
 * provider, free with the mock one), and a separate build step is one more
 * thing to run or forget.
 *
 * <p>Idempotent by design: each chunk's {@code contentHash} + {@code embeddingModel}
 * is checked before re-embedding, so calling this repeatedly (including
 * every retrieval) only ever does real work the first time a meeting is
 * asked about, or after a genuine change (transcript re-ingested with
 * different chunking config, or the embedding model switched).
 */
@Service
public class EmbeddingIndexService {

    private final MeetingRepository meetingRepository;
    private final MeetingChunkRepository chunkRepository;
    private final ChunkBuilder chunkBuilder;
    private final LlmProviderRegistry providerRegistry;
    private final RagProperties ragProperties;

    public EmbeddingIndexService(
            MeetingRepository meetingRepository,
            MeetingChunkRepository chunkRepository,
            ChunkBuilder chunkBuilder,
            LlmProviderRegistry providerRegistry,
            RagProperties ragProperties
    ) {
        this.meetingRepository = meetingRepository;
        this.chunkRepository = chunkRepository;
        this.chunkBuilder = chunkBuilder;
        this.providerRegistry = providerRegistry;
        this.ragProperties = ragProperties;
    }

    /** Ensures {@code meeting_chunks} is up to date for {@code meetingId}, embedding only what's missing or changed. */
    public void ensureIndexed(String meetingId, String embeddingProviderId) {
        Meeting meeting = meetingRepository.findFullById(meetingId)
                .orElseThrow(() -> ResourceNotFoundException.meeting(meetingId));
        EmbeddingProvider provider = providerRegistry.resolveEmbedding(embeddingProviderId);
        String model = provider.descriptor().embeddingModel();
        String providerId = provider.descriptor().id();

        List<ChunkCandidate> candidates = chunkBuilder.build(
                meeting.transcriptSegments(), ragProperties.chunkTargetTokens(), ragProperties.chunkOverlapSegments());

        List<MeetingChunk> existing = chunkRepository.findByMeetingId(meetingId);
        Map<String, MeetingChunk> existingById = existing.stream().collect(Collectors.toMap(MeetingChunk::id, Function.identity()));

        Map<ChunkCandidate, String> hashByCandidate = new HashMap<>();
        List<ChunkCandidate> needsEmbedding = new ArrayList<>();
        for (ChunkCandidate candidate : candidates) {
            String hash = Sha256.hex(candidate.text());
            hashByCandidate.put(candidate, hash);
            MeetingChunk current = existingById.get(chunkId(meetingId, candidate.chunkIndex()));
            boolean upToDate = current != null && hash.equals(current.contentHash()) && model.equals(current.embeddingModel());
            if (!upToDate) {
                needsEmbedding.add(candidate);
            }
        }

        if (!needsEmbedding.isEmpty()) {
            embedAndSave(meetingId, needsEmbedding, hashByCandidate, provider, model, providerId);
        }
        pruneStaleChunks(existing, candidates.size());
    }

    private void embedAndSave(
            String meetingId,
            List<ChunkCandidate> needsEmbedding,
            Map<ChunkCandidate, String> hashByCandidate,
            EmbeddingProvider provider,
            String model,
            String providerId
    ) {
        List<String> texts = needsEmbedding.stream().map(ChunkCandidate::text).toList();
        EmbeddingResult result = provider.embed(EmbeddingQuery.of(texts));
        Instant now = Instant.now();

        List<MeetingChunk> toSave = new ArrayList<>(needsEmbedding.size());
        for (int i = 0; i < needsEmbedding.size(); i++) {
            ChunkCandidate candidate = needsEmbedding.get(i);
            toSave.add(new MeetingChunk(
                    chunkId(meetingId, candidate.chunkIndex()), meetingId, candidate.chunkIndex(),
                    candidate.startIndex(), candidate.endIndex(), candidate.text(), candidate.speakers(),
                    candidate.tokenEstimate(), hashByCandidate.get(candidate),
                    EmbeddingCodec.encode(result.vectors()[i]), model, providerId, result.dims(), now
            ));
        }
        chunkRepository.saveAll(toSave);
    }

    /** Removes chunks left over from a previous run whose chunking produced more chunks than the current run does. */
    private void pruneStaleChunks(List<MeetingChunk> existing, int currentChunkCount) {
        for (MeetingChunk chunk : existing) {
            if (chunk.chunkIndex() >= currentChunkCount) {
                chunkRepository.deleteById(chunk.id());
            }
        }
    }

    private static String chunkId(String meetingId, int chunkIndex) {
        return meetingId + "#" + String.format("%04d", chunkIndex);
    }
}
