package com.diagna.logic.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Ensures the indexes this application relies on exist, idempotently, on
 * every startup.
 *
 * <p>{@code meetings} and {@code dialogs} already get their indexes from
 * {@code tools/ingest/ingest_mised.py} at load time; they are re-declared
 * here too so the app is self-sufficient even if those collections were
 * populated some other way. {@code meeting_chunks}, {@code chat_conversations}
 * and {@code llm_invocations} are populated entirely by this application, so
 * this is their only source of indexes.
 *
 * <p>{@code createIndex} is a no-op when an equivalent index already exists,
 * so running this on every startup is safe and cheap.
 */
@Component
public class MongoIndexConfig implements InitializingBean {

    private final MongoTemplate mongoTemplate;

    public MongoIndexConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        ensureMeetingIndexes();
        ensureDialogIndexes();
        ensureMeetingChunkIndexes();
        ensureChatConversationIndexes();
        ensureLlmInvocationIndexes();
    }

    private void ensureMeetingIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("meetings");
        ops.createIndex(new Index().on("corpus", Direction.ASC));
        ops.createIndex(new Index().on("domain", Direction.ASC));
        ops.createIndex(new Index().on("split", Direction.ASC));
        ops.createIndex(new Index().on("segmentCount", Direction.ASC));
        ops.createIndex(TextIndexDefinition.builder().onField("transcriptSegments.text").build());
    }

    private void ensureDialogIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("dialogs");
        ops.createIndex(new Index().on("meetingId", Direction.ASC));
        ops.createIndex(new Index().on("split", Direction.ASC));
        ops.createIndex(new Index().on("turns.queryType", Direction.ASC));
        ops.createIndex(new Index().on("stats.unanswerableCount", Direction.ASC));
        ops.createIndex(TextIndexDefinition.builder()
                .onField("turns.query")
                .onField("turns.response")
                .build());
    }

    private void ensureMeetingChunkIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("meeting_chunks");
        ops.createIndex(new Index().on("meetingId", Direction.ASC).on("chunkIndex", Direction.ASC));
        ops.createIndex(new Index().on("meetingId", Direction.ASC).on("contentHash", Direction.ASC));
        ops.createIndex(TextIndexDefinition.builder().onField("text").build());
    }

    private void ensureChatConversationIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("chat_conversations");
        ops.createIndex(new Index().on("meetingId", Direction.ASC));
        ops.createIndex(new Index().on("updatedAt", Direction.DESC));
    }

    private void ensureLlmInvocationIndexes() {
        IndexOperations ops = mongoTemplate.indexOps("llm_invocations");
        // Invocations older than 30 days are pruned automatically. _id already
        // doubles as the cache key, so no separate unique index is needed.
        ops.createIndex(new Index().on("createdAt", Direction.ASC).expire(30, TimeUnit.DAYS));
    }
}
