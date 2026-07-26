package com.meetingiq.platform.llm.spi;

/** Thrown by a {@link ResponseParser} when a provider's raw text can't be parsed into the query's expected payload type. */
public class LlmParseException extends LlmException {
    public LlmParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmParseException(String message) {
        super(message);
    }
}
