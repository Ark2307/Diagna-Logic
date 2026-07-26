package com.meetingiq.platform.api.dto;

import java.util.Map;

public record DialogStatsDto(int unanswerableCount, int attributedTurnCount, Map<String, Integer> queryTypeCounts) {
}
