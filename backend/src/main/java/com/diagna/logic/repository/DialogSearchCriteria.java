package com.diagna.logic.repository;

import com.diagna.logic.domain.enums.Corpus;
import com.diagna.logic.domain.enums.DatasetSplit;
import com.diagna.logic.domain.enums.QueryType;

/**
 * Filters for {@code GET /api/v1/dialogs}. Every field is nullable — {@code
 * null} means "no filter on this dimension". Use {@link #empty()} as the
 * base case rather than null-checking six parameters everywhere.
 */
public record DialogSearchCriteria(
        String meetingId,
        DatasetSplit split,
        Corpus corpus,
        /** Matches dialogs containing at least one turn of this type. */
        QueryType queryType,
        /** When true, only dialogs with at least one unanswerable turn; when false, only dialogs with none. */
        Boolean hasUnanswerable,
        Integer minTurns
) {
    public static DialogSearchCriteria empty() {
        return new DialogSearchCriteria(null, null, null, null, null, null);
    }
}
