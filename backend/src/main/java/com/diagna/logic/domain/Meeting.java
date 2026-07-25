package com.diagna.logic.domain;

import com.diagna.logic.domain.enums.Corpus;
import com.diagna.logic.domain.enums.DatasetSplit;
import com.diagna.logic.domain.enums.MeetingDomain;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * A meeting transcript from the MISeD dataset, stored as a single document
 * with its transcript segments embedded (see {@code CLAUDE.md} for why:
 * attribution indices are array positions, so the meeting is the natural
 * unit of storage and a citation resolves via a native {@code $slice}
 * projection rather than a join).
 *
 * <p>The {@code _id} is the original MISeD {@code meetingId} (e.g.
 * {@code "Bmr019"}) — stable, human-readable, and already unique across the
 * dataset, so no surrogate key is introduced.
 *
 * <p>Produced by {@code tools/ingest/ingest_mised.py}; 207 of the 225 real
 * documents are the result of merging two dialogs' worth of metadata onto
 * one shared transcript ({@code dialogCount > 1}) — see
 * {@code mised_transform.merge_meeting}.
 */
@Document(collection = "meetings")
public record Meeting(
        @Id String id,
        Corpus corpus,
        MeetingDomain domain,
        DatasetSplit split,
        int segmentCount,
        int charCount,
        /** Rough estimate (~4 chars/token), used only for context-budget planning. */
        int estimatedTokens,
        int speakerCount,
        /** Number of MISeD dialogs whose transcript is this meeting (1 or 2 in practice). */
        int dialogCount,
        List<SpeakerStat> speakers,
        /**
         * The full transcript, in order. May be absent (null) when this document
         * was read via a projection that excludes it — see
         * {@code MeetingQueryRepository.findSummaryById} vs {@code findFullById}.
         */
        List<TranscriptSegment> transcriptSegments,
        String sourceFile,
        /** ISO-8601 UTC timestamp string set by the ETL; informational only, not queried on. */
        String ingestedAt
) {
}
