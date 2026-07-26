package com.meetingiq.platform.llm.spi;

/** Base type for every failure in the LLM layer. */
public class LlmException extends RuntimeException {
    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
