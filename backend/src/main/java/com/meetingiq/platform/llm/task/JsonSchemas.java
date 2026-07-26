package com.meetingiq.platform.llm.task;

import com.meetingiq.platform.llm.spi.JsonResponseSchema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-authored JSON Schemas for this app's two structured-JSON task types
 * ({@link GenerationQuery}, {@link GroundedAnswerQuery}), for providers that support
 * OpenAI-style Structured Outputs. Loose JSON-mode (valid syntax, but no guarantee every field
 * is present) turned out not to be enough in practice: asked only in prose to "always include
 * every field," a real model will sometimes fold a task's structured content into free-form
 * text and leave the actual structured field null or absent instead. A strict schema makes the
 * shape a contract the API enforces server-side, not a request the model can partially ignore.
 */
final class JsonSchemas {

    private JsonSchemas() {
    }

    static final JsonResponseSchema GENERATION_RESULT = new JsonResponseSchema("generation_result", object(
            List.of("text", "structured"),
            Map.of(
                    "text", string(),
                    "structured", object(
                            List.of("overview", "keyPoints", "decisions", "actionItems", "topics", "participants"),
                            Map.of(
                                    "overview", string(),
                                    "keyPoints", stringArray(),
                                    "decisions", stringArray(),
                                    "actionItems", stringArray(),
                                    "topics", stringArray(),
                                    "participants", stringArray()
                            )
                    )
            )
    ));

    static final JsonResponseSchema GROUNDED_ANSWER = new JsonResponseSchema("grounded_answer", object(
            List.of("answer", "unanswerable", "citedSegmentIndices"),
            Map.of(
                    "answer", string(),
                    "unanswerable", Map.of("type", "boolean"),
                    "citedSegmentIndices", Map.of("type", "array", "items", Map.of("type", "integer"))
            )
    ));

    private static Map<String, Object> object(List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> string() {
        return Map.of("type", "string");
    }

    private static Map<String, Object> stringArray() {
        return Map.of("type", "array", "items", Map.of("type", "string"));
    }
}
