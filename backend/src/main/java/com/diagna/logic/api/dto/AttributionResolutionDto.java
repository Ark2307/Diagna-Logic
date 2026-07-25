package com.diagna.logic.api.dto;

import java.util.List;

/** Response for {@code GET /dialogs/{id}/turns/{turnIndex}/attribution} — the resolver, standalone. */
public record AttributionResolutionDto(
        String dialogId,
        int turnIndex,
        String meetingId,
        List<ResolvedCitationDto> citations
) {
}
