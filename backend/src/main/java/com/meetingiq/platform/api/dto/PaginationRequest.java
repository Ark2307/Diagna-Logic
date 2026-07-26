package com.meetingiq.platform.api.dto;

/**
 * {@code pagination} block of a POST search request body. Both fields are
 * optional — {@link #DEFAULT_SIZE} and page {@code 0} apply when omitted,
 * matching this API's previous {@code ?page&size} query-param defaults.
 */
public record PaginationRequest(Integer page, Integer size) {

    private static final int DEFAULT_SIZE = 20;
    private static final PaginationRequest EMPTY = new PaginationRequest(null, null);

    public static PaginationRequest orEmpty(PaginationRequest pagination) {
        return pagination != null ? pagination : EMPTY;
    }

    public int pageOrDefault() {
        return page != null ? page : 0;
    }

    public int sizeOrDefault() {
        return size != null ? size : DEFAULT_SIZE;
    }
}
