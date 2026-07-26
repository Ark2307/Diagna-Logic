package com.meetingiq.platform.llm.spi;

import com.meetingiq.platform.domain.TokenUsage;

/**
 * The neutral, vendor-independent shape of one chat-completion call —
 * exactly what a concrete provider's {@code doComplete} produces, before
 * {@link LlmQuery#parser()} turns {@link #text()} into the task's actual
 * payload type. Every vendor SDK's own response shape is mapped into this
 * one record, and nothing above this line in the call stack ever sees a
 * vendor type again.
 */
public record LlmCompletion(
        String text,
        String model,
        TokenUsage usage,
        FinishReason finishReason
) {
}
