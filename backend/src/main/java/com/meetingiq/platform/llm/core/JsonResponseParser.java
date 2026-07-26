package com.meetingiq.platform.llm.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingiq.platform.llm.spi.LlmParseException;
import com.meetingiq.platform.llm.spi.ResponseParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tolerant JSON {@link ResponseParser}: models rarely return perfectly
 * bare JSON even when asked to, so this handles the three shapes actually
 * seen in practice — a fenced ```json code block, prose wrapped around a
 * JSON object/array ("Here's the summary: {...}"), and genuinely bare JSON —
 * before handing the extracted text to Jackson. Every JSON-producing task
 * query in {@code com.meetingiq.platform.llm.task} uses one of these as its parser.
 */
public class JsonResponseParser<T> implements ResponseParser<T> {

    private static final Pattern FENCED_BLOCK = Pattern.compile("```(?:json)?\\s*([\\[{].*[\\]}])\\s*```", Pattern.DOTALL);
    private static final int MAX_ERROR_EXCERPT = 500;

    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public JsonResponseParser(ObjectMapper objectMapper, Class<T> type) {
        this.objectMapper = objectMapper;
        this.type = type;
    }

    @Override
    public T parse(String rawText) {
        String json = extractJson(rawText);
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new LlmParseException(
                    "Could not parse a " + type.getSimpleName() + " from the model's JSON response: " + e.getMessage(), e);
        }
    }

    /** Package-visible for unit testing the extraction heuristic independent of Jackson deserialization. */
    static String extractJson(String rawText) {
        if (rawText == null) {
            throw new LlmParseException("Response was empty; expected JSON");
        }
        String trimmed = rawText.trim();

        Matcher fenced = FENCED_BLOCK.matcher(trimmed);
        if (fenced.find()) {
            return fenced.group(1).trim();
        }

        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }

        int start = firstJsonStart(trimmed);
        if (start >= 0) {
            char opener = trimmed.charAt(start);
            char closer = opener == '{' ? '}' : ']';
            int end = trimmed.lastIndexOf(closer);
            if (end > start) {
                return trimmed.substring(start, end + 1);
            }
        }

        throw new LlmParseException("No JSON object or array found in response: " + excerpt(rawText));
    }

    private static int firstJsonStart(String text) {
        int brace = text.indexOf('{');
        int bracket = text.indexOf('[');
        if (brace < 0) {
            return bracket;
        }
        if (bracket < 0) {
            return brace;
        }
        return Math.min(brace, bracket);
    }

    private static String excerpt(String text) {
        return text.length() <= MAX_ERROR_EXCERPT ? text : text.substring(0, MAX_ERROR_EXCERPT) + "...";
    }
}
