package com.meetingiq.platform.domain.enums;

/**
 * The subject-matter domain of a meeting, one-to-one with {@link Corpus}
 * (AMI -&gt; PRODUCT, ICSI -&gt; ACADEMIC, PARLIAMENT -&gt; PARLIAMENTARY). Kept as
 * a separate field (denormalised alongside corpus) because it is the more
 * human-readable filter/label in the UI.
 */
public enum MeetingDomain {
    PRODUCT,
    ACADEMIC,
    PARLIAMENTARY
}
