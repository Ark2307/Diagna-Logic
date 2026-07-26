package com.meetingiq.platform.llm.spi;

/** Thrown when a resolved provider reports {@link LlmProvider#isAvailable()} false (e.g. no API key configured). Maps to HTTP 503. */
public class ProviderUnavailableException extends LlmException {
    public ProviderUnavailableException(String message) {
        super(message);
    }
}
