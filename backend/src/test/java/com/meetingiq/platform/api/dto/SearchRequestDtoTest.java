package com.meetingiq.platform.api.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchRequestDtoTest {

    @Test
    void nullFiltersAndPaginationFallBackToDefaults() {
        SearchRequestDto request = new SearchRequestDto("digits", null, null);

        assertThat(request.scopeOrDefault()).isEqualTo("all");
        assertThat(request.limitOrDefault()).isEqualTo(20);
    }

    @Test
    void blankScopeFallsBackToAll() {
        SearchRequestDto request = new SearchRequestDto("digits", new SearchRequestDto.SearchFilters("  "), null);

        assertThat(request.scopeOrDefault()).isEqualTo("all");
    }

    @Test
    void explicitScopeAndLimitAreHonoured() {
        SearchRequestDto request = new SearchRequestDto("digits", new SearchRequestDto.SearchFilters("dialogs"), new PaginationRequest(0, 5));

        assertThat(request.scopeOrDefault()).isEqualTo("dialogs");
        assertThat(request.limitOrDefault()).isEqualTo(5);
    }
}
