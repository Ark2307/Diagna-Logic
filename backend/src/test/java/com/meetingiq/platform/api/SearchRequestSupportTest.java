package com.meetingiq.platform.api;

import com.meetingiq.platform.api.dto.PaginationRequest;
import com.meetingiq.platform.api.dto.SortRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code SearchRequestSupport} turns a POST search body's optional
 * {@code pagination}/{@code sort} blocks into a {@link Pageable} — every
 * list endpoint that moved from {@code ?page&size&sort} to a request body
 * depends on these defaults matching the old query-param behaviour.
 */
class SearchRequestSupportTest {

    @Test
    void nullPaginationAndSortFallBackToDefaults() {
        Pageable pageable = SearchRequestSupport.toPageable(null, null, "_id");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by("_id"));
    }

    @Test
    void explicitPageAndSizeAreHonoured() {
        Pageable pageable = SearchRequestSupport.toPageable(new PaginationRequest(2, 50), null, "_id");

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
    }

    @Test
    void partialPaginationFillsOnlyTheMissingField() {
        Pageable pageable = SearchRequestSupport.toPageable(new PaginationRequest(3, null), null, "_id");

        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(20);
    }

    @Test
    void sortFieldAndDescendingOrderAreApplied() {
        Pageable pageable = SearchRequestSupport.toPageable(null, new SortRequest("segmentCount", "desc"), "_id");

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "segmentCount"));
    }

    @Test
    void unrecognisedOrderDefaultsToAscending() {
        Pageable pageable = SearchRequestSupport.toPageable(null, new SortRequest("segmentCount", "sideways"), "_id");

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "segmentCount"));
    }

    @Test
    void blankSortFieldFallsBackToTheDefaultField() {
        Pageable pageable = SearchRequestSupport.toPageable(null, new SortRequest("  ", "desc"), "_id");

        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "_id"));
    }
}
