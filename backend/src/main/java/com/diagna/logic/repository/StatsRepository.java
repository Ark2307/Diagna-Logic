package com.diagna.logic.repository;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backing aggregations for {@code GET /api/v1/stats}.
 *
 * <p>MongoDB's {@code $facet} runs multiple parallel sub-pipelines against a
 * single base collection — it cannot itself span two collections. So this
 * runs one {@code $facet} aggregation over {@code meetings} and a second
 * over {@code dialogs}, then combines both single-document results into one
 * {@link StatsSummary} in Java. The dialog-side facet deliberately sums each
 * dialog's already-precomputed {@code stats} sub-document (turn counts,
 * unanswerable counts, query-type counts) rather than re-deriving them by
 * unwinding every turn — that rollup already exists once, written by the
 * ETL, and re-computing it here would be duplicated logic over the same data.
 *
 * <p>Built on the raw driver ({@link MongoCollection}) rather than Spring
 * Data's {@code Aggregation} DSL: {@code $facet} and {@code $objectToArray}
 * (needed to sum the {@code queryTypeCounts} map across dialogs) are exact,
 * well-documented MongoDB operators, and expressing them as literal pipeline
 * stages is more predictable here than working around DSL coverage gaps.
 */
@Repository
public class StatsRepository {

    private final MongoTemplate mongoTemplate;

    public StatsRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public StatsSummary overallStats() {
        Document meetingFacets = runFacet("meetings", meetingsPipeline());
        Document dialogFacets = runFacet("dialogs", dialogsPipeline());

        long totalMeetings = countFrom(meetingFacets, "totals");
        long totalSegments = sumLong(firstOrNull(meetingFacets, "totals"), "totalSegments");
        double avgSegmentsPerMeeting = totalMeetings == 0 ? 0.0 : (double) totalSegments / totalMeetings;

        Document dialogTotals = firstOrNull(dialogFacets, "totals");
        long totalDialogs = dialogTotals == null ? 0 : sumLong(dialogTotals, "totalDialogs");
        long totalTurns = dialogTotals == null ? 0 : sumLong(dialogTotals, "totalTurns");
        long unanswerableTurns = dialogTotals == null ? 0 : sumLong(dialogTotals, "unanswerableTurns");
        long attributedTurns = dialogTotals == null ? 0 : sumLong(dialogTotals, "attributedTurns");
        double avgTurnsPerDialog = totalDialogs == 0 ? 0.0 : (double) totalTurns / totalDialogs;
        double unanswerableRate = totalTurns == 0 ? 0.0 : (double) unanswerableTurns / totalTurns;
        double attributionCoverage = totalTurns == 0 ? 0.0 : (double) attributedTurns / totalTurns;

        return new StatsSummary(
                totalMeetings,
                totalDialogs,
                totalTurns,
                totalSegments,
                groupCounts(meetingFacets, "byCorpus"),
                groupCounts(meetingFacets, "byDomain"),
                groupCounts(dialogFacets, "bySplit"),
                groupCounts(dialogFacets, "queryTypeCounts"),
                unanswerableTurns,
                unanswerableRate,
                attributedTurns,
                attributionCoverage,
                avgTurnsPerDialog,
                avgSegmentsPerMeeting,
                topSpeakers(meetingFacets),
                meetingRef(firstOrNull(meetingFacets, "longest")),
                meetingRef(firstOrNull(meetingFacets, "shortest"))
        );
    }

    private Document runFacet(String collectionName, Document facetStage) {
        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);
        Document result = collection.aggregate(List.of(facetStage)).first();
        return result != null ? result : new Document();
    }

    private Document meetingsPipeline() {
        return new Document("$facet", new Document()
                .append("totals", List.of(new Document("$group", new Document("_id", (Object) null)
                        .append("count", new Document("$sum", 1))
                        .append("totalSegments", new Document("$sum", "$segmentCount")))))
                .append("byCorpus", groupCountStage("$corpus"))
                .append("byDomain", groupCountStage("$domain"))
                .append("topSpeakers", List.of(
                        new Document("$unwind", "$speakers"),
                        new Document("$group", new Document("_id", "$speakers.name")
                                .append("segmentCount", new Document("$sum", "$speakers.segmentCount"))),
                        new Document("$sort", new Document("segmentCount", -1)),
                        new Document("$limit", 10)
                ))
                .append("longest", List.of(
                        new Document("$sort", new Document("segmentCount", -1)),
                        new Document("$limit", 1),
                        new Document("$project", new Document("_id", 1).append("segmentCount", 1))
                ))
                .append("shortest", List.of(
                        new Document("$sort", new Document("segmentCount", 1)),
                        new Document("$limit", 1),
                        new Document("$project", new Document("_id", 1).append("segmentCount", 1))
                )));
    }

    private Document dialogsPipeline() {
        return new Document("$facet", new Document()
                .append("totals", List.of(new Document("$group", new Document("_id", (Object) null)
                        .append("totalDialogs", new Document("$sum", 1))
                        .append("totalTurns", new Document("$sum", "$turnCount"))
                        .append("unanswerableTurns", new Document("$sum", "$stats.unanswerableCount"))
                        .append("attributedTurns", new Document("$sum", "$stats.attributedTurnCount")))))
                .append("bySplit", groupCountStage("$split"))
                .append("queryTypeCounts", List.of(
                        new Document("$project", new Document("kv", new Document("$objectToArray", "$stats.queryTypeCounts"))),
                        new Document("$unwind", "$kv"),
                        new Document("$group", new Document("_id", "$kv.k")
                                .append("count", new Document("$sum", "$kv.v")))
                )));
    }

    private static List<Document> groupCountStage(String field) {
        return List.of(new Document("$group", new Document("_id", field).append("count", new Document("$sum", 1))));
    }

    // --- facet-result readers --------------------------------------------

    @SuppressWarnings("unchecked")
    private static List<Document> facetList(Document facetResult, String facetName) {
        Object value = facetResult.get(facetName);
        return value instanceof List<?> list ? (List<Document>) list : List.of();
    }

    private static Document firstOrNull(Document facetResult, String facetName) {
        List<Document> list = facetList(facetResult, facetName);
        return list.isEmpty() ? null : list.get(0);
    }

    private static long countFrom(Document facetResult, String facetName) {
        Document first = firstOrNull(facetResult, facetName);
        return first == null ? 0 : sumLong(first, "count");
    }

    private static long sumLong(Document doc, String field) {
        if (doc == null) {
            return 0;
        }
        Number n = doc.get(field, Number.class);
        return n == null ? 0 : n.longValue();
    }

    private static Map<String, Long> groupCounts(Document facetResult, String facetName) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Document group : facetList(facetResult, facetName)) {
            Object id = group.get("_id");
            if (id == null) {
                continue;
            }
            counts.put(String.valueOf(id), sumLong(group, "count"));
        }
        return counts;
    }

    private static List<StatsSummary.SpeakerCount> topSpeakers(Document meetingFacets) {
        List<StatsSummary.SpeakerCount> speakers = new ArrayList<>();
        for (Document group : facetList(meetingFacets, "topSpeakers")) {
            Object id = group.get("_id");
            if (id == null) {
                continue;
            }
            speakers.add(new StatsSummary.SpeakerCount(String.valueOf(id), sumLong(group, "segmentCount")));
        }
        return speakers;
    }

    private static StatsSummary.MeetingRef meetingRef(Document doc) {
        if (doc == null) {
            return null;
        }
        Object id = doc.get("_id");
        Number segmentCount = doc.get("segmentCount", Number.class);
        return new StatsSummary.MeetingRef(
                id == null ? null : String.valueOf(id),
                segmentCount == null ? 0 : segmentCount.intValue()
        );
    }
}
