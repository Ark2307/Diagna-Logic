package com.diagna.logic.domain;

import com.diagna.logic.domain.enums.Corpus;
import com.diagna.logic.domain.enums.DatasetSplit;
import com.diagna.logic.domain.enums.MeetingDomain;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * A MISeD information-seeking dialog: up to ten query/response turns about
 * one {@link Meeting}. The {@code _id} is the original MISeD
 * {@code dialogId} (a hex string, already unique).
 *
 * <p>{@code corpus}/{@code domain} are denormalised from the referenced
 * meeting at ingest time so dialog list filters (e.g. "AMI dialogs only")
 * never need a join back to {@code meetings}.
 */
@Document(collection = "dialogs")
public record Dialog(
        @Id String id,
        String meetingId,
        DatasetSplit split,
        Corpus corpus,
        MeetingDomain domain,
        int turnCount,
        List<DialogTurn> turns,
        DialogStats stats,
        /** ISO-8601 UTC timestamp string set by the ETL; informational only. */
        String ingestedAt
) {
}
