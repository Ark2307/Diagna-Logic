package com.diagna.logic.domain.enums;

/**
 * The gold query classification carried by every MISeD dialog turn, stripped
 * of its source {@code QUERY_TYPE_} protobuf-enum prefix by the ETL
 * (see {@code mised_transform.normalise_query_type}).
 *
 * <p>Verified distribution across the full dataset: SPECIFIC 2,179 · YES_NO
 * 1,112 · GENERAL 870.
 */
public enum QueryType {
    /** A question with a narrow, fact-seeking answer. */
    SPECIFIC,
    /** A question answerable yes/no (or "unclear"/"partially"). */
    YES_NO,
    /** A broad question about the meeting as a whole, e.g. "what was this about?" */
    GENERAL
}
