package com.meetingiq.platform.api.dto;

import com.meetingiq.platform.domain.enums.Corpus;
import com.meetingiq.platform.domain.enums.DatasetSplit;
import com.meetingiq.platform.domain.enums.MeetingDomain;

/**
 * Request body for {@code POST /api/v1/meetings/search}. {@code filters} and
 * its fields, {@code pagination} and {@code sort} are all optional — a bare
 * {@code {}} body returns the first unfiltered page, same as the old
 * query-param endpoint with none supplied.
 */
public record MeetingSearchRequestDto(
        String q,
        MeetingFilters filters,
        PaginationRequest pagination,
        SortRequest sort
) {

    public record MeetingFilters(
            Corpus corpus,
            MeetingDomain domain,
            DatasetSplit split,
            /** Exact speaker name; matches meetings where this speaker appears at all. */
            String speaker,
            Integer minSegments,
            /** Case-insensitive substring match against the meeting id. */
            String meetingId
    ) {
        static final MeetingFilters EMPTY = new MeetingFilters(null, null, null, null, null, null);
    }

    public MeetingFilters filtersOrEmpty() {
        return filters != null ? filters : MeetingFilters.EMPTY;
    }
}
