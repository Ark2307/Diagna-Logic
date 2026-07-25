package com.diagna.logic.llm.spi;

/** Thrown when a request names a {@code provider} id that isn't registered. Maps to HTTP 400. */
public class UnknownProviderException extends LlmException {
    public UnknownProviderException(String message) {
        super(message);
    }
}
