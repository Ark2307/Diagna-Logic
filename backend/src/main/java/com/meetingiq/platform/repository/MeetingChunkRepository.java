package com.meetingiq.platform.repository;

import com.meetingiq.platform.domain.MeetingChunk;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Plain Spring Data CRUD for {@code meeting_chunks} — the RAG index.
 *
 * <p>Retrieval always reads {@link #findByMeetingId(String)} first (loading
 * a single meeting's chunk set, at most ~90 documents) and scores them
 * in-memory; see {@code MeetingRetriever}. There is deliberately no
 * cross-meeting query here — that would defeat the hard scoping the RAG
 * layer relies on.
 */
public interface MeetingChunkRepository extends MongoRepository<MeetingChunk, String> {

    List<MeetingChunk> findByMeetingId(String meetingId);

    long countByMeetingId(String meetingId);

    void deleteByMeetingId(String meetingId);

    boolean existsByMeetingIdAndContentHashAndEmbeddingModel(String meetingId, String contentHash, String embeddingModel);
}
