package com.meetingiq.platform.api.dto;

import com.meetingiq.platform.domain.enums.Corpus;
import com.meetingiq.platform.domain.enums.DatasetSplit;
import com.meetingiq.platform.domain.enums.QueryType;

/**
 * Request body for {@code POST /api/v1/dialogs/search}. {@code filters} and
 * its fields, {@code pagination} and {@code sort} are all optional — a bare
 * {@code {}} body returns the first unfiltered page, same as the old
 * query-param endpoint with none supplied.
 */
public record DialogSearchRequestDto(
        DialogFilters filters,
        PaginationRequest pagination,
        SortRequest sort
) {

    public record DialogFilters(
            String meetingId,
            DatasetSplit split,
            Corpus corpus,
            /** Matches dialogs containing at least one turn of this type. */
            QueryType queryType,
            /** When true, only dialogs with at least one unanswerable turn; when false, only dialogs with none. */
            Boolean hasUnanswerable,
            Integer minTurns
    ) {
        static final DialogFilters EMPTY = new DialogFilters(null, null, null, null, null, null);
    }

    public DialogFilters filtersOrEmpty() {
        return filters != null ? filters : DialogFilters.EMPTY;
    }
}
