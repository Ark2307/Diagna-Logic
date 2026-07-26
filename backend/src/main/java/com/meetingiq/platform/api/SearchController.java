package com.meetingiq.platform.api;

import com.meetingiq.platform.api.dto.SearchResponseDto;
import com.meetingiq.platform.service.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponseDto search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return searchService.search(q, scope, limit);
    }
}
