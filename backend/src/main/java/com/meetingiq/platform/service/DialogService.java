package com.meetingiq.platform.service;

import com.meetingiq.platform.api.dto.AttributionResolutionDto;
import com.meetingiq.platform.api.dto.CitationDto;
import com.meetingiq.platform.api.dto.DialogDto;
import com.meetingiq.platform.api.dto.ResolvedCitationDto;
import com.meetingiq.platform.api.exception.BadRequestException;
import com.meetingiq.platform.api.exception.ResourceNotFoundException;
import com.meetingiq.platform.api.mapper.DialogMapper;
import com.meetingiq.platform.domain.Dialog;
import com.meetingiq.platform.domain.DialogTurn;
import com.meetingiq.platform.domain.Meeting;
import com.meetingiq.platform.repository.DialogRepository;
import com.meetingiq.platform.repository.DialogSearchCriteria;
import com.meetingiq.platform.repository.MeetingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DialogService {

    private final DialogRepository dialogRepository;
    private final MeetingRepository meetingRepository;
    private final DialogMapper dialogMapper;
    private final AttributionResolver attributionResolver;

    public DialogService(
            DialogRepository dialogRepository,
            MeetingRepository meetingRepository,
            DialogMapper dialogMapper,
            AttributionResolver attributionResolver
    ) {
        this.dialogRepository = dialogRepository;
        this.meetingRepository = meetingRepository;
        this.dialogMapper = dialogMapper;
        this.attributionResolver = attributionResolver;
    }

    /** List view never resolves attributions — that would mean loading every result's meeting transcript. */
    public Page<DialogDto> search(DialogSearchCriteria criteria, Pageable pageable) {
        return dialogRepository.search(criteria, pageable).map(dialogMapper::toDto);
    }

    public DialogDto getById(String id, boolean resolveAttributions) {
        Dialog dialog = findDialogOrThrow(id);
        if (!resolveAttributions) {
            return dialogMapper.toDto(dialog);
        }
        Meeting meeting = meetingRepository.findFullById(dialog.meetingId())
                .orElseThrow(() -> ResourceNotFoundException.meeting(dialog.meetingId()));
        return dialogMapper.toDto(dialog, meeting.transcriptSegments(), meeting.segmentCount());
    }

    public AttributionResolutionDto getTurnAttribution(String dialogId, int turnIndex) {
        Dialog dialog = findDialogOrThrow(dialogId);
        if (turnIndex < 0 || turnIndex >= dialog.turns().size()) {
            throw new BadRequestException(
                    "Dialog '" + dialogId + "' has " + dialog.turns().size() + " turns; turnIndex " + turnIndex + " is out of range");
        }
        DialogTurn turn = dialog.turns().get(turnIndex);

        Meeting meeting = meetingRepository.findFullById(dialog.meetingId())
                .orElseThrow(() -> ResourceNotFoundException.meeting(dialog.meetingId()));

        var resolved = attributionResolver.resolve(turn.attributionRanges(), meeting.transcriptSegments(), meeting.segmentCount());
        var citationDtos = resolved.stream()
                .map(rc -> new ResolvedCitationDto(
                        rc.startIndex(),
                        rc.endIndex(),
                        rc.segments().stream().map(s -> new CitationDto(s.index(), s.speakerName(), s.text())).toList()
                ))
                .toList();

        return new AttributionResolutionDto(dialogId, turnIndex, dialog.meetingId(), citationDtos);
    }

    private Dialog findDialogOrThrow(String id) {
        return dialogRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.dialog(id));
    }
}
