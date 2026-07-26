package com.meetingiq.platform.api;

import com.meetingiq.platform.api.dto.MeetingStatsDto;
import com.meetingiq.platform.repository.StatsSummary;
import com.meetingiq.platform.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public StatsSummary overall() {
        return statsService.getOverallStats();
    }

    @GetMapping("/meetings/{id}")
    public MeetingStatsDto meeting(@PathVariable String id) {
        return statsService.getMeetingStats(id);
    }
}
