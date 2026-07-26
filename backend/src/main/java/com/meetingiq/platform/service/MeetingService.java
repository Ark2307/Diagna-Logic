package com.meetingiq.platform.service;

import com.meetingiq.platform.api.dto.DialogDto;
import com.meetingiq.platform.api.dto.MeetingDto;
import com.meetingiq.platform.api.dto.SpeakerStatDto;
import com.meetingiq.platform.api.dto.TranscriptPageDto;
import com.meetingiq.platform.api.exception.ResourceNotFoundException;
import com.meetingiq.platform.api.mapper.DialogMapper;
import com.meetingiq.platform.api.mapper.MeetingMapper;
import com.meetingiq.platform.domain.Meeting;
import com.meetingiq.platform.repository.DialogRepository;
import com.meetingiq.platform.repository.MeetingRepository;
import com.meetingiq.platform.repository.MeetingSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final DialogRepository dialogRepository;
    private final MeetingMapper meetingMapper;
    private final DialogMapper dialogMapper;

    public MeetingService(
            MeetingRepository meetingRepository,
            DialogRepository dialogRepository,
            MeetingMapper meetingMapper,
            DialogMapper dialogMapper
    ) {
        this.meetingRepository = meetingRepository;
        this.dialogRepository = dialogRepository;
        this.meetingMapper = meetingMapper;
        this.dialogMapper = dialogMapper;
    }

    public Page<MeetingDto> search(MeetingSearchCriteria criteria, Pageable pageable) {
        return meetingRepository.search(criteria, pageable).map(meetingMapper::toDto);
    }

    public MeetingDto getById(String id, boolean includeTranscript) {
        Meeting meeting = (includeTranscript ? meetingRepository.findFullById(id) : meetingRepository.findSummaryById(id))
                .orElseThrow(() -> ResourceNotFoundException.meeting(id));
        return meetingMapper.toDto(meeting);
    }

    /**
     * A windowed read of the transcript. {@code from}/{@code to} are clamped
     * into {@code [0, segmentCount)} against the meeting's real segment
     * count — an out-of-range request degrades to the nearest valid window
     * rather than erroring.
     */
    public TranscriptPageDto getTranscript(String id, int from, int to) {
        Meeting summary = meetingRepository.findSummaryById(id)
                .orElseThrow(() -> ResourceNotFoundException.meeting(id));
        int segmentCount = summary.segmentCount();
        int clampedFrom = Math.max(0, Math.min(from, Math.max(0, segmentCount - 1)));
        int clampedTo = Math.max(clampedFrom, Math.min(to, Math.max(0, segmentCount - 1)));
        int count = clampedTo - clampedFrom + 1;

        Meeting sliced = meetingRepository.findTranscriptSlice(id, clampedFrom, count)
                .orElseThrow(() -> ResourceNotFoundException.meeting(id));
        return meetingMapper.toTranscriptPageDto(id, clampedFrom, clampedTo, segmentCount, sliced.transcriptSegments());
    }

    public List<SpeakerStatDto> getSpeakers(String id) {
        return getById(id, false).speakers();
    }

    public List<DialogDto> getDialogsForMeeting(String id) {
        if (!meetingRepository.existsById(id)) {
            throw ResourceNotFoundException.meeting(id);
        }
        return dialogRepository.findByMeetingId(id).stream().map(dialogMapper::toDto).toList();
    }
}
