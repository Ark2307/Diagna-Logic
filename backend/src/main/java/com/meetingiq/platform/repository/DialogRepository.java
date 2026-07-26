package com.meetingiq.platform.repository;

import com.meetingiq.platform.domain.Dialog;
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
 * Persistence access for {@code dialogs}, built directly on
 * {@link MongoTemplate} for the same reason as {@link MeetingRepository}:
 * the filter combinations {@code GET /api/v1/dialogs} needs don't map
 * cleanly onto Spring Data's method-name query derivation.
 */
@Repository
public class DialogRepository {

    private final MongoTemplate mongoTemplate;

    public DialogRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Optional<Dialog> findById(String id) {
        return Optional.ofNullable(mongoTemplate.findById(id, Dialog.class));
    }

    public List<Dialog> findByMeetingId(String meetingId) {
        return mongoTemplate.find(Query.query(where("meetingId").is(meetingId)), Dialog.class);
    }

    /** Filtered, paginated search — the backing query for {@code GET /api/v1/dialogs}. */
    public Page<Dialog> search(DialogSearchCriteria criteria, Pageable pageable) {
        Query query = new Query();
        applyFilters(query, criteria);

        long total = mongoTemplate.count(query, Dialog.class);
        query.with(pageable);
        List<Dialog> content = mongoTemplate.find(query, Dialog.class);
        return new PageImpl<>(content, pageable, total);
    }

    /** Free-text search over turn query/response text — the backing query for {@code scope=dialogs} in {@code /api/v1/search}. */
    public List<Dialog> searchText(String q, int limit) {
        TextQuery query = TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(q)).sortByScore();
        query.limit(limit);
        return mongoTemplate.find(query, Dialog.class);
    }

    private void applyFilters(Query query, DialogSearchCriteria criteria) {
        if (criteria.meetingId() != null) {
            query.addCriteria(where("meetingId").is(criteria.meetingId()));
        }
        if (criteria.split() != null) {
            query.addCriteria(where("split").is(criteria.split()));
        }
        if (criteria.corpus() != null) {
            query.addCriteria(where("corpus").is(criteria.corpus()));
        }
        if (criteria.queryType() != null) {
            query.addCriteria(where("turns.queryType").is(criteria.queryType()));
        }
        if (criteria.hasUnanswerable() != null) {
            query.addCriteria(criteria.hasUnanswerable()
                    ? where("stats.unanswerableCount").gt(0)
                    : where("stats.unanswerableCount").is(0));
        }
        if (criteria.minTurns() != null) {
            query.addCriteria(where("turnCount").gte(criteria.minTurns()));
        }
    }
}
