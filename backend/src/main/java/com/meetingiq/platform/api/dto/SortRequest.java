package com.meetingiq.platform.api.dto;

import org.springframework.data.domain.Sort;

/**
 * {@code sort} block of a POST search request body — {@code order} is
 * {@code "asc"} or {@code "desc"} (case-insensitive), defaulting to
 * {@code asc} for anything else.
 */
public record SortRequest(String field, String order) {

    public Sort toSort(String defaultField) {
        String sortField = (field != null && !field.isBlank()) ? field : defaultField;
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, sortField);
    }
}
