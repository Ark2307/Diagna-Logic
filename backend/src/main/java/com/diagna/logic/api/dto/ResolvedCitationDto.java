package com.diagna.logic.api.dto;

import java.util.List;

/** One merged attribution span with its segments resolved — see {@code AttributionResolver}. */
public record ResolvedCitationDto(int startIndex, int endIndex, List<CitationDto> segments) {
}
