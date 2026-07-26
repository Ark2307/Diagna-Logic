package com.meetingiq.platform.rag;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Blends two independently-ranked lists (cosine similarity and lexical
 * {@code $text} match) into one fused ranking, using the standard
 * Reciprocal Rank Fusion formula: {@code score(item) = sum over rankings
 * containing item of 1 / (K + rank + 1)}. An item ranked highly in both
 * lists outranks one that only appears in a single list — which is exactly
 * why {@code MeetingRetriever} fuses embeddings (semantic recall) with text
 * search (catches meeting-specific jargon and proper nouns embeddings tend
 * to blur).
 */
public final class ReciprocalRankFusion {

    /** Standard RRF damping constant — large enough that rank 1 vs rank 2 in one list isn't overwhelmingly decisive. */
    private static final double K = 60.0;

    private ReciprocalRankFusion() {
    }

    /**
     * @param rankingA first ranked list, best first
     * @param rankingB second ranked list, best first
     * @param idOf     extracts a stable identity key for deduplicating an item that appears in both rankings
     * @return items from both rankings (each once), sorted by fused score descending
     */
    public static <T> List<T> fuse(List<T> rankingA, List<T> rankingB, Function<T, String> idOf) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, T> byId = new LinkedHashMap<>();
        accumulate(rankingA, idOf, scores, byId);
        accumulate(rankingB, idOf, scores, byId);

        return byId.values().stream()
                .sorted(Comparator.comparingDouble((T item) -> scores.get(idOf.apply(item))).reversed())
                .toList();
    }

    private static <T> void accumulate(List<T> ranking, Function<T, String> idOf, Map<String, Double> scores, Map<String, T> byId) {
        for (int rank = 0; rank < ranking.size(); rank++) {
            T item = ranking.get(rank);
            String id = idOf.apply(item);
            byId.putIfAbsent(id, item);
            scores.merge(id, 1.0 / (K + rank + 1), Double::sum);
        }
    }
}
