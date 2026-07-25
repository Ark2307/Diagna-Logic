package com.diagna.logic.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * One RAG retrieval unit: a window of consecutive transcript segments
 * (never splitting a segment), embedded with its vector representation.
 *
 * <p>Deliberately shares {@link AttributionRange}'s inclusive
 * {@code [startIndex, endIndex]} contract — a retrieved chunk and a gold
 * dataset attribution are the same kind of thing, so
 * {@code AttributionResolver} resolves both without a separate code path.
 *
 * <p>Built lazily by {@code EmbeddingIndexService.ensureIndexed}, not by the
 * ETL: chunking needs no external service, but embedding does, so this
 * collection is populated the first time a meeting is actually asked a
 * question, not at load time. {@code contentHash} + {@code embeddingModel}
 * make that build idempotent — re-running it only re-embeds what changed.
 *
 * <p>The embedding is stored as raw little-endian float32 bytes (BSON
 * BinData) rather than a BSON array of doubles — half the size for the same
 * precision we actually need. See {@code EmbeddingCodec} for the codec.
 */
@Document(collection = "meeting_chunks")
public record MeetingChunk(
        /** {@code "<meetingId>#<chunkIndex, zero-padded to 4 digits>"}, e.g. {@code "ES2002c#0012"}. */
        @Id String id,
        @Field("meetingId") String meetingId,
        int chunkIndex,
        int startIndex,
        int endIndex,
        /** Numbered transcript lines, e.g. {@code "[240] Project Manager: ..."}. */
        String text,
        List<String> speakers,
        int tokenEstimate,
        /** SHA-256 of {@link #text()}; the idempotency key for (re)embedding. */
        String contentHash,
        byte[] embedding,
        String embeddingModel,
        String embeddingProvider,
        int dims,
        Instant createdAt
) {

    /** This chunk's range as an {@link AttributionRange}, for reuse with {@code AttributionResolver}. */
    public AttributionRange range() {
        return new AttributionRange(startIndex, endIndex);
    }
}
