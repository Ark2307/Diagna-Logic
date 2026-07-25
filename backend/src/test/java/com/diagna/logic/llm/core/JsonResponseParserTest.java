package com.diagna.logic.llm.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.diagna.logic.llm.spi.LlmParseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonResponseParserTest {

    record Payload(String answer, boolean unanswerable) {
    }

    private final JsonResponseParser<Payload> parser = new JsonResponseParser<>(new ObjectMapper(), Payload.class);

    @Test
    void parsesBareJson() {
        Payload result = parser.parse("{\"answer\":\"yes\",\"unanswerable\":false}");
        assertThat(result).isEqualTo(new Payload("yes", false));
    }

    @Test
    void parsesFencedJsonBlockWithLanguageTag() {
        String raw = "Here is my answer:\n```json\n{\"answer\":\"yes\",\"unanswerable\":false}\n```\nHope that helps.";
        assertThat(parser.parse(raw)).isEqualTo(new Payload("yes", false));
    }

    @Test
    void parsesFencedBlockWithoutLanguageTag() {
        String raw = "```\n{\"answer\":\"no\",\"unanswerable\":true}\n```";
        assertThat(parser.parse(raw)).isEqualTo(new Payload("no", true));
    }

    @Test
    void parsesProseWrappedJson() {
        String raw = "Sure, here's the structured result: {\"answer\":\"maybe\",\"unanswerable\":false} — let me know if you need anything else.";
        assertThat(parser.parse(raw)).isEqualTo(new Payload("maybe", false));
    }

    @Test
    void handlesNestedBracesInProseWrappedJson() {
        // The naive "first { to first }" approach would truncate this at the wrong brace;
        // extraction must span to the LAST closing brace in the text.
        String raw = "Result: {\"outer\": \"has {braces} inside\"} done.";
        assertThat(JsonResponseParser.extractJson(raw)).isEqualTo("{\"outer\": \"has {braces} inside\"}");
    }

    @Test
    void throwsParseExceptionWhenNoJsonPresent() {
        assertThatThrownBy(() -> parser.parse("I cannot help with that."))
                .isInstanceOf(LlmParseException.class);
    }

    @Test
    void throwsParseExceptionOnNullInput() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(LlmParseException.class);
    }

    @Test
    void throwsParseExceptionOnMalformedJson() {
        assertThatThrownBy(() -> parser.parse("{\"answer\": \"unterminated"))
                .isInstanceOf(LlmParseException.class);
    }
}
