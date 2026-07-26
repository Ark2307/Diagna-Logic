package com.meetingiq.platform.api;

import com.meetingiq.platform.api.dto.AttributionResolutionDto;
import com.meetingiq.platform.api.dto.DialogDto;
import com.meetingiq.platform.api.dto.DialogSearchRequestDto;
import com.meetingiq.platform.api.dto.PageResponse;
import com.meetingiq.platform.repository.DialogSearchCriteria;
import com.meetingiq.platform.service.DialogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * Filterable, paginated listing — a POST body rather than query params,
     * matching {@code POST /meetings/search}.
     */
    @PostMapping("/search")
    public PageResponse<DialogDto> search(@RequestBody(required = false) DialogSearchRequestDto request) {
        DialogSearchRequestDto req = request != null ? request : new DialogSearchRequestDto(null, null, null);
        DialogSearchRequestDto.DialogFilters filters = req.filtersOrEmpty();
        DialogSearchCriteria criteria = new DialogSearchCriteria(
                filters.meetingId(), filters.split(), filters.corpus(), filters.queryType(), filters.hasUnanswerable(), filters.minTurns()
        );
        Pageable pageable = SearchRequestSupport.toPageable(req.pagination(), req.sort(), "_id");
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
