package com.diagna.logic.service;

import com.diagna.logic.api.dto.MeetingStatsDto;
import com.diagna.logic.api.exception.ResourceNotFoundException;
import com.diagna.logic.domain.Dialog;
import com.diagna.logic.domain.Meeting;
import com.diagna.logic.repository.DialogRepository;
import com.diagna.logic.repository.MeetingRepository;
import com.diagna.logic.repository.StatsRepository;
import com.diagna.logic.repository.StatsSummary;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {

    private final StatsRepository statsRepository;
    private final MeetingRepository meetingRepository;
    private final DialogRepository dialogRepository;

    public StatsService(StatsRepository statsRepository, MeetingRepository meetingRepository, DialogRepository dialogRepository) {
        this.statsRepository = statsRepository;
        this.meetingRepository = meetingRepository;
        this.dialogRepository = dialogRepository;
    }

    public StatsSummary getOverallStats() {
        return statsRepository.overallStats();
    }

    /**
     * Per-meeting rollup, derived from that meeting's own dialogs rather than
     * a fresh aggregation — each dialog already carries a precomputed
     * {@code stats} sub-document (see the ETL), so this is a simple in-memory
     * sum over at most two dialogs, not a database round-trip per field.
     */
    public MeetingStatsDto getMeetingStats(String meetingId) {
        Meeting meeting = meetingRepository.findSummaryById(meetingId)
                .orElseThrow(() -> ResourceNotFoundException.meeting(meetingId));
        List<Dialog> dialogs = dialogRepository.findByMeetingId(meetingId);

        int totalTurns = dialogs.stream().mapToInt(Dialog::turnCount).sum();
        int unanswerableTurns = dialogs.stream().mapToInt(d -> d.stats().unanswerableCount()).sum();
        int attributedTurns = dialogs.stream().mapToInt(d -> d.stats().attributedTurnCount()).sum();

        Map<String, Integer> queryTypeCounts = new LinkedHashMap<>();
        for (Dialog dialog : dialogs) {
            dialog.stats().queryTypeCounts().forEach((type, count) -> queryTypeCounts.merge(type, count, Integer::sum));
        }

        return new MeetingStatsDto(
                meeting.id(),
                meeting.corpus().name(),
                meeting.domain().name(),
                meeting.split().name(),
                meeting.segmentCount(),
                meeting.speakerCount(),
                meeting.dialogCount(),
                totalTurns,
                unanswerableTurns,
                attributedTurns,
                queryTypeCounts
        );
    }
}
