package com.meetingiq.platform.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/search}. {@code pagination.size} is
 * reused as the result limit — full-text search returns a flat ranked hit
 * list rather than a real page, so {@code page} is ignored if supplied.
 */
public record SearchRequestDto(
        @NotBlank String q,
        SearchFilters filters,
        PaginationRequest pagination
) {

    public record SearchFilters(
            /** {@code transcripts}, {@code dialogs}, or {@code all} (default). */
            String scope
    ) {
        static final SearchFilters EMPTY = new SearchFilters(null);
    }

    public SearchFilters filtersOrEmpty() {
        return filters != null ? filters : SearchFilters.EMPTY;
    }

    public String scopeOrDefault() {
        String scope = filtersOrEmpty().scope();
        return (scope != null && !scope.isBlank()) ? scope : "all";
    }

    public int limitOrDefault() {
        return PaginationRequest.orEmpty(pagination).sizeOrDefault();
    }
}
