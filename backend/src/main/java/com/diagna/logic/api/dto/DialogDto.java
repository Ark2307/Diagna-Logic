package com.diagna.logic.api.dto;

import java.util.List;

public record DialogDto(
        String id,
        String meetingId,
        String split,
        String corpus,
        String domain,
        int turnCount,
        List<DialogTurnDto> turns,
        DialogStatsDto stats,
        String ingestedAt
) {
}
