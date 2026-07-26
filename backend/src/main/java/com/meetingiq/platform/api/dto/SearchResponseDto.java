package com.meetingiq.platform.api.dto;

import java.util.List;

public record SearchResponseDto(String query, String scope, List<SearchHitDto> hits) {
}
