package com.meetingiq.platform.api.mapper;

import com.meetingiq.platform.api.dto.MeetingDto;
import com.meetingiq.platform.api.dto.SpeakerStatDto;
import com.meetingiq.platform.api.dto.TranscriptPageDto;
import com.meetingiq.platform.api.dto.TranscriptSegmentDto;
import com.meetingiq.platform.domain.Meeting;
import com.meetingiq.platform.domain.SpeakerStat;
import com.meetingiq.platform.domain.TranscriptSegment;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps {@link Meeting} (and its embedded types) to the API's response shapes. */
@Component
public class MeetingMapper {

    public MeetingDto toDto(Meeting meeting) {
        return new MeetingDto(
                meeting.id(),
                meeting.corpus().name(),
                meeting.domain().name(),
                meeting.split().name(),
                meeting.segmentCount(),
                meeting.charCount(),
                meeting.estimatedTokens(),
                meeting.speakerCount(),
                meeting.dialogCount(),
                toSpeakerDtos(meeting.speakers()),
                toSegmentDtos(meeting.transcriptSegments()),
                meeting.sourceFile(),
                meeting.ingestedAt()
        );
    }

    public TranscriptPageDto toTranscriptPageDto(String meetingId, int from, int to, int segmentCount, List<TranscriptSegment> segments) {
        return new TranscriptPageDto(meetingId, from, to, segmentCount, toSegmentDtos(segments));
    }

    private List<SpeakerStatDto> toSpeakerDtos(List<SpeakerStat> speakers) {
        if (speakers == null) {
            return null;
        }
        return speakers.stream()
                .map(s -> new SpeakerStatDto(s.name(), s.segmentCount(), s.charCount()))
                .toList();
    }

    private List<TranscriptSegmentDto> toSegmentDtos(List<TranscriptSegment> segments) {
        if (segments == null) {
            return null;
        }
        return segments.stream()
                .map(s -> new TranscriptSegmentDto(s.index(), s.speakerName(), s.text()))
                .toList();
    }
}
