package com.meetingiq.platform.api;

import com.meetingiq.platform.api.dto.SearchRequestDto;
import com.meetingiq.platform.api.dto.SearchResponseDto;
import com.meetingiq.platform.service.SearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** Full-text search across transcripts and/or dialogs — a POST body rather than query params. */
    @PostMapping
    public SearchResponseDto search(@Valid @RequestBody SearchRequestDto request) {
        return searchService.search(request.q(), request.scopeOrDefault(), request.limitOrDefault());
    }
}
