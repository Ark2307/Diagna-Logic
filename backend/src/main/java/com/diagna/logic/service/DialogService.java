package com.diagna.logic.service;

import com.diagna.logic.api.dto.AttributionResolutionDto;
import com.diagna.logic.api.dto.CitationDto;
import com.diagna.logic.api.dto.DialogDto;
import com.diagna.logic.api.dto.ResolvedCitationDto;
import com.diagna.logic.api.exception.BadRequestException;
import com.diagna.logic.api.exception.ResourceNotFoundException;
import com.diagna.logic.api.mapper.DialogMapper;
import com.diagna.logic.domain.Dialog;
import com.diagna.logic.domain.DialogTurn;
import com.diagna.logic.domain.Meeting;
import com.diagna.logic.repository.DialogRepository;
import com.diagna.logic.repository.DialogSearchCriteria;
import com.diagna.logic.repository.MeetingRepository;
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
