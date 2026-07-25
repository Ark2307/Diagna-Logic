package com.diagna.logic.llm.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A dedicated {@link ObjectMapper} for parsing LLM JSON output, deliberately
 * separate from Spring MVC's own request/response mapper (kept exactly as
 * Boot auto-configures it) and deliberately lenient about unknown
 * properties — real models (and {@code MockLlmProvider}, which emits one
 * envelope shared across several task shapes) routinely include fields a
 * given task's parser doesn't read, and that should never be a hard error.
 */
public final class LlmJsonMapper {

    private LlmJsonMapper() {
    }

    public static ObjectMapper create() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
