package com.meetingiq.platform.api;

import com.meetingiq.platform.api.dto.PaginationRequest;
import com.meetingiq.platform.api.dto.SortRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds a Spring Data {@link Pageable} from a POST search request's
 * {@code pagination}/{@code sort} blocks — shared by every list endpoint
 * that moved from {@code ?page&size&sort} query params to a request body.
 */
final class SearchRequestSupport {

    private SearchRequestSupport() {
    }

    static Pageable toPageable(PaginationRequest pagination, SortRequest sort, String defaultSortField) {
        PaginationRequest page = PaginationRequest.orEmpty(pagination);
        return PageRequest.of(
                page.pageOrDefault(),
                page.sizeOrDefault(),
                sort != null ? sort.toSort(defaultSortField) : Sort.by(defaultSortField)
        );
    }
}
