package com.meetingiq.platform.api;

import com.meetingiq.platform.api.dto.AttributionResolutionDto;
import com.meetingiq.platform.api.dto.DialogDto;
import com.meetingiq.platform.api.dto.PageResponse;
import com.meetingiq.platform.domain.enums.Corpus;
import com.meetingiq.platform.domain.enums.DatasetSplit;
import com.meetingiq.platform.domain.enums.QueryType;
import com.meetingiq.platform.repository.DialogSearchCriteria;
import com.meetingiq.platform.service.DialogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dialogs")
public class DialogController {

    private final DialogService dialogService;

    public DialogController(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    @GetMapping
    public PageResponse<DialogDto> search(
            @RequestParam(required = false) String meetingId,
            @RequestParam(required = false) DatasetSplit split,
            @RequestParam(required = false) Corpus corpus,
            @RequestParam(required = false) QueryType queryType,
            @RequestParam(required = false) Boolean hasUnanswerable,
            @RequestParam(required = false) Integer minTurns,
            @PageableDefault(size = 20, sort = "_id") Pageable pageable
    ) {
        DialogSearchCriteria criteria = new DialogSearchCriteria(meetingId, split, corpus, queryType, hasUnanswerable, minTurns);
        Page<DialogDto> page = dialogService.search(criteria, pageable);
        return PageResponse.of(page);
    }

    @GetMapping("/{id}")
    public DialogDto getById(@PathVariable String id, @RequestParam(defaultValue = "false") boolean resolveAttributions) {
        return dialogService.getById(id, resolveAttributions);
    }

    @GetMapping("/{id}/turns/{turnIndex}/attribution")
    public AttributionResolutionDto getTurnAttribution(@PathVariable String id, @PathVariable int turnIndex) {
        return dialogService.getTurnAttribution(id, turnIndex);
    }
}
