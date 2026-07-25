package com.diagna.logic.domain.enums;

/**
 * The three source corpora that make up the MISeD dataset (derived from
 * QMSum). Classification is by meeting id prefix — see the Python ETL's
 * {@code mised_transform.classify()}, whose bucketing this mirrors exactly
 * so the two sides never disagree about a meeting's corpus.
 */
public enum Corpus {
    /** AMI meetings (Augmented Multi-party Interaction corpus). Ids prefixed ES/IS/TS. */
    AMI,
    /** ICSI meetings (International Computer Science Institute). Ids prefixed B*. */
    ICSI,
    /** Parliamentary committee meetings (Canada/Wales). Ids prefixed {@code covid} or {@code education}. */
    PARLIAMENT
}
