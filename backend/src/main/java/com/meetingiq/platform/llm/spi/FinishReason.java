package com.meetingiq.platform.llm.spi;

/** Normalised across providers — every {@code doComplete} implementation maps its SDK's own finish/stop reason into one of these. */
public enum FinishReason {
    STOP,
    LENGTH,
    CONTENT_FILTER,
    ERROR,
    UNKNOWN
}
