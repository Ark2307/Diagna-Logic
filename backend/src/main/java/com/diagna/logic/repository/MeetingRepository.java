package com.diagna.logic.repository;

import com.diagna.logic.domain.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static org.springframework.data.mongodb.core.query.Criteria.where;

/**
 * Persistence access for {@code meetings}, built directly on
 * {@link MongoTemplate} rather than a Spring Data derived-query interface.
 *
 * <p>This collection needs filtered pagination, a field-excluding
 * projection (list views never load the transcript) and a {@code $slice}
 * projection (paged transcript reads) — combinations Spring Data's method-name
 * query derivation does not express cleanly. Writing the queries directly
 * keeps every one of them a single, readable, testable method instead of a
 * derived-query name nobody can predict the generated query from.
 */
@Repository
public class MeetingRepository {

    /** Fields loaded for list/summary views. Excludes {@code transcriptSegments} — see {@link #search}. */
    private static final String[] SUMMARY_FIELDS = {
            "corpus", "domain", "split", "segmentCount", "charCount", "estimatedTokens",
            "speakerCount", "dialogCount", "speakers", "sourceFile", "ingestedAt",
    };

    private final MongoTemplate mongoTemplate;

    public MeetingRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /** Full document, including every transcript segment. */
    public Optional<Meeting> findFullById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Meeting.class));
    }

    /** Metadata only — {@code transcriptSegments} is null on the returned instance. */
    public Optional<Meeting> findSummaryById(String id) {
        Query query = Query.query(where("_id").is(id));
        includeSummaryFields(query);
        return Optional.ofNullable(mongoTemplate.findOne(query, Meeting.class));
    }

    public boolean existsById(String id) {
        return mongoTemplate.exists(Query.query(where("_id").is(id)), Meeting.class);
    }

    /**
     * A single page of {@code transcriptSegments}, via MongoDB's
     * {@code $slice} projection — only the requested window crosses the
     * network, not the whole (possibly 1,530-segment) transcript.
     *
     * <p>Deliberately calls {@code .slice(...)} and NOTHING else on the field
     * spec: adding any {@code .include(...)} alongside it flips Mongo's
     * projection into inclusive-only mode, which drops every other field —
     * including the record's primitive ones ({@code charCount} etc.), which
     * Spring Data's generated record instantiator then rejects as null
     * rather than defaulting to 0. A bare {@code .slice()} keeps "everything
     * included, this one array field windowed," which is what's needed here
     * (the caller already knows {@code segmentCount} from a prior
     * {@link #findSummaryById} call, so it isn't re-read from this method).
     *
     * @param fromIndex inclusive start segment index (already clamped by the caller)
     * @param count     number of segments to return
     */
    public Optional<Meeting> findTranscriptSlice(String id, int fromIndex, int count) {
        Query query = Query.query(where("_id").is(id));
        query.fields().slice("transcriptSegments", fromIndex, count);
        return Optional.ofNullable(mongoTemplate.findOne(query, Meeting.class));
    }

    /**
     * Full documents (including {@code transcriptSegments}) matching a text
     * search, sorted by relevance — used by {@code SearchService} to extract
     * segment-level snippets, which a summary projection can't provide.
     */
    public List<Meeting> searchTranscriptsText(String q, int limit) {
        TextQuery query = TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(q)).sortByScore();
        query.limit(limit);
        return mongoTemplate.find(query, Meeting.class);
    }

    /** Filtered, paginated, summary-projected search — the backing query for {@code GET /api/v1/meetings}. */
    public Page<Meeting> search(MeetingSearchCriteria criteria, Pageable pageable) {
        Query query = criteria.q() != null && !criteria.q().isBlank()
                ? TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(criteria.q()))
                : new Query();
        applyFilters(query, criteria);

        // Count against the filter criteria BEFORE fields/pagination are applied —
        // neither affects which documents match, so counting first avoids having
        // to reconstruct an equivalent unpaginated/unprojected query afterwards.
        long total = mongoTemplate.count(query, Meeting.class);

        includeSummaryFields(query);
        query.with(pageable);
        List<Meeting> content = mongoTemplate.find(query, Meeting.class);
        return new PageImpl<>(content, pageable, total);
    }

    private void applyFilters(Query query, MeetingSearchCriteria criteria) {
        if (criteria.corpus() != null) {
            query.addCriteria(where("corpus").is(criteria.corpus()));
        }
        if (criteria.domain() != null) {
            query.addCriteria(where("domain").is(criteria.domain()));
        }
        if (criteria.split() != null) {
            query.addCriteria(where("split").is(criteria.split()));
        }
        if (criteria.speaker() != null && !criteria.speaker().isBlank()) {
            query.addCriteria(where("speakers.name").is(criteria.speaker()));
        }
        if (criteria.minSegments() != null) {
            query.addCriteria(where("segmentCount").gte(criteria.minSegments()));
        }
    }

    private void includeSummaryFields(Query query) {
        for (String field : SUMMARY_FIELDS) {
            query.fields().include(field);
        }
    }
}
