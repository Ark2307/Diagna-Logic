package com.diagna.logic.api.mapper;

import com.diagna.logic.api.dto.AttributionRangeDto;
import com.diagna.logic.api.dto.CitationDto;
import com.diagna.logic.api.dto.DialogDto;
import com.diagna.logic.api.dto.DialogStatsDto;
import com.diagna.logic.api.dto.DialogTurnDto;
import com.diagna.logic.domain.Dialog;
import com.diagna.logic.domain.DialogStats;
import com.diagna.logic.domain.DialogTurn;
import com.diagna.logic.domain.TranscriptSegment;
import com.diagna.logic.service.AttributionResolver;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps {@link Dialog} to its API response shape. Attribution resolution is
 * opt-in: pass {@code meetingSegments == null} for a plain read (list views,
 * or a detail read without {@code ?resolveAttributions=true}), or the
 * meeting's transcript segments to have every turn's {@code resolvedCitations}
 * populated.
 */
@Component
public class DialogMapper {

    private final AttributionResolver attributionResolver;

    public DialogMapper(AttributionResolver attributionResolver) {
        this.attributionResolver = attributionResolver;
    }

    public DialogDto toDto(Dialog dialog) {
        return toDto(dialog, null, 0);
    }

    public DialogDto toDto(Dialog dialog, List<TranscriptSegment> meetingSegments, int meetingSegmentCount) {
        List<DialogTurnDto> turns = dialog.turns().stream()
                .map(turn -> toTurnDto(turn, meetingSegments, meetingSegmentCount))
                .toList();
        return new DialogDto(
                dialog.id(),
                dialog.meetingId(),
                dialog.split().name(),
                dialog.corpus().name(),
                dialog.domain().name(),
                dialog.turnCount(),
                turns,
                toStatsDto(dialog.stats()),
                dialog.ingestedAt()
        );
    }

    private DialogTurnDto toTurnDto(DialogTurn turn, List<TranscriptSegment> meetingSegments, int meetingSegmentCount) {
        List<CitationDto> resolvedCitations = null;
        if (meetingSegments != null) {
            resolvedCitations = attributionResolver
                    .resolveFlat(turn.attributionRanges(), meetingSegments, meetingSegmentCount)
                    .stream()
                    .map(c -> new CitationDto(c.segmentIndex(), c.speakerName(), c.text()))
                    .toList();
        }
        List<AttributionRangeDto> ranges = turn.attributionRanges().stream()
                .map(r -> new AttributionRangeDto(r.startIndex(), r.endIndex()))
                .toList();
        return new DialogTurnDto(
                turn.turnIndex(),
                turn.query(),
                turn.response(),
                turn.queryType().name(),
                turn.unanswerable(),
                turn.contextDependent(),
                ranges,
                turn.attributedSegmentCount(),
                resolvedCitations
        );
    }

    private DialogStatsDto toStatsDto(DialogStats stats) {
        return new DialogStatsDto(stats.unanswerableCount(), stats.attributedTurnCount(), stats.queryTypeCounts());
    }
}
