package com.meetingiq.platform.service;

import com.meetingiq.platform.api.dto.SearchHitDto;
import com.meetingiq.platform.api.dto.SearchResponseDto;
import com.meetingiq.platform.api.exception.BadRequestException;
import com.meetingiq.platform.domain.Dialog;
import com.meetingiq.platform.domain.DialogTurn;
import com.meetingiq.platform.domain.Meeting;
import com.meetingiq.platform.domain.TranscriptSegment;
import com.meetingiq.platform.repository.DialogRepository;
import com.meetingiq.platform.repository.MeetingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backs {@code GET /api/v1/search}. Deliberately simple: MongoDB's
 * {@code $text} index does the actual relevance ranking (with stemming and
 * scoring), and this service only extracts a human-readable snippet from
 * whichever already-matched document/segment/turn contains the literal
 * query text — it is not a general-purpose highlighter, and a stemmed
 * match that doesn't contain the literal substring falls back to a
 * truncated excerpt rather than failing.
 */
@Service
public class SearchService {

    private static final int SNIPPET_RADIUS = 80;

    private final MeetingRepository meetingRepository;
    private final DialogRepository dialogRepository;

    public SearchService(MeetingRepository meetingRepository, DialogRepository dialogRepository) {
        this.meetingRepository = meetingRepository;
        this.dialogRepository = dialogRepository;
    }

    public SearchResponseDto search(String q, String scope, int limit) {
        if (q == null || q.isBlank()) {
            throw new BadRequestException("Query parameter 'q' must not be blank");
        }
        String effectiveScope = scope == null || scope.isBlank() ? "all" : scope;

        List<SearchHitDto> hits = new ArrayList<>();
        if (effectiveScope.equals("transcripts") || effectiveScope.equals("all")) {
            hits.addAll(searchTranscripts(q, limit));
        }
        if (effectiveScope.equals("dialogs") || effectiveScope.equals("all")) {
            hits.addAll(searchDialogs(q, limit));
        }
        return new SearchResponseDto(q, effectiveScope, hits);
    }

    private List<SearchHitDto> searchTranscripts(String q, int limit) {
        List<SearchHitDto> hits = new ArrayList<>();
        for (Meeting meeting : meetingRepository.searchTranscriptsText(q, limit)) {
            TranscriptSegment match = firstMatchingSegment(meeting.transcriptSegments(), q);
            if (match != null) {
                hits.add(new SearchHitDto("meeting", meeting.id(), meeting.id(), match.index(), snippetAround(match.text(), q), null));
            }
        }
        return hits;
    }

    private List<SearchHitDto> searchDialogs(String q, int limit) {
        List<SearchHitDto> hits = new ArrayList<>();
        for (Dialog dialog : dialogRepository.searchText(q, limit)) {
            DialogTurn match = firstMatchingTurn(dialog.turns(), q);
            if (match != null) {
                String field = containsIgnoreCase(match.query(), q) ? match.query() : match.response();
                hits.add(new SearchHitDto("dialog", dialog.id(), dialog.meetingId(), null, snippetAround(field, q), null));
            }
        }
        return hits;
    }

    private static TranscriptSegment firstMatchingSegment(List<TranscriptSegment> segments, String q) {
        if (segments == null) {
            return null;
        }
        return segments.stream().filter(s -> containsIgnoreCase(s.text(), q)).findFirst().orElse(null);
    }

    private static DialogTurn firstMatchingTurn(List<DialogTurn> turns, String q) {
        return turns.stream()
                .filter(t -> containsIgnoreCase(t.query(), q) || containsIgnoreCase(t.response(), q))
                .findFirst()
                .orElse(null);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static String snippetAround(String text, String query) {
        String lower = text.toLowerCase(Locale.ROOT);
        int matchIndex = lower.indexOf(query.toLowerCase(Locale.ROOT));
        if (matchIndex < 0) {
            return text.length() <= 200 ? text : text.substring(0, 200) + "...";
        }
        int start = Math.max(0, matchIndex - SNIPPET_RADIUS);
        int end = Math.min(text.length(), matchIndex + query.length() + SNIPPET_RADIUS);
        String snippet = text.substring(start, end);
        return (start > 0 ? "..." : "") + snippet + (end < text.length() ? "..." : "");
    }
}
