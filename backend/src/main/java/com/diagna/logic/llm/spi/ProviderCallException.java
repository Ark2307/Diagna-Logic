package com.diagna.logic.llm.spi;

/** Thrown when a provider's SDK call itself fails (network error, API error, rate limit). Maps to HTTP 502. */
public class ProviderCallException extends LlmException {
    public ProviderCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
