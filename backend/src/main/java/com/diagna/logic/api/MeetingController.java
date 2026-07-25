package com.diagna.logic.api;

import com.diagna.logic.api.dto.DialogDto;
import com.diagna.logic.api.dto.MeetingDto;
import com.diagna.logic.api.dto.PageResponse;
import com.diagna.logic.api.dto.SpeakerStatDto;
import com.diagna.logic.api.dto.TranscriptPageDto;
import com.diagna.logic.domain.TranscriptSegment;
import com.diagna.logic.domain.enums.Corpus;
import com.diagna.logic.domain.enums.DatasetSplit;
import com.diagna.logic.domain.enums.MeetingDomain;
import com.diagna.logic.repository.MeetingSearchCriteria;
import com.diagna.logic.service.MeetingService;
import com.diagna.logic.service.TranscriptFormatter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read APIs over {@code meetings}. Listing and detail reads never include
 * the transcript unless explicitly asked for ({@code ?includeTranscript=true}
 * or the dedicated {@code /transcript} endpoint) — the transcript is the
 * expensive part of a meeting document (up to 1,530 segments), so it is
 * opt-in everywhere.
 */
@RestController
@RequestMapping("/api/v1/meetings")
public class MeetingController {

    /** Default window size for a transcript page when {@code to} is not supplied. */
    private static final int DEFAULT_TRANSCRIPT_WINDOW = 200;

    private final MeetingService meetingService;
    private final TranscriptFormatter transcriptFormatter;

    public MeetingController(MeetingService meetingService, TranscriptFormatter transcriptFormatter) {
        this.meetingService = meetingService;
        this.transcriptFormatter = transcriptFormatter;
    }

    @GetMapping
    public PageResponse<MeetingDto> search(
            @RequestParam(required = false) Corpus corpus,
            @RequestParam(required = false) MeetingDomain domain,
            @RequestParam(required = false) DatasetSplit split,
            @RequestParam(required = false) String speaker,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer minSegments,
            @PageableDefault(size = 20, sort = "_id") Pageable pageable
    ) {
        MeetingSearchCriteria criteria = new MeetingSearchCriteria(corpus, domain, split, speaker, q, minSegments);
        Page<MeetingDto> page = meetingService.search(criteria, pageable);
        return PageResponse.of(page);
    }

    @GetMapping("/{id}")
    public MeetingDto getById(@PathVariable String id, @RequestParam(defaultValue = "false") boolean includeTranscript) {
        return meetingService.getById(id, includeTranscript);
    }

    @GetMapping("/{id}/transcript")
    public ResponseEntity<?> getTranscript(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(required = false) Integer to,
            @RequestParam(defaultValue = "json") String format
    ) {
        int effectiveTo = (to != null) ? to : from + DEFAULT_TRANSCRIPT_WINDOW - 1;
        TranscriptPageDto page = meetingService.getTranscript(id, from, effectiveTo);

        if ("text".equalsIgnoreCase(format)) {
            List<TranscriptSegment> segments = page.segments().stream()
                    .map(s -> new TranscriptSegment(s.index(), s.speakerName(), s.text()))
                    .toList();
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(transcriptFormatter.format(segments));
        }
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}/speakers")
    public List<SpeakerStatDto> getSpeakers(@PathVariable String id) {
        return meetingService.getSpeakers(id);
    }

    @GetMapping("/{id}/dialogs")
    public List<DialogDto> getDialogs(@PathVariable String id) {
        return meetingService.getDialogsForMeeting(id);
    }
}
